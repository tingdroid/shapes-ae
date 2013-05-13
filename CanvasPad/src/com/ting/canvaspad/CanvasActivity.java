package com.ting.canvaspad;

import java.lang.reflect.Field;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

public class CanvasActivity extends Activity {

	public static final String IMAGE_ID_EXTRA = "imageId";
	public static final String IMAGE_TITLE_EXTRA = "imageTitle";
	public static final String RESULT_EXTRA = "result";

	static Image lastImage; // save last image to restore when View is
							// re-created
	CanvasView canvasView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_canvas);
		setupActionBar();
		canvasView = (CanvasView) findViewById(R.id.canvasView1);

		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.containsKey(IMAGE_ID_EXTRA)) {
			int imageId = extras.getInt(IMAGE_ID_EXTRA);
			String title = extras.getString(IMAGE_TITLE_EXTRA);
			if (title != null && title.trim().length() > 0) setTitle(title);
			selectImage(imageId);
		} else if (lastImage == null) {
			selectImage(R.id.pic_wide);
		} else {
			selectImage(lastImage);
		}
	}

	/** Set up the {@link android.app.ActionBar}, if the API is available. */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
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
		if (item.getOrder() == getResources()
				.getInteger(R.integer.pic_category)) {
			setTitle(item.getTitle());
			return selectImage(item.getItemId());
		}
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//

			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void finish() {
		Intent data = new Intent();
		data.putExtra(RESULT_EXTRA, "Canvas closed");
		setResult(RESULT_OK, data);
		super.finish();
	}

	boolean selectImage(int menuItemId) {
		String name = menuName(menuItemId);
		if (name == null)
			return false;

		// save last image to restore when View is re-created
		lastImage = Image.fromResource(name, getResources());
		return selectImage(lastImage);
	}

	boolean selectImage(Image image) {
		if (image == null)
			return false;
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
