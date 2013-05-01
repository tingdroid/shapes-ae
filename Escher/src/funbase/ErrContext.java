/*
 * ErrContext.java
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

import java.io.Serializable;

import funbase.Evaluator.EvalException;
import funbase.Value.WrongKindException;

/** Context for runtime error messages */
public class ErrContext implements Serializable {
    private String name, callee;
    
    public ErrContext(String name) {
        this.name = name;
    }
    
    public String format(String message) {
	return (name == null ? message : message + " in function " + name);
    }
    
    public void setCallee(String callee) { this.callee = callee; }
    
    public String getName() { return name; }
    
    public ErrContext parent() { return this; }
    
    public String getFunction() { return callee; }
    
    public ErrContext freezeEnter(String name) {
	return new FrozenContext(this, name);
    }
    
    public static final ErrContext initContext = new ErrContext(null);

    /** A context that hides internal functions of the library */
    public static class FrozenContext extends ErrContext {
        /* For a library function implemented as a closure, we want errors
         * that are detected in the primitives it calls to be reported as if
         * they occurred in the first library function on the call stack, and
         * we want to know which user function called it. */
        
        private final ErrContext parent;
        
        public FrozenContext(ErrContext parent, String name) {
            super(name);
            this.parent = parent;
        }
        
        @Override
        public String toString() {
            String caller = parent.getName();
            if (caller == null)
        	return "in function " + getName();
            else
        	return "in function " + getName() + ", called from " + caller;
        }
        
        @Override
        public ErrContext parent() { return parent; }
        
        @Override
        public String getFunction() { return getName(); }
        
        @Override
        public ErrContext freezeEnter(String name) { return this; }
    }
    
    // Utility methods for primitives
    
    public void primFail(String msg) {
        throw new EvalException(msg, parent());
    }

    public void primFail(String msg, String errtag) {
        throw new EvalException(msg, parent(), errtag);
    }

    public void expect(String expected) {
        primFail("'" + getFunction() + "' expects a " 
        	+ expected + " argument", "#type");
    }

    /** Fetch value of a NumValue object, or throw EvalException */
    public double number(Value a) {
        try {
            return a.asNumber();
        }
        catch (WrongKindException e) {
            expect("numeric");
            return 0.0;
        }
    }

    /** Fetch value of a BoolValue object, or throw EvalException */ 
    public boolean bool(Value a) {
        try {
            return a.asBoolean();
        }
        catch (WrongKindException e) {
            expect("boolean");
            return false;
        }
    }

    /** Fetch value of a StringValue object, or throw EvalException */ 
    public String string(Value a) {
        try {
            return a.asString();
        }
        catch (WrongKindException e) {
            expect("string");
            return null;
        }
    }
    
    /** Fetch head of a ConsValue object, or throw EvalException */ 
    public Value head(Value xs) {
        try {
            return xs.getHead();
        }
        catch (WrongKindException e) {
            primFail("taking head of " 
        	    + (xs.isNilValue() ? "the empty list" : "a non-list"),
        	    "#head");
            return null;
        }
    }

    /** Fetch tail of a ConsValue object, or throw EvalException */ 
    public Value tail(Value xs) {
        try {
            return xs.getTail();
        }
        catch (WrongKindException e) {
            primFail("taking tail of " 
        	    + (xs.isNilValue() ? "the empty list" : "a non-list"),
        	    "#tail");
            return null;
        }
    }

    /** Compute length of a list argument */ 
    public int listLength(Value xs) {
        int n = 0;
        while (xs.isConsValue()) {
            xs = tail(xs); n++;
        }
        if (! xs.isNilValue()) expect("list");
        return n;
    }

    public <T extends Value> T cast(Class<T> cl, Value v, String expected) {
        try {
            return cl.cast(v);
        }
        catch (ClassCastException _) {
            expect(expected);
            return null;
        }
    }
}