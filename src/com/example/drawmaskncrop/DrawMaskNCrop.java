package com.example.drawmaskncrop;

import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class DrawMaskNCrop extends Activity {
	
	private Mask maskView;
	private Bitmap imageBitmap;
	private ImageView matteView, backgroundImageView;
	private LayoutInflater layoutInflater;
	private LayoutParams layoutParamsControl, layoutParamsMask;
	private View controlsView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		maskView = new Mask(this, null);
		if(Build.VERSION.SDK_INT >= 11)
			maskView.setAlpha(0.4f);
		
		setContentView(R.layout.activity_draw_mask_ncrop);
		
		layoutInflater = LayoutInflater.from(getBaseContext());
		layoutParamsControl = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParamsMask = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParamsMask.addRule(RelativeLayout.ABOVE, R.id.control_panel);
		controlsView = layoutInflater.inflate(R.layout.controls, null);
		this.addContentView(maskView, layoutParamsMask);
		this.addContentView(controlsView, layoutParamsControl);
		
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		int height = displayMetrics.heightPixels;
		int width = displayMetrics.widthPixels;
		
		//imageBitmap is the image to be cropped
		imageBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.antigua_guatemala), width, height, false);
		backgroundImageView = (ImageView)findViewById(R.id.img_background);
		backgroundImageView.setImageBitmap(imageBitmap);
		
		LinearLayout controlsLayout = (LinearLayout)findViewById(R.id.control_panel);
		controlsLayout.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
			}
		});
		
		maskView.setState(maskView.states.DRAW);
		matteView = (ImageView)findViewById(R.id.img_matte);
		matteView.setVisibility(View.INVISIBLE);
		
		Button btnDraw = (Button)findViewById(R.id.btn_draw);
		btnDraw.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				maskView.setState(maskView.states.DRAW);
			}
		});
		
		Button btnErase = (Button)findViewById(R.id.btn_erase);
		btnErase.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				maskView.setState(maskView.states.ERASE);
			}
		});
		
		Button btnClear = (Button)findViewById(R.id.btn_clear);
		btnClear.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				maskView.setState(maskView.states.CLEAR);
				maskView.invalidate();
			}
		});
		
		Button btnUndo = (Button)findViewById(R.id.btn_undo);
		btnUndo.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				maskView.setState(maskView.states.UNDO);
				maskView.invalidate();
			}
		});
		
		Button btnApply = (Button)findViewById(R.id.btn_apply);
		btnApply.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				Bitmap maskBitmap = Bitmap.createBitmap(maskView.getWidth(), maskView.getHeight(), Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(maskBitmap);
				maskView.blackPaint.setColor(Color.BLACK);
				maskView.blackPaint.setAlpha(255);
				maskView.whitePaint.setColor(Color.WHITE);
				maskView.whitePaint.setAlpha(255);
				maskView.setState(maskView.states.FINALIZE);
				maskView.draw(canvas);
				
				Bitmap maskBitmapTransparent = createTransparentBitmapFromBitmap(maskBitmap, Color.BLACK);
				Bitmap resultBitmap = Bitmap.createBitmap(maskBitmap.getWidth(), maskBitmap.getHeight(), Config.ARGB_8888);
				Canvas mCanvas = new Canvas(resultBitmap);
				Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
				paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
				mCanvas.drawBitmap(imageBitmap, 0, 0, null);
				mCanvas.drawBitmap(maskBitmapTransparent, 0, 0, paint);
				paint.setXfermode(null);
				backgroundImageView.setImageBitmap(resultBitmap);
				
				try{
					File storagePath = new File(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ?
							getExternalCacheDir().getAbsolutePath() :
							getCacheDir().getAbsolutePath());
					String maskName = Long.toString(System.currentTimeMillis()) + ".png";
					File finalImage = new File(storagePath, maskName);
					FileOutputStream out = new FileOutputStream(finalImage);
					resultBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.draw_mask_ncrop, menu);
		return true;
	}
	
	public static Bitmap createTransparentBitmapFromBitmap(Bitmap bitmap, int replaceThisColor){
		if (bitmap != null) {
			int picw = bitmap.getWidth();
			int pich = bitmap.getHeight();
			int[] pix = new int[picw * pich];
			bitmap.getPixels(pix, 0, picw, 0, 0, picw, pich);
			
			for (int y = 0; y < pich; y++) {
				for (int x = 0; x < picw; x++) {
					int index = y * picw + x;
					int r = (pix[index] >> 16) & 0xff;
					int g = (pix[index] >> 8) & 0xff;
					int b = pix[index] & 0xff;
					if (pix[index] != replaceThisColor)
						pix[index] = Color.TRANSPARENT;
				}
  	      	}
			
			Bitmap bm = Bitmap.createBitmap(pix, picw, pich, Bitmap.Config.ARGB_8888);
			
			return bm;
		}
		return null;
	}

}
