package explib;

import java.io.IOException;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.SystemClock;
import android.util.Log;


public class devhandler{
    private final String TAG = "usb device";

    private UsbInterface mControlInterface;
    private UsbInterface mDataInterface;

    private UsbEndpoint mControlEndpoint;
    private UsbEndpoint mReadEndpoint;
    private UsbEndpoint mWriteEndpoint;
    

    private boolean mRts = false;
    private boolean mDtr = false;

    private static final int USB_RECIP_INTERFACE = 0x01;
    private static final int USB_RT_ACM = UsbConstants.USB_TYPE_CLASS | USB_RECIP_INTERFACE;

	public static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
    public static final int DEFAULT_WRITE_BUFFER_SIZE = 16 * 1024;

    public UsbDevice mDevice;
    
    protected final Object mReadBufferLock = new Object();
    protected final Object mWriteBufferLock = new Object();

    /** Internal read buffer.  Guarded by {@link #mReadBufferLock}. */
    protected byte[] mReadBuffer;

    /** Internal write buffer.  Guarded by {@link #mWriteBufferLock}. */
    protected byte[] mWriteBuffer;

	private UsbDeviceConnection mConnection;

    public boolean device_found = false;
    private UsbManager usbman;
    public devhandler(UsbManager mUsbManager) {
		usbman = mUsbManager;
    	Log.d(TAG,"Attempting to look for an expeyes device");
    	mDevice = null; //initialize mDevice
		
        for (final UsbDevice device : mUsbManager.getDeviceList().values()) {
        	Log.d("Devices",device.getVendorId()+":"+device.getProductId());
        	if(device.getVendorId()==1240 && device.getProductId()==223){
        		Log.d("Success","great success!!  Found a connected device");
        		mDevice = device;
        		device_found = true;
        	}

        }

        mReadBuffer = new byte[DEFAULT_READ_BUFFER_SIZE];
        mWriteBuffer = new byte[DEFAULT_WRITE_BUFFER_SIZE];
    }

	public void open() throws IOException{
		if(device_found == false){
			throw new IOException("DEVICE NOT CONNECTED");
			}
        
		mConnection = usbman.openDevice(mDevice);
		Log.d("Success","more success!!");
		
		Log.d(TAG, "Claiming control interface."+mDevice.getInterfaceCount());
        mControlInterface = mDevice.getInterface(0);

       Log.d(TAG, "Control iface=" + mControlInterface);
       // class should be USB_CLASS_COMM

       if (!mConnection.claimInterface(mControlInterface, true)) {
           throw new IOException("Could not claim control interface.");
       }
       mControlEndpoint = mControlInterface.getEndpoint(0);
       Log.d(TAG, "Control endpoint direction: " + mControlEndpoint.getDirection());

       Log.d(TAG, "Claiming data interface.");
       mDataInterface = mDevice.getInterface(1);
       Log.d(TAG, "data iface=" + mDataInterface);
       // class should be USB_CLASS_CDC_DATA

       if (!mConnection.claimInterface(mDataInterface, true)) {
           throw new IOException("Could not claim data interface.");
       }
       mReadEndpoint = mDataInterface.getEndpoint(1);
       Log.d(TAG, "Read endpoint direction: " + mReadEndpoint.getDirection());
       mWriteEndpoint = mDataInterface.getEndpoint(0);
       Log.d(TAG, "Write endpoint direction: " + mWriteEndpoint.getDirection());

	}

    private int sendAcmControlMessage(int request, int value, byte[] buf) {
    	
        return mConnection.controlTransfer(
                USB_RT_ACM, request, value, 0, buf, buf != null ? buf.length : 0, 5000);
    }
    
    public void close() throws IOException {
    	mConnection.releaseInterface(mDataInterface);
    	mConnection.releaseInterface(mControlInterface);
        mConnection.close();
    }

    public void clear(){
    	mConnection.bulkTransfer(mReadEndpoint, mReadBuffer, 100, 50);
    }
    
    
    public int read(byte[] dest,int bytes_to_be_read, int timeoutMillis) throws IOException {
        int numBytesRead=0;
        synchronized (mReadBufferLock) {
            int  readnow=0;
            //Log.d(TAG, "Readings bytes: " + bytes_to_be_read);
            while(numBytesRead<bytes_to_be_read){
            	readnow = mConnection.bulkTransfer(mReadEndpoint, mReadBuffer, bytes_to_be_read,
                    timeoutMillis);
            	
                //Log.d("READ", "Read bytes: " + readnow + "counter " + counter);
            	if (readnow < 0) {
            		Log.e(TAG, "Read Error: " + numBytesRead);

            		return numBytesRead;
            		
            	}
            	else{
            		//Log.d(TAG, "Read packets: "+readnow + mReadBuffer);
            		System.arraycopy(mReadBuffer, 0, dest, numBytesRead, readnow);
            		numBytesRead += readnow;
            		}
            }
            
        }
        //Log.d("READ", "Read bytes: " + numBytesRead);
        return numBytesRead;
    }
        
    

    public int write(byte[] src, int timeoutMillis) throws IOException {
        int written = 0;

        while (written < src.length) {
            final int writeLength;
            final int amtWritten;

            synchronized (mWriteBufferLock) {
                final byte[] writeBuffer;

                writeLength = Math.min(src.length - written, mWriteBuffer.length);
                if (written == 0) {
                    writeBuffer = src;
                } else {
                    // bulkTransfer does not support offsets, make a copy.
                    System.arraycopy(src, written, mWriteBuffer, 0, writeLength);
                    writeBuffer = mWriteBuffer;
                }

                amtWritten = mConnection.bulkTransfer(mWriteEndpoint, writeBuffer, writeLength,
                        timeoutMillis);
            }
            if (amtWritten <= 0) {
                throw new IOException("Error writing " + writeLength
                        + " bytes at offset " + written + " length=" + src.length);
            }

            //Log.d(TAG, "Wrote amt=" + amtWritten + " attempted=" + writeLength);
            written += amtWritten;
        }
        return written;
    }

    public void setBaud(int baudRate) {
                byte[] msg = {
                (byte) ( baudRate & 0xff),
                (byte) ((baudRate >> 8 ) & 0xff),
                (byte) ((baudRate >> 16) & 0xff),
                (byte) ((baudRate >> 24) & 0xff),
                (byte) 0,
                (byte) 0,
                (byte) 8};
        sendAcmControlMessage(0x20, 0, msg);
        SystemClock.sleep(100);
        clear();
    }

    public void setDtrRts() {
        int value = (mRts ? 0x2 : 0) | (mDtr ? 0x1 : 0);
        sendAcmControlMessage(0x22, value, null); // Control line state set
    }

	
}

