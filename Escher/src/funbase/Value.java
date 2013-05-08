/*
 * Value.java
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


import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import funbase.Evaluator.Continuation;
import funbase.Evaluator.EvalException;
import funbase.Evaluator.Result;

/** Abstract superclass of all values in GeomLab */
public abstract class Value implements Serializable {
	private static final long serialVersionUID = 1L;
    
    /* The actual classes used to represent values are those contained
     * in this file, together with the classes Closure and Primitive -- plus
     * others that are installed dynamically, like ColorValue and Picture.
     * These classes are partially decoupled from the rest of the
     * system, and hidden behind static factory methods in the Value
     * class. */
    
    @Override
    public String toString() {
	StringWriter buf = new StringWriter();
	this.printOn(new PrintWriter(buf));
	return buf.toString();
    }
    
    /** Print the value on a stream */
    public abstract void printOn(PrintWriter out);
    
    /** Dump the value to standard output in boot format */
    public void dump() {
	throw new EvalException(String.format("can't dump a %s",
		this.getClass()), null, "#nohelp");
    }
    
    /** Apply the value to a list of arguments */
    public Result apply(Value args[], int base, int nargs, 
	    ErrContext cxt, Continuation k) {
	assert cxt != null;
	throw new EvalException("applying a non-function", 
		cxt, "#apply");
    }
    
    /** Match the value as a constructor */
    public boolean pattMatch(Value obj, Value stack[], int base, int nargs, 
	    ErrContext cxt) {
	throw new EvalException("matching with a non-constructor", 
		cxt, "#constr");
    }
    
    public void freeze() { }
    
    // Type tests
    
    public boolean isConsValue() { return (this instanceof ConsValue); }
    public boolean isNilValue() { return (this instanceof NilValue); }
    public boolean isNumValue() { return (this instanceof NumValue); }
    
    // Accessors: the default implementations raise WrongKindException.
    // (see also Primitive.head etc.)
    
    public boolean asBoolean() throws WrongKindException {
	throw new WrongKindException();
    }
    
    public double asNumber() throws WrongKindException {
	throw new WrongKindException();
    }
    
    public String asString() throws WrongKindException {
	throw new WrongKindException();
    }
    
    public Value getHead() throws WrongKindException {
	throw new WrongKindException();
    }
    
    public Value getTail() throws WrongKindException {
	throw new WrongKindException();
    }
    
    public void setTail(Value tail) throws WrongKindException {
	throw new WrongKindException();
    }
    
    // Factory methods
    
    public static Value makeNumValue(double val) { 
	return new NumValue(val);
    }
    
    public static Value makeBoolValue(boolean val) {
	return BoolValue.getInstance(val);
    }
    
    public static Value makeStringValue(char ch) {
	return StringValue.getInstance(ch);
    }
    
    public static Value makeStringValue(String text) {
	return StringValue.getInstance(text);
    }
    
    public static final Value nil = NilValue.getInstance();
    
    public static Value cons(Value hd, Value tl) {
	return new ConsValue(hd, tl);
    }
    
    public static Value makeList(Value... elems) {
	return makeList(elems, 0, elems.length);
    }
    
    public static Value makeList(Value elems[], int base, int count) {
        Value val = nil;
        for (int i = count-1; i >= 0; i--)
            val = cons(elems[base+i], val);
        return val;
    }
    
    public static void printNumber(PrintWriter out, double x) {
	if (x == (int) x)
	    out.print((int) x);
	else if (Double.isNaN(x))
	    out.print("NaN");
	else {
	    if (x < 0.0) {
		out.print('-');
		x = -x;
	    }
	    if (Double.isInfinite(x))
		out.print("Infinity");
	    else {
		// Sometimes stupid persistence is the best way ...
		String pic;
		if (x < 0.001)           pic = "0.0######E0";
		else if (x < 0.01)       pic = "0.000#######";
		else if (x < 0.1)        pic = "0.00#######";
		else if (x < 1.0)        pic = "0.0#######";
		else if (x < 10.0)       pic = "0.0######";
		else if (x < 100.0)      pic = "#0.0#####";
		else if (x < 1000.0)     pic = "##0.0####";
		else if (x < 10000.0)    pic = "###0.0###";
		else if (x < 100000.0)   pic = "####0.0##";
		else if (x < 1000000.0)  pic = "#####0.0#";
		else if (x < 10000000.0) pic = "######0.0";
		else                     pic = "0.0######E0";
		NumberFormat fmt = new DecimalFormat(pic);
		out.print(fmt.format(x));
	    }
	}
    }

    /** Exception that is thrown when accessors are applied to the
     *  wrong kind of Value */
    public class WrongKindException extends Exception { }
    
    /** A numeric value represented as a double-precision float */
    private static class NumValue extends Value {
	private static final long serialVersionUID = 1L;
	
	/** The value */
	private double val;
	
	public NumValue(double val) {
	    this.val = val;
	}
	
	public void printOn(PrintWriter out) {
	    Value.printNumber(out, val);
	}

	public double asNumber() {
	    return val;
	}
	
	public boolean equals(Object a) {
	    return (a instanceof NumValue && val == ((NumValue) a).val);
	}
	
	@Override
	public void dump() {
	    if (val == (int) val)
		System.out.printf("integer %d\n", (int) val);
	    else
		System.out.printf("real %.12g\n", val);
	}
    }
    
    /** A boolean value */
    private static class BoolValue extends Value {
	private static final long serialVersionUID = 1L;
	
	private boolean val;
	
	private BoolValue(boolean val) {
	    this.val = val;
	}
	
	public void printOn(PrintWriter out) {
	    out.print((val ? "true" : "false"));
	}
	
	public boolean asBoolean() { return val; }
	
	public boolean equals(Object a) {
	    return (a instanceof BoolValue && val == ((BoolValue) a).val);
	}
	
	/** Singletons */
	private static BoolValue truth = new BoolValue(true), 
	    falsity = new BoolValue(false);
	
	public static BoolValue getInstance(boolean val) {
	    return (val ? truth : falsity);
	}
	
	/* After input from a serialized stream, readResolve lets us replace
	 * the constructed instance with one of the standard instances. */
	public Object readResolve() { return getInstance(val); }
	
	@Override
	public void dump() {
	    System.out.printf("boolean %d\n", (val ? 1 : 0));
	}
    }
    
    private static class StringValue extends Value {
	private static final long serialVersionUID = 1L;
	
	private String text;
	
	private StringValue(String text) {
	    Evaluator.countCons();
	    this.text = text;
	}
	
	public void printOn(PrintWriter out) {
	    out.format("\"%s\"", text);
	}
	
	public String asString() { return text; }
	public String toString() { return text; }
	
	public boolean equals(Object a) {
	    return (a instanceof StringValue 
		    && text.equals(((StringValue) a).text));
	}

	/** The empty string as a value */
	private static Value emptyString = new StringValue("");

	/** Singletons for one-character strings */
	private static Value charStrings[] = new StringValue[256];

	/* The "explode" primitive can create lists of many one-character 
	 * strings, so we create shared instances in advance */
	static {
	    for (int i = 0; i < 256; i++)
		charStrings[i] = new StringValue(String.valueOf((char) i));
	}

	public static Value getInstance(char ch) {
	    if (ch < 256)
		return charStrings[ch];

	    return new StringValue(String.valueOf(ch));
	}

	public static Value getInstance(String text) {
	    if (text.length() == 0)
		return emptyString;
	    else if (text.length() == 1)
		return charStrings[text.charAt(0)];
	    else
		return new StringValue(text);
	}

	/* After input from a serialized stream, readResolve lets us replace
	 * the constructed instance with a singleton. */
	public Object readResolve() {
	    if (text.length() < 2)
		return getInstance(text);
	    else
		return this;
	}
	
	@Override
	public void dump() {
	    System.out.printf("string \"%s\"\n", text);
	}
    }
    
    private static class NilValue extends Value {
	private static final long serialVersionUID = 1L;
	
	private NilValue() { }
	
	public void printOn(PrintWriter out) {
	    out.print("[]");
	}
	
	public boolean equals(Object a) {
	    return (a instanceof NilValue);
	}
	
	private static final NilValue instance = new NilValue();
	
	public static NilValue getInstance() { return instance; }
	
	public Object readResolve() { return getInstance(); }
	
	@Override
	public void dump() {
	    System.out.printf("nil\n");
	}
    }
    
    private static class ConsValue extends Value {
	private static final long serialVersionUID = 1L;
	
	private Value car, cdr;
	
	public ConsValue(Value car, Value cdr) {
	    Evaluator.countCons();
	    this.car = car;
	    this.cdr = cdr;
	}
	
	public void printOn(PrintWriter out) {
	    out.print("[");
	    car.printOn(out);
	    
	    Value xs = cdr;
	    while (xs instanceof ConsValue) {
		ConsValue cons = (ConsValue) xs;
		out.print(", ");
		cons.car.printOn(out);
		xs = cons.cdr;
	    }
	    if (! (xs instanceof NilValue)) {
		out.print(" . ");
		xs.printOn(out);
	    }
	    out.print("]");
	}
	
	public Value getHead() { return car; }
	public Value getTail() { return cdr; }
	public void setTail(Value tail) { cdr = tail; }
	
	public boolean equals(Object a) {
	    if (! (a instanceof ConsValue)) return false;
	    ConsValue acons = (ConsValue) a;
	    return (car.equals(acons.car) && cdr.equals(acons.cdr));
	}
    }
}
