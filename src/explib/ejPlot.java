/* ExpEYES plotting library.
   Library for plotting data from ExpEYES (http://expeyes.in) under Android
   Copyright (C) 2014 Jithin B.P. , IISER Mohali (jithinbp@gmail.com)

	Can be used for plotting float arrays not related to expeyes hardware.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2, or (at your option)
   any later version.
*/

package explib;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

public class ejPlot{
	public View myv;
	private int MAXCHAN=5;
	Paint[] paints = new Paint[MAXCHAN];
	Path[]  paths = new Path[MAXCHAN]; 
	Paint backgroundPaint = new Paint();
	Paint borderPaint = new Paint();
	Paint axesPaint = new Paint();
	Paint gridPaint = new Paint();
	Paint dashPaint = new Paint();

	public  float XMIN = (float) 0.0, XMAX = (float) 100.0, YMIN = (float) -5.0, YMAX = (float) 5.0;// Limits, User program will set this
	public	String xlabel = "milli Seconds";
	public	String ylabel = "Volts";
	private float xscale = (float) 1.0, yscale = (float) 1.0;
	private final int LEFTBORDER = 40;
	private final int RTBORDER = 3;
	private final int BOTBORDER = 30;
	private final int TOPBORDER = 3;
	private final int NGRID = 10;
	private int WIDTH, HEIGHT;
	public CaptureConstants Par;
	public boolean touched=false;
	public ejPlot(Context context,LinearLayout ll) {
		for(int i=0;i<MAXCHAN;i++){
			paints[i]= new Paint();
			paths[i] = new Path();
			}
		myv = new MyView(context);
		ll.addView(myv);
		Par=new CaptureConstants();
		setWorld(XMIN, XMAX, YMIN, YMAX);		
	}
	
	
	public void setWorld(double xmin, double xmax, double ymin, double ymax) {
		XMIN = (float) xmin;
		XMAX = (float) xmax;
		YMIN = (float) ymin;
		YMAX = (float) ymax;	
		xscale = (float) (WIDTH-LEFTBORDER-RTBORDER-4) / (XMAX - XMIN);		
		yscale = (float) (HEIGHT-BOTBORDER-TOPBORDER-4)/ (YMAX - YMIN);
		//Log.e("x y", "W=  " + WIDTH + " H= " +HEIGHT + " xs= " + xscale + "ys= " + yscale); 
	}

	
	public void line(float[] xa,float[] ya, int size, int channel){	
		paths[channel].reset();
		paths[channel].moveTo((xa[0]-XMIN) * xscale + 2 + LEFTBORDER, HEIGHT - (ya[0]-YMIN) * yscale - BOTBORDER-1);
		for (int i = 1; i < size; i++)
			paths[channel].lineTo((xa[i]-XMIN) * xscale + 2+ LEFTBORDER, HEIGHT - (ya[i]-YMIN) * yscale - BOTBORDER-1);	
		}

	
	public void updatePlots(){ 
		myv.postInvalidate();
	}
	
	
	public void clearPlots(){
		for(int i=0;i<MAXCHAN;i++)paths[i].reset();
	}
	public void clearPlot(int i){
		paths[i].reset();
	}
	

	
	public class MyView extends View {	  
		public MyView(Context context) {
			super(context);
			backgroundPaint.setColor(Color.GRAY); 
			backgroundPaint.setStyle(Paint.Style.FILL);
			paints[0].setColor(Color.BLACK);
			paints[1].setColor(Color.RED);
			paints[2].setColor(Color.rgb(0, 155, 0));
			paints[3].setColor(Color.BLUE);
			paints[4].setColor(Color.CYAN);
			for(int i=0;i<MAXCHAN;i++){
				paints[i].setStrokeWidth(2);
				paints[i].setStyle(Paint.Style. STROKE);
				}
			borderPaint.setColor(Color.rgb(244, 244, 235));
			borderPaint.setStrokeWidth(2);
			borderPaint.setStyle(Paint.Style.FILL);			
			axesPaint.setStrokeWidth(0);
			axesPaint.setStyle(Paint.Style.STROKE);
			axesPaint.setColor(Color.WHITE);
			axesPaint.setTextAlign(Align.CENTER);
			gridPaint.setStyle(Paint.Style.STROKE);
			gridPaint.setStrokeWidth(0);
			gridPaint.setColor(Color.GREEN);
			dashPaint.setPathEffect(new DashPathEffect(new float[]{2, 5}, 0));
			dashPaint.setStyle(Paint.Style.STROKE);
			dashPaint.setStrokeWidth(1);
			dashPaint.setColor(Color.BLACK);
			
		}
		
		@Override
		public void onWindowFocusChanged(boolean hasFocus) {
			super.onWindowFocusChanged(hasFocus);
		
			WIDTH = getWidth();			// Calculate the x and y scale factors
			HEIGHT = getHeight();   
			xscale = (float) (WIDTH-LEFTBORDER-RTBORDER-4) / (XMAX - XMIN);		
			yscale = (float) (HEIGHT-BOTBORDER-TOPBORDER-4)/ (YMAX - YMIN);
			//Log.e("x y", "W=  " + WIDTH + " H= " +HEIGHT + " xs= " + xscale + "ys= " + yscale);
		}
		
		
		private float w2sX(float x) {
			return (x-XMIN) * xscale + 2 + LEFTBORDER;
			}
		private float w2sY(float y) {
			return HEIGHT - (y-YMIN) * yscale - BOTBORDER-1;
			}
		
		private void drawAxes(Canvas canvas){
			DecimalFormat df = new DecimalFormat("#.##");
			float dx = (XMAX-XMIN)/NGRID; 
			float dy = (YMAX-YMIN)/NGRID;
			float ypos, xpos;
			
			Path gpath = new Path();		// for grids		
			axesPaint.setTextAlign(Align.CENTER);
			
			xpos = XMIN+dx;
			while (xpos <= XMAX-0.5*dx) {
				canvas.drawText(df.format(xpos), w2sX(xpos),  HEIGHT - BOTBORDER/2 - 3, axesPaint);
				gpath.moveTo(w2sX(xpos), w2sY(YMIN));
				gpath.lineTo(w2sX(xpos), w2sY(YMAX));
				xpos += dx;
				}
			canvas.drawText(xlabel, w2sX((XMIN+XMAX)/2), HEIGHT -5, axesPaint);

			canvas.rotate(-90, LEFTBORDER/2,  w2sY((YMIN+YMAX)/2));
			canvas.drawText(ylabel, w2sX(XMIN) - 10, w2sY((YMIN+YMAX)/2), axesPaint);			// rotated label for Y axis
			canvas.rotate(90, LEFTBORDER/2, w2sY((YMIN+YMAX)/2));
			
			axesPaint.setTextAlign(Align.RIGHT);
			
			ypos = YMIN+dy;
			while (ypos <= YMAX-dy) {
				canvas.drawText(df.format(ypos), w2sX(XMIN)-3,  w2sY((float) (ypos)), axesPaint);
				gpath.moveTo(w2sX(XMIN), w2sY(ypos));
				gpath.lineTo(w2sX(XMAX), w2sY(ypos));
				ypos += dy;
				}
			canvas.drawPath(gpath, dashPaint);
		}

		
		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			 
			canvas.drawRect(1, 1, WIDTH-1, HEIGHT-1, backgroundPaint);  // Outer boundary
			canvas.drawRect(1+LEFTBORDER, 1+TOPBORDER, WIDTH-1-RTBORDER, HEIGHT-1-BOTBORDER, borderPaint);  //plot window
			drawAxes(canvas);
			for(int i=0;i<MAXCHAN;i++)
					canvas.drawPath(paths[i], paints[i]);
			

		}
		
		@Override
		public boolean onTouchEvent(MotionEvent event){
			
			if(event.getAction() == MotionEvent.ACTION_DOWN)
		    {
				//float x = event.getX();
		        //float y = event.getY();
		        touched=true;
		        }
			
			return true;
			}
		

  
	  }

	public CaptureConstants setTimebase(double msPerDiv) {
		//int selectedTimebase = timebase.getProgress();    				// Slider returns a number from 0 to 9
		double totalTime = msPerDiv * 10 * 1000;  	// in microseconds
		//timebase_label.setText(msPerDiv[selectedTimebase]+"mS/div");	
		Par.NS = 300;		// start with 300
		Par.TG = (int) (totalTime / Par.NS);
		if(Par.TG < 4) Par.TG = 4;                   // minimum value of TG
		Par.NS = (int)(totalTime /Par.TG);
		while (Par.NS*Par.TG > (int)totalTime)--Par.NS;
		while (Par.NS*Par.TG < (int)totalTime)++Par.NS;
		
		if (Par.TG > 1000) Par.TG = 1000;
		//if(selectedTimebase == 8) Par.NS = 400;
		
		Log.e("setTimebase","NS= "+Par.NS+ " tg ="+ Par.TG +msPerDiv+ " and "+ 0.001*Par.NS*Par.TG);	
		setWorld(0.0,totalTime/1000.0, -5, 5);
		return Par;
		
	}
	
	
	
}
