/*
 * Picture.java
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import funbase.Evaluator;
import funbase.Primitive;
import funbase.Value;
import geomlab.GraphBox;

/* The pictures that GeomLab works with do not have a fixed size, but
 may be scaled uniformly to achieve any desired height or
 width.  Thus the aspect ratio (width/height) is fixed even if the
 size is not.  When two pictures are put side-by-side, there is a
 constraint that they should have the same height, and this removes
 one degree of freedom in the scaling; similarly when pictures are put
 one above another.  Each picture thus has a total of one degree of 
 freedom for scaling.
 
 Pictures are drawn in two layers: first the coloured fills, then
 black outlines over the top.  The colours used for the fills rotate
 as the pictures themselves are rotated: this makes Escher tilings
 work neatly. */

/** A rectangular graphical object of scalable size but fixed aspect ratio */
public class Picture extends Value implements Drawable {

    private static final long serialVersionUID = 1L;

    private final float aspect; // = width / height
    private final boolean interactive; // whether we use slider value
    private static final int STROKE_COLOR = RGB.BLACK;
    
    public Picture(float aspect) { this(aspect, false); }
    
    public Picture(float aspect, boolean interactive) {
	Evaluator.countCons();
	this.aspect = aspect;
	this.interactive = interactive;
    }
    
    public float getAspect() { return aspect; }
    
    public boolean isInteractive() { return interactive; }
    
    public void draw(Tablet tablet, int ww, int hh, int backgroundColor) {
	Tran2D t = Tran2D.translation(0, hh).scale(ww, -hh);
	tablet.fillOutline(unitsquare, backgroundColor, t);
	paintPart(FILL, -1, tablet, t);
	paintPart(DRAW, -1, tablet, t);
    }
    
    public static final int DRAW = 1, FILL = 2;
    
    public static final Vec2D[] unitsquare = { 
	new Vec2D(0, 0), new Vec2D(0, 1), new Vec2D(1, 1), new Vec2D(1, 0) 
    };
    
    public final void paintPart(int layer, int col, Tablet g, Tran2D t) { 
	// Give up if the drawing space is negligibly small
	if (g.isTiny(t)) {
	    if (layer == DRAW)
		g.fillOutline(unitsquare, STROKE_COLOR, t);
	    return;
	}
	
	this.paint(layer, col, g, t);
    }
    
    /** Draw the picture on a specified Tablet.  Subclasses
     *  should override this method to implement their own
     *  drawing. Use drawPart() to draw sub-pictures so that they are
     *  replaced with a solid fill if they are negligibly small.
     *
     *  paint is called with a trasfomr t that takes into account
     *  the desired aspect ratio of the picture: thus the picture
     *  should draw itself in a rectangle that is the image of the 
     *  unit square under t.  In the case of stretched pictures, 
     *  this rectangle will have an aspect ratio that is different from
     *  the picture's own aspect ratio. */
    protected void paint(int layer, int col, Tablet g, Tran2D t) {
	// Default: paint nothing
    }
    
    public void printOn(PrintWriter out) {
	out.print("<picture>");
    }
    
    /** Palette for Escher picture */
    protected static float hbase = 0.3f, hstep = 0.1f, 
	svalue = 0.5f, bvalue = 1.0f;

    public static int[] makePalette(float slider) {
	int palette[] = new int[4];
	float base = hbase + 2.0f * slider - 1.0f;
	for (int i = 0; i < 4; i++)
	    palette[i] = RGB.fromHSB(base + i * hstep, svalue, bvalue);
	return palette;
    }
	    
    /** A primitive that acts on pictures.  Special methods allow
     *  checked access to Picture or ColorValue arguments. */
    public abstract static class PicturePrimitive extends Primitive {
	public PicturePrimitive(String name, int arity) {
	    super(name, arity);
	}
	
	/** Access an argument that should be a picture */
	protected Picture picture(Value a) {
	    return cxt.cast(Picture.class, a, "picture");
	}
	
	/** Access an argument that should be a color */
	protected ColorValue color(Value a) {
	    return cxt.cast(ColorValue.class, a, "colour");
	}
    }
    
    private static class BesidePicture extends Picture {
	private static final long serialVersionUID = 1L;

	private final Picture left, right;
	private final Tran2D ltrans, rtrans;
	
	public BesidePicture(float aspect, Picture left, Tran2D ltrans,
		Picture right, Tran2D rtrans) {
	    super(aspect, left.isInteractive() || right.isInteractive());
	    this.left = left; this.right = right;
	    this.ltrans = ltrans; this.rtrans = rtrans;
	}

	public void paint(int layer, int col, Tablet g, Tran2D t) {
	    left.paintPart(layer, col, g, t.concat(ltrans));
	    right.paintPart(layer, col, g, t.concat(rtrans));
	}
    }

    private static class TransPicture extends Picture {
	private static final long serialVersionUID = 1L;

	private final Picture base;
	private final Tran2D trans;
	
	public TransPicture(float aspect, Picture base, Tran2D trans) {
	    super(aspect, base.isInteractive());
	    this.base = base; this.trans = trans;
	}

	public void paint(int layer, int col, Tablet g, Tran2D t) {
	    base.paintPart(layer, col, g, t.concat(trans));
	}
    }

    public static final Primitive primitives[] = {
	new Primitive("null", 0) {
	    public Value invoke(Value args[], int base) {
		return new Picture(0.0f);
	    }
	},
	
	new PicturePrimitive("$", 2) {
	    public Value invoke(Value args[], int base) {
		Picture left = picture(args[base+0]);
		Picture right = picture(args[base+1]);
		float la = left.getAspect(), ra = right.getAspect();
		if (la == 0)
		    return right;
		else if (ra == 0)
		    return left;
		else {
		    Tran2D ltrans = 
			Tran2D.scaling(la/(la+ra), 1);
		    Tran2D rtrans = 
			Tran2D.translation(la/(la+ra), 0).scale(ra/(la+ra), 1);
		    return 
		    	new BesidePicture(la+ra, left, ltrans, right, rtrans);
		}
	    }
	},
	
	new PicturePrimitive("&", 2) {
	    public Value invoke(Value args[], int base) {
		Picture top = picture(args[base+0]);
		Picture bottom = picture(args[base+1]);
		float ta = top.getAspect(), ba = bottom.getAspect();
		if (ta == 0)
		    return bottom;
		else if (ba == 0)
		    return top;
		else {
		    float aspect = ta*ba/(ta+ba);
		    Tran2D ttrans = 
			Tran2D.translation(0, ta/(ta+ba)).scale(1, ba/(ta+ba));
		    Tran2D btrans =
			Tran2D.scaling(1, ta/(ta+ba));
		    return 
		    	new BesidePicture(aspect, top, ttrans, bottom, btrans);
		}
	    }
	},
	
	new PicturePrimitive("super", 2) {
	    public Value invoke(Value args[], int base) {
		Picture lower = picture(args[base+0]);
		Picture upper = picture(args[base+1]);
		float la = lower.getAspect(), ua = upper.getAspect();
		Tran2D trans;
		if (la <= ua)
		    trans = Tran2D.translation(0, (1-la/ua)/2).scale(1, la/ua);
		else
		    trans = Tran2D.translation((1-ua/la)/2, 0).scale(ua/la, 1);
		return new BesidePicture(la, lower, Tran2D.identity,
			upper, trans);
	    }
	},
	
	new PicturePrimitive("rot", 1) {
	    public Value invoke(Value args[], int base) {
		/* A picture that has been rotated anticlockwise by 90 
		 * degrees.  The colours used for filling rotate in a 
		 * cycle of four too; this makes Escher pictures come 
		 * out nicely. */

		final Picture pic = picture(args[base+0]);
		final float r = pic.getAspect();
		final Tran2D trans = Tran2D.translation(1, 0).rot90();
		
		if (r == 0)
		    return pic;
		else {
		    return new TransPicture (1/r, pic, trans) {
		    	private static final long serialVersionUID = 1L;

		    	public void paint(int layer, int col, 
		    		Tablet g, Tran2D t) {
		    	    int col1 = (col >= 0 ? col+1 : col);
		    	    super.paint(layer, col1, g, t);
		    	}
		    };
		}
	    }
	},
	
	new PicturePrimitive("colour", 1) {
	    public Value invoke(Value args[], int base) {
		final Picture pic = picture(args[base+0]);

		return new Picture(pic.getAspect(), true) {
		    private static final long serialVersionUID = 1L;

		    public void paint(int layer, int col, 
			    Tablet g, Tran2D t) {
			/* Replace the colour index (presumably -1) with
			     zero, so indexed fills are enabled */
			pic.paintPart(layer, 0, g, t);
		    }
		};
	    }
	},
	
	new PicturePrimitive("flip", 1) {
	    public Value invoke(Value args[], int base) {
		final Picture pic = picture(args[base+0]);
		final Tran2D trans = Tran2D.translation(1, 0).scale(-1, 1);
		return new TransPicture(pic.getAspect(), pic, trans);
	    }
	},

	new PicturePrimitive("stretch", 2) {
	    public Value invoke(Value args[], int base) {
		final float r = (float) cxt.number(args[base+0]);
		final Picture pic = picture(args[base+1]);
		return new TransPicture(r * pic.getAspect(), 
			pic, Tran2D.identity);
	    }
	},
	
	new PicturePrimitive("aspect", 1) {
	    public Value invoke(Value args[], int base) {
		Picture pic = picture(args[base+0]);
		return makeNumValue(pic.getAspect());
	    }
	},
	
	/** Set the palette of colours used for rendering Escher picture. */
	new Primitive("palette", 4) {
	    public Value invoke(Value args[], int base) {
		hbase = (float) cxt.number(args[base+0]);
		hstep = (float) cxt.number(args[base+1]);
		svalue = (float) cxt.number(args[base+2]);
		bvalue = (float) cxt.number(args[base+3]);
		return Value.nil;
	    }
	},
	
	/** Save a picture on a file */
	new PicturePrimitive("savepic", 4) {
	    public Value invoke(Value args[], int base) {
		Picture pic = picture(args[base+0]);
		String fname = cxt.string(args[base+1]);
		int meanSize = (int) cxt.number(args[base+2]);
		float greyLevel = (float) cxt.number(args[base+3]);
		int background = RGB.fromRGB(greyLevel, greyLevel, greyLevel); 
		
		try {
		    GraphBox.writePicture(pic, meanSize, 0.5f, background, 
			    new File(fname));
		}
		catch (IOException e) {
		    cxt.primFail("I/O failed: " + e.getMessage());
		}
		
		return Value.nil;
	    }
	}
    };
}
