package explib;

public class CaptureConstants {
	public int NS,TG;
	public void CaptureConstants(){
		NS=300;
		TG=4;
	}
	public int getNS(){return NS;}
	public int getTG(){return TG;}
	public boolean setNS(int num){if(num<1800){NS=num;return true;}else{return false;}}
	public boolean setTG(int gap){if(gap>=4 && gap<1000){TG=gap;return true;}else{return false;}}
	
}
