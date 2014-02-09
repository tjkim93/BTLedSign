package com.tj.btledsign;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

public class BTLedSignApp extends Application {
	public String TAG = "BTLedSign";
	
	private static String STICKMODE = "STICKMODE";
	public int m_nStickMode;
	
	private static String MACADDERSS = "MACADDERSS";
	public String m_strMacAddress = "";
	
	private static String REVTHROTTLE = "REVTHROTTLE";
	public boolean m_bRevThrottle = false;

	private static String REVAILERON = "REVAILERON";
	public boolean m_bRevAileron = false;
	
	private static String REVELEVATOR = "REVELEVATOR";
	public boolean m_bRevElevator = false;
	
	private static String REVRUDDER = "REVRUDDER";
	public boolean m_bRevRudder = false;
	
	private static String TRIM_THR = "TRIM_THR";
	public int  m_nTrimThrottle = 0;

	private static String TRIM_AIL = "TRIM_AIL";
	public int  m_nTrimAileron  = 127;
	
	private static String TRIM_ELE = "TRIM_ELE";
	public int  m_nTrimElevator = 127;
	
	private static String TRIM_RUD = "TRIM_RUD";
	public int	m_nTrimRudder   = 127;
	
	private static String SENSOR_SENSITIVITY = "SENSOR_SENSITIVITY";
	public int	m_nSensitivity   = 0;
	
	
	private SharedPreferences m_spBTCon;
	private Editor m_editorBTCon;
	
	
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
	
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    
	private BTSerialService mBTService = null;
	private String mStrBTMac;
	private LedSignBitmap mLedSign = new LedSignBitmap(16 * 5, 16, 16 * 20, 16, 2);
	
    public void connectBT(Context context, Handler handler, String strBTMac) {
        if (mBTService == null)
        {
            // Initialize the BluetoothChatService to perform bluetooth connections
            mBTService = new BTSerialService(context, handler);
            mBTService.setIndexOfMessages(MESSAGE_STATE_CHANGE, 
            		MESSAGE_READ, 
            		MESSAGE_DEVICE_NAME, 
            		MESSAGE_TOAST);
            mBTService.setDeviceNameString(DEVICE_NAME);
            mBTService.setToastString(TOAST);
        }
        
        if (strBTMac != null) {
        	mStrBTMac = strBTMac;
	        BluetoothDevice device =  BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mStrBTMac);
	        // Attempt to connect to the device
	        mBTService.connect(device);
        }
    }
    
    public LedSignBitmap getLedSignBitmap() {
    	return mLedSign;
    }
    
    public BTSerialService getBTService() {
    	return mBTService;
    }
    
    public void shutDownBTService() {
    	if (mBTService != null)
    		mBTService.stop();
    }
    
	@Override
	public void onCreate() {
		super.onCreate();

		m_spBTCon = PreferenceManager.getDefaultSharedPreferences(this);
		m_editorBTCon = m_spBTCon.edit();
//		ReadSettings();
	}

/*	
	public void ReadSettings() {
		m_nStickMode    = m_spBTCon.getInt(STICKMODE, 0);
		m_strMacAddress = m_spBTCon.getString(MACADDERSS, "");
		m_bRevThrottle  = m_spBTCon.getBoolean(REVTHROTTLE, false);
		m_bRevAileron   = m_spBTCon.getBoolean(REVAILERON, false);
		m_bRevElevator  = m_spBTCon.getBoolean(REVELEVATOR, false);
		m_bRevRudder    = m_spBTCon.getBoolean(REVRUDDER, false);
		m_nTrimThrottle = m_spBTCon.getInt(TRIM_THR, 0);
		m_nTrimAileron  = m_spBTCon.getInt(TRIM_AIL, 127);
		m_nTrimElevator = m_spBTCon.getInt(TRIM_ELE, 127);
		m_nTrimRudder   = m_spBTCon.getInt(TRIM_RUD, 127);
		m_nSensitivity  = m_spBTCon.getInt(SENSOR_SENSITIVITY, 0);
	}

	public void SaveSettings() {
		m_editorBTCon.putInt(STICKMODE, m_nStickMode);
		m_editorBTCon.putString(MACADDERSS, m_strMacAddress);
		m_editorBTCon.putBoolean(REVTHROTTLE, m_bRevThrottle);
		m_editorBTCon.putBoolean(REVAILERON, m_bRevAileron);
		m_editorBTCon.putBoolean(REVELEVATOR, m_bRevElevator);
		m_editorBTCon.putBoolean(REVRUDDER, m_bRevRudder);
		m_editorBTCon.putInt(TRIM_THR, m_nTrimThrottle);
		m_editorBTCon.putInt(TRIM_AIL, m_nTrimAileron);
		m_editorBTCon.putInt(TRIM_ELE, m_nTrimElevator);
		m_editorBTCon.putInt(TRIM_RUD, m_nTrimRudder);
		m_editorBTCon.putInt(SENSOR_SENSITIVITY, m_nSensitivity);
		m_editorBTCon.commit();
	}
*/
	
	@Override
	public void onTerminate() {
		super.onTerminate();
	}
	
	private final static int _IOC_WRITE = 1;
	private final static int _IOC_READ = 0;

	private static int _IOC(int dir, int nr, int size) {
		int cmd;
		
		cmd =   (((dir)            << 15) |
	            (((size) & 0x7ff) << 4) |
	            (((nr) & 0x0f)    << 0));

		return cmd;
	}

	private static int _IOR(int nr, int size) {
		return _IOC(_IOC_READ,nr, size);
	}

	private static int _IOW(int nr, int size) {
		return _IOC(_IOC_WRITE,nr, size);
	}

	public final static int IOCTL_LED_GET_INFO     = _IOR(1, 11);
	public final static int IOCTL_LED_SET_CONNECT  = _IOW(2, 0);
	public final static int IOCTL_LED_SET_DATA     = _IOW(3, 2);
	public final static int IOCTL_LED_PLAY         = _IOW(4, 2);
	public final static int IOCTL_LED_STOP         = _IOW(5, 2);	
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 3];
	    int v;
	    for ( int j = 0; j < bytes.length; j++ ) {
	        v = bytes[j] & 0xFF;
	        hexChars[j * 3] = hexArray[v >> 4];
	        hexChars[j * 3 + 1] = hexArray[v & 0x0F];
	        hexChars[j * 3 + 2] = ' ';
	    }
	    return new String(hexChars);
	}
   
    public void sendData(int wCmd, byte[] data, int delay) {
        byte[] cmds = new byte[2];

        cmds[0] = (byte)(wCmd & 0xff);
        cmds[1] = (byte)((wCmd >> 8) & 0xff); 
        getBTService().write(cmds);
        
        String str = String.format("cmd : %x", wCmd);
        Log.d(TAG, str);

        if (data != null){
        	if (delay > 0) {
	            try {
	    			Thread.sleep(delay);
	    		} catch (InterruptedException e) {
	    			// TODO Auto-generated catch block
	    			e.printStackTrace();
	    		}
        	}
            getBTService().write(data);
        }
    }
}
