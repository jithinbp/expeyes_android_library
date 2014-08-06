package explib;

import android.util.Log;

/* Curve Fitting Routine 
Copyright (C) 2014 Ambar Chatterjee (drambar@gmail.com)
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.
*/
public class ejMath {

	public ejMath(){}

	public static int MatInv(int N,double[] C)
	{
	double Q=0.0,T=0.0;

	for (int i=0;i<N;++i)
	    {
	    if ((C[i*N+i])==0.0) return -1;
	    Q=1.0/C[i*N+i];
	    for (int j=0;j<N;++j) C[i*N+j]*=Q;
	    for (int j=0;j<N;++j)
	        {
	        if (j==i) C[i*N+j]=Q;
	        else { T=C[j*N+i]; for (int k=0;k<N;k++) C[j*N+k]-=C[i*N+k]*T; C[j*N+i]=-Q*T; }
	        }
	    }
	return 0;
	}
	
	//----------------------------------------------------------------------------------------------------------------------
	public static double Func_sine(double[] P,double x)
	{
	return P[0]*Math.sin(P[1]*x +P[2])+P[3];
	}

	//----------------------------------------------------------------------------------------------------------------------

	
	public static boolean SetSineInitialVals(int N,float [] Xf,float [] Yf,double [] Par,int Np) 
	//Search for sine or square wave parameters. Return FALSE if the function fails.
	//N=no of data points with (Xf,Yf) values, Par[]=sine wave parameters,Np=No of useful data to be used in DoFit
	{
	int i,I1,I2,I3;
	double Thr=0.1;
	double YMin,YMax;

	YMax=-10.0; YMin=10.0;
	for (i=0,I1=-1;i<(N-3);++i) //Locate 1st point above threshold
	    { if (Yf[i]>YMax) YMax=Yf[i]; if (Yf[i]<YMin) YMin=Yf[i]; if (Yf[i]>Thr) { I1=i; break; } }
	if (I1==-1) return false;
	for (i=I1,I2=-1;i<(N-2);++i) //Locate subsequent point below threshold
	    { if (Yf[i]>YMax) YMax=Yf[i]; if (Yf[i]<YMin) YMin=Yf[i]; if (Yf[i]<Thr) { I2=i; break; } }
	if (I2==-1)
	   {
	   Par[0]=YMax; //Amplitude
	   Par[1]=3.1415/Xf[N-1]; //Omega
	   Par[2]=-Par[1]*Xf[I1];//Phase;
	   Par[3]=0.01; //DC offset
	   Np=N-1;
	   return true;
	   }
	for (i=I2,I3=-1;i<N;++i) //Locate subsequent point above threshold
	    { if (Yf[i]>YMax) YMax=Yf[i]; if (Yf[i]<YMin) YMin=Yf[i]; if (Yf[i]>Thr) { I3=i; break; } }
	Par[0]=0.5*(YMax-YMin); //Amplitude
	Par[3]=0.5*(YMax+YMin); //DC offset
	if (I3==-1)                                                            //For cases where only the positive lobe is found
	   {
	   Par[1]=3.1415/(Xf[I2]-Xf[I1]);//Omega
	   Par[2]=-Par[1]*Xf[I1];//Phase
	   Np=I2+5; if (Np>N) Np=N;
	   return true;
	   }
	Par[1]=2.0*3.1415/(Xf[I3]-Xf[I1]);//Omega
	Par[2]=-Par[1]*Xf[I1];//Phase
	Np=I3+5; if (Np>N) Np=N;
	return true;
	}
	
	
	
	
	
	//----------------------------------------------------------------------------------------------------------------------
	public static boolean DoFitSine(int NPts,int NPara,float[] X,float[] Y,double[] P,double[] Err)
	//Returns false on failure, true on success
	{
	double PAR_STEP=0.0001;        //For derivative calculation
	double FRAC_CHISQ=0.01;        //Relative difference in Chisq for termination
	double ABS_CHISQ=0.01;         //Absolute difference in Chisq for termination
	double LIM_CHISQ=1.0e10;       //To abort before overflow problems
	double MIN_CHISQ=1.0e-05;      //Stop if ChisqDF goes below this
	int MAX_ITER=1000;             //Max iterations before termination
	boolean Finished=false;
	double PrevChisq=1.0e25,CurrChisq=0.0,ChisqDf=0.0,DChi=0.0,S=0.0,C=0.0,A=0.0,Q=0.0,Hh=1.0;
	int Temp=0;
	double[] Rd=new double[NPara],Cd=new double[NPara*NPara],D=new double[NPara],Qd=new double[NPara];
	
	if(!SetSineInitialVals(NPts,X,Y,P,NPts) )return false;
	if (NPts<=NPara) return false; //Too few points
	for (int Iter=0;Iter<MAX_ITER;++Iter)
	    {
	    CurrChisq=0.0;
	    for (int k=0;k<NPara;++k)
	       {
	       Rd[k]=0.0; Temp=k*NPara;
	       for (int h=0;h<=k;++h) Cd[Temp+h]=0.0;
	       }
	    for (int i=0;i<NPts;++i)
	       {
	       S=Func_sine(P,X[i]); C=Y[i]-S; CurrChisq+=C*C;
	       if (CurrChisq>LIM_CHISQ) return false;  //Overflow
	       A=S;
	       for (int k=0;k<NPara;++k)
	          {
	          Temp=k*NPara; Q=P[k];
	          if (Q!=0.0) Hh=0.0; else Hh=1.0;
	          P[k]=Q+(Q+Hh)*PAR_STEP;
	          S=Func_sine(P,X[i]); P[k]=Q; D[k]=(S-A)/PAR_STEP/(Q+Hh); Rd[k]+=C*D[k];
	          for (int h=0;h<=k;++h) Cd[Temp+h]+=D[k]*D[h];
	          }
	       }
	   ChisqDf=CurrChisq/(NPts-NPara); DChi=Math.abs(CurrChisq-PrevChisq);
	   //System.out.printf("Iter# %d, Chisq/DF= %8g P[0]=%f P[1]=%f\n",Iter,ChisqDf,P[0],P[1]);
	   if ( (DChi<FRAC_CHISQ*CurrChisq) || (DChi<ABS_CHISQ) || (ChisqDf<MIN_CHISQ) ) Finished=true;
	   if ( (CurrChisq>PrevChisq) && !Finished ) for (int k=0;k<NPara;++k) { Qd[k]=0.5*Qd[k]; P[k]-=Qd[k]; }
	   else
	      {
	      for (int k=0;k<NPara;++k)
	         {
	         Temp=k*NPara;
	         for (int h=k+1;h<NPara;++h) Cd[Temp+h]=Cd[h*NPara+k];
	         }
	      if (MatInv(NPara,Cd)==-1) return true;  //Exact fit (rarely error)
	      if (Finished)
	         {
	         for (int ii=0,jj=0;ii<NPara;++ii)
	             { Err[ii]=Math.sqrt(Math.abs(ChisqDf*Cd[jj*NPara+jj])); ++jj; }
	         //System.out.printf("Final... Iter= %d, Chisq/DF= %8g P[0]=%f P[1]=%f\n",Iter,ChisqDf,P[0],P[1]);
	         return true;
	         }
	      for (int k=0;k<NPara;++k)
	         {
	         Qd[k]=0.0; Temp=k*NPara;
	         for (int h=0;h<NPara;++h) Qd[k]+=Cd[Temp+h]*Rd[h];
	         }
	      for (int k=0;k<NPara;++k) P[k]+=Qd[k];
	      PrevChisq=CurrChisq;
	      }
	   }
	return false;  //Iteration limit reached without convergence
	}

	//----------------------------------------------------------------------------------------------------------------------
		public static double Func_Dsine(double[] P,double x)
		{
		return P[0]*Math.sin(P[1]*x +P[2])*Math.exp(-P[4]*x)+P[3]; //amp,freq,phase,offset,decay const
		}

		//----------------------------------------------------------------------------------------------------------------------

		
		public static boolean SetDSineInitialVals(int N,float [] Xf,float [] Yf,double [] Par,int Np) 
		//Search for sine or square wave parameters. Return FALSE if the function fails.
		//N=no of data points with (Xf,Yf) values, Par[]=sine wave parameters,Np=No of useful data to be used in DoFit
		{
		int i,I1,I2,I3;
		double Thr=0.1;
		double YMin,YMax;

		YMax=-10.0; YMin=10.0;
		for (i=0,I1=-1;i<(N-3);++i) //Locate 1st point above threshold
		    { if (Yf[i]>YMax) YMax=Yf[i]; if (Yf[i]<YMin) YMin=Yf[i]; if (Yf[i]>Thr) { I1=i; break; } }
		if (I1==-1) return false;
		for (i=I1,I2=-1;i<(N-2);++i) //Locate subsequent point below threshold
		    { if (Yf[i]>YMax) YMax=Yf[i]; if (Yf[i]<YMin) YMin=Yf[i]; if (Yf[i]<Thr) { I2=i; break; } }
		if (I2==-1)
		   {
		   Par[0]=YMax; //Amplitude
		   Par[1]=3.1415/Xf[N-1]; //Omega
		   Par[2]=-Par[1]*Xf[I1];//Phase;
		   Par[3]=0.01; //DC offset
		   Np=N-1;
		   return true;
		   }
		for (i=I2,I3=-1;i<N;++i) //Locate subsequent point above threshold
		    { if (Yf[i]>YMax) YMax=Yf[i]; if (Yf[i]<YMin) YMin=Yf[i]; if (Yf[i]>Thr) { I3=i; break; } }
		Par[0]=0.5*(YMax-YMin); //Amplitude
		Par[3]=0.5*(YMax+YMin); //DC offset
		if (I3==-1)                                                            //For cases where only the positive lobe is found
		   {
		   Par[1]=3.1415/(Xf[I2]-Xf[I1]);//Omega
		   Par[2]=-Par[1]*Xf[I1];//Phase
		   Np=I2+5; if (Np>N) Np=N;
		   return true;
		   }
		Par[1]=2.0*3.1415/(Xf[I3]-Xf[I1]);//Omega
		Par[2]=-Par[1]*Xf[I1];//Phase
		Np=I3+5; if (Np>N) Np=N;
		Par[4]=0; //decay constant
		return true;
		}
		
		
		
		
		
		//----------------------------------------------------------------------------------------------------------------------
		public static boolean DoFitDSine(int NPts,int NPara,float[] X,float[] Y,double[] P,double[] Err)
		//Returns false on failure, true on success
		{
		double PAR_STEP=0.0001;        //For derivative calculation
		double FRAC_CHISQ=0.01;        //Relative difference in Chisq for termination
		double ABS_CHISQ=0.01;         //Absolute difference in Chisq for termination
		double LIM_CHISQ=1.0e10;       //To abort before overflow problems
		double MIN_CHISQ=1.0e-05;      //Stop if ChisqDF goes below this
		int MAX_ITER=1000;             //Max iterations before termination
		boolean Finished=false;
		double PrevChisq=1.0e25,CurrChisq=0.0,ChisqDf=0.0,DChi=0.0,S=0.0,C=0.0,A=0.0,Q=0.0,Hh=1.0;
		int Temp=0;
		double[] Rd=new double[NPara],Cd=new double[NPara*NPara],D=new double[NPara],Qd=new double[NPara];
		
		if(!SetDSineInitialVals(NPts,X,Y,P,NPts) )return false;
		if (NPts<=NPara) return false; //Too few points
		for (int Iter=0;Iter<MAX_ITER;++Iter)
		    {
		    CurrChisq=0.0;
		    for (int k=0;k<NPara;++k)
		       {
		       Rd[k]=0.0; Temp=k*NPara;
		       for (int h=0;h<=k;++h) Cd[Temp+h]=0.0;
		       }
		    for (int i=0;i<NPts;++i)
		       {
		       S=Func_Dsine(P,X[i]); C=Y[i]-S; CurrChisq+=C*C;
		       if (CurrChisq>LIM_CHISQ) return false;  //Overflow
		       A=S;
		       for (int k=0;k<NPara;++k)
		          {
		          Temp=k*NPara; Q=P[k];
		          if (Q!=0.0) Hh=0.0; else Hh=1.0;
		          P[k]=Q+(Q+Hh)*PAR_STEP;
		          S=Func_Dsine(P,X[i]); P[k]=Q; D[k]=(S-A)/PAR_STEP/(Q+Hh); Rd[k]+=C*D[k];
		          for (int h=0;h<=k;++h) Cd[Temp+h]+=D[k]*D[h];
		          }
		       }
		   ChisqDf=CurrChisq/(NPts-NPara); DChi=Math.abs(CurrChisq-PrevChisq);
		   //System.out.printf("Iter# %d, Chisq/DF= %8g P[0]=%f P[1]=%f\n",Iter,ChisqDf,P[0],P[1]);
		   if ( (DChi<FRAC_CHISQ*CurrChisq) || (DChi<ABS_CHISQ) || (ChisqDf<MIN_CHISQ) ) Finished=true;
		   if ( (CurrChisq>PrevChisq) && !Finished ) for (int k=0;k<NPara;++k) { Qd[k]=0.5*Qd[k]; P[k]-=Qd[k]; }
		   else
		      {
		      for (int k=0;k<NPara;++k)
		         {
		         Temp=k*NPara;
		         for (int h=k+1;h<NPara;++h) Cd[Temp+h]=Cd[h*NPara+k];
		         }
		      if (MatInv(NPara,Cd)==-1) return true;  //Exact fit (rarely error)
		      if (Finished)
		         {
		         for (int ii=0,jj=0;ii<NPara;++ii)
		             { Err[ii]=Math.sqrt(Math.abs(ChisqDf*Cd[jj*NPara+jj])); ++jj; }
		         //System.out.printf("Final... Iter= %d, Chisq/DF= %8g P[0]=%f P[1]=%f\n",Iter,ChisqDf,P[0],P[1]);
		         return true;
		         }
		      for (int k=0;k<NPara;++k)
		         {
		         Qd[k]=0.0; Temp=k*NPara;
		         for (int h=0;h<NPara;++h) Qd[k]+=Cd[Temp+h]*Rd[h];
		         }
		      for (int k=0;k<NPara;++k) P[k]+=Qd[k];
		      PrevChisq=CurrChisq;
		      }
		   }
		return false;  //Iteration limit reached without convergence
		}





	
	
	
	
//-------------------------------------------------------------------------------------
	

	public static boolean SetExpDecayInitialVals(int N,float [] Xf,float [] Yf,double [] Par){
		Par[0] = Yf[0];  //Initial amplitude
		Par[2] = 0.0;
		double halfval=Yf[0]/2.0,tHalf=0;
		int halfpos=0,n=0;
		if(Par[0]<halfval)for(n=0;n<N;n++){	if(Yf[n]>halfval){halfpos = n;break;}	}
		else for(n=0;n<N;n++){	if(Yf[n]<halfval){halfpos = n;break;}	}
		
		if(n==N)return false ; //Halflife never reached!
		tHalf=Xf[n];
		Par[1] = 0.693/tHalf ; //Decay constant 
		Log.e("RLDischargeFIT","AMP:"+Par[0]+" ,const"+Par[1]);
		return true;
		
	}


	//----------------------------------------------------------------------------------------------------------------------
	public static double Func_ExpDecay(double[] P,double t)
	{
	return P[0]*Math.exp(-P[1]*t)+P[2];
	}
	
	//----------------------------------------------------------------------------------------------------------------------
	public static boolean DoFitExpDecay(int NPts,int NPara,float[] X,float[] Y,double[] P,double[] Err)
	//Returns false on failure, true on success
	{
	double PAR_STEP=0.0001;        //For derivative calculation
	double FRAC_CHISQ=0.01;        //Relative difference in Chisq for termination
	double ABS_CHISQ=0.01;         //Absolute difference in Chisq for termination
	double LIM_CHISQ=1.0e10;       //To abort before overflow problems
	double MIN_CHISQ=1.0e-05;      //Stop if ChisqDF goes below this
	int MAX_ITER=1000;             //Max iterations before termination
	boolean Finished=false;
	double PrevChisq=1.0e25,CurrChisq=0.0,ChisqDf=0.0,DChi=0.0,S=0.0,C=0.0,A=0.0,Q=0.0,Hh=1.0;
	int Temp=0;
	double[] Rd=new double[NPara],Cd=new double[NPara*NPara],D=new double[NPara],Qd=new double[NPara];
	
	if(!SetExpDecayInitialVals(NPts,X,Y,P) )return false;
	for (int Iter=0;Iter<MAX_ITER;++Iter)
	    {
	    CurrChisq=0.0;
	    for (int k=0;k<NPara;++k)
	       {
	       Rd[k]=0.0; Temp=k*NPara;
	       for (int h=0;h<=k;++h) Cd[Temp+h]=0.0;
	       }
	    for (int i=0;i<NPts;++i)
	       {
	       S=Func_ExpDecay(P,X[i]); C=Y[i]-S; CurrChisq+=C*C;
	       if (CurrChisq>LIM_CHISQ) return false;  //Overflow
	       A=S;
	       for (int k=0;k<NPara;++k)
	          {
	          Temp=k*NPara; Q=P[k];
	          if (Q!=0.0) Hh=0.0; else Hh=1.0;
	          P[k]=Q+(Q+Hh)*PAR_STEP;
	          S=Func_ExpDecay(P,X[i]); P[k]=Q; D[k]=(S-A)/PAR_STEP/(Q+Hh); Rd[k]+=C*D[k];
	          for (int h=0;h<=k;++h) Cd[Temp+h]+=D[k]*D[h];
	          }
	       }
	   ChisqDf=CurrChisq/(NPts-NPara); DChi=Math.abs(CurrChisq-PrevChisq);
	   System.out.printf("Iter# %d, Chisq/DF= %8g P[0]=%f P[1]=%f\n",Iter,ChisqDf,P[0],P[1]);
	   if ( (DChi<FRAC_CHISQ*CurrChisq) || (DChi<ABS_CHISQ) || (ChisqDf<MIN_CHISQ) ) Finished=true;
	   if ( (CurrChisq>PrevChisq) && !Finished ) for (int k=0;k<NPara;++k) { Qd[k]=0.5*Qd[k]; P[k]-=Qd[k]; }
	   else
	      {
	      for (int k=0;k<NPara;++k)
	         {
	         Temp=k*NPara;
	         for (int h=k+1;h<NPara;++h) Cd[Temp+h]=Cd[h*NPara+k];
	         }
	      if (MatInv(NPara,Cd)==-1) return true;  //Exact fit (rarely error)
	      if (Finished)
	         {
	         for (int ii=0,jj=0;ii<NPara;++ii)
	             { Err[ii]=Math.sqrt(Math.abs(ChisqDf*Cd[jj*NPara+jj])); ++jj; }
	         System.out.printf("Final... Iter= %d, Chisq/DF= %8g P[0]=%f P[1]=%f\n",Iter,ChisqDf,P[0],P[1]);
	         return true;
	         }
	      for (int k=0;k<NPara;++k)
	         {
	         Qd[k]=0.0; Temp=k*NPara;
	         for (int h=0;h<NPara;++h) Qd[k]+=Cd[Temp+h]*Rd[h];
	         }
	      for (int k=0;k<NPara;++k) P[k]+=Qd[k];
	      PrevChisq=CurrChisq;
	      }
	   }
	return false;  //Iteration limit reached without convergence
	}







}


