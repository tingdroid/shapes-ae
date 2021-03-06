package com.ting.escher;

import geomlab.GeomLab;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class Console extends Activity {

	ActionEditText mConsoleText = null;
	EvalListener evalListener = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.console);

		mConsoleText = (ActionEditText) this.findViewById(R.id.consoleText);

		// workaround for android:scrollHorizontally="true"
		mConsoleText.setHorizontallyScrolling(true);

		mConsoleText.setOnEditorActionListener(mEditorActionListener);
		
		try {
			GeomLab.withFrame(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.console, menu);
		return true;
	}
	
	public void setEvalListener(EvalListener evalListener) {
		this.evalListener = evalListener;
	}

	private TextView.OnEditorActionListener mEditorActionListener = new OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			Log.d(getLocalClassName(), event == null ? "Action: " + actionId
					: event.toString());
			if (actionId == EditorInfo.IME_ACTION_GO) {
				handleEnter();
				return true;
			} else {
				if (event == null)
					return false;

				if (event.getAction() == KeyEvent.ACTION_DOWN
						&& event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
					handleEnter();
					return true;
				}
			}
			return false;
		}
	};

	private void handleEnter() {
		Entry entry = new Entry(mConsoleText);
		if (!entry.isValid())
			return;

		if (entry.isLastLine()) {
			evalEntry(entry);
		} else {
			copyEntry(entry);
		}
	}

	private void copyEntry(Entry entry) {
		entry.appendClear(entry.getText());
	}

	private void evalEntry(Entry entry) {
		String input = entry.getText().toString().trim();
		entry.appendClear("");
		String result = "";
		if (input.length() > 0 && evalListener != null) {
			result = evalListener.evalPerformed(input);
		}
		if (result != null)
			entry.append(result + "\n");
		entry.append("   ");
	}

	public PrintWriter getPrintWriter() {
		return new PrintWriter(new BufferedWriter(new Writer() {
			public void write(char buf[], int base, int len) {
				mConsoleText.append(new String(buf), base, base + len);
			}

			public void flush() {
				mConsoleText.setSelection(mConsoleText.length());
			}

			public void close() {
			}
		}));
	}
}
