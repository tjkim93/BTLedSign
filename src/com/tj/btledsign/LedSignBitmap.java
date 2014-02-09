package com.tj.btledsign;

import java.util.Arrays;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.util.Log;

public class LedSignBitmap {
    public static final int   COLOR_OFF_LED  = Color.argb(255, 100, 100, 0);
	private static final int mIntColorTbl4[][] = 
		{ { 0xff000000, 0xffff0000, 0xff00ff00, 0xffffc100 },
		  { 0xfffffffe, 0xfffffffe, 0xfffffffe, 0x00000000 }
		};

	private int mIntBPP;
	private int mIntColors;
	private int mIntXRes;
	private int mIntXResVirtual;
	private int mIntYRes;
	private int mIntYResVirtual;

	private int mIntZoomFont     = 16;
	private int mIntDispPos      = 14;
	private int[][] mCurColorTbl = mIntColorTbl4;

	
	private String mStrBanner;
	private String mStrFontName;
	private Bitmap mBitmapBanner;

	// constructor
	LedSignBitmap(int xres, int yres, int xres_virtual, int yres_virtual, int bpp) {
		mIntXRes = xres;
		mIntYRes = yres;
		mIntXResVirtual = xres_virtual;
		mIntYResVirtual = yres_virtual;
		mIntBPP    = bpp;
		mIntColors = (int)Math.pow(2,  mIntBPP);
		
		if (mStrBanner != null)
			genBitmap();
	}

	public void setInfo(int xres, int yres, int xres_virtual, int yres_virtual, int bpp) {
		if (mIntXRes != xres ||
				mIntYRes != yres ||
				mIntXResVirtual != xres_virtual ||
				mIntYResVirtual != yres_virtual ||
				mIntBPP != bpp) {
			
			mIntXRes = xres;
			mIntYRes = yres;
			mIntXResVirtual = xres_virtual;
			mIntYResVirtual = yres_virtual;
			mIntBPP    = bpp;
			mIntColors = (int)Math.pow(2,  mIntBPP);
			genBitmap();
		}
	}
	
	public int getXRes() {
		return mIntXRes;
	}
	
	public int getYRes() {
		return mIntYRes; 
	}
	
	public int getXResVirtual() {
		return mIntXResVirtual;
	}
	
	public int getYResVirtual() {
		return mIntYResVirtual;
	}
	
	public int getBPP() {
		return mIntBPP;
	}
	
	public int getColorCount() {
		return mIntColors;
	}
	
	public int getInverseIndex() {
		return mIntColors;
	}
	
	public int getFillIndex() {
		return mIntColors + 1;
	}
	
	public int getFlashIndex() {
		return mIntColors + 2;
	}
	
	public void setBanner(String strBanner) {
		if (mStrBanner != strBanner) {
			mStrBanner = strBanner;
			genBitmap();
		}
	}
	
	public String getBanner() {
		return mStrBanner;
	}
	
	public void setFontName(String strFontName) {
		if (mStrFontName != strFontName) {
			mStrFontName = strFontName;
			genBitmap();
		}
	}
	
	public void setFontSize(int nFontSize) {
		if (mIntZoomFont != nFontSize) {
			mIntZoomFont = nFontSize;
			genBitmap();
		}
	}
	
	public int getFontSize() {
		return mIntZoomFont;
	}
	
	public void setFontYPos(int nYPos) {
		if (mIntDispPos != nYPos) {
			mIntDispPos  = nYPos;
			genBitmap();
		}
	}
	
	public int getFontYPos() {
		return mIntDispPos;
	}
	
	public String getFontName() {
		return mStrFontName;
	}

	public int getDefaultColor() {
		int nRow = mIntColors / (4 + 1);
	    int nCol = (mIntColors - 1) % 4;
	    
	    return mCurColorTbl[nRow][nCol];
	}
	
	public int[][] getColorTable() {
		return mCurColorTbl;
	}

	// Generate Bitmap
    public void genBitmap() {
    	Typeface face = null;
    	
    	if (mStrBanner == null)
    		return;
    	
        // Bitmap
		if (mStrFontName != null)
			face = Typeface.createFromFile(mStrFontName);
		
    	mBitmapBanner = Bitmap.createBitmap(mIntXResVirtual, mIntYRes, 
    			Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(mBitmapBanner);
		c.drawColor(Color.WHITE);

		Paint paint = new Paint();
		paint.setColor(Color.BLACK);
		c.drawRect(0, 0, mIntXResVirtual, mIntYRes, paint);
			
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
		paint.setTextSize(mIntZoomFont);
		paint.setTextScaleX(1.f);
		paint.setAlpha(0);
		paint.setAntiAlias(false);
		paint.setColor(getDefaultColor()); 
		
		if (face != null)
			paint.setTypeface(face);

		paint.setTextAlign(Paint.Align.LEFT);
		c.drawText(mStrBanner, 0, mIntDispPos, paint);
    }
    
    public void genBitmap(Bitmap bitmap) {
    	mBitmapBanner = Bitmap.createBitmap(bitmap);
    }
    
    public Bitmap getBitmap() {
    	return mBitmapBanner;
    }
    
    private int mLeftPixels[];
    private int mRightPixels[];
    private Bitmap mSavedBitmap;
    
    
    public void shiftBitmap2Left() {
    	mBitmapBanner.getPixels(mLeftPixels, 0, 1, 0, 0, 1, getYResVirtual());
    	
    	mBitmapBanner.getPixels(mRightPixels, 0, getXResVirtual() - 1,
    			1, 0, getXResVirtual() - 1, getYResVirtual());
    	
    	mBitmapBanner.setPixels(mRightPixels, 0, getXResVirtual() - 1,
    			0, 0, getXResVirtual() - 1, getYResVirtual());
    	mBitmapBanner.setPixels(mLeftPixels, 0, 1,
    			getXResVirtual() - 1, 0, 1, getYResVirtual());
    }
    
    public void saveBackup() {
    	mLeftPixels  = new int[getYResVirtual()];
    	mRightPixels = new int[(getXResVirtual() - 1) * getYResVirtual()];
    	mSavedBitmap = Bitmap.createBitmap(mBitmapBanner);
    }

    public void restoreBackup() {
    	mBitmapBanner = Bitmap.createBitmap(mSavedBitmap);
    }
    
    // putPixel2Buf to Buffer
	private void putPixel2Buf(byte[] buf, int x, int y, int color, int bpp) {
	  int   nBit;
	  byte  ucRead;
	  int   dwOff;
	  int   nBPPMask = (1 << bpp) - 1;
	  int   nPPB = 8 / bpp;

	  if (x >= mIntXResVirtual || y >= mIntYResVirtual)
	  {
	    return;
	  }

	  dwOff  = x + y * mIntXResVirtual;
	  nBit   = (((nPPB - 1) - (dwOff % nPPB)) * bpp);
	  ucRead = buf[dwOff / nPPB];
	  ucRead = (byte)((byte)(ucRead & ~(nBPPMask << nBit)) | (color << nBit));
	  buf[dwOff / nPPB]  = ucRead;
	}

	
	// getDisplayBuffer
	public byte[] getDispBuf() {
		byte[] bufBitmap = new byte[mIntXResVirtual * mIntYResVirtual / (8 / mIntBPP)];
		int     pix;
		int     nRow, nCol;
		
		Arrays.fill(bufBitmap, (byte) 0);
		
		for (int i = 0; i < mIntXResVirtual; i++) {
			for (int j = 0; j < mIntYResVirtual; j++) {
				int pixel = mBitmapBanner.getPixel(i, j);
				
				if (Color.alpha(pixel) == 128) {
					pixel = Color.argb(0xff, Color.red(pixel), Color.green(pixel), Color.blue(pixel));
					String str = String.format("colod : %x", pixel);
					Log.d("BTLedSign", str);
				}
				
				if (pixel != Color.BLACK) {
					pix = 0;
					
					for (int k = 0; k < mIntColors; k++) {
						nRow = k / 4;
					    nCol = k % 4;
					    
						if (pixel == mCurColorTbl[nRow][nCol]) {
							pix = k;
							break;
						}
					}

					putPixel2Buf(bufBitmap, i, j, pix, mIntBPP);
				}
			}
		}
		
		return bufBitmap;
	}
	
	// getDisplayAttr
	public byte[] getAttrBuf() {
		byte[] bufAttr   = new byte[mIntXResVirtual / 8];
		boolean bFlash;
		
		Arrays.fill(bufAttr, (byte) 0);
		
		for (int i = 0; i < mIntXResVirtual; i++) {
			bFlash = false;
			for (int j = 0; j < mIntYResVirtual; j++) {
				int pixel = mBitmapBanner.getPixel(i, j);
				if (Color.alpha(pixel) == 128) {
					bFlash = true;
				}
			}
			if (bFlash)
				putPixel2Buf(bufAttr, i, 0, 1, 1);
		}
		
		return bufAttr;
	}	
}
