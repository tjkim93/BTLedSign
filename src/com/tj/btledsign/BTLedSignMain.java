package com.tj.btledsign;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import com.tj.btledsign.ColorPickerDialog.OnColorChangedListener;
import com.tj.btledsign.R;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;

public class BTLedSignMain extends Activity implements OnColorChangedListener {
	private final static String TAG = "BTLedSign";

	// Definition of the one requestCode we use for receiving results.
    static final private int GET_CODE = 0;
    static final private int GET_OPEN = 1;
    static final private int GET_SAVE = 2;
	
	private HashMap< String, String > mFontList;
	private ArrayAdapter<String> mFontListAdapter;
	private Spinner mFontSpinner;
	private LedSignView mBannerView;
	private int mIntZoomFont  = 16;
	private int mFontSelIdx = 0;
	private boolean mBoolPlay = false;

	private int mIntPlaySpeed   = 8;
	private boolean mBTConnected = false;
	private BTLedSignApp mApp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mApp = (BTLedSignApp)getApplication();
		setContentView(R.layout.btledsign_main);
	    mBannerView = (LedSignView)findViewById(R.id.ViewBanner);
	    mBannerView.setApplication(mApp);
	    
	    SeekBar speedBar = (SeekBar)findViewById(R.id.seekBarSpeed);
	    if (speedBar != null) {
		    speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
	
	            @Override
	            public void onStopTrackingTouch(SeekBar seekBar)
	            {
	            	if (mBoolPlay) {
	        			if (getResources().getConfiguration().orientation == 
	        					Configuration.ORIENTATION_LANDSCAPE) {
	        				getWindow().getDecorView()
	        		           .setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
	        			}	            		
	            	}
	            }
	
	            @Override
	            public void onStartTrackingTouch(SeekBar seekBar)
	            {
	            }
	
	            @Override
	            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
	            {
	            	mIntPlaySpeed = progress;
	            	mBannerView.setPlaySpeed(mIntPlaySpeed);
	            	Log.d(TAG, "H3 " + mIntPlaySpeed);
	            	
	            	if (mBTConnected) {
		            	byte[] bufSpeed = new byte[1];
		                bufSpeed[0] = (byte)(9 - mIntPlaySpeed);
		                String str = String.format("VAL : %d", bufSpeed[0]);
		                Log.d(TAG, str);
		        		mApp.sendData(BTLedSignApp.IOCTL_LED_PLAY, bufSpeed, 20);
	            	}
	            }
	        });
		    speedBar.setProgress(0);
	    }
    
	    // Font List
		mFontList = FontManager.enumerateFonts();
		
		if (mFontList != null) {
			Iterator<String> s = mFontList.keySet().iterator();
			mFontSpinner = (Spinner) findViewById(R.id.SpinnerFont);
			if (mFontSpinner != null) {
				//mFontSelIdx = mFontSpinner.getSelectedItemPosition();
				mFontListAdapter = new ArrayAdapter<String>(this, R.layout.font_list);
				if (mFontListAdapter != null) {
					mFontSpinner.setAdapter(mFontListAdapter);
					String strFontFile;
					while (s.hasNext()) {
						strFontFile = s.next();
						//Log.d("TEST", ">>>" + strFontFile + " : " + mFontList.get(strFontFile));
						mFontListAdapter.add(strFontFile);
					}
					mFontListAdapter.sort(null);
					mFontSpinner.setOnItemSelectedListener(
						new OnItemSelectedListener() {
							public void onItemSelected(
							        AdapterView<?> parent, View view, int position, long id) {
								if (mFontSelIdx != position) {
									String strFontName;
									strFontName = (String)mFontSpinner.getItemAtPosition(position);
									mFontSelIdx = position; 
									Log.d("TEST", "SEL >>>" + strFontName);
									mApp.getLedSignBitmap().setFontName(strFontName);
									mBannerView.invalidate();
								}
							}
		
							public void onNothingSelected(AdapterView<?> parent) {
							}
						});
				}
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		// Save UI state changes to the savedInstanceState.
		// This bundle will be passed to onCreate if the process is
		// killed and restarted.
		savedInstanceState.putInt("font_idx", mFontSelIdx);
		savedInstanceState.putInt("font_size", mIntZoomFont);
		savedInstanceState.putInt("play_speed", mIntPlaySpeed);
		savedInstanceState.putBoolean("bt_connected", mBTConnected);

		Log.d("TEST", "H5 " + mIntPlaySpeed);
		
		mBannerView.saveInstanceState(savedInstanceState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	  	// Restore UI state from the savedInstanceState.
	  	// This bundle has also been passed to onCreate.
	  	mFontSelIdx  = savedInstanceState.getInt("font_idx");
	  	mIntZoomFont  = savedInstanceState.getInt("font_size");
	  	mIntPlaySpeed = savedInstanceState.getInt("play_speed");
	  	mBannerView.restoreInstanceState(savedInstanceState);
	  	mBTConnected = savedInstanceState.getBoolean("bt_connected");
	  	Log.d("TEST", "H4 " + mIntPlaySpeed);
	}
	
	public void updateLedSignInfo(boolean updated) {
		TextView textInfo = (TextView)findViewById(R.id.TextViewPhyRes);
		if (textInfo != null) {
			String strInfo = String.format(Locale.getDefault(), "%d x %d",
				mApp.getLedSignBitmap().getXRes(), mApp.getLedSignBitmap().getYRes());
			textInfo.setText(strInfo);
		}
		
		textInfo = (TextView)findViewById(R.id.TextViewVirRes);
		if (textInfo != null) {
			String strInfo = String.format(Locale.getDefault(), "%d x %d",
				mApp.getLedSignBitmap().getXResVirtual(), mApp.getLedSignBitmap().getYResVirtual());
			textInfo.setText(strInfo);
		}
		
		textInfo = (TextView)findViewById(R.id.TextViewBPP);
		if (textInfo != null) {
			String strInfo = String.format(Locale.getDefault(), "%d (%d)",
					mApp.getLedSignBitmap().getColorCount(), mApp.getLedSignBitmap().getBPP());
			textInfo.setText(strInfo);
		}

		if (updated)
			mBannerView.invalidate();
	}
	
    @Override
    public synchronized void onResume() {
    	super.onResume();

        String str = mApp.getLedSignBitmap().getBanner();
  	  	EditText editBanner = (EditText)findViewById(R.id.editTextBanner);
  	  	if (editBanner != null && str != null) {
  	  		editBanner.setText(str.toCharArray(), 0, str.length());
  	  	}
  	  	
  	  	if (mFontSpinner != null) {
        	mFontSpinner.setSelection(mFontSelIdx);
  	  	}
  	  	
  	  	updateLedSignInfo(false);
  
		CheckBox checkVScrollLock = (CheckBox)findViewById(R.id.checkBoxVScroll);
		if (checkVScrollLock != null) {
			boolean check = mBannerView.getVScroll();
			checkVScrollLock.setChecked(check);
		}

	    int nColor = mApp.getLedSignBitmap().getDefaultColor();
	    mBannerView.setSelColor(mApp.getLedSignBitmap().getColorCount() - 1, nColor);

	    int pos = mApp.getLedSignBitmap().getColorCount() - 1;
		ImageButton btn = (ImageButton)findViewById(R.id.buttonColor);
		if (btn != null) {
			Bitmap bm = ColorPickerAdapter.getColorBitmap(pos, 80, 0, nColor, 
					mApp.getLedSignBitmap().getColorCount(), false); 
			btn.setImageBitmap(Bitmap.createScaledBitmap(
					bm, 64, 64, true));
		}
	    
	    SeekBar speedBar = (SeekBar)findViewById(R.id.seekBarSpeed);
	    if (speedBar != null) {
	    	speedBar.setProgress(mIntPlaySpeed);
	    }
	    
	    changeSyncButtonState(mBTConnected);
	    
	    mBannerView.invalidate();
    }

    @Override
    public synchronized void onPause() {
    	super.onPause();
    	Log.d(TAG, "onPause !!!");
    }
    
    @Override
    public synchronized void onStop() {
    	super.onStop();
    	if (mBoolPlay)
    		mBannerView.stopPlay();
    	Log.d(TAG, "onStop !!!");
    }
    
    @Override
    public synchronized void onDestroy() {
    	super.onDestroy();
    	mApp.shutDownBTService();
    	Log.d(TAG, "onDestroy !!!");
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		String str;
		
		str = String.format("sel : %d", item.getItemId());
		Log.d(TAG, str);
        switch (item.getItemId()) {
        case R.id.action_settings:
        	Intent intent = new Intent(this, BTConSetting.class);
    		startActivityForResult(intent, GET_CODE);
            break;
 
        default:
            break;
        }
 
        return super.onOptionsItemSelected(item);
    }
	
	public void OnButtonDone(View v) { 
		EditText editBanner = (EditText)findViewById(R.id.editTextBanner);
		String str = editBanner.getText().toString();
		mApp.getLedSignBitmap().setBanner(str);
		InputMethodManager imm = (InputMethodManager)getSystemService(
			      Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(editBanner.getWindowToken(), 0);
		mBannerView.invalidate();
	}

	@Override
	public void colorChanged(int pos, int color) {
		ImageButton btn = (ImageButton)findViewById(R.id.buttonColor);
		
		int btnColor = 0;
		if (pos >= mApp.getLedSignBitmap().getColorCount())
			btnColor = mBannerView.getSelColor();
		
		Bitmap bm = ColorPickerAdapter.getColorBitmap(pos, 80, btnColor, 
				color, mApp.getLedSignBitmap().getColorCount(), false); 
		btn.setImageBitmap(Bitmap.createScaledBitmap(
				bm, 64, 64, true));
		
		Log.d(TAG, "color " + pos);
		if (mBannerView != null)
			mBannerView.setSelColor(pos, color);
	}
	
	public void onClickColorPickerDialog(View v) {
		ColorPickerDialog dialog;
		dialog = new ColorPickerDialog(this, this, 
				mApp.getLedSignBitmap().getColorTable(), 
				mApp.getLedSignBitmap().getColorCount(),
				mBannerView.getSelColor());
		dialog.show();
	}
	
	public void onClickVScrollLock(View v) {
		CheckBox checkVScrollLock = (CheckBox)findViewById(R.id.checkBoxVScroll);
		if (checkVScrollLock != null) {
			mBannerView.lockVScroll(checkVScrollLock.isChecked());
		}
	}
	
	public void onClickPlay(View v) {
		Button btn = (Button)findViewById(R.id.buttonPlay);
		if (!mBoolPlay) {
			if (getResources().getConfiguration().orientation == 
					Configuration.ORIENTATION_LANDSCAPE) {
				getWindow().getDecorView()
		           .setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
			}
			mBannerView.startPlay();
			btn.setText(R.string.strStop);
			mBoolPlay = true;
		} else {
			mBannerView.stopPlay();
			btn.setText(R.string.strPlay);
			mBoolPlay = false;
		}
	}

	public void onClickZoomPlus(View v) {
		if (mIntZoomFont < 50)
			mIntZoomFont++;
		if (mBannerView != null) {
			mApp.getLedSignBitmap().setFontSize(mIntZoomFont);
			mBannerView.invalidate();
		}
	}
	
	public void onClickZoomMinus(View v) {
		if (mIntZoomFont > 4)
			mIntZoomFont--;
		if (mBannerView != null) {
			mApp.getLedSignBitmap().setFontSize(mIntZoomFont);
			mBannerView.invalidate();
		}
	}
	
	public void changeSyncButtonState(boolean enable) {
		ImageButton btn = (ImageButton)findViewById(R.id.buttonSync);
		if (btn != null)
			btn.setEnabled(enable);
	}

	public void onClickFile(View v) {
		Intent intent = new Intent(this, BTLedSignFile.class);
		
		Bundle b = new Bundle();
		b.putInt("key", 0);
		intent.putExtras(b);
		startActivityForResult(intent, GET_OPEN);
	}
	
	public void onClickSave(View v) {
		Intent intent = new Intent(this, BTLedSignFile.class);
		
		Bundle b = new Bundle();
		b.putInt("key", 1);
		intent.putExtras(b);
		startActivityForResult(intent, GET_SAVE);
	}
	
	private String mStrDeviceName;
	
	private void handleConnect(Message msg) {
    	int cmd = 0;
    	int nCtr = 0;
    	ByteBuffer bb;
    	
    	do {
    		nCtr++;
    		mApp.sendData(BTLedSignApp.IOCTL_LED_GET_INFO, null, 0);
    		byte[] buf = mApp.getBTService().readData(11, 1000); 
    		
    	    bb = ByteBuffer.wrap(buf);
    	    bb.order(ByteOrder.LITTLE_ENDIAN);
    	    
    	    cmd = bb.getShort();
        	Log.d(TAG, BTLedSignApp.bytesToHex(buf));
            
        	String str = String.format("rx cmd : %x", cmd);
            Log.d(TAG, str);
    	} while (nCtr < 5 && cmd != BTLedSignApp.IOCTL_LED_GET_INFO);

    	if (nCtr == 5 && cmd != BTLedSignApp.IOCTL_LED_GET_INFO) {
    		mApp.getBTService().stop();
        	String str = getString(R.string.unable_connect);
        	if (mStrDeviceName != null)
        		str = str + mStrDeviceName;
        	Toast.makeText(getApplicationContext(),
                    str,
                    Toast.LENGTH_SHORT).show();
    		return;
    	}

    	mApp.sendData(BTLedSignApp.IOCTL_LED_SET_CONNECT, null, 0);
    	
    	int xres, yres, xres_virtual, yres_virtual, bpp;
    	
    	xres = bb.getShort();
    	yres = bb.getShort();
    	xres_virtual = bb.getShort();
    	yres_virtual = bb.getShort();
    	bpp  = bb.get();
    	mApp.getLedSignBitmap().setInfo(xres, yres, xres_virtual, yres_virtual, bpp);
    	mBannerView.updateViewInfo();
    	mBTConnected = true;
    	changeSyncButtonState(true);
    	
    	String str = getString(R.string.title_connected_to);
    	if (mStrDeviceName != null)
    		str = str + mStrDeviceName;
    	Toast.makeText(getApplicationContext(),
                str,
                Toast.LENGTH_SHORT).show();
	}


    // The Handler that gets information back from the BluetoothChatService
    private final Handler mBTHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BTLedSignApp.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BTSerialService.STATE_CONNECTED:
                	handleConnect(msg);
                    break;
                    
                case BTSerialService.STATE_CONNECTING:
                	String str = getString(R.string.title_connecting);
                	if (mStrDeviceName != null)
                		str = str + mStrDeviceName;
                	
                    Toast.makeText(
                            getApplicationContext(),
                            str,
                            Toast.LENGTH_SHORT).show();                    
                    break;
                    
                case BTSerialService.STATE_LISTEN:
                case BTSerialService.STATE_NONE:
                    break;
                }
                break;

            case BTLedSignApp.MESSAGE_DEVICE_NAME:
                // save the connected device's name
            	mStrDeviceName = msg.getData().getString(BTLedSignApp.DEVICE_NAME);
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.connected_to) + mStrDeviceName,
                        Toast.LENGTH_SHORT).show();
                break;
                
            case BTLedSignApp.MESSAGE_TOAST:
                if (msg.getData().getString(BTLedSignApp.TOAST).equals("unable connect")) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.unable_connect),
                            Toast.LENGTH_SHORT).show();
                } else if (msg.getData().getString(BTLedSignApp.TOAST)
                        .equals("connection lost")) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.connection_lost),
                            Toast.LENGTH_SHORT).show();
                    mBTConnected = false;
                    changeSyncButtonState(false);
                } else {
                    Toast.makeText(getApplicationContext(),
                            msg.getData().getString(BTLedSignApp.TOAST), Toast.LENGTH_SHORT)
                            .show();
                }
                break;
                
            case BTLedSignApp.MESSAGE_READ:
                //byte[] readBuf = (byte[]) msg.obj;
                
                break;
            }
        }
    };
    
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.e(TAG, "Activity result" + requestCode + ", result code" + resultCode);
    	switch (requestCode) {
    		case GET_CODE:
		    	if (data != null) {
		    		String strBTMac = data.getAction();
		    	    Log.e(TAG, "RESULT " + strBTMac);
		    	    mApp.connectBT(this, mBTHandler, strBTMac);
		    	}
		    	break;
		    	
    		case GET_OPEN:
    			CheckBox checkVScrollLock = (CheckBox)findViewById(R.id.checkBoxVScroll);
    			if (checkVScrollLock != null) {
    				checkVScrollLock.setChecked(true);
    			}
    			mBannerView.lockVScroll(true);
    			break;

    		case GET_SAVE:
    			break;
    	}
    }
    
	public void onClickBT(View v) {
		Intent intent = new Intent(this, BTConSetting.class);
		startActivityForResult(intent, GET_CODE);
	}	
	
	public void onClickSync(View v) {
		if (!mBTConnected)
			return;
		
		byte[] bufDisp = mApp.getLedSignBitmap().getDispBuf();
		byte[] bufAttr = mApp.getLedSignBitmap().getAttrBuf();
		byte[] buf = new byte[bufDisp.length + bufAttr.length];
		
		System.arraycopy(bufDisp, 0, buf, 0, bufDisp.length);
		System.arraycopy(bufAttr, 0, buf, bufDisp.length, bufAttr.length);
		mApp.sendData(BTLedSignApp.IOCTL_LED_SET_DATA, buf, 300);
		
        try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        byte[] bufSpeed = new byte[1];
        bufSpeed[0] = (byte)(9 - mIntPlaySpeed);
		mApp.sendData(BTLedSignApp.IOCTL_LED_PLAY, bufSpeed, 0);
	}
}

