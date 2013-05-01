/*
 * TurtlePicture.java
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


/** A picture drawn with a sequence of left, ahead, right commands. */
public class TurtlePicture extends Picture {
    private static final String svnid =
	"$Id: TurtlePicture.java 365 2008-06-11 17:11:29Z mike $";
    private static final long serialVersionUID = 1L;
    
    public final Command commands[];
    public final float xmin, xmax, ymin, ymax;
    
    public static final float R = 0.5f;
    private static final int STROKE_COLOR = RGB.BLACK;

    public TurtlePicture(Command commands[]) {
	super(calcAspect(commands));
	this.commands = commands;
	this.xmin = _xmin; this.xmax = _xmax;
	this.ymin = _ymin; this.ymax = _ymax;
    }

    // Global variables (yuck!) for values returned by calcAspect
    private static float _xmin, _xmax, _ymin, _ymax;

    /** Calculate aspect ratio and set _xmin, etc. */
    public static float calcAspect(Command commands[]) {
	float x = 0, y = 0, dir = 0;
	float xmin = -1, xmax = 1, ymin = -1, ymax = 1;
	
	for (Command cmd : commands) {
	    switch (cmd.kind) {
		case Command.LEFT: {
		    float a = cmd.arg;
		    float xc = x - R * Vec2D.sind(dir);
		    float yc = y + R * Vec2D.cosd(dir);
		    x = xc + R * Vec2D.sind(dir+a); 
		    y = yc - R * Vec2D.cosd(dir+a);
		    dir += a;
		    break;
		}

		case Command.RIGHT: {
		    float a = cmd.arg;
		    float xc = x + R * Vec2D.sind(dir);
		    float yc = y - R * Vec2D.cosd(dir);
		    x = xc - R * Vec2D.sind(dir-a); 
		    y = yc + R * Vec2D.cosd(dir-a);
		    dir -= a;
		    break;
		}

		case Command.AHEAD: {
		    x += cmd.arg * Vec2D.cosd(dir);
		    y += cmd.arg * Vec2D.sind(dir);
		}
	    }

	    // Update min/max values
	    if (x-1 < xmin) xmin = x-1;
	    if (x+1 > xmax) xmax = x+1;
	    if (y-1 < ymin) ymin = y-1;
	    if (y+1 > ymax) ymax = y+1;
	}
	
	_xmin = xmin; _xmax = xmax;
	_ymin = ymin; _ymax = ymax; 
	return (xmax - xmin)/(ymax - ymin); 
    }
    
    /** Make a vector by scaling coords */
    private Vec2D posVec(float x, float y) {
	return new Vec2D((x-xmin)/(xmax-xmin), (y-ymin)/(ymax-ymin));
    }
    
    protected void paint(int layer, int c, Tablet g, Tran2D t) {
	if (layer == DRAW) g.drawPath(this, t);
    }
    
    public void defaultDraw(Tablet g, Tran2D t) {
	g.save();
	g.setStroke(2);

	float x = 0, y = 0, dir = 0;

	for (Command cmd : commands) {
	    switch (cmd.kind) {
		case Command.LEFT: {
		    float a = cmd.arg;
		    float xc = x - R * Vec2D.sind(dir);
		    float yc = y + R * Vec2D.cosd(dir);
		    Vec2D centre = posVec(xc, yc);
		    g.drawArc(centre, R/(xmax-xmin), R/(ymax-ymin),
			    dir-90, a, STROKE_COLOR, t);
		    x = xc + R * Vec2D.sind(dir+a); 
		    y = yc - R * Vec2D.cosd(dir+a);
		    dir += a;
		    break;
		}

		case Command.RIGHT: {
		    float a = cmd.arg;
		    float xc = x + R * Vec2D.sind(dir);
		    float yc = y - R * Vec2D.cosd(dir);
		    Vec2D centre = posVec(xc, yc);
		    g.drawArc(centre, 0.5f/(xmax-xmin), 0.5f/(ymax-ymin),
			    dir+90, -a, STROKE_COLOR, t);
		    x = xc - R * Vec2D.sind(dir-a); 
		    y = yc + R * Vec2D.cosd(dir-a);
		    dir -= a;
		    break;
		}
		    
		case Command.AHEAD: {
		    Vec2D oldpos = posVec(x, y);
		    x += cmd.arg * Vec2D.cosd(dir); 
		    y += cmd.arg * Vec2D.sind(dir);
		    Vec2D pos = posVec(x, y);
		    g.drawLine(oldpos, pos, STROKE_COLOR, t);
		    break;
		}
	    }
	}
	
	g.restore();
    }
    
    /** A turtle command */
    public static class Command extends Value {
	private static final long serialVersionUID = 1L;

	/** Values for kind */
	public static final int LEFT = 0, RIGHT = 1, AHEAD = 2;
	
	/** The kind of command */
	public final int kind;

	/** Argument for the command */
	public final float arg;
	
	public Command(int kind, float arg) {
	    Evaluator.countCons();
	    this.kind = kind;
	    this.arg = arg;
	}
	
	public String name() {
	    switch (kind) {
		case LEFT: return "left";
		case RIGHT: return "right";
		case AHEAD: return "ahead";
		default: return "*unknown-command*";
	    }
	}
	
	public void printOn(PrintWriter pr) {
	    pr.print(name());
	    pr.print("(");
	    Value.printNumber(pr, arg);
	    pr.print(")");
	}

	public boolean equals(Object a) {
	    return (a instanceof Command && this.equals((Command) a));
	}

	public boolean equals(Command a) {
	    return (this.kind == a.kind && this.arg == a.arg);
	}
    }

    /** A constructor primitive for commands */
    private static class CommandPrimitive extends Primitive.Constructor {
	private int kind;
	
	public CommandPrimitive(String name, int kind) {
	    super(name, 1);
	    this.kind = kind;
	}
	
	public Value invoke(Value args[], int base) {
	    return new Command(kind, (float) cxt.number(args[base+0]));
	}
	
	public boolean match(Value obj, Value args[], int base) {
	    try {
		Command c = (Command) obj;
		if (c.kind != kind) return false;
		args[base+0] = Value.makeNumValue(c.arg);
		return true;
	    }
	    catch (ClassCastException _) {
		return false;
	    }
	}
    }
    
    public static final Primitive primitives[] = {
	new Primitive("turtle", 1) {
	    public Value invoke(Value args[], int base) {
		Value xs = args[base+0];
		Command commands[] = new Command[cxt.listLength(xs)];
		
		// Convert the commands to an array of Command objects
		try {
		    for (int i = 0; i < commands.length; i++) { 
			commands[i] = (Command) cxt.head(xs);
			xs = cxt.tail(xs);
		    }
		}
		catch (ClassCastException e) {
		    cxt.expect("command");
		}
		
		return new TurtlePicture(commands);
	    }	    
	},
	
	new CommandPrimitive("ahead", Command.AHEAD),
	new CommandPrimitive("left", Command.LEFT),
	new CommandPrimitive("right", Command.RIGHT)
    };
}
