package com.ting.tingconsole;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.EditText;

public class Console extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.console);
		
		EditText mConsoleText = (EditText) this.findViewById(R.id.consoleText);

		// workaround for android:scrollHorizontally="true"
		mConsoleText.setHorizontallyScrolling(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.console, menu);
		return true;
	}

}
