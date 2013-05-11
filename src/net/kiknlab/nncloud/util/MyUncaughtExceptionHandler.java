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

	/** �o�O���|�[�g�̃��[�����M��. */
	// TODO �K�؂ȃ��[���A�h���X�ɕϊ�����.
	private static final String MAILTO_ADDRESS = "nari.okubo@gmail.com";

	/** �ۑ�����logcat�̍s��. ���܂葽������ƕ����������Ƃ��Ɉ��������鋰�ꂪ���邵, �K�v�Ȃ��̂�. */
	private static final int LOGCAT_LINES = 300;

	/** �p�b�P�[�W���. */
	private final PackageInfo mPackageInfo;

	/** �L���b�`����Ȃ�����Exception�̃n���h��. */
	private final UncaughtExceptionHandler mHandler;

	/** �o�O���|�[�g�̃t�@�C��. */
	private final File mFile;

	/**
	 * �R���X�g���N�^. �^����ꂽ�R���e�L�X�g����p�b�P�[�W���Ȃǂ�ێ����Ă���.
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
	 * ���ۂ�Exception�������������ɑ��鏈��. �t�@�C���ɒ[�����, �X�^�b�N�g���[�X, logcat��ۑ�����.
	 */
	@Override
	public void uncaughtException(final Thread thread, Throwable throwable) {
		// SD�J�[�h���}�E���g����Ă��Ȃ������牽�����Ȃ�.
		if (mFile == null) {
			mHandler.uncaughtException(thread, throwable);
			return;
		}

		final PrintWriter writer;
		try {
			writer = new PrintWriter(new FileOutputStream(mFile));
		} catch (FileNotFoundException e) {
			// ���̂��[�����̃t�@�C�����J���Ȃ�. �d���Ȃ�. �������s��Ȃ�.
			e.printStackTrace();
			mHandler.uncaughtException(thread, throwable);
			return;
		}

		// �g�p�[��, �A�v���̃o�[�W�����̓��e���t�@�C���ɏ�������.
		writer.print("�[��: ");
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

		// Exception�̏����t�@�C���ɏ�������.
		throwable.printStackTrace(writer);

		// logcat�R�}���h�����s. ���X�g�ɓ˂�����.
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
			// ���̂�logcat�R�}���h�����s�ł��Ȃ�. �d���Ȃ�. �������s��Ȃ�.
			e.printStackTrace();
			mHandler.uncaughtException(thread, throwable);
			return;
		} finally {
			if (process != null) {
				process.destroy();
			}
		}

		// ���X�g�����ɍŐV�̃��O����������.
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
	 * Gmail���g�p���ăo�O���|�[�g��Y�t�t�@�C���Ƃ��ēY�t���ă��[�����M���s���ׂ̃_�C�A���O��\������.
	 * <p>
	 * ���̃��\�b�h�����s���Ƀt�@�C��������������_�C�A���O��\�����Ȃ�.
	 * �t�@�C������������, ���M�p�̃t�@�C���Ƃ��ă��l�[�����đҋ@.
	 * �i�܂蓯���o�O���|�[�g�t�@�C���ɑ΂���_�C�A���O��2�x�ƕ\������Ȃ��j
	 * </p>
	 *
	 * @param context Activity�N���X�̃T�u�N���X�̃C���X�^���X�i�_�C�A���O���o�����߂ɕK�v�j
	 */
	public static <A extends Activity> void sendWithGmail(final A context) {
		// �o�O���|�[�g�t�@�C�������݂��Ȃ������牽�����Ȃ�.
		final File file = getFile(context);
		if (file == null || !file.exists()) {
			return;
		}

		// �o�O���|�[�g�p�Ƀt�@�C�����ړ�����.
		final File sendFile = new File("/sdcard/NNCloud/send_bug.txt");
		//		Environment.getExternalStorageDirectory().getPath() +
		//		File.separator + context.getPackageName() +
		//		File.separator + "send_bug.txt");
		file.renameTo(sendFile);

		// �_�C�A���O�Ńo�O���|�[�g�𑗐M���邩�ۂ��𕷂�.
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(false);
		builder.setIcon(android.R.drawable.ic_dialog_email);
		builder.setTitle("�[���ȃG���[���畜�A���܂���");
		builder.setMessage("�����f�����|�����Đ\���󂲂����܂���B�J���҂ɑ΂�"
				+ "�o�O���|�[�g�@�\���g�p���Č��ۂ𑗐M���邱�Ƃ��ł��܂��B���M���܂����H");
		builder.setPositiveButton("���M����", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// �o�O���|�[�g�̃R�����g���𐶐�.
				final StringBuilder sb = new StringBuilder();
				sb.append("�ǂ̂悤�ȑ����������A�v�������������Ȃǂ̏󋵐������ȉ��ɂ��������������B\n");
				sb.append("(�Y�t�t�@�C���̓��O�t�@�C���Ȃ̂ō폜���Ȃ��ł�������)\n");
				sb.append("------------------------------\n");

				// Gmail�Ɍ��肵���[���𑗂�.
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
				intent.setData(Uri.parse("mailto:" + MAILTO_ADDRESS));
				intent.putExtra(Intent.EXTRA_SUBJECT, "�o�O���|�[�g(" + context.getPackageName() + ")");
				intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
				intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(sendFile));
				context.startActivity(intent);
			}
		});
		builder.setNegativeButton("���M���Ȃ�", null);
		builder.create().show();
	}

	/**
	 * �o�O���|�[�g��ۑ�����t�@�C���I�u�W�F�N�g���擾����.
	 * �A�v�������̈�ɒu���Ă������̂���, ���ꂾ��Gmail������̎Q�ƂɎ��s�����
	 * �iGmail������A�v�����̈�ɃA�N�Z�X���錠���������Ǝv����j, �d���Ȃ�SD�J�[�h���̗̈���g�p���Ă���.
	 *
	 * @param context �R���e�L�X�g
	 * @return �o�O���|�[�g��ۑ�����t�@�C��
	 */
	private static File getFile(final Context context) {
		// SD�J�[�h���}�E���g����Ă��邩�̃`�F�b�N���s��.
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			return null;
		}

		// �f�B���N�g�����݃`�F�b�N��, ���݂��Ȃ�������ċA�I�Ƀf�B���N�g������.
		File directory = new File(context.getExternalFilesDir(null).getParent() + File.separator + "bugs");
		if (!directory.exists()) {
			directory.mkdirs();
		}
		return new File(directory.getPath() + File.separator + "bug.txt");
	}
}