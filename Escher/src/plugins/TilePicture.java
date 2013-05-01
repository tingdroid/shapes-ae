/*
 * TilePicture.java
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

import funbase.Primitive;
import funbase.Value;

/** A picture with fills and strokes specified by lists of coordinates */
public class TilePicture extends Picture {
    private static final String svnid =
	"$Id: TilePicture.java 365 2008-06-11 17:11:29Z mike $";
    private static final long serialVersionUID = 1L;

    private static int ntiles = 0;
    
    public final int tileid = ntiles++;
    protected Vec2D strokes[][];
    protected Vec2D outlines[][];
    protected int colours[];

    public TilePicture(float width, float height, 
	    Vec2D strokes[][], Vec2D outlines[][], int colours[]) {
	super(width / height);
	this.strokes = strokes;
	this.outlines = outlines;
	this.colours = colours;
    }
    
    public void paint(int layer, int col, Tablet g, Tran2D t) {
	g.drawTile(this, layer, col, t);
    }

    public void defaultDraw(int layer, int col, Tablet g, Tran2D t) {
	switch (layer) {
	case Picture.DRAW:
	    for (int i = 0; i < strokes.length; i++)
		g.drawStroke(strokes[i], t);
	    break;

	case Picture.FILL:
	    for (int i = 0; i < outlines.length; i++)
		g.fillOutline(outlines[i], colours[i], col, t);
	    break;
	}
    }

    public static final Primitive primitives[] = {
	new PicturePrimitive("tile", 6) {
	    private float width, height, xshift, yshift;

	    public Value invoke(Value args[], int base) {
		width = (float) cxt.number(args[base+0]);
		height = (float) cxt.number(args[base+1]);
		xshift = (float) cxt.number(args[base+2]);
		yshift = (float) cxt.number(args[base+3]);
		int nStrokes = cxt.listLength(args[base+4]);
		int nOutlines = cxt.listLength(args[base+5]);
		Vec2D strokes[][] = new Vec2D[nStrokes][], 
		outlines[][] = new Vec2D[nOutlines][];
		int colours[] = new int[nOutlines];
		Value xss;

		xss = args[base+4];
		for (int i = 0; i < nStrokes; i++) {
		    strokes[i] = convertPolygon(cxt.head(xss));
		    xss = cxt.tail(xss);
		}

		xss = args[base+5];
		for (int i = 0; i < nOutlines; i++) {
		    Value xs = cxt.head(xss);
		    Value spec = cxt.head(xs);
		    if (spec instanceof ColorValue)
			colours[i] = color(spec).asColor();
		    else if (spec.isNumValue())
			colours[i] = (int) cxt.number(spec);
		    else
			cxt.expect("colour or integer");
		    outlines[i] = convertPolygon(cxt.tail(xs));
		    xss = cxt.tail(xss);
		}

		return new TilePicture(width, height, strokes, 
			outlines, colours);
	    }
	    
	    private Vec2D[] convertPolygon(Value xs) {
		int nPoints = cxt.listLength(xs) / 2;
		Vec2D poly[] = new Vec2D[nPoints];
		for (int i = 0; i < nPoints; i++) {
		    float x = (float) cxt.number(cxt.head(xs)) + xshift, 
		    y = (float) cxt.number(cxt.head(cxt.tail(xs))) + yshift;
		    poly[i] = new Vec2D(x/width, y/height);
		    xs = cxt.tail(cxt.tail(xs));
		}
		return poly;
	    }
	}
    };
}