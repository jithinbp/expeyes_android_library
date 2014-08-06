/* ExpEYES communication library.
   Library for ExpEYES (http://expeyes.in) under Android
   Copyright (C) 2014 Jithin B.P. , IISER Mohali (jithinbp@gmail.com)

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2, or (at your option)
   any later version.
*/

package explib;


public class ejData {	// Storage area for data from EJ

	public float[] ch1,ch2,ch3,ch4,time_ch1,t1,t2,t3,t4;
	public double ddata = 0;
	public int idata = 0,length=0,l1=0,l2=0,l3=0,l4=0;
	public long timestamp; //call System.nanoTime()
	
	public ejData(){
		ch1 = new float[1800];		// Processed data from capture calls
		ch2 = new float[1800];
		ch3 = new float[1800];
		ch4 = new float[1800];
		t1 = new float[1800];
		t2 = new float[1800];
		t3 = new float[1800];
		t4 = new float[1800];
	}	
   	
}
