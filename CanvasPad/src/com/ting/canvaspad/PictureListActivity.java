package com.ting.canvaspad;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class PictureListActivity extends ListActivity {

	private static final int REQUEST_CODE = 10;
	int PICTURE_ITEM_ORDER;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PICTURE_ITEM_ORDER = getResources().getInteger(R.integer.pic_category);
		setupActionBar();
	}

	/** Set up the {@link android.app.ActionBar}, if the API is available. */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(false);
		}
	}

	int[] listValues;
	String[] listTitles;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.canvas, menu);

		int size = 0;
		for (int i = 0; i < menu.size(); i++) {
			if (menu.getItem(i).getOrder() == PICTURE_ITEM_ORDER)
				size++;
		}

		listValues = new int[size];
		listTitles = new String[size];

		int j = 0;
		for (int i = 0; i < menu.size(); i++) {
			if (menu.getItem(i).getOrder() == PICTURE_ITEM_ORDER) {
				listTitles[j] = menu.getItem(i).getTitle().toString();
				listValues[j] = menu.getItem(i).getItemId();
				j++;
			}
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, listTitles);
		setListAdapter(adapter);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getOrder() == PICTURE_ITEM_ORDER) {
			showCanvas(item.getItemId(), item.getTitle().toString());
		}
		return super.onOptionsItemSelected(item);
	}

	void showCanvas(int itemId, String itemTitle) {
		Toast.makeText(this, itemTitle + " selected", Toast.LENGTH_LONG).show();

		Intent intent = new Intent(this, CanvasActivity.class);
		intent.putExtra(CanvasActivity.IMAGE_ID_EXTRA, itemId);
		intent.putExtra(CanvasActivity.IMAGE_TITLE_EXTRA, itemTitle);
		startActivityForResult(intent, REQUEST_CODE /* , options - animation */);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// String item = (String) getListAdapter().getItem(position);
		showCanvas(listValues[position], listTitles[position]);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
			if (data.hasExtra(CanvasActivity.RESULT_EXTRA)) {
				Toast.makeText(
						this,
						data.getExtras().getString(CanvasActivity.RESULT_EXTRA),
						Toast.LENGTH_SHORT).show();
			}
		}
	}

}
