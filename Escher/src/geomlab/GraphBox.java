package geomlab;

import java.io.File;
import java.io.IOException;

import plugins.Drawable;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.ting.escher.R;

public class GraphBox extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.graphbox);
		// Show the Up button in the action bar.
		setupActionBar();
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.graph_box, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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

	public void writePicture(File file) throws IOException {
/*		writePicture(picture, imageMean, sliderValue(), Color.white.getRGB(),
				file);
*/	}

	public static void writePicture(Drawable pic, int meanSize, float slider,
			int background, File file) throws IOException {
		/*
		 * The dimensions of the image are chosen to give approximately the
		 * right aspect ratio, and so that the geometric mean of width and
		 * height is approx. meanSize
		 */
		float aspect = pic.getAspect();
		float sqrtAspect = (float) Math.sqrt(aspect);
		int width = Math.round(meanSize * sqrtAspect);
		int height = Math.round(meanSize / sqrtAspect);
/*		BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		ScreenTablet tablet = new ScreenTablet(g2, slider);
		pic.draw(tablet, width, height, background);
		ImageIO.write(image, "png", file);
*/	}

}
