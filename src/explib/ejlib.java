/* ExpEYES communication library.
   Library for ExpEYES (http://expeyes.in) under Android
   Copyright (C) 2014 Jithin B.P. , IISER Mohali (jithinbp@gmail.com)

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2, or (at your option)
   any later version.
*/

package explib;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;

import android.os.SystemClock;
import android.util.Log;


public class ejlib {
	private final int BUFSIZE = 2000;
   	private byte[] buffer = new byte[BUFSIZE];		// for communication with the uC
   	private int timeout = 500;						// Timeout limit
    private final String TAG = "expeyes library";
    protected final Object ejLock = new Object();
    
	 //commands without arguments   (1 to 40)
	 private final int GETVERSION = 1;	
	 private final int READCMP =2;
	 private final int READTEMP =3;
	 private final int GETPORTB =4;
	 
	 //commands with One byte argument (41 to 80)
	 private final int READADC =41;
	 private final int GETSTATE =42;
	 private final int NANODELAY = 43;
	 private final int SETADCREF =44;
	 private final int READADCSM =45; 
	 private final int IRSEND1 = 46;  	// Sends one byte over IR on SQR1
	 private final int RDEEPROM	= 47;	// Read nwords starting from address
	 
	 // Commands with Two bytes argument (81 to 120)
	 private final int R2RTIME = 81;      	// 1 + 1 + 4 bytes are returned for all time measurement calls.
	 private final int R2FTIME = 82;      	//
	 private final int F2RTIME = 83;      	//
	 private final int F2FTIME = 84;      	//
	 private final int MULTIR2R = 85;      	//
	 private final int SET2RTIME = 86;      // From a Dout transition to the Din transition
	 private final int SET2FTIME = 87;      //
	 private final int CLR2RTIME = 88;      //
	 private final int CLR2FTIME = 89;      //
	 private final int HTPUL2RTIME = 90;    // High True Pulse to HIGH
	 private final int HTPUL2FTIME = 91;    // High True Pulse to LOW
	 private final int LTPUL2RTIME = 92;    //
	 private final int LTPUL2FTIME = 93;    //
	 private final int SETPULWIDTH = 94;    //
	 private final int SETSTATE = 95;      	// Pin number, hi/lo
	 private final int SETDAC = 96;			//
	 private final int SETCURRENT = 97;		// channel, Irange
	 private final int SETACTION = 98;      // capture modifiers, action, target pin
	 private final int SETTRIGVAL = 99;     // Analog trigger level
	 
	 // Commands with Three bytes argument (121 to 160)
	 private final int SETSQR1 = 121;		// Set squarewave/level on OC2
	 private final int SETSQR2 = 122;		// Set squarewave/level on OC3
	 private final int WREEPROM	= 123;  		// write 1 word to the address
			 
	 // Commands with Four bytes argument (161 to 200)
	 private final int MEASURECV = 163;     // ch, irange, duration
	 private final int SETPWM1 = 164;	    // PWM on SQR1. scale, pr, ocrs
	 private final int SETPWM2 = 165;       // PWM on SQR1. scale, pr, ocrs
	 private final int IRSEND4 = 166;		// 4 bytes  IR transmission
	 
	 //commands with Four bytes argument
	 private final int CAPTURE = 201;
	 private final int CAPTURE_HR = 202;	// 1 byte CH, 2 byte NS, 2 byte TG
	 private final int SETSQRS =  203;     // scale, ocr, phase diff
			 
	 private final int CAPTURE2 = 241;
	 private final int CAPTURE2_HR	= 242; 	// ch1, ch2, NS, TG (1, 1, 2, 2)bytes
	 private final int CAPTURE3	= 243;		// ch1&ch2, ch3, ns , tg
	 private final int CAPTURE4	= 244;	 	// ch1&ch2, ch3&ch4, ns , tg
	 
	 // Responses from the device
	 public byte SUCCESS 		= (byte)'D';			// Command executed successfully 
	 private byte WAITING 		= (byte)'W';			// Command under processing, for threaded version 
	 private byte INVCMD		= (byte)'C';			// Invalid Command
	 private byte INVARG		= (byte)'A';			// Invalid input data
	 private byte INVBUFSIZE	= (byte)'B';			// Resulting data exceeds buffer size
	 private byte TIMEOUT		= (byte)'T';			// Time measurement timed out
	 private byte COMERR		= (byte)'S';			// Serial Communication error
	 private byte INVSIZE		= (byte)'Z';			// Size mismatch, result of capture
	 
	 
	 public devhandler mcp2200;
	 public String version;
	 public double[] m8 =new double[20], m12=new double[20], c=new double[20];	// scale factors for analog data
	 public double dacm = 5.0/4095;		// For DAC
	 public double tgap = 0.004;		// Time gap between digitisation of two channels
	 public	double socket_cap= 30.0, cap_calib = 1.0;	// Default calibration value for capacitance
	 public double sen_pullup = 5100.0;
	 public ejData ejdata;
	 public boolean connected = false, messageUpdated=false;
	 public int commandStatus = 0;
	 public String message = new String();
	 private int k,nb;
	 
	 
	 /*----------Constructor routine.  Also initialises calibration data---------*/
	 public ejlib(devhandler acm) {
	        mcp2200 = acm;

			for(int k=0; k <13; ++k) 			// Initialize the scale factors, assume 0 to 5V range
				{
				m12[k] = 5.0/4095; 
				m8[k] = 5.0/255;
				c[k] = 0.0;
				}
			m12[1] = m12[2] = 10.0/4095;		// Default for 1 & 2, have -5 to +5 volts range
			m8[1] = m8[2] = 10.0/255;
			c[1] = c[2] = -5.0;
			
			ejdata = new ejData();
	 
	 }
	 
	
	 /*---------------Opens the assigns device, sets BAUDRATE, and checks the version--------*/
	 public boolean open(){
		 try {
			 
			mcp2200.open();
			SystemClock.sleep(200);
			mcp2200.setBaud(115200);
			SystemClock.sleep(400);
			Log.d(TAG,"opened device");
			mcp2200.clear();
			connected = true;  // if you get this far, assume you've connected. 
			message = new String("Connected to mcp2200");
			
			version = get_version();
			if(version.substring(0,2).equals("ej")){
				connected = true;
				setMessage("Connected to device");
				return true;
			}
			else{
				connected=false;
				setMessage("Received wrong version");
				return false;
			}
			
		 } catch (IOException e) {
			 connected = false;
			 Log.e(TAG,"FAILED TO OPEN DEVICE. CHECK CONNECTIONS.");
			setMessage("Failed to connect to device");

		 }		 

		return false;
	  }
	 
	 public boolean close(){
		 try{
			 mcp2200.close();
			 connected=false;
			 return true;
		 }catch (IOException e){
			 Log.d(TAG,"Closed device!!");
		 }
		 return false;
	 }
	 
	public void setMessage(String msg){
		message = msg;
		messageUpdated = true;
	}
	
	//-------------- Internal functions, to send/receive byte and integer data ---------------------------------
	
	private void sendByte(int val) throws IOException{  // Sends lower byte of the received integer to the device
		if(!connected){
			throw new IOException("DEVICE NOT CONNECTED");
			}
		
		try {
			mcp2200.write(new byte[] {(byte) (val & 0xff)}, timeout);
		} catch (IOException e) {
			setMessage("Failed to send data. check connections");
			connected = false;
			throw new IOException("DEVICE NOT CONNECTED");
			}
		
		SystemClock.sleep(10);        // may not be required
		}
	
	private void sendInt(int val) throws IOException{		// Sends integer data as two bytes
		if(!connected){
			throw new IOException("DEVICE NOT CONNECTED");
			}
		
		try {
			mcp2200.write(new byte[] {(byte) (val & 0xff),(byte) ((val >> 8) & 0xff)}, timeout);
		} catch (IOException e) {
			setMessage("Failed to send data. check connections");
			connected = false;
			throw new IOException("DEVICE NOT CONNECTED");
		}

		SystemClock.sleep(10);        // may not be required
		}

	private boolean systemBusy(){
		if(commandStatus == WAITING ) {Log.d(TAG, "system busy"); return true;}
		else if(connected == false ) {Log.e(TAG, "Disconnected!"); commandStatus = COMERR; return true;}
		commandStatus = WAITING;
		return false;
		}
	
	/*--------------------retrieve the version number from the connected device------------*/
	public String get_version(){
		if (systemBusy()) return "err";
		
		try {
			sendByte(GETVERSION);
			//mcp2200.write(new byte[] {GETVERSION}, 10);
			SystemClock.sleep(10);
    		mcp2200.read(buffer,6,timeout);
			version = new String( Arrays.copyOfRange(buffer, 1, 6), Charset.forName("UTF-8"));
			commandStatus = SUCCESS;	
			return version;
		} catch (IOException e) {
			Log.e(TAG,"communication error");
			e.printStackTrace();
			connected = false;
			version = "err";
			commandStatus = COMERR;	
			return "err";
			}
		
		}
	
	//----------------- Functions to Store and Load Calibration factors to/from EEPROM of expEYES -------------------------
	private final int AM1 = 0;		// EEPROM location of the parameters, y = mx + c, for A1 and A2
	private final int AC1 = 2;
	private final int AM2 = 4;
	private final int AC2 = 6;
	private final int ASOC = 8;		// Socket cap IN1
	private final int ACCF = 10;	// Capacitance error factor
	private final int ARP = 12;		// Pull-up Resistor value 
	
	public void eeprom_write(int addr, int data) {
		//Writes a 16 bit number to EEPROM
		if (systemBusy()) return;
		try {
			sendByte(WREEPROM);
			sendByte(addr);
			sendInt(data);
			mcp2200.read(buffer, 1, timeout); 		// Response byte + 2 data bytes
			if (buffer[0] != SUCCESS) {	
				commandStatus =  COMERR;
				return;
				} 
			}catch (IOException e) {
				e.printStackTrace(); commandStatus = COMERR;connected = false;
				}
		commandStatus =  SUCCESS;
		}
	
	public void eeprom_read(int addr) {
		//Writes a 16 bit number to EEPROM
		if (systemBusy()) return;
		try {
			sendByte(RDEEPROM);
			sendByte(addr);
			mcp2200.read(buffer, 3, timeout); 		// Response byte + 2 data bytes
			if (buffer[0] != SUCCESS) {	
				commandStatus =  COMERR;
				return;
				} 
			}catch (IOException e) {
				e.printStackTrace(); commandStatus = COMERR;connected = false;
				}
		ejdata.idata = ( buffer[1] & 0xff) | ((buffer[2] & 0xff) << 8);
		commandStatus =  SUCCESS;
		}
	
		
	public void store_float(int addr, double data) { // store a floating point number to EEPROM
		Float f = Float.valueOf((float) data); 
		int iv = Float.floatToIntBits(f);
		eeprom_write(addr, iv & 0xffff);
		if (commandStatus != SUCCESS) return;
		eeprom_write(addr+1, (iv >> 16 ) & 0xffff);
		}

	public void restore_float(int addr) { // store a floating point number to EEPROM
		int iv;
		eeprom_read(addr);
		if (commandStatus != SUCCESS) return;
		iv = ejdata.idata & 0xffff;
		eeprom_read(addr+1);
		if (commandStatus != SUCCESS) return;
		iv = (ejdata.idata << 16) | iv;
		Float f = Float.valueOf(Float.intBitsToFloat(iv));
		ejdata.ddata = (double) f;
		}

	public void storeCF_a1a2(double m1, double c1, double m2, double c2) { 
		// Stores the four calibration factors, of A1&A2, to EEPROM
		float f =(float) m1;
		store_float(AM1, f);
		if (commandStatus != SUCCESS) return;
		f= (float) c1;
		store_float(AC1, f);
		if (commandStatus != SUCCESS) return;
		f= (float) m2;
		store_float(AM2, f);
		if (commandStatus != SUCCESS) return;
		f= (float) c2;
		store_float(AC2, f);
		}
		
	public void storeCF_cap(double soc, double ccf) { // Socket capacitance and error factor
		float f =(float) soc;
		store_float(ASOC, f);
		if (commandStatus != SUCCESS) return;
		f= (float) ccf;
		store_float(ACCF, f);
		}
		
	public void storeCF_sen(double res) { // Pulll-up resistor on SEN
		float f =(float) res;
		store_float(ASOC, f);
		}
		
	public void load_calibration() {
		double m1,c1,m2,c2,soc,ccf,res;
		double mm = 10.0/4095, cc = -5.0;
		double dm = mm * 0.02, dc = cc * 0.02;		// maximum 2% gain/offset deviation
		
		restore_float(AM1); if (commandStatus != SUCCESS) return;
		m1 = ejdata.ddata;
		restore_float(AC1); if (commandStatus != SUCCESS) return;
		c1 = ejdata.ddata;
		restore_float(AM2); if (commandStatus != SUCCESS) return;
		m2 = ejdata.ddata;
		restore_float(AC2); if (commandStatus != SUCCESS) return;
		c2 = ejdata.ddata;
		//Log.e(TAG, "loadcal: m1=" + m1 + " c1= " + c1 + " m2= " + m2 + " c2= " + c2);
		
		if( (Math.abs(m1-mm) < dm) && (Math.abs(m2-mm) < dm) && (Math.abs(c1-cc) < dc) && (Math.abs(c2-cc) < dc) ) {
			m12[1] = m1;				// Accept the values
			c[1] = c1;
			m12[2] = m2;
			c[2] = c2;
			m8[1] = m1 * 4095./255;		//Scale factors for 8 bit read
			m8[2] = m2 * 4095./255;
			}
			
		restore_float(ASOC); if (commandStatus != SUCCESS) return;
		soc = ejdata.ddata;
		restore_float(ACCF); if (commandStatus != SUCCESS) return;
		ccf = ejdata.ddata;
		if ( (ccf >.75) && (ccf < 1.25) && (soc > 20) && (soc < 50) ) { // Validate the calibration factors
			socket_cap = soc;
			cap_calib = ccf;
			}
		
		restore_float(ARP); if (commandStatus != SUCCESS) return;
		res = ejdata.ddata;
		if ( (res > 4930) && (res < 5250) ){
			sen_pullup = res;
			}
		}

	
//------------------------- Digital Input/Output Functions -----------------------------

	public void set_state(int pin, int state) {	
		if (systemBusy()) return;
		try {
			sendByte(SETSTATE);	
			sendByte(pin);	
			sendByte(state);
			mcp2200.read(buffer,1, timeout); // Response byte 
			if (buffer[0] != SUCCESS) {
				commandStatus =  COMERR;
				return;
				}		
			} catch (IOException e) {
				e.printStackTrace(); commandStatus = COMERR;connected = false;
				}
		commandStatus =  SUCCESS;
		}

	
	public void get_state(int pin) 	{	//	gets the status of the digital input pin.
		if (systemBusy()) return;
		try {
			sendByte(GETSTATE);	
			sendByte(pin);
			mcp2200.read(buffer,2, timeout); // Response byte + result
			if (buffer[0] != SUCCESS) {
				commandStatus =  COMERR;
				return;
				}		
			ejdata.idata = buffer[1];
		}catch (IOException e) {
			e.printStackTrace(); commandStatus = COMERR;connected = false;
			}
		commandStatus =  SUCCESS;
		}

	
	//------------------------- Infrared transmission over SQR1 ----------------
	
	public void irsend1(int d1) {
		// 	Sends one byte of data over SQR1, using Infrared transmission protocol
		if (systemBusy()) return;
		try {
			sendByte(IRSEND1);
			sendByte(d1);
			mcp2200.read(buffer, 1, timeout); 		// Response byte + 2 data bytes
			if (buffer[0] != SUCCESS) {	
				commandStatus =  COMERR;
				return;
				} 
			}catch (IOException e) {
				e.printStackTrace(); commandStatus = COMERR;connected = false;
				}
		commandStatus =  SUCCESS;
		}


	public void irsend4(int d1,int d2, int d3, int d4) {
			// 	Sends one byte of data over SQR1, using Infrared transmission protocol
			if (systemBusy()) return;
			try {
				sendByte(IRSEND4);
				sendByte(d1);
				sendByte(d2);
				sendByte(d3);
				sendByte(d4);
				mcp2200.read(buffer, 1, timeout); 		// Response byte + 2 data bytes
				if (buffer[0] != SUCCESS) {	
					commandStatus =  COMERR;
					return;
					} 
				}catch (IOException e) {
					e.printStackTrace(); commandStatus = COMERR;connected = false;
					}
			commandStatus =  SUCCESS;
			}

	
	//=================== Charge Time Measurement Unit related functions ==========================
	public void read_temp(){
		// Reads the temperature of uC, currently of no use. Have to see whether this can be used for correcting
		// the drift of the 5V regulator with temperature.
		if (systemBusy())return;
		try {
			sendByte(READTEMP);					// Command 
			mcp2200.read(buffer,3, timeout); 	// Response byte + 2 data byte 
			if (buffer[0] != SUCCESS) {
				commandStatus =  COMERR;
				return;
				}		
			}catch (IOException e) {
				e.printStackTrace(); commandStatus = COMERR;connected = false;
				}
		ejdata.idata = ( buffer[1] & 0xff) | ((buffer[2] & 0xff) << 8);
		commandStatus =  SUCCESS;	
		}


	public void measure_cv(int ch, int ctime, double i) {
		// Using the CTMU of PIC, charges a capacitor connected to IN1, IN2 or SEN, for 'ctime' microseconds
		// and then measures the voltage across it. The value of current can be set to .55uA, 5.5 uA, 55uA or 550 uA
	
		int iv, irange;
		
		if (systemBusy())return;
		if(i > 500)			// 550 uA range
			irange = 0;
		else if(i > 50)		//	55 uA
			irange = 3;
		else if(i > 5)		// 5.5 uA
			irange = 2;
		else				// 0.55 uA
			irange = 1;

		if( (ch != 3) && ( ch !=4) ) {
			commandStatus =  SUCCESS;
			return;
			}
		
		try {
			sendByte(MEASURECV);
			sendByte(ch);
			sendByte(irange);
			sendInt(ctime);
			mcp2200.read(buffer,3, timeout); 	// Response byte + 2 data byte 
			if (buffer[0] != SUCCESS) {
				commandStatus =  COMERR;
				return;
				}		
			}catch (IOException e) {
				e.printStackTrace(); commandStatus = COMERR;connected = false;
				}
		iv = ( buffer[1] & 0xff) | ((buffer[2] & 0xff) << 8);
		ejdata.ddata =  (iv & 0xffff) * m12[ch] + c[ch];
		commandStatus =  SUCCESS;	
		}
	
			
	public void measure_cap_raw() {
		/*
		Measures the capacitance connected between IN1 and GND. Stray capacitance 
		should be subtracted from the measured value. Measurement is done by charging 
		the capacitor with 5.5 uA for a given time interval. Any error in the value of
		current is corrected by calibrating.
		*/
		int ctime, ctmin = 10, ctmax = 1000;
		
		for (ctime= ctmin; ctime < ctmax; ctime += 10) {
			measure_cv(3, ctime, 5.5);  	// 5.5 uA range is chosen
			if (commandStatus != SUCCESS) return;
			if (ejdata.ddata > 2.0) break;
			}
			
		if ( (ejdata.ddata > 4) || (ejdata.ddata < 0.1) ) {
			commandStatus =  INVARG;
			return;
			}
		ejdata.ddata = 5.5 * ctime / ejdata.ddata;		// returns value in pF 
		}
	
				
	public void measure_cap() {
		// Returns capacitance connected between IN1 and GND, after applying corrections.
		
		measure_cap_raw();
		ejdata.ddata = (ejdata.ddata - socket_cap) * cap_calib;
		}
	
	public void measure_res() {
		// Measures the resistance connected between SEN and GND.
		get_voltage(5);
		if ( (ejdata.ddata < 0.1) || (ejdata.ddata > 4.9) ) {		// Resistance > 100k or < 100 Ohm
			ejdata.ddata = -1.0;
			}
		else
			ejdata.ddata = (sen_pullup *ejdata.ddata) / (5.0-ejdata.ddata);
		}
	
	
	//----------------------------------- Waveform Generation --------------------------------------------

	final double mtvals[] = {0.125e-6, 8*0.125e-6, 64*0.125e-6, 256*0.125e-6};	// Possible Timer period values

	private boolean calc_regvals(double freq, int[] registers){       // Find out OCR and TCKPS for the given frequency
		double	period = 1.0/freq;
		int k, OCR;
		for(k=0; k < 4; ++k)				// Find the optimum scaling, OCR value
			if(period < mtvals[k]*50000)
				{
				OCR = (int) (period/mtvals[k] + 0.5);
				if (OCR > 0)
					{
					registers[0] = k;
					registers[1] = OCR;
					Log.d(TAG,"OCR = " + OCR + "clks= "+k);
					return true; 
					}
				}
		return false;
		}  	
	
		
	private void set_osc(int chan, double freq){  // Sets the output frequency of the SQR1 or SQR2. 
		int registers[] = {0,0}, cmds[] = {SETSQR1, SETSQR2};
		if(freq < 0)	        // Disable Timer and Set Output LOW
			registers[0] = 254;
		else if(freq <0.01 )		// Disable Timer and Set Output HIGH
			registers[0] = 255;
		else   					// Set the frequency
			{
			if (calc_regvals(freq, registers) == false)
				{
				Log.e(TAG, "Set_osc Invalid ARG");
				commandStatus =  INVARG;
				return;
				}
			}
		try {
			sendByte(cmds[chan]);				// Command depends on the channel number
			sendByte(registers[0]);				// prescale for timer
			sendInt(registers[1]);				// OCRS value
			mcp2200.read(buffer,1, timeout); 	// Response byte 
			if (buffer[0] != SUCCESS) {
				commandStatus =  COMERR;
				return;
				}		
			}catch (IOException e) {
				e.printStackTrace(); commandStatus = COMERR;connected = false;
				}
		if(registers[0]==254 || registers[0]==255)ejdata.ddata = 0;
		else ejdata.ddata = 1.0/(mtvals[registers[0]] * registers[1]); 
		}
		
	public void set_sqr1(double freq) {
		// Sets the frequency of SQR1 (between .7Hz and 200kHz). All intermediate values are not possible.
		// Returns the actual value set.
		if (systemBusy())	return;
		set_osc(0, freq);
		commandStatus =  SUCCESS;	
		}

	public void set_sqr2(double freq) {
		//	Sets the frequency of SQR2 (between .7Hz and 200kHz). All intermediate values are not possible.
		//	Returns the actual value set.
		if (systemBusy()) return;
		set_osc(1, freq);
		commandStatus =  SUCCESS;	
		}

	public void set_sqrs(double freq, double dphase)       // Freq in Hertz, phase difference in degrees
		{
		// Sets the output frequency of both SQR1 & SQR2, with given phase difference 
		int registers[] = {0,0}, TG;
		if (systemBusy())return;
		   
		if(freq < 0) {			// Disable both Square waves
			set_osc(0,-1);
			set_osc(1,-1);
			commandStatus =  SUCCESS;	
			return;
			}
		else if(freq < 0.5) {				// Disable both Square waves
			set_osc(0,0);
			set_osc(1,0);
			commandStatus =  SUCCESS;	
			return;
			}
		
		if( (dphase < 0) || (dphase >= 360.0) )
			{
			commandStatus =  INVARG;
			return;
			}
		if (calc_regvals(freq, registers) == false)
			{
			commandStatus =  INVARG;
			return;
			}
			
		TG = (int)(dphase*registers[1]/360.0 +0.5);
		if(TG == 0) TG = 1;	
		try {
			sendByte(SETSQRS);					// Command 
			sendByte(registers[0]);				// prescale for timer
			sendInt(registers[1]);				// OCRS value
			sendInt(TG);						// phase difference
			mcp2200.read(buffer,1, timeout); 	// Response byte 
			if (buffer[0] != SUCCESS) {
				commandStatus =  COMERR;
				return;
				}		
			}catch (IOException e) {
				e.printStackTrace(); commandStatus = COMERR;connected = false;
				}
		ejdata.ddata = 1.0/(mtvals[registers[0]] * registers[1]); 
		commandStatus =  SUCCESS;	
		}


	private void set_pwm(int osc, double ds, int resol) {  // osc#, duty cycle, resolution 
		// Sets PWM on SQR1 / SQR2. The frequency is decided by the resolution in bits.
		int ocxrs, ocx;
		if (systemBusy()) return;
		if( (ds > 100) || (resol < 6) || (resol > 16) ) {
			commandStatus =  INVARG;
			return;		
			}
		ocxrs = (int) (Math.pow(2.0, (double)resol));  
		ocx = (int)(0.01 * ds * ocxrs + 0.5) & 0xffff;
		try {
			if(osc == 0) 
				sendByte(SETPWM1);
			else
				sendByte(SETPWM2);
			sendInt(ocxrs-1);
			sendInt(ocx);
			mcp2200.read(buffer, 1, timeout); 		// Response byte + 2 data bytes
			if (buffer[0] != SUCCESS) {	
				commandStatus =  COMERR;
				return;
				} 
			}catch (IOException e) {
				e.printStackTrace(); commandStatus = COMERR;connected = false;
				}
		commandStatus =  SUCCESS;
		}

	
	public void set_sqr1_pwm(int dc) {
		// 	Sets 488 Hz PWM on SQR1. Duty cycle is specified in percentage. The third argument, PWM resolution, is 
		//	14 bits by default. Decreasing this by one doubles the frequency.
		set_pwm(0,dc,14);
		}
	
	public void set_sqr2_pwm(int dc) {
		// 	Sets 488 Hz PWM on SQR2. Duty cycle is specified in percentage. The third argument, PWM resolution, is 
		//	14 bits by default. Decreasing this by one doubles the frequency.
		set_pwm(0,dc,14);
		}
	
	public void set_sqr1_dc(double volt) {
		// PWM DAC on SQR1. Resolution is 10 bits (f = 7.8 kHz) by default. External Filter is required to get the DC
		// The voltage can be set from 0 to 5 volts.
		set_pwm(0, volt*20, 10);   // 100% means 5 volts., 10 bit resolution, 8kHz
	}
	
	public void set_sqr2_dc(double volt) {
		// PWM DAC on SQR2. Resolution is 10 bits (f = 7.8 kHz) by default. External Filter is required to get the DC
		// The voltage can be set from 0 to 5 volts.
		set_pwm(1, volt*20, 10);   // 100% means 5 volts., 10 bit resolution, 8kHz
	}

		
	//-------------------------------------- Time Interval Measurement Routines ----------------------
	
	private void tim_helper(int cmd, int src, int dst) {
		// Helper function for all Time measurement calls. Command, Source and destination pins are inputs.
		// Returns time in microseconds, -1 on error.
		if (systemBusy())return;

		if(cmd == MULTIR2R) {
			if(src > 7) {
				commandStatus =  INVARG;
				return;
				}
			if(dst > 249) {
				commandStatus =  INVARG;
				return;
				}
			}

		if( (cmd == R2RTIME) || (cmd == R2FTIME) || (cmd == F2RTIME) || (cmd == F2FTIME) )
			{
			if( (src > 7) || (dst > 7) ) {
				commandStatus =  INVARG;
				return;
				}			}

		if( (cmd == SET2RTIME) || (cmd == CLR2RTIME) ||(cmd == SET2FTIME) ||(cmd == CLR2FTIME) ||
			(cmd == HTPUL2RTIME) ||(cmd == HTPUL2FTIME) ||(cmd == LTPUL2RTIME) || (cmd == LTPUL2FTIME) ) {
				if( (src < 8) || (src > 11) ) {
					commandStatus =  INVARG;
					return;
					}
				if(dst > 7) {
					commandStatus =  INVARG;
					return;
					}
			}
		try {
			sendByte(cmd);	
			sendByte(src);	
			sendByte(dst);	
			nb = mcp2200.read(buffer,6, timeout+3000); 	// 1 + 1 + 4 bytes. Calls can timeout , 3 seconds to wait 
			Log.e(TAG, "TIME HELPER:" + commandStatus);
			if (buffer[0] == TIMEOUT) {             //  
				commandStatus =  buffer[0];
				ejdata.ddata = -1;				// indicates timeout error
				return;
				}		
			if (buffer[0] != SUCCESS) {             //  
				commandStatus =  buffer[0];
				return;
				}		
			if(nb != 6) {
				Log.e(TAG, "TIME HELPER: Expected 6 bytes. Got " + nb + " only\n");
				commandStatus =  INVSIZE;
				return;
				}
			}catch (IOException e) {
				e.printStackTrace(); commandStatus = COMERR;connected = false;
				}
		//float f = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getFloat();
		ejdata.ddata = 
				0.125 * ( ( buffer[2] & 0xff) | ((buffer[3] & 0xff) << 8) | ((buffer[4] & 0xff) << 16) |  ((buffer[5] & 0xff) << 24) );
		commandStatus =  SUCCESS;
		}
	
	public void r2rtime(int pin1, int pin2) {
		// Time between a rising edge to a rising edge. The pins must be distinct.
		tim_helper(R2RTIME, pin1, pin2);
		}
	
	public void f2ftime(int pin1, int pin2)	{
		// Time between a falling edge to a rising edge. The pins must be distinct.
		tim_helper(F2FTIME, pin1, pin2);
		}
	
	public void r2ftime(int pin1, int pin2) {
		// Time between a rising edge to a falling edge. The pins could be same or distinct.
		tim_helper(R2FTIME, pin1, pin2);
		}	 
	
	public void f2rtime(int pin1, int pin2) {
		// Time between a falling edge to a rising edge. The pins could be same or distinct.
		tim_helper(F2RTIME, pin1, pin2);
		}	 
	
	public void multi_r2rtime(int pin, int skip) {
		//Time between rising edges, could skip desired number of edges in between. (pin, 9) will give time required for
		//	10 cycles of a square wave, increases resolution.
		tim_helper(MULTIR2R, pin, skip);
		}
	
	public void get_frequency(int pin) {
		// This function measures the frequency of an external 0 to 5V PULSE on digital inputs, by calling multi_r2rtime().
		multi_r2rtime(pin, 0);
		if (commandStatus != SUCCESS) return;
		if (ejdata.ddata < 10000){			// Averaging for lower Time periods, to increase accuracy
			multi_r2rtime(pin, 9);
			ejdata.ddata = 1.0e7 / ejdata.ddata;
			}
		else
			ejdata.ddata = 1.0e6 / ejdata.ddata;
		}
	
	//======================== Active time interval measurements ==========================
	public void set2rtime(byte pin1, byte pin2)
		{
		// Time from setting pin1 to a rising edge on pin2.
		tim_helper(SET2RTIME, pin1, pin2);
		}	
	
	public void set2fime(byte pin1, byte pin2) {
		// Time from setting pin1 to a falling edge on pin2.
		tim_helper(SET2FTIME, pin1, pin2);
		}	

	public void clr2rtime(byte pin1, byte pin2) {
		// Time from clearing pin1 to a rising edge on pin2.
		tim_helper(CLR2RTIME, pin1, pin2);
		}	

	public void clr2ftime(byte pin1, byte pin2){
		// Time from clearing pin1 to a falling edge on pin2.
		tim_helper(CLR2FTIME, pin1, pin2);
		}	

	public void htpulse2rtime(byte pin1, byte pin2) {
		// Time from a HIGH True pulse on pin1 to a rising edge on pin2.
		tim_helper(HTPUL2RTIME, pin1, pin2);
		}	

	public void ltpulse2rtime(byte pin1, byte pin2) {
		// Time from a LOW True pulse on pin1 to a rising edge on pin2.
		tim_helper(LTPUL2RTIME, pin1, pin2);
		}	
	
	public void htpulse2ftime(byte pin1, byte pin2){
		// Time from a HIGH True pulse on pin1 to a falling edge on pin2.
		tim_helper(HTPUL2FTIME, pin1, pin2);
		}	
	
	public void ltpulse2ftime(byte pin1, byte pin2){
		// Time from a LOW True pulse on pin1 to a falling edge on pin2.
		tim_helper(HTPUL2FTIME, pin1, pin2);
		}	
	
	
	//====================== Analogue Set, Get & Capture functions =======================================
	public void read_adc(int chan) {	 // Read ADC, in SLEEP mode
		if (systemBusy()) return;
		if ((chan < 0) || (chan > 12)) {
			commandStatus =  INVARG;
			return;
			}	
		try {
			sendByte(READADCSM);
			sendByte(chan);
			mcp2200.read(buffer, 3, timeout); 		// Response byte + 2 data bytes
			if (buffer[0] != SUCCESS) {	
				commandStatus =  COMERR;
				return;
				} 
			}catch (IOException e) {
			e.printStackTrace(); commandStatus = COMERR;connected = false;
			}
		ejdata.idata = ( buffer[1] & 0xff) | ((buffer[2] & 0xff) << 8);
		commandStatus =  SUCCESS;	
		}
	
	public void read_adcNS(int chan) { 	 // Read ADC,  no SLEEP mode
		if (systemBusy()) return;
		if ((chan < 0) || (chan > 12)) {
			commandStatus =  INVARG;
			return;
			}	
		try {
			sendByte(READADC);
			sendByte(chan);
			mcp2200.read(buffer, 3, timeout); 		// Response byte + 2 data bytes
			if (buffer[0] != SUCCESS) {	
				commandStatus =  COMERR;
				return;
				} 
			}catch (IOException e) {
				e.printStackTrace(); commandStatus = COMERR;connected = false;
				}
		ejdata.idata = ( buffer[1] & 0xff) | ((buffer[2] & 0xff) << 8);
		commandStatus =  SUCCESS;	
		}	
	
	public void get_voltage(int chan) {	// Returns value in volts
		read_adc(chan);
		ejdata.ddata = ejdata.idata * m12[chan] + c[chan];
		//Log.e(TAG,  "ReadADC:  ddata  " + ejdata.ddata+ "idata = " + ejdata.idata + "m12= " + m12[12]);
		}

	
	public void get_voltageNS(int chan) { // Returns value in volts
		read_adcNS(chan);
		ejdata.ddata = ejdata.idata * m12[chan] + c[chan];
		}

		public void get_voltage_time(int chan) {
		read_adc(chan);
		ejdata.ddata = ejdata.idata * m12[chan] + c[chan];
		ejdata.timestamp = System.nanoTime();
		} 
	
	
	public void write_dac(int iv) {		// Returns zero on success
		if (systemBusy()) return;
		if(iv < 0) 
			iv = 0;		// Keep within limits
		else if (iv > 4095) 
			iv = 4095;
		try {
			sendByte(SETDAC);
			sendInt(iv);
			mcp2200.read(buffer, 1, timeout); 		// Response byte + 2 data bytes
			if (buffer[0] != SUCCESS) {	
				commandStatus =  COMERR;
				return;
				} 
			}catch (IOException e) {
				e.printStackTrace(); commandStatus = COMERR;connected = false;
				}
		commandStatus =  SUCCESS;
		}

	public void set_voltage(double v) {
		//	Sets the PVS voltage. Reads it back and applies correction in a loop.
		int iv, goal;
		goal = (int)(v / dacm + 0.5);
		iv = goal;
		for(k=0; k < 15; ++k) {
			write_dac(iv); if (commandStatus != SUCCESS) return;
			read_adc(12); if (commandStatus != SUCCESS) return;
			if (Math.abs(ejdata.idata - goal) <= 1) break;
			if (ejdata.idata > goal) iv -= 1;
			else if(ejdata.idata < goal) iv += 1;
			}
		ejdata.ddata = m12[12] * ejdata.idata + c[12];		//The voltage actually set
		}	
	
		
	//----------------------------- Capture functions ---------------------------------------
	
	public void capture(int ch, int ns, int tg){	// Single Channel, 8 bit resolution
		if (systemBusy()) return;
		try {
			mcp2200.clear();
			sendByte(CAPTURE);		// the command
			sendByte(ch);			// channel number
			sendInt(ns);			// number of samples
			sendInt(tg);			// time gap between them, in microseconds*/
			SystemClock.sleep((long) (0.001 * ns * tg ));  	// 
			nb = mcp2200.read(buffer,ns+2, timeout); 		// Response byte + one pad byte
			
			ejdata.length = ns;
			ejdata.l1=ns;
			ejdata.l2=ns;
			
			
			if (buffer[0] != SUCCESS) {
				commandStatus =  COMERR;
				Log.e("ERROR","Response not equal to SUCCESS");
				return;
				}

			ejdata.length = ns;
			if(nb != (ns+2)) {
				//Log.e(TAG, "CAPTURE:Expected "+ns+" bytes. Got "+nb+" only\n");
				commandStatus =  INVSIZE;
				return;
				}
			
			for(k=0; k < ns; ++k) ejdata.t1[k] = (float) (0.001 * k * tg);			// Fill Time, microseconds to milliseconds
			for(k=0; k < ns; ++k) ejdata.ch1[k] = (float) ((buffer[k+2] & 0xFF) * m8[ch] + c[ch]); // Fill voltage. ignore first two bytes.
		
			} catch (IOException e) {e.printStackTrace(); commandStatus = COMERR;connected = false;}
	commandStatus = SUCCESS;	
	}


	public void capture(int ch1, int ch2, int ns, int tg){
		if (systemBusy())return; 
		try {
			mcp2200.clear();
			sendByte(CAPTURE2);		// the command
			sendByte(ch1);			// channel number
			sendByte(ch2);			// channel number
			sendInt(ns);			// number of samples
			sendInt(tg);			// time gap between them, in microseconds
			SystemClock.sleep((long) (ns*tg*0.001));
			nb = mcp2200.read(buffer,2*ns+2, timeout); // Response byte + one pad byte + data		
			if (buffer[0] != SUCCESS) {
				commandStatus =  COMERR;
				Log.d("ERROR","Response not equal to SUCCESS");
				return;
				}
	
			if(nb != (2*ns+2)) {
				//Log.e(TAG, "CAPTURE:Expected "+(ns*2+2)+" bytes. Got "+nb+" only\n");
				commandStatus =  INVSIZE;
				return;
				}
			
			for(k=0; k < ns; ++k) ejdata.t1[k] = (float) (0.001 * k * tg);			// Fill Time, microseconds to milliseconds
			for(k=0; k < ns; ++k) ejdata.t2[k] = (float) (0.001 * k * tg + 0.004);			// Fill Time, microseconds to milliseconds
			ejdata.length = ns;
			ejdata.l1=ns;
			ejdata.l2=ns;
			
			for(k=0; k < ns; ++k){							//split interleaved voltage values
				ejdata.ch1[k] = (float) ((buffer[2 + 2*k] & 0xFF) * m8[ch1] + c[ch1]); // Fill voltage. ignore first two bytes.
				ejdata.ch2[k] = (float) ((buffer[2 + 2*k+1] & 0xFF) * m8[ch2] + c[ch2]); // Fill voltage
				}
			} catch (IOException e) {e.printStackTrace(); commandStatus = COMERR;connected = false;}
		commandStatus = SUCCESS;	
		}
	

	public void capture(int ch1, int ch2, int ch3, int ns, int tg){
		if (systemBusy())return; 
		try {
			int ch1and2 = ((ch2 & 0x0f) << 4) | (ch1 & 0x0f);	// ch1 & ch2 packed in 1 byte
			mcp2200.clear();
			sendByte(CAPTURE3);		// the command
			sendByte(ch1and2);			// channel numbers 1 & 2
			sendByte(ch3);			// channel number
			sendInt(ns);			// number of samples
			sendInt(tg);			// time gap between them, in microseconds
			//Log.e(TAG, "CAPTURE3: 1 nd 2 "+ ch1 + " 3  "  + ch3 + "ns " + ns + " tg " + tg + "\n");
			SystemClock.sleep((long) (ns*tg*0.001));
			nb = mcp2200.read(buffer,3*ns+2, timeout); // Response byte + one pad byte + data		
			if (buffer[0] != SUCCESS) {
				commandStatus =  COMERR;
				Log.e(TAG, "Response not equal to SUCCESS " + commandStatus);
				return;
				}
	
			if(nb != (3*ns+2)) {
				//Log.e(TAG, "CAPTURE:Expected "+(ns*3+2)+" bytes. Got "+nb+" only\n");
				commandStatus =  INVSIZE;
				return;
				}
			
			for(k=0; k < ns; ++k) ejdata.t1[k] = (float) (0.001 * k * tg);			// Fill Time, microseconds to milliseconds
			for(k=0; k < ns; ++k) ejdata.t2[k] = (float) (0.001 * k * tg + 0.004);	// Fill Time, microseconds to milliseconds
			for(k=0; k < ns; ++k) ejdata.t3[k] = (float) (0.001 * k * tg + 0.008);	// Fill Time, microseconds to milliseconds
			ejdata.length = ns;
			ejdata.l1=ns;
			ejdata.l2=ns;
			ejdata.l3=ns;
			
			for(k=0; k < ns; ++k){											//split interleaved voltage values
				ejdata.ch1[k] = (float) ((buffer[2 + 3*k] & 0xFF) * m8[ch1] + c[ch1]); 	// Fill voltage. ignore first two bytes.
				ejdata.ch2[k] = (float) ((buffer[2 + 3*k+1] & 0xFF) * m8[ch2] + c[ch2]); 	// Fill voltage
				ejdata.ch3[k] = (float) ((buffer[2 + 3*k+2] & 0xFF) * m8[ch3] + c[ch3]); 	// Fill voltage
				}
			} catch (IOException e) {e.printStackTrace(); commandStatus = COMERR;connected = false;}
		commandStatus = SUCCESS;	
		}
	
	public void capture(int ch1, int ch2, int ch3, int ch4, int ns, int tg){
		if (systemBusy())return; 
		try {
			int ch1and2 = ((ch2 & 0x0f) << 4) | (ch1 & 0x0f);	//	first two channels packed in 1 byte
			int ch3and4 = ((ch4 & 0x0f) << 4) | (ch3 & 0x0f);	// other two channels packed in 1 byte
			mcp2200.clear();
			sendByte(CAPTURE4);		// the command
			sendByte(ch1and2);			// channel number
			sendByte(ch3and4);			// channel number
			sendInt(ns);			// number of samples
			sendInt(tg);			// time gap between them, in microseconds
			//Log.e(TAG, "CAPTURE4: "+ ch1 + " 3 and 4 "  + ch3 + "ns " + ns + " tg " + tg + "\n");
			SystemClock.sleep((long) (ns*tg*0.001));
			nb = mcp2200.read(buffer,4*ns+2, timeout); // Response byte + one pad byte + data		
			if (buffer[0] != SUCCESS) {
				commandStatus =  COMERR;
				Log.e(TAG, "Response not equal to SUCCESS " + commandStatus + "nb = " + nb);
				return;
				}
	
			if(nb != (4*ns+2)) {
				//Log.e(TAG, "CAPTURE:Expected "+(ns*4+2)+" bytes. Got "+nb+" only\n");
				commandStatus =  INVSIZE;
				return;
				}
			
			for(k=0; k < ns; ++k) ejdata.t1[k] = (float) (0.001 * k * tg);			// Fill Time, microseconds to milliseconds
			for(k=0; k < ns; ++k) ejdata.t2[k] = (float) (0.001 * k * tg + 0.004);	// Fill Time, microseconds to milliseconds
			for(k=0; k < ns; ++k) ejdata.t3[k] = (float) (0.001 * k * tg + 0.008);	// Fill Time, microseconds to milliseconds
			for(k=0; k < ns; ++k) ejdata.t4[k] = (float) (0.001 * k * tg + 0.012);	// Fill Time, microseconds to milliseconds
			ejdata.length = ns;
			ejdata.l1=ns;
			ejdata.l2=ns;
			ejdata.l3=ns;
			ejdata.l4=ns;
			
			for(k=0; k < ns; ++k){											//split interleaved voltage values
				ejdata.ch1[k] = (float) ((buffer[2 + 4*k+0] & 0xFF) * m8[ch1] + c[ch1]); 	// Fill voltage. ignore first two bytes.
				ejdata.ch2[k] = (float) ((buffer[2 + 4*k+1] & 0xFF) * m8[ch2] + c[ch2]); 	// Fill voltage
				ejdata.ch3[k] = (float) ((buffer[2 + 4*k+2] & 0xFF) * m8[ch3] + c[ch3]); 	// Fill voltage
				ejdata.ch4[k] = (float) ((buffer[2 + 4*k+3] & 0xFF) * m8[ch4] + c[ch4]); 	// Fill voltage
				}
			} catch (IOException e) {e.printStackTrace(); commandStatus = COMERR;connected = false;}
		commandStatus = SUCCESS;	
		}
	
	
	public void capture_hr(int ch, int ns, int tg){	// Single Channel, 12 bit resolution
		if (systemBusy()) return;
		try {
			mcp2200.clear();
			sendByte(CAPTURE_HR);	// the command
			sendByte(ch);			// channel number
			sendInt(ns);			// number of samples
			sendInt(tg);			// time gap between them, in microseconds*/
			SystemClock.sleep((long) (0.001 * ns * tg ));  	// 
			nb = mcp2200.read(buffer,2*ns+2, timeout); 		// Response byte + one pad byte
			if (buffer[0] != SUCCESS) {
				commandStatus =  COMERR;
				Log.e("ERROR","Response not equal to SUCCESS");
				return;
				}

			ejdata.length = ns;
			ejdata.l1=ns;
			
			if(nb != (2*ns+2)) {
				//Log.e(TAG, "CAPTURE:Expected "+2*ns+2+" bytes. Got "+nb+" only\n");
				commandStatus =  INVSIZE;
				return;
				}
			
			for(k=0; k < ns; ++k) ejdata.t1[k] = (float) (0.001 * k * tg);			// Fill Time, microseconds to milliseconds
			for(k=0; k < ns; ++k) ejdata.ch1[k] = 
					(float) (( (buffer[2 +(2*k)] & 0xFF) | ((buffer[2 + (2*k)+1] & 0xFF)<<8) ) * m12[ch] + c[ch]); // Fill voltage, from 12bit data
		
			} catch (IOException e) {e.printStackTrace(); commandStatus = COMERR;connected = false;}
	commandStatus = SUCCESS;	
	}
	
	
	public void capture_hr(int ch1, int ch2, int ns, int tg){	// Two Channels, 12 bit resolution
		if (systemBusy()) return;
		try {
			mcp2200.clear();
			sendByte(CAPTURE2_HR);	// the command
			sendByte(ch1);			// channel number
			sendByte(ch2);			// channel number
			sendInt(ns);			// number of samples
			sendInt(tg);			// time gap between them, in microseconds*/
			SystemClock.sleep((long) (0.001 * ns * tg ));  	// 
			int expected = 2 + 2 * 2 * ns;
			nb = mcp2200.read(buffer,expected, timeout); 		// Response byte + one pad byte
			if (buffer[0] != SUCCESS) {
				commandStatus =  COMERR;
				Log.e("ERROR","Response not equal to SUCCESS");
				return;
				}

			ejdata.length = ns;
			ejdata.l1=ns;
			ejdata.l2=ns;
			
			
			if(nb != expected) {
				//Log.e(TAG, "CAPTURE:Expected "+ expected +" bytes. Got "+nb+" only\n");
				commandStatus =  INVSIZE;
				return;
				}
			
			for(k=0; k < ns; ++k) ejdata.t1[k] = (float) (0.001 * k * tg);			// Fill Time, microseconds to milliseconds
			for(k=0; k < ns; ++k) ejdata.t2[k] = (float) (0.001 * k * tg + 0.004);			// Fill Time, microseconds to milliseconds
			
			for(k=0; k < ns; ++k) {
				ejdata.ch1[k] = (float) (( (buffer[2+2*(2*k)] & 0xFF)   | ((buffer[2+2*(2*k)+1] & 0xFF)<<8) ) * m12[ch1] + c[ch1]); //error was in [4*k]
				ejdata.ch2[k] = (float) (( (buffer[2+2*(2*k)+2] & 0xFF) | ((buffer[2+2*(2*k)+3] & 0xFF)<<8) ) * m12[ch2] + c[ch2]);
				}
			} catch (IOException e) {e.printStackTrace(); commandStatus = COMERR;connected = false;}
	commandStatus = SUCCESS;	
	}

	public void set_trigval(int chan, double tval) {
		int itval = (int) ( ((tval-c[chan])/m12[chan]) +.05);       // TODO, to be verified
		//Log.e(TAG,  "tval "  + tval + "itval= " + itval + "m12= " +m12[chan] + "c=" + c[chan]);
		if (systemBusy()) return;
		try {
			sendByte(SETTRIGVAL);
			sendInt(itval);
			mcp2200.read(buffer, 1, timeout); 		// Response byte + 2 data bytes
			if (buffer[0] != SUCCESS) {	
				commandStatus =  COMERR;
				return;
				} 
			}catch (IOException e) {
			e.printStackTrace(); commandStatus = COMERR;connected = false;
			}
		commandStatus =  SUCCESS;	
		}	
	
//------------------- Modifiers for Capture , Actions before capturing waveforms
	private final int AANATRIG  = 0;	// Trigger on analog input level
	private final int AWAITHI   = 1;
	private final int AWAITLO	= 2;
	private final int AWAITRISE	= 3;
	private final int AWAITFALL	= 4;
	private final int ASET		= 5;
	private final int ACLR		= 6;
	private final int APULSEHT	= 7;
	private final int APULSELT	= 8;
	
	public void disable_actions() {
		//	Disable all modifiers to the capture call. The capture calls will be set to 
		// do analog triggering on the first channel captured.
		if (systemBusy()) return;
		try {
			sendByte(SETACTION);
			sendByte(AANATRIG);
			sendByte(0);	//Self trigger on channel zero means the first channel captured
			mcp2200.read(buffer, 1, timeout); 		// Response byte + 2 data bytes
			if (buffer[0] != SUCCESS) {	
				commandStatus =  COMERR;
				return;
				} 
			}catch (IOException e) {
			e.printStackTrace(); commandStatus = COMERR;connected = false;
			}
		//Log.e(TAG,  "disable Action: "  + commandStatus);
		commandStatus =  SUCCESS;	
		}	

	public void enable_action(int action, int ch) {
		//	Disable all modifiers to the capture call. The capture calls will be set to 
		// do analog triggering on the first channel captured.
		if (systemBusy()) return;
		try {
			sendByte(SETACTION);
			sendByte(action);
			sendByte(ch);
			//Log.e(TAG,  "Action: "  + action + " chan = " + ch);
			mcp2200.read(buffer, 1, timeout); 		// Response byte + 2 data bytes
			if (buffer[0] != SUCCESS) {	
				commandStatus =  COMERR;
				return;
				} 
			}catch (IOException e) {
			e.printStackTrace(); commandStatus = COMERR;connected = false;
			}
		commandStatus =  SUCCESS;	
		}	

	public void set_pulsewidth(int width) {
		// Sets the 'pulse_width' parameter for pulse2rtime() command. 
		// Also used by usound_time() and the elable_pulse_high/low() functions
		if (systemBusy()) return;
		try {
			sendByte(SETPULWIDTH);
			sendByte(width);
			mcp2200.read(buffer, 1, timeout); 		// Response byte + 2 data bytes
			if (buffer[0] != SUCCESS) {	
				commandStatus =  COMERR;
				return;
				} 
			}catch (IOException e) {
			e.printStackTrace(); commandStatus = COMERR;connected = false;
			}
		commandStatus =  SUCCESS;	
		}	

	public void set_trigsource(int ch){
		//Analog Trigger of the desired channel
		enable_action(AANATRIG, ch);
	}

	public void enable_wait_high(int ch){
		// Wait for a HIGH on the specified 'pin' just before every Capture.
		enable_action(AWAITHI, ch);
	}

	public void enable_wait_low(int ch){
		// Wait for a LOW on the specified 'pin' just before every Capture.
		enable_action(AWAITLO, ch);
	}

	public void enable_wait_rising(int ch){ 
		//Wait for a rising EDGE on the specified 'pin' just before every Capture.
		enable_action(AWAITRISE, ch);
	}

	public void enable_wait_falling(int ch){
		//Wait for a falling EDGE on the specified 'pin' just before every Capture.
		enable_action(AWAITFALL, ch);
	}

	public void enable_set_high(int ch){
		//Sets the specified 'pin' HIGH, just before every Capture.
		enable_action(ASET, ch);
	}

	public void enable_set_low(int ch){
		// Sets the specified 'pin' LOW, just before every Capture.
		enable_action(ACLR, ch);
	}
	
	public void enable_pulse_high(int ch){
		// Generate a HIGH TRUE Pulse on the specified 'pin', just before every Capture.
		// width is specified by the set_pulsewidth() function.
		enable_action(APULSEHT, ch);
	}
	
	public void enable_pulse_low(int ch){
		// Generate a LOW TRUE Pulse on the specified 'pin', just before every Capture.
		enable_action(APULSELT, ch);
	}
	
	
	
//-------------------- For Testing Only --------------------------------------
	public void get_portB() {  // Read PORTB of uC in digital mode
		if (systemBusy()) return;
		try {
			sendByte(GETPORTB);
			mcp2200.read(buffer, 3, timeout); 		// Response byte + 2 data bytes
			if (buffer[0] != SUCCESS) {	
				commandStatus =  COMERR;
				return;
				} 
			}catch (IOException e) {
			e.printStackTrace(); commandStatus = COMERR;connected = false;
			}
		ejdata.idata = ( buffer[1] & 0xff) | ((buffer[2] & 0xff) << 8);
		commandStatus =  SUCCESS;	
		}	

	
	private double fr = 100.0;
	public void doTest() {		
		set_voltage(2.2);
		Log.e(TAG,  "set_voltage: "  + ejdata.ddata);

		get_voltage(1);
		Log.e(TAG,  "get_voltage: "  + ejdata.ddata);

		set_sqr1(fr);  
		Log.e(TAG,  "set_sqr1: "  + ejdata.ddata);
		
		get_frequency(6);
		Log.e(TAG,  "get_frequency: "  + ejdata.ddata);

		set_sqrs(fr, 10);  
		Log.e(TAG,  "set_sqrs: "  + ejdata.ddata);
		
		get_frequency(7);
		Log.e(TAG,  "get_frequency: "  + ejdata.ddata);

		fr = fr  + fr;
		}
	
	
	
	private float x[] = new float[BUFSIZE], y[] = new float[BUFSIZE];
	public double calc_frequency(int chan){  
		int k, count, numNodes = 0, MAXNODES = 10;
		double	nodes[] = new double[MAXNODES];
		double hp, meanval, sum=0.0, diff, node, de;
		
		if(chan == 1)
			for(k = 0; k < ejdata.length; ++k){
				x[k] = ejdata.t1[k];
				y[k] = ejdata.ch1[k];
				sum += y[k];			// calculate the DC offset of the waveform
				}
		else if(chan == 2)
				for(k = 0; k < ejdata.length; ++k){
				x[k] = ejdata.t2[k];
				y[k] = ejdata.ch2[k];
				sum += y[k];			// calculate the DC offset of the waveform				
				}
		else if(chan == 3)
			for(k = 0; k < ejdata.length; ++k){
			x[k] = ejdata.t3[k];
			y[k] = ejdata.ch3[k];
			sum += y[k];			// calculate the DC offset of the waveform				
			}
		else if(chan == 4)
			for(k = 0; k < ejdata.length; ++k){
			x[k] = ejdata.t4[k];
			y[k] = ejdata.ch4[k];
			sum += y[k];			// calculate the DC offset of the waveform				
			}	
		meanval = sum / ejdata.length;
		
		for(k=0; k < ejdata.length-1; ++k)
		if( ( (y[k] <= meanval) && (y[k+1] >= meanval)) || ( (y[k] >= meanval) && (y[k+1] <= meanval) ) ) {
			de = (y[k]-meanval) * (x[k+1]-x[k])/(y[k+1]-y[k]);
			node = x[k]+ Math.abs(de);
			//Log.e("MEAN", "k= "+ k + " "+node/Math.PI+ "node "+node);
			nodes[numNodes++] = (float) node;
			if(numNodes==MAXNODES) break;   // enough nodes
			}
		hp = 0.0;
		count = 0;
		for(k=0; k < numNodes-1; ++k) {
			diff = (nodes[k+1] - nodes[k]);
			if(diff < 0.001) continue;
			//Log.e("node", "diff "+ diff);
			hp += diff;
			++ count;
		}
		hp /= count;
		if(hp == 0) return -1;
		//Log.e("T", "haf period= "+ hp + "  freq= "+500.0/hp);
		return 500.0/hp;
		}

	//-------------CURVE FITTING----------------------------------------
	
		
	
	
	
	//--------------execute command from string----------------------------------------
	public void executeString(String name){
		try {
			//Log.e("EXECUTING",name);
			String[] pieces = name.split("(\\()|(\\))");
			String[] args = pieces[1].split(",");
			Class[] param_classes = new Class[args.length];
			Object[] params = new Object[args.length];
			
			for(int i=0;i<args.length;i++){
				 param_classes[i]	=	int.class; //scr.hasNextDouble() ? double.class :
		                			   //scr.hasNextInt() ? int.class : String.class;
		         int val = toInt(args[i]);
		         if(val==-1){Log.e("ARG ERROR","UGH!");return;}
		         params[i] = val;
		         //Log.e("PAR",val+"");
			 }
			Method meth = this.getClass().getDeclaredMethod(pieces[0],param_classes);
			meth.invoke(this,params);
			
			} catch (SecurityException e) {
			  // ...
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
			  // ...
				e.printStackTrace();
			}
		catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	private int toInt(String arg) {
		if (arg == null || arg.isEmpty()) {
			return -1;
		} else {
			return Integer.parseInt(arg);
		}
	}
	
}
