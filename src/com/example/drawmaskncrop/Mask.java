package com.example.drawmaskncrop;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class Mask extends View implements OnTouchListener{
	
	//States. This indicates the state on wich onDraw() executes.
	public enum States{
		DRAW,
		ERASE,
		CLEAR,
		UNDO,
		FINALIZE
	}
	
	public enum PathsColors{
		BLACK,
		WHITE
	}
	
	private States currentState;
	private States previousState;
	
	public States states;
	//Paint is the brush
	//blackPaint is for painting and whitePaint is for erasing
	public Paint blackPaint, whitePaint;
	
	//Paths is the stroke.
	//Array of paths allows Undo
	private ArrayList<Path> paths;

	//Associates a color (white, black) to a path. Indicates which color is the respective path
	private ArrayList<PathsColors> pathsColors;
	//Width of the stroke
	private final float STROKEWIDTH = 30.0f;
	//Current path
	private int currentPath;
	
	public Mask(Context context, AttributeSet attrs) {
		super(context, attrs);
		setFocusable(true);
		setFocusableInTouchMode(true);
		setOnTouchListener(this);
		currentPath = -1;
		paths = new ArrayList<Path>();
		pathsColors = new ArrayList<PathsColors>();
		blackPaint = new Paint();
		whitePaint = new Paint();
		blackPaint.setAntiAlias(true);
		whitePaint.setAntiAlias(true);
		blackPaint.setColor(Color.BLACK);
		whitePaint.setColor(Color.WHITE);
		if(Build.VERSION.SDK_INT >= 11){
			blackPaint.setAlpha(255);
			whitePaint.setAlpha(255);
		}else{
			blackPaint.setAlpha(60);
			whitePaint.setAlpha(60);
		}
		blackPaint.setStrokeWidth(this.STROKEWIDTH);
		whitePaint.setStrokeWidth(this.STROKEWIDTH);
		blackPaint.setStyle(Paint.Style.STROKE);
		whitePaint.setStyle(Paint.Style.STROKE);
		blackPaint.setStrokeJoin(Paint.Join.ROUND);
		whitePaint.setStrokeJoin(Paint.Join.ROUND);
	}
	
	protected void onDraw(Canvas canvas){
		
		switch(currentState){
			case CLEAR://Clear all the mask
				for(int i = 0; i < paths.size(); i++){
					paths.get(i).reset();
				}
				currentPath = - 1;
				currentState = states.DRAW;
				//this.invalidate();
				break;
			case UNDO://Undo. Last stroke is erased
				if(paths.size() > 0 && currentPath > -1){
					//paths.remove(paths.size() - 1);
					paths.get(currentPath).reset();
					currentPath--;
				}
				currentState = previousState;
				this.invalidate();
				break;
			case FINALIZE://Turn the view's background transparent
				Log.i("FINALIZE", "is in finalize!!!");
				canvas.drawARGB(255, 255, 255, 255);
				break;
			default:
				break;
		}
		
		if(currentState == States.DRAW || currentState == States.ERASE || currentState == States.FINALIZE){
			//Make the view's background translucent white
			if(Build.VERSION.SDK_INT >= 11)
				//Works on Android 2.3.* to 4.*
				canvas.drawARGB(255, 255, 255, 255);
			else
				//Woks on Android 2.2.*
				canvas.drawARGB(80, 255, 255, 255);
			//Is drawing or erasing the mask
			for(int i = 0; i < paths.size(); i++){
				if(pathsColors.get(i) == PathsColors.BLACK)
					canvas.drawPath(paths.get(i), blackPaint);
				else
					canvas.drawPath(paths.get(i), whitePaint);
			}
		}
	}
	
	public boolean onTouch(View view, MotionEvent event){
		
		float eventX = event.getX();
		float eventY = event.getY();
		switch(event.getAction()){
			case android.view.MotionEvent.ACTION_DOWN:
				currentPath++;
				if(currentPath >= paths.size()){
					paths.add(new Path());
					if(currentState == States.ERASE)
						pathsColors.add(PathsColors.WHITE);
					else
						pathsColors.add(PathsColors.BLACK);
				}
				paths.get(currentPath).moveTo(eventX, eventY);
				if(currentState == States.ERASE)
					pathsColors.set(currentPath, PathsColors.WHITE);
				else
					pathsColors.set(currentPath, PathsColors.BLACK);
				return true;
			case android.view.MotionEvent.ACTION_MOVE:
				paths.get(currentPath).lineTo(eventX, eventY);
				break;
			case android.view.MotionEvent.ACTION_UP:
				break;
			default:
				//nothing to do
				return false;
		}
		//Schedules a repaint
		this.invalidate();
		return true;
	}
	
	public void setState(States state){
		this.previousState = this.currentState;
		this.currentState = state;
	}
}
