package com.ting.canvaspad;

import java.lang.reflect.Field;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class CanvasActivity extends Activity {

	CanvasView canvasView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_canvas);
		canvasView = (CanvasView) findViewById(R.id.canvasView1);
		selectImage(R.id.pic_wide);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.canvas, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// ContextMenuInfo info = item.getMenuInfo();
		if (item.getOrder() == getResources().getInteger(R.integer.pic_category)) {
			return selectImage(item.getItemId());
		}
		return super.onOptionsItemSelected(item);
	}
	
	boolean selectImage(int menuItemId) {
		String name = menuName(menuItemId);
		if (name == null)
			return false;

		Image image = Image.fromResource(name, getResources());
		canvasView.setImage(image);
		return true;
	}

	static String menuName(int menuItemId) {
		String name = null;
		try {
			for (Field f : R.id.class.getFields()) {
				if (f.getInt(null) == menuItemId) {
					name = f.getName();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return name;
	}

	enum PictureType {
		drawable, color, sketch
	}

}
