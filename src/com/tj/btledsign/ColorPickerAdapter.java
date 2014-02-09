package com.tj.btledsign;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ColorPickerAdapter extends BaseAdapter {
	private static final int EXTRA_INVERSE = 0;
	private static final int EXTRA_FILL    = 1;
	private static final int EXTRA_FLASH   = 2;
	private static final int EXTRA_COLORS  = 3;
	
	private Context context;
	// list which holds the colors to be displayed
	private List<Integer> mColorList = new ArrayList<Integer>();
	// width of grid column
	int colorGridColumnWidth;
	int mColors;
	int mSelColor;

	public ColorPickerAdapter(Context context, List<Integer> colorlist, int nColors, int nSelColor) {
		this.context = context;
		colorGridColumnWidth = 80;
		mColors = nColors;
		mSelColor = nSelColor;
		mColorList = colorlist;
	}

	public static Bitmap getColorBitmap(int pos, int size, int nSelColor, int nCurColor, int nColors, boolean bDrawOutline) {
    	Bitmap bitmap = Bitmap.createBitmap(size, size, 
    			Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bitmap);

		Paint paint = new Paint();
		
		if (bDrawOutline) {
			if (nSelColor == nCurColor)
				paint.setColor(Color.YELLOW);
			else
				paint.setColor(Color.GRAY);
			
			c.drawRect(0, 0, size, size, paint);
		}

		int color = 0;
		if (pos == (nColors + EXTRA_INVERSE) || pos == (nColors + EXTRA_FLASH))
			color = nSelColor;
		else if (pos == (nColors + EXTRA_FILL))
			color = Color.BLACK;
		else
			color = nCurColor;
		
		paint.setColor(color);
		c.drawRect(10, 10, size - 10, size - 10, paint);
		
		if (pos == (nColors + EXTRA_INVERSE)) {
			paint.setColor(Color.BLACK);
			c.drawRect(20, 20, size - 20, size - 20, paint);
		} else if (pos == (nColors + EXTRA_FILL)) {
			paint.setColor(nSelColor);
			c.drawRect(20, 20, size - 20, size - 20, paint);
		} else if (pos == (nColors + EXTRA_FLASH)) {
			paint.setColor(nSelColor);
			int width = (size - 20) / 2;
			paint.setColor(Color.BLACK);
			c.drawRect(10 + width, 10, size - 10, size - 10, paint);
		}
		
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
		
		if (pos >= nColors) {
			paint.setTextScaleX(1.f);
			paint.setAlpha(0);
			paint.setAntiAlias(true);
			paint.setTextAlign(Paint.Align.LEFT);
			
			String strColor = null;

			if (pos == (nColors + EXTRA_INVERSE))
				strColor = "Inverse";
			else if (pos == (nColors + EXTRA_FILL))
				strColor = "Fill";
			else
				strColor = "Flash";

			Typeface face = null;
			face = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
			if (face != null)
				paint.setTypeface(face);			
			
			paint.setTextSize(14);
			paint.setColor(Color.WHITE);
			paint.setStyle(Paint.Style.STROKE);
			c.drawText(strColor, 14, size - 14, paint);
		}
		
		return bitmap;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView;

		// can we reuse a view?
		if (convertView == null) {
			imageView = new ImageView(context);
			// set the width of each color square
			imageView.setLayoutParams(new GridView.LayoutParams(colorGridColumnWidth, 
					colorGridColumnWidth));
		} else {
			imageView = (ImageView) convertView;
		}

		//imageView.setBackgroundColor(mColorList.get(position));

		if (position >= mColors + EXTRA_COLORS)
			return imageView;
		Bitmap bitmap = getColorBitmap(position, colorGridColumnWidth, 
				mSelColor, mColorList.get(position), mColors, true);
		imageView.setImageBitmap(bitmap);
		imageView.setId(position);

		return imageView;
	}

	public int getCount() {
		return mColorList.size();
	}

	public Object getItem(int position) {
		return mColorList.get(position);
	}

	public long getItemId(int position) {
		return 0;
	}
}
