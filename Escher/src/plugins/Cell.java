/*
 * Cell.java
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


/** Assignable cells */
public class Cell extends Value {
    private static final String svnid =
	"$Id: Cell.java 356 2008-05-28 13:53:15Z mike $";
    private static final long serialVersionUID = 1L;
    
    protected Value contents;
    
    public Cell(Value contents) { 
	Evaluator.countCons();
	this.contents = contents; 
    }
   
    public void printOn(PrintWriter out) {
	out.print("ref ");
	contents.printOn(out);
    }
    
    public static final Primitive primitives[] = {
	new Primitive("new", 1) {
	    /** Allocate and initialize a new cell */
	    public Value invoke(Value args[], int base) {
		return new Cell(args[base+0]);
	    }
	},

	new Primitive("!", 1) {
	    /** Fetch the contents of a cell */
	    public Value invoke(Value args[], int base) {
		try {
		    Cell x = (Cell) args[base+0];
		    return x.contents;
		}
		catch (ClassCastException e) {
		    cxt.primFail("get expects a cell");
		    return null;
		}
	    }
	},

	new Primitive(":=", 2) {
	    /** Change the contents of a cell */
	    public Value invoke(Value args[], int base) {
		try {
		    Cell x = (Cell) args[base+0];
		    return (x.contents = args[base+1]);
		}
		catch (ClassCastException e) {
		    cxt.primFail(":= expects a cell");
		    return null;
		}
	    }
	}
    };
}
