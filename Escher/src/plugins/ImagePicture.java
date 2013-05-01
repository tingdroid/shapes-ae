/*
 * ImagePicture.java
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

import funbase.ErrContext;
import funbase.Evaluator;
import funbase.Evaluator.Continuation;
import funbase.Evaluator.EvalException;
import funbase.Evaluator.Result;
import funbase.Primitive;
import funbase.Value;
import geomlab.Image;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.URL;

public class ImagePicture extends Picture {
    private static final String svnid =
	"$Id: ImagePicture.java 365 2008-06-11 17:11:29Z mike $";
    private static final long serialVersionUID = 1L;
    
    public transient Image image;
    private String resourceName = null;
    
    public ImagePicture(Image image, String resourceName) {
	super((float) image.getWidth() / image.getHeight());
	this.image = image;
	this.resourceName = resourceName;
    }
    
    public ImagePicture(Image image) {
	this(image, null);
    }
    
    public void printOn(PrintWriter w) {
	w.print("<image>");
    }
    
    protected void paint(int layer, int col, Tablet g, Tran2D t) {
	if (layer != FILL) return;
	g.drawImage(image, t);
    }
    
    private void writeObject(ObjectOutputStream stream) throws IOException {
	stream.defaultWriteObject();
	
	if (resourceName != null) return;

	int w = image.getWidth(), h = image.getHeight();
	stream.writeInt(image.getWidth());
	stream.writeInt(image.getHeight());
	for (int y = 0; y < h; y++)
	    for (int x = 0; x < w; x++)
		stream.writeInt(image.getRGB(x, y));
    }
    
    private void readObject(ObjectInputStream stream) 
    		throws IOException, ClassNotFoundException {
	stream.defaultReadObject();
	
	if (resourceName != null) {
	    image = Image.fromResource(resourceName);
	    return;
	}
	
	int w = stream.readInt();
	int h = stream.readInt();
	image = new Image(w, h, Image.TYPE_INT_RGB);
	for (int y = 0; y < h; y++)
	    for (int x = 0; x < w; x++)
		image.setRGB(x, y, stream.readInt());
    }
    
    /** A process that controls filling in an image by calling a function
     	for each pixel. */
    private static class PixelLoop extends Continuation implements Result {
	private final Image image;
	private final int width, height;
	private final Value fun;
	private final ErrContext cxt;
	private final Continuation cont;
	
	private int x = 0, y = 0;
	
	public PixelLoop(int width, int height, Value fun, 
		ErrContext cxt, Continuation cont) {
	    this.width = width;
	    this.height = height;
	    this.image = 
		new Image(width, height, Image.TYPE_INT_RGB);
	    this.fun = fun;
	    this.cxt = cxt;
	    this.cont = cont;
	}
	
	public Result resume(Evaluator ev) {
	    if (y < height) 
		return fun.apply(new Value[] { Value.makeNumValue(x),
			Value.makeNumValue(y) }, 0, 2, cxt, this);
	    else
		return cont.result(new ImagePicture(image));
	}
	
	public Result result(Value v) {
	    try {
		int rgb = ((ColorValue) v).rgb;
		image.setRGB(x, height-y-1, rgb);
	    }
	    catch (ClassCastException _) {
		throw new EvalException("'funpic' expects a colour", cxt);
	    }

	    if (++x >= width) { x = 0; y++; }
	    return this;
	}
    }
    
    public static Value fromResource(String name) {
	try {
	    return new ImagePicture(Image.fromResource(name), name);
	}
	catch (IOException e) {
	    return Value.nil;
	}
    }
    
    public static final Primitive primitives[] = {
	new Primitive("photo", 1) {
	    public Value invoke(Value args[], int base) {
		try {
		    String name = cxt.string(args[base + 0]);
		    if (name.indexOf(':') < 0)
			name = "file:" + name;
		    Image image = Image.fromUrl(new URL(name));
		    if (image == null)
			cxt.primFail("Error loading photo: " + name);
		    return new ImagePicture(image);
		}
		catch (IOException e) {
		    cxt.primFail("Image I/O error - " + e);
		    return null;
		}
	    }
	},
	
	new Primitive("resource", 1) {
	    public Value invoke(Value args[], int base) {
		try {
		    String name = cxt.string(args[base+0]);
		    Image image = Image.fromResource(name);
		    if (image == null) cxt.primFail("Error loading resource: " + name);
		    return new ImagePicture(image, name);
		}
		catch (IOException e) {
		    cxt.primFail("Image I/O error - " + e);
		    return Value.nil;
		}
	    }
	},
	
	new Primitive("image", 3) {
	    @Override
	    public Result apply(Value[] args, int base, int nargs, 
		    ErrContext cxt, Continuation k) {
		if (nargs != arity) badArity(nargs, cxt);
		this.cxt = cxt;
		int width = (int) cxt.number(args[base+0]);
		int height = (int) cxt.number(args[base+1]);
		Value fun = args[base+2];
		return new PixelLoop(width, height, fun, cxt, k);
	    }
	},
	
	new Primitive("pixel", 3) {
	    public Value invoke(Value args[], int base) {
		try {
		    ImagePicture p = (ImagePicture) args[base+0];
		    int w = p.image.getWidth(), h = p.image.getHeight();
		    int x = (int) Math.round(cxt.number(args[base+1]));
		    int y = (int) Math.round(cxt.number(args[base+2]));
		    if (0 <= x && x < w && 0 <= y && y < h) {
			int rgb = p.image.getRGB(x, h-y-1);
			return new ColorValue(rgb);
		    }
		    else {
			return ColorValue.white;
		    }
		}
		catch (ClassCastException e) {
		    cxt.expect("image");
		    return null;
		}
	    }
	},
	
	new Primitive("width", 1) {
	    public Value invoke(Value args[], int base) {
		try {
		    ImagePicture p = (ImagePicture) args[base+0];
		    return Value.makeNumValue(p.image.getWidth());
		}
		catch (ClassCastException e) {
		    cxt.expect("image");
		    return null;
		}
	    }
	},
	
	new Primitive("height", 1) {
	    public Value invoke(Value args[], int base) {
		try {
		    ImagePicture p = (ImagePicture) args[base+0];
		    return Value.makeNumValue(p.image.getHeight());
		}
		catch (ClassCastException e) {
		    cxt.expect("image");
		    return null;
		}
	    }
	}

    };
}
