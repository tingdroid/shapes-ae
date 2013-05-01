package plugins;


/** A scalable picture with fixed aspect ratio */
public interface Drawable {
    public float getAspect();
    public boolean isInteractive();
    public void draw(Tablet tablet, int ww, int hh, 
    	int backgroundColor);
}
