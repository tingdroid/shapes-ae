package com.ting.escher;

import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.util.Log;
import android.widget.EditText;

class Entry {
	int offset = -1;
	int line = -1;
	int lineCount = 0;
	int start;
	int end;
	EditText editText;
	Editable text;

	boolean isValid() {
		Log.d(getClass().getSimpleName(), String.format(
				"isValid offset: %d, line: %d, lineCount: %d", offset, line, lineCount));
		return offset >= 0 && line >= 0 && lineCount > 0;
	}

	boolean isLastLine() {
		return line == lineCount - 1;
	}

	Entry(EditText editText) {
		if (editText == null)
			return;
		this.editText = editText;
		text = editText.getText();

		offset = Selection.getSelectionEnd(text);
		if (offset < 0)
			return;

		lineCount = editText.getLineCount();
		if (lineCount <= 0)
			return;

		Layout layout = editText.getLayout();
		if (layout == null)
			return;

		line = layout.getLineForOffset(offset);
		start = layout.getLineStart(line);
		end = layout.getLineEnd(line);
	}

	CharSequence getText() {
		if (text == null || text.length() == 0 || end == 0)
			return "";
		int eol = start < end && text.charAt(end - 1) == '\n' ? 1 : 0;
		Log.d(getClass().getSimpleName(), String.format(
				"getText start: %d, end: %d, eol: %d", start, end, eol));
		return text.subSequence(start, end - eol);
	}

	void appendClear(CharSequence seq) {
		clearEnd();
		append(seq);
	}

	void append(CharSequence seq) {
		if (editText == null)
			return;
		editText.append(seq);
		editText.setSelection(editText.length());
	}

	void clearEnd() {
		Layout layout = editText.getLayout();
		if (layout == null)
			return;

		int lastLine = layout.getLineForOffset(text.length());
		int lastStart = layout.getLineStart(lastLine);
		int lastEnd = layout.getLineEnd(lastLine);
		String last = text.subSequence(lastStart, lastEnd).toString();
		if (last.trim().length() == 0) {
			text.delete(lastStart, lastEnd);
		} else {
			text.append("\n");
		}
	}
}
