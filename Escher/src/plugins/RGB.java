package plugins;

public class RGB {
    public static final int BLACK       = 0xFF000000;
    public static final int DKGRAY      = 0xFF444444;
    public static final int GRAY        = 0xFF888888;
    public static final int LTGRAY      = 0xFFCCCCCC;
    public static final int WHITE       = 0xFFFFFFFF;
    public static final int RED         = 0xFFFF0000;
    public static final int GREEN       = 0xFF00FF00;
    public static final int BLUE        = 0xFF0000FF;
    public static final int YELLOW      = 0xFFFFFF00;
    public static final int CYAN        = 0xFF00FFFF;
    public static final int MAGENTA     = 0xFFFF00FF;
    public static final int OPAQUE      = 0xFF000000;
    public static final int TRANSPARENT = 0;

    /** Color only if opacity is specified. Used to overload integers as palette indices. */
    public static boolean isColor(int color) {
	return alpha(color) != 0;
    }

    /** Opacity component, most significant byte. */
    public static int alpha(int color) {
        return color >>> 24;
    }

    /** Red component, third least significant byte. */
    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    /** Green component, second least significant byte. */
    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    /** Blue component, least significant byte. */
    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int fromRGB(int r, int g, int b) {
        return  ((    0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                ((b & 0xFF) << 0);
    }
    
    public static int fromRGB(float r, float g, float b) {
        return fromRGB( (int) (r*255+0.5), (int) (g*255+0.5), (int) (b*255+0.5));
    }
        
    public static int fromHSB(float hue, float saturation, float brightness) {
	int r = 0, g = 0, b = 0;
    	if (saturation == 0) {
	    r = g = b = (int) (brightness * 255.0f + 0.5f);
	} else {
	    float h = (hue - (float)Math.floor(hue)) * 6.0f;
	    float f = h - (float)java.lang.Math.floor(h);
	    float p = brightness * (1.0f - saturation);
	    float q = brightness * (1.0f - saturation * f);
	    float t = brightness * (1.0f - (saturation * (1.0f - f)));
	    switch ((int) h) {
	    case 0:
		r = (int) (brightness * 255.0f + 0.5f);
		g = (int) (t * 255.0f + 0.5f);
		b = (int) (p * 255.0f + 0.5f);
		break;
	    case 1:
		r = (int) (q * 255.0f + 0.5f);
		g = (int) (brightness * 255.0f + 0.5f);
		b = (int) (p * 255.0f + 0.5f);
		break;
	    case 2:
		r = (int) (p * 255.0f + 0.5f);
		g = (int) (brightness * 255.0f + 0.5f);
		b = (int) (t * 255.0f + 0.5f);
		break;
	    case 3:
		r = (int) (p * 255.0f + 0.5f);
		g = (int) (q * 255.0f + 0.5f);
		b = (int) (brightness * 255.0f + 0.5f);
		break;
	    case 4:
		r = (int) (t * 255.0f + 0.5f);
		g = (int) (p * 255.0f + 0.5f);
		b = (int) (brightness * 255.0f + 0.5f);
		break;
	    case 5:
		r = (int) (brightness * 255.0f + 0.5f);
		g = (int) (p * 255.0f + 0.5f);
		b = (int) (q * 255.0f + 0.5f);
		break;
	    }
	}
	return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
    }

}
