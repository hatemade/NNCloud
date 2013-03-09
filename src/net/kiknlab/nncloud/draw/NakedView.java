package net.kiknlab.nncloud.draw;

//import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

//@SuppressLint("ViewConstructor")
public class NakedView extends SurfaceView implements SurfaceHolder.Callback{
	private SurfaceHolder holder;//�T�[�t�F�C�X�z���_�[
	public Thread mThread;//�X���b�h
	private Canvas canvas;

	public NakedView(Context context){
		super(context);//�J�X�^���r���[�ɓ����ƌx������ĂƂ肠������������c�g���\�肪�Ȃ��c
	}
	
	public NakedView(Context context, Thread thread) {
		super(context);

		//�T�[�t�F�C�X�z���_�[�̐���
		holder=getHolder();
		holder.addCallback(this);

		mThread = thread;
	}

	public void onDraw(String text){
		canvas = holder.lockCanvas();
		canvas.save();
		canvas.drawColor(Color.WHITE);
		Paint paint = new Paint();
		paint.setColor(Color.RED);
		paint.setTextSize(32);
		paint.setAntiAlias(true);
		lineText(paint, text);
		//canvas.drawText(text, 0, 32, paint);
		canvas.restore();
		holder.unlockCanvasAndPost(canvas);
	}
	
	public void lineText(Paint paint, String text){
		String[] line = text.split(":");
		for(int i = 1;i <= line.length;i++){
			canvas.drawText(line[i-1], 0, 32*i, paint);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		mThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		mThread=null;
	}
}
