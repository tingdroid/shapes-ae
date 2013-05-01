/*
 * ColorValue.java
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

import java.io.PrintWriter;

import funbase.Evaluator;
import funbase.Primitive;
import funbase.Value;


/** A colour wrapped as a value */
public class ColorValue extends Value {
    private static final String svnid =
	"$Id: ColorValue.java 365 2008-06-11 17:11:29Z mike $";
    private static final long serialVersionUID = 1L;

    /* The most computationally intensive use of colours is in
     * computing bitmap images with the image() primitive.  So we want
     * to be able to create a colour and find its RGB encoding as
     * efficiently as possible, without needing to create an AWT Color
     * object. */
    
    public final double r, g, b;
    public final int rgb;
    private int color = 0;
    
    public ColorValue(double rf, double gf, double bf) {
	Evaluator.countCons();
	r = cutoff(rf); g = cutoff(gf); b = cutoff(bf);
	int rx = Math.round(255.0f * (float) r); 
	int gx = Math.round(255.0f * (float) g);
	int bx = Math.round(255.0f * (float) b);
	rgb = (rx << 16) + (gx << 8) + bx;
    }
    
    public ColorValue(int rgb) {
	Evaluator.countCons();
	this.rgb = rgb;
	r = ((rgb >> 16) & 0xff)/255.0;
	g = ((rgb >> 8) & 0xff)/255.0;
	b = (rgb & 0xff)/255.0;
    }
    
    public int asColor() { 
	return rgb | 0xff000000; 
    }
    
    public void printOn(PrintWriter out) {
	out.print("rgb("); Value.printNumber(out, r); out.print(", ");
	Value.printNumber(out, g); out.print(", ");
	Value.printNumber(out, b); out.print(")");
    }
    
    public static final ColorValue white = new ColorValue(1.0f, 1.0f, 1.0f);
    
    public static double cutoff(double arg) {
	if (arg < 0.0)
	    return 0.0;
	else if (arg > 1.0)
	    return 1.0;
	else 
	    return arg;
    }
    
    public static final Primitive primitives[] =  {
	new Primitive.Constructor("rgb", 3) {
	    /* Create a colour from RGB values in the range [0, 1] */
	    public Value invoke(Value args[], int base) {
		return new ColorValue(cxt.number(args[base+0]),
			cxt.number(args[base+1]), cxt.number(args[base+2]));
	    }
	    
	    public boolean match(Value obj, Value args[], int base) {
		try {
		    ColorValue v = (ColorValue) obj;
		    args[base+0] = Value.makeNumValue(v.r);
		    args[base+1] = Value.makeNumValue(v.g);
		    args[base+2] = Value.makeNumValue(v.b);
		    return true;
		}
		catch (ClassCastException _) {
		    return false;
		}
	    }
	},
	
	new Primitive("hsv", 3) {
	    /* Create a colour from HSV values in the range [0, 1] */
	    public Value invoke(Value args[], int base) {
		int rgb = RGB.fromHSB(
			(float) cxt.number(args[base+0]),
			(float) cutoff(cxt.number(args[base+1])),
			(float) cutoff(cxt.number(args[base+2])));
		return new ColorValue(rgb);
	    }
	}
    };
}
