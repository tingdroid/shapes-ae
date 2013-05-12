package com.ting.canvaspad;

import java.lang.reflect.Field;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;

import com.ting.canvaspad.CanvasActivity.PictureType;

class Image {
	String name;
	PictureType type;
	int background;
	Drawable drawable;
	float width;
	float height;
	float[] values;
	Picture picture;

	public static Image fromResource(String name, Resources res) {

		Field arrayField;
		int arrayId;
		try {
			arrayField = R.array.class.getField(name);
			arrayId = arrayField.getInt(null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		TypedArray a = res.obtainTypedArray(arrayId);
		Image p = new Image();
		p.name = name;
		p.type = PictureType.valueOf(a
				.getString(R.styleable.PictureAttr_picType));
		if (p.type.equals(PictureType.drawable)) {
			p.background = a.getColor(R.styleable.PictureAttr_picBackground, 0);
			p.drawable = a.getDrawable(R.styleable.PictureAttr_picDrawable);
			p.width = p.drawable.getIntrinsicWidth();
			p.height = p.drawable.getIntrinsicHeight();
		}
		if (p.type.equals(PictureType.color)) {
			p.background = a.getColor(R.styleable.PictureAttr_picBackground, 0);
			p.width = a.getFloat(R.styleable.PictureAttr_picWidth, 1);
			p.height = a.getFloat(R.styleable.PictureAttr_picHeight, 1);
			p.drawable = new ColorDrawable(a.getColor(
					R.styleable.PictureAttr_picDrawable, 0));
		}
		if (p.type.equals(PictureType.sketch)) {
			p.background = a.getColor(R.styleable.PictureAttr_picBackground, 0);
			p.width = a.getFloat(R.styleable.PictureAttr_picWidth, 1);
			p.height = a.getFloat(R.styleable.PictureAttr_picHeight, 1);
			String valueList = a.getString(R.styleable.PictureAttr_picDrawable);
			String[] vals = valueList.split("[ \n\r\t,]+");
			int color = Color.argb(Integer.parseInt(vals[0]),
					Integer.parseInt(vals[1]), Integer.parseInt(vals[2]),
					Integer.parseInt(vals[3]));

			Path path = new Path();
			
			// x0,y0-x1,y1 x1,y1-x2,y2 x2,y2-x3,y3 ...
			// 0 1 1 2 2 3
			int size = (vals.length - 4) / 2;
			float[] lines = new float[((size - 2) * 2 + 2) * 2];

			int k = 4;
			path.moveTo(Float.parseFloat(vals[k++]), Float.parseFloat(vals[k++]));
			for (int i = 1; i < size; i++) {
				path.lineTo(Float.parseFloat(vals[k++]), Float.parseFloat(vals[k++]));
			}
			p.values = lines;
			p.picture = new Picture();
			{
				Canvas canvas = p.picture.beginRecording(Math.round(p.width),
						Math.round(p.height));
				canvas.scale(1, -1, 0, p.height / 2);   // invert coordinate plane
				Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
				paint.setStyle(Paint.Style.FILL);
				paint.setColor(p.background);
				canvas.drawRect(1, 1, p.width - 1, p.height - 1, paint);

				paint.setColor(color);
				canvas.drawPath(path, paint);
				paint.setStyle(Paint.Style.STROKE);
				paint.setColor(Color.BLACK);
				paint.setStrokeWidth(0); // hairline
				canvas.drawPath(path, paint);
				p.picture.endRecording();
			}
			p.drawable = new PictureDrawable(p.picture);
		}

		a.recycle();
		return p;
	}
}