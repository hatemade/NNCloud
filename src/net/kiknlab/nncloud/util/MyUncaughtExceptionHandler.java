package net.kiknlab.nncloud.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

public final class MyUncaughtExceptionHandler implements UncaughtExceptionHandler {

	/** バグレポートのメール送信先. */
	// TODO 適切なメールアドレスに変換せよ.
	private static final String MAILTO_ADDRESS = "nari.okubo@gmail.com";

	/** 保存するlogcatの行数. あまり多くすると文字数制限とかに引っかかる恐れがあるし, 必要ないので. */
	private static final int LOGCAT_LINES = 300;

	/** パッケージ情報. */
	private final PackageInfo mPackageInfo;

	/** キャッチされなかったExceptionのハンドラ. */
	private final UncaughtExceptionHandler mHandler;

	/** バグレポートのファイル. */
	private final File mFile;

	/**
	 * コンストラクタ. 与えられたコンテキストからパッケージ情報などを保持しておく.
	 *
	 * @param context
	 */
	public MyUncaughtExceptionHandler(final Context context) {
		try {
			mPackageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			throw new RuntimeException(e);
		}
		mHandler = Thread.getDefaultUncaughtExceptionHandler();
		mFile = getFile(context);
	}

	/**
	 * 実際にExceptionが発生した時に走る処理. ファイルに端末情報, スタックトレース, logcatを保存する.
	 */
	@Override
	public void uncaughtException(final Thread thread, Throwable throwable) {
		// SDカードがマウントされていなかったら何もしない.
		if (mFile == null) {
			mHandler.uncaughtException(thread, throwable);
			return;
		}

		final PrintWriter writer;
		try {
			writer = new PrintWriter(new FileOutputStream(mFile));
		} catch (FileNotFoundException e) {
			// 何故か端末内のファイルが開けない. 仕方ない. 処理を行わない.
			e.printStackTrace();
			mHandler.uncaughtException(thread, throwable);
			return;
		}

		// 使用端末, アプリのバージョンの内容をファイルに書きこむ.
		writer.print("端末: ");
		writer.print(Build.MODEL);
		writer.print("(");
		writer.print(Build.DEVICE);
		writer.print(")");
		writer.print("   Android ");
		writer.print(Build.VERSION.RELEASE);
		writer.print("(");
		writer.print(Build.VERSION.SDK);
		writer.println(")");
		writer.print(mPackageInfo.packageName);
		writer.print("   ver.");
		writer.print(mPackageInfo.versionName);
		writer.print("(");
		writer.print(mPackageInfo.versionCode);
		writer.println(")  ");
		writer.println();

		// Exceptionの情報をファイルに書きこむ.
		throwable.printStackTrace(writer);

		// logcatコマンドを実行. リストに突っ込む.
		final List<String> logList = new ArrayList<String>();
		Process process = null;
		try {
			process = Runtime.getRuntime().exec("logcat -d -v time");
			final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 1024);
			String line;
			while ((line = reader.readLine()) != null) {
				logList.add(line);
			}
		} catch (IOException e) {
			// 何故かlogcatコマンドが実行できない. 仕方ない. 処理を行わない.
			e.printStackTrace();
			mHandler.uncaughtException(thread, throwable);
			return;
		} finally {
			if (process != null) {
				process.destroy();
			}
		}

		// リストを元に最新のログを書き込む.
		writer.println();
		for (int i = logList.size() - 1, size = logList.size() - LOGCAT_LINES; i >= size; i--) {
			writer.print("(");
			writer.print(logList.size() - i);
			writer.print(") ");
			writer.println(logList.get(i));
		}
		writer.close();
		mHandler.uncaughtException(thread, throwable);
	}

	/**
	 * Gmailを使用してバグレポートを添付ファイルとして添付してメール送信を行う為のダイアログを表示する.
	 * <p>
	 * このメソッドを実行時にファイルが無かったらダイアログを表示しない.
	 * ファイルがあったら, 送信用のファイルとしてリネームして待機.
	 * （つまり同じバグレポートファイルに対するダイアログは2度と表示されない）
	 * </p>
	 *
	 * @param context Activityクラスのサブクラスのインスタンス（ダイアログを出すために必要）
	 */
	public static <A extends Activity> void sendWithGmail(final A context) {
		// バグレポートファイルが存在しなかったら何もしない.
		final File file = getFile(context);
		if (file == null || !file.exists()) {
			return;
		}

		// バグレポート用にファイルを移動する.
		final File sendFile = new File("/sdcard/NNCloud/send_bug.txt");
		//		Environment.getExternalStorageDirectory().getPath() +
		//		File.separator + context.getPackageName() +
		//		File.separator + "send_bug.txt");
		file.renameTo(sendFile);

		// ダイアログでバグレポートを送信するか否かを聞く.
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(false);
		builder.setIcon(android.R.drawable.ic_dialog_email);
		builder.setTitle("深刻なエラーから復帰しました");
		builder.setMessage("ご迷惑をお掛けして申し訳ございません。開発者に対し"
				+ "バグレポート機能を使用して現象を送信することができます。送信しますか？");
		builder.setPositiveButton("送信する", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// バグレポートのコメント欄を生成.
				final StringBuilder sb = new StringBuilder();
				sb.append("どのような操作をしたらアプリが落ちたかなどの状況説明を以下にお書きください。\n");
				sb.append("(添付ファイルはログファイルなので削除しないでください)\n");
				sb.append("------------------------------\n");

				// Gmailに限定しメールを送る.
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
				intent.setData(Uri.parse("mailto:" + MAILTO_ADDRESS));
				intent.putExtra(Intent.EXTRA_SUBJECT, "バグレポート(" + context.getPackageName() + ")");
				intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
				intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(sendFile));
				context.startActivity(intent);
			}
		});
		builder.setNegativeButton("送信しない", null);
		builder.create().show();
	}

	/**
	 * バグレポートを保存するファイルオブジェクトを取得する.
	 * アプリ内部領域に置いてもいいのだが, それだとGmail側からの参照に失敗する為
	 * （Gmail側からアプリ内領域にアクセスする権限が無いと思われる）, 仕方なくSDカード内の領域を使用している.
	 *
	 * @param context コンテキスト
	 * @return バグレポートを保存するファイル
	 */
	private static File getFile(final Context context) {
		// SDカードがマウントされているかのチェックを行う.
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			return null;
		}

		// ディレクトリ存在チェックと, 存在しなかったら再帰的にディレクトリ生成.
		File directory = new File(context.getExternalFilesDir(null).getParent() + File.separator + "bugs");
		if (!directory.exists()) {
			directory.mkdirs();
		}
		return new File(directory.getPath() + File.separator + "bug.txt");
	}
}