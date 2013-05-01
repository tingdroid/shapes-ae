/*
 * Primitive.java
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

package funbase;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import funbase.Evaluator.Continuation;
import funbase.Evaluator.EvalException;
import funbase.Evaluator.Result;

/** A value that represents a primitive function like 'sqrt' or '+'. */
public abstract class Primitive extends Function {
    
    /** Context of latest invocation */
    public transient ErrContext cxt;
    
    protected Primitive(String name, int arity) {
	super(name, arity);
    }
    
    /** Compute the result of the primitive, assuming correct number 
        of args. */
    protected Value invoke(Value args[], int base) {
	// self subclassResponsibility
	throw new EvalException("primitive " + name 
		+ " has no invoke method", cxt);
    }
    
    @Override
    public Result apply(Value args[], int base, int nargs, 
	    ErrContext cxt, Continuation k) {
	assert cxt != null;
	if (nargs != arity) badArity(nargs, cxt);
	this.cxt = cxt; cxt.setCallee(name);
	return k.result(this.invoke(args, base));
    }
    
    public boolean equals(Object a) { return false; }
    
    public void printOn(PrintWriter out) {
	out.format("<primitive %s>", name);
    }
    
    public static abstract class Constructor extends Primitive {
	public Constructor(String name, int arity) {
	    super(name, arity);
	}

	@Override
	public boolean pattMatch(Value obj, Value stack[], int base, int nargs, 
		ErrContext cxt) {
	    if (nargs != arity)
		throw new Evaluator.EvalException("matching constructor '" 
			+ name + "' with wrong number of arguments",
			cxt, "#patnargs");
	    return match(obj, stack, base);
	}
	
	public abstract boolean match(Value obj, Value stack[], int base);
    }
    
    /** Table of all primitives */
    protected static Map<String, Primitive> primitives = 
	new HashMap<String, Primitive>(100);
    
    /** Register a new primitive */
    public static void register(Primitive p) {
	primitives.put(p.name, p);
    }
    
    /** Find a registered primitive */
    public static Primitive find(String name) {
	Primitive prim = primitives.get(name);
	if (prim == null)
	    throw new EvalException("Primitive " + name 
		    + " is not defined", null);
	return prim;
    }
    
    /** Discard all registered primitives */
    public static void clearPrimitives() {
	primitives.clear();
    }
    
    /* Primitives are replaced by Memento objects when making a serialized 
     * stream. This provides independence of the stream from the particular 
     * classes that are used to implement primitives.  Since these are mostly
     * anonymous inner classes scattered throughout the code, this is a
     * very good thing. */
    
    protected Object writeReplace() throws ObjectStreamException {
	return new Memento(this);
    }
    
    /** Serialized substitute for a primitive */
    private static class Memento implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String name;
	
	public Memento(Primitive prim) {
	    this.name = prim.name;
	}
	
	private Object readResolve() throws ObjectStreamException {
	    /* Replace the memento by the genuine primitive */
	    Object prim = primitives.get(name);
	    if (prim == null)
		throw new InvalidObjectException(
			"Primitive " + name + " could not be found");
	    return prim;
	}
	
    }
    
    @Override
    public void dump() {
	System.out.printf("primitive \"%s\"\n", name);
    }
}
