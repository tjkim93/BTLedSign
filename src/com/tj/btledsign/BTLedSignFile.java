package com.tj.btledsign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BTLedSignFile extends Activity {
	private final static String TAG = "BTLedSign";
	private BTLedSignApp mApp;
	private boolean mOpenMode = true;
	FileItemAdapter mFileAdapter = null;
	
	public class FileItem {
	    public String  mStrName;
	    public Bitmap  mBitmap;
	    public int     mBPP;
	    public boolean mChecked;
	    public Date    mModDate;

	    public FileItem(String name, Bitmap bitmap, int bpp, Date date) {
	    	mStrName = name;
	    	mBitmap  = bitmap;
	    	mBPP     = bpp;
	    	mModDate = date;
	    	mChecked = false;
	    }
	}

	public class FileItemAdapter extends BaseAdapter {
		private Context context;
	    private final int rowResourceId;
	    private ArrayList <FileItem> data;

	    public FileItemAdapter(Context context, int textViewResourceId, ArrayList <FileItem> d) {
	        //super(context, textViewResourceId, objects);

	        this.context = context;
	        this.data = d;
	        this.rowResourceId = textViewResourceId;
	    }

	    @Override
	    public int getCount() {
	        return data.size();
	    }
	 
	    @Override
	    public Object getItem(int position) {
	        return data.get(position);
	    }
	    
	    public Object removeItem(int position) {
	    	return data.remove(position);
	    }
	    
		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}	    
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	        View rowView = inflater.inflate(rowResourceId, parent, false);
	        ImageView imageView = (ImageView) rowView.findViewById(R.id.imageView);
	        TextView textView = (TextView) rowView.findViewById(R.id.textFile);
	        TextView textVir = (TextView) rowView.findViewById(R.id.textVir);
	        TextView textBPP = (TextView) rowView.findViewById(R.id.textBPP);
	        
	        FileItem item = data.get(position);

	        Calendar calendar = GregorianCalendar.getInstance();
	        calendar.setTime(item.mModDate); 
	        String strTime = String.format("%2d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
	        textView.setText(item.mModDate.toString() + strTime);
	        
	        String str = String.format("%dx%d", item.mBitmap.getWidth(), item.mBitmap.getHeight());
	        textVir.setText(str);
	        str = String.format("%d bpp", item.mBPP);
	        textBPP.setText(str);
			imageView.setImageBitmap(item.mBitmap);
	        if (item.mChecked) {
	        	rowView.setBackgroundColor(getResources().getColor(R.color.background));
	        }			

	        return rowView;
	    }
	}
	
	private ArrayList<FileItem> buildFileList(Context context) {
		ArrayList<FileItem> items;
		
		items = new ArrayList<FileItem>();
		
		File files[] = context.getFilesDir().listFiles();
		Log.d(TAG, "Size: "+ files.length);
		
		int width = 0, height = 0, total, bpp = 0;
        byte[] szSigBuf = new byte[4];
        final byte[] szSig = new byte[]{'B', 'T', 'L', 'E' };
        Log.d(TAG, BTLedSignApp.bytesToHex(szSig));

		for (int i=0; i < files.length; i++)
		{
		    Log.d(TAG, "FileName:" + files[i].getName());
		    
			String strFile = getFilesDir().getPath() + "/" + files[i].getName();
			File file = new File(strFile);
			Date lastModDate = new Date(file.lastModified());
		    
	        FileInputStream inputStream;
	        ByteBuffer bufBitmap = null;
	        ByteBuffer bb = ByteBuffer.allocate(4);
       
			try {
				inputStream = openFileInput(files[i].getName());
				
				// signature
				inputStream.read(szSigBuf);
				//Log.d(TAG, BTLedSignApp.bytesToHex(szSigBuf));

				if (Arrays.equals(szSigBuf, szSig)) {
					// vir width
					inputStream.read(bb.array());
					width = bb.getInt();
					bb.clear();
					// vir height
					inputStream.read(bb.array());
					height  = bb.getInt();
					bb.clear();
					// bpp
					inputStream.read(bb.array());
					bpp  = bb.getInt();
					bb.clear();
					// total
					inputStream.read(bb.array());
					total = bb.getInt();
					bb.clear();
					// bitmap size
					bufBitmap = ByteBuffer.allocate(total);
					// bitmap data
					inputStream.read(bufBitmap.array());
					inputStream.close();
					//String str = String.format("%dx%d %d", width, height, total);
					//Log.d(TAG, str);
				}
			} catch (Exception e) {
			  e.printStackTrace();
			}

			Bitmap bitmap = null;
			if (bufBitmap != null) {
				bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				bitmap.copyPixelsFromBuffer(bufBitmap);
			}
		    items.add(new FileItem(files[i].getName(), bitmap, bpp, lastModDate));
		}

		Comparator<FileItem> comperator = new Comparator<FileItem>() {
			@Override
			public int compare(FileItem object1, FileItem object2) {
				return object1.mModDate.compareTo(object2.mModDate);
			}
		};
		Collections.sort(items, comperator);
		
		return items;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file_save_load);
		mApp = (BTLedSignApp)getApplication();
		
		final ListView listView = (ListView) findViewById(R.id.listViewFiles);
		if (listView != null) {
			ArrayList<FileItem> items = buildFileList(this);
			FileItemAdapter adapter = new FileItemAdapter(this, R.layout.file_list_row, items);
	    	listView.setAdapter(adapter);
	    	
	    	listView.setOnItemClickListener(new OnItemClickListener() {
	    		public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
	    			//listView.setItemChecked(position, true);
	    			FileItemAdapter fia = (FileItemAdapter) parent.getAdapter();
	    			FileItem item = (FileItem)fia.getItem(pos);
	    			Log.d(TAG, "sel " + pos + ", name " + item.mStrName);
	    			
	    			if (mOpenMode == true) {
		    			mApp.getLedSignBitmap().genBitmap(item.mBitmap);
		    			finish();
	    			} else {
	    				saveFile(item.mStrName);
	    				finish();
	    			}
	    	    }
	    	});
	    	
	    	listView.setOnItemLongClickListener(new OnItemLongClickListener() {
	            public boolean onItemLongClick(AdapterView<?> parent, View v, int pos, long id) {
	                // TODO Auto-generated method stub

	                Log.v("long clicked","pos: " + pos);
	                FileItemAdapter fia = (FileItemAdapter) parent.getAdapter();
	    			FileItem item = (FileItem)fia.getItem(pos);
	    			item.mChecked = !item.mChecked;
	    			fia.notifyDataSetChanged();

	    			int i;
	    			Button  btnDel = (Button) findViewById(R.id.buttonDel);
	    			for (i = 0; i < fia.getCount(); i++) {
	    				item = (FileItem)fia.getItem(i);
	    				if (item.mChecked) {
	    	    			btnDel.setVisibility(View.VISIBLE);
	    	    			break;
	    				}
	    			}

	    			if (i == fia.getCount())
	    				btnDel.setVisibility(View.GONE);

	                return true;
	            }
	        }); 
	    	
		}
		
		Bundle b = getIntent().getExtras();
		int value = b.getInt("key");
		
		Button  btnNew = (Button) findViewById(R.id.buttonNew);
		Button  btnDel = (Button) findViewById(R.id.buttonDel);
		if (value == 0) {
			this.setTitle(R.string.strFileOpen);
			btnNew.setVisibility(View.GONE);
			btnDel.setVisibility(View.GONE);
			mOpenMode = true;
		} else {
			this.setTitle(R.string.strFileSave);
			btnDel.setVisibility(View.GONE);
			btnNew.setVisibility(View.VISIBLE);
			mOpenMode = false;
		}
	}
    @Override
    public synchronized void onPause() {
    	super.onPause();
    	Log.d(TAG, "onPause !!!");
    }
    
    @Override
    public synchronized void onStop() {
    	super.onStop();
    	Log.d(TAG, "onStop !!!");
    }
    
    @Override
    public synchronized void onDestroy() {
    	super.onDestroy();
    	Log.d(TAG, "onDestroy !!!");
    }
    
    private void saveFile(String strFile) {
	    final byte[] szSig = new byte[]{'B', 'T', 'L', 'E' };
	    ByteBuffer bb = ByteBuffer.allocate(4);
		FileOutputStream outputStream;
	    
		try {
			Bitmap bitmap = mApp.getLedSignBitmap().getBitmap();
			
			ByteBuffer bufBitmap  = ByteBuffer.allocate(bitmap.getByteCount());
			bitmap.copyPixelsToBuffer(bufBitmap);
			
			outputStream = openFileOutput(strFile, Context.MODE_PRIVATE);
			// signature
			outputStream.write(szSig);
			// vir width
			outputStream.write(bb.putInt(bitmap.getWidth()).array());
			bb.clear();
			// vir height
			outputStream.write(bb.putInt(bitmap.getHeight()).array());
			bb.clear();
			// bpp
			outputStream.write(bb.putInt(mApp.getLedSignBitmap().getBPP()).array());
			bb.clear();
			// bitmap size
			outputStream.write(bb.putInt(bitmap.getByteCount()).array());
			bb.clear();
			// bitmap data
			outputStream.write(bufBitmap.array());
			outputStream.close();
		} catch (Exception e) {
		  e.printStackTrace();
		}
		
		Toast.makeText(
                getApplicationContext(),
                getString(R.string.strSavedTo) + strFile,
                Toast.LENGTH_SHORT).show();
    }
    
	public void OnClickNewFile(View v) {
		int nIdx = 0;
		String strFile;
		
		do {
			strFile = getFilesDir().getPath() + String.format("/%08d", nIdx);
			File file = new File(strFile);
			if (!file.exists()) {
				Log.d(TAG, "do not exist : " + strFile);
				break;
			}
			nIdx++;
		} while (true);

		strFile = String.format("%08d", nIdx);
		saveFile(strFile);

		finish();
        return;
	}
	
	public void OnClickDelFiles(View v) {
		final ListView listView = (ListView) findViewById(R.id.listViewFiles);
		FileItemAdapter adapter = (FileItemAdapter)listView.getAdapter();

		if (adapter == null)
			return;
		
		for (int i = 0; i < adapter.getCount(); i++) {
			FileItem item = (FileItem)adapter.getItem(i);
			if (item.mChecked) {
				Log.d(TAG, "FileName2Del:" + item.mStrName);
				String strFile = getFilesDir().getPath() + "/" + item.mStrName;
			    File file = new File(strFile);
			    file.delete();
			    adapter.removeItem(i);
			}
		}
		Button  btnDel = (Button) findViewById(R.id.buttonDel);
 		btnDel.setVisibility(View.GONE);		
		adapter.notifyDataSetChanged();
	}
}
