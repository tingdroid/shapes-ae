package geomlab;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import plugins.ImagePicture;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Image {
	public static final int TYPE_INT_RGB = 1;
	Bitmap bitmap;

	public Image(Bitmap bi) {
		bitmap = bi;
//		bitmap = bi.copy(bi.getConfig(), true);
	}

	public Image(int w, int h, int typeIntRgb) {
		bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
	}

	public static Image fromResource(String name) throws IOException {
		ClassLoader loader = ImagePicture.class.getClassLoader();
		return fromUrl(loader.getResource(name));
	}

	public static Image fromUrl(URL url) throws IOException {
		if (url == null)
			return null;
		InputStream input = null;
		try {
			//TODO if file, consider BitmapFactory.decodeFile
			URLConnection connection = url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			input = connection.getInputStream();
			Bitmap myBitmap = BitmapFactory.decodeStream(input);
			return new Image(myBitmap);
		} finally {
			if (input != null) input.close();
		}
	}

	public int getWidth() {
		return bitmap.getWidth();
	}

	public int getHeight() {
		return bitmap.getHeight();
	}

	public int getRGB(int x, int y) {
		return bitmap.getPixel(x, y);
	}

	public void setRGB(int x, int y, int color) {
		bitmap.setPixel(x, y, color);
	}

}
