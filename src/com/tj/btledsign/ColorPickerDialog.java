package com.tj.btledsign;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class ColorPickerDialog extends Dialog {

	public interface OnColorChangedListener {
	    void colorChanged(int pos, int color);
	}
	
	private OnColorChangedListener mListener;
	private int mColors;
	private int mSelColor;
	private List<Integer> colorList = new ArrayList<Integer>();
	
	public ColorPickerDialog(Context context,
			OnColorChangedListener listener, 
			int colors[][], int nColors,
			int nSelColor) {
		super(context);
		//this.setTitle(R.string.strColorPick);
		mListener = listener;
		mColors   = nColors;
		mSelColor = nSelColor;
		
		colorList = new ArrayList<Integer>();
		// add the color array to the list
		for (int i = 0; i < colors.length; i++) {
			for (int j = 0; j < colors[i].length; j++) {
				colorList.add(colors[i][j]);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.color_picker);
		
		GridView gridViewColors = (GridView) findViewById(R.id.gridViewColors);
		gridViewColors.setAdapter(new ColorPickerAdapter(getContext(), colorList, mColors, mSelColor));
		
		// close the dialog on item click
		gridViewColors.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				GridView gridViewColors = (GridView) findViewById(R.id.gridViewColors);
				int col = (Integer)gridViewColors.getItemAtPosition(position);
				mListener.colorChanged(position, col);
				ColorPickerDialog.this.dismiss();
			}
		});
	}
}
