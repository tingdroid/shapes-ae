/*
 * Tablet.java
 * 
 * This file is part of GeomLab
 * Copyright (c) 2005 J. M. Spivey
 * All rights reserved
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer.      
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products 
 *    derived from this software without specific prior written permission.
 *    
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR 
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES 
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package plugins;

import geomlab.Image;

/** A 'drawing tablet' on which a picture can be drawn */
public abstract class Tablet {
    private final float slider;
    protected final int palette[];

    public Tablet(float slider) {
	this.slider = slider;
	palette = Picture.makePalette(slider);
    }

    public float getSlider() { return slider; }

    /** Fill an outline, using the palette for indexed colours */
    public void fillOutline(Vec2D outline[],
	    int spec, int col, Tran2D t) {
	if (RGB.isColor(spec)) {
	    fillOutline(outline, spec, t);
	}
	else if (col >= 0) {
	    int color = palette[(spec + col) % palette.length];
	    fillOutline(outline, color, t);
	}
    }

    public abstract void drawStroke(Vec2D stroke[], Tran2D t);
    public abstract void fillOutline(Vec2D outline[], 
	    int color, Tran2D t);
    public abstract void drawLine(Vec2D from, Vec2D to, 
	    int color, Tran2D t);

    /** Draw an arc of the unit circle, magnified by xrad in the 
     * x direction and yrad in the y direction.  The arc starts at
     * angle start, measured in degrees counterclockwise from the
     * x axis, and extends to angle start+extent. */
    public abstract void drawArc(Vec2D centre, float xrad, float yrad, 
	    float start, float extent, int color, Tran2D t);

    /** Save the graphics state on a stack */
    public abstract void save();
    
    /** Restore the graphics state from the stack */
    public abstract void restore();
    
    /** Set the stroke width for future drawing operations */
    public abstract void setStroke(float width);
    
    /** Draw a raster image */
    public abstract void drawImage(Image image, Tran2D t);
    
    public abstract boolean isTiny(Tran2D t);

    public void close() { }

    /* These two methods use the default painting methods that
     * are contained in TilePicture and TurtlePicture, but can be
     * overridden with special-purpose implementations.  This is
     * done in EPSWrite, for example. */

    public void drawTile(TilePicture tile, int layer, 
	    int col, Tran2D t) {
	tile.defaultDraw(layer, col, this, t);
    }

    public void drawPath(TurtlePicture pic, Tran2D t) {
	pic.defaultDraw(this, t);
    }
}