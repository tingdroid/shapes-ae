package com.ting.tingconsole;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class Console extends Activity {
	
	ActionEditText mConsoleText = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.console);
		
		mConsoleText = (ActionEditText) this.findViewById(R.id.consoleText);

		// workaround for android:scrollHorizontally="true"
		mConsoleText.setHorizontallyScrolling(true);
		
		mConsoleText.setOnEditorActionListener(mEditorActionListener);
		
		int imeOptions = mConsoleText.getImeOptions();
		mConsoleText.setImeOptions(imeOptions &~ EditorInfo.IME_FLAG_NO_ENTER_ACTION);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.console, menu);
		return true;
	}

	private TextView.OnEditorActionListener mEditorActionListener = new OnEditorActionListener() {
	    @Override
	    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
	    	Log.d(getLocalClassName(), event == null ? "Action: "+actionId : event.toString());
	        if (actionId == EditorInfo.IME_ACTION_GO) {
        		handleEnter();
	            return true;
	        } else {
	        	if (event == null)
	        		return false;
	        	
	        	if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
	        		handleEnter();
	        		return true;
	        	}
	        }
	        return false;
	    }
	};
	
	private void handleEnter() {
		int selectionLine = getSelectionLine(mConsoleText);
		if (selectionLine < 0) return;

		int lineCount = mConsoleText.getLineCount();
		if (lineCount <= 0) return;

		if (selectionLine == lineCount - 1) {
			evalEntry();
		} else {
			copyEntry();
		}
		selectEnd();
	}
	
	private void copyEntry() {
		mConsoleText.append("\n[COPY]\n   ");
	}
	
	private void evalEntry() {
		mConsoleText.append("\n[EVAL]\n   ");
	}

	private void selectEnd() {
		Selection.setSelection(mConsoleText.getText(), mConsoleText.length());
	}
	
	public static int getSelectionLine(EditText editText)
	{    
	    if (editText == null) return -1;

	    int selectionStart = Selection.getSelectionEnd(editText.getText());
	    if (selectionStart < 0) return -1;

	    Layout layout = editText.getLayout();
	    if (layout == null) return -1;

	    return layout.getLineForOffset(selectionStart);
	}	
}
