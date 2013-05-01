/*
 * Evaluator.java
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

import funbase.Machine.ByteCode;

/** This class provides the context for evaluating paragraphs: it imposes
 *  a time limit, and provides the final continuations that are invoked
 *  when evaluation is complete. */
public class Evaluator {    
    private final Value phrase;
    private final String text;
    private final boolean display;
    private final PrintWriter out;
    private Value answer = null;

    protected static boolean runFlag;
    private static int steps;
    private static int conses;
	
    protected static int timeLimit = 10000;
    protected static int stepLimit = 200000;
    protected static int consLimit = 100000;

    public Evaluator(Value phrase, String text, boolean display,
		     PrintWriter out) {
	this.phrase = phrase;
	this.text = text;
	this.display = display;
	this.out = out;
    }
    
    private static Result compileAndGo(Value p) {
	Value toplevel = Name.find("_top").getGlodef();

	Continuation go = 
	    new Continuation() {
		public Result result(final Value v) {
		    return new Result() {
			public Result resume(Evaluator ev) {
			    ev.reset();
			    return new Machine((ByteCode) v);
			}
		    };
		}
	    };
	
	return toplevel.apply(new Value[] { p }, 
			      0, 1, ErrContext.initContext, go);
    }


    public Value execute() {
	Thread timer = null;
	
	if (timeLimit > 0) {
	    timer = new Thread() {
		public synchronized void run() {
		    try {
			wait(timeLimit);
			runFlag = false;
		    }
		    catch (InterruptedException e) { }
		}
	    };
	    timer.start();
	}

	conses = 0; steps = 0; runFlag = true;
	try {
	    Result result = compileAndGo(phrase);
	    while (result != null) {
		checkpoint();
		result = result.resume(this);
	    }
	}
	catch (StackOverflowError e) {
	    throw new EvalException("recursion went too deep", 
		    null, "#stack");
	}
	finally {
	    if (timer != null) timer.interrupt();
	}
	
	return answer;
    }

    public static void checkpoint() {
	steps++;
	if (! runFlag) timeout("long");
	if (stepLimit > 0 && steps > stepLimit)
	    timeout("many steps");
	if (consLimit > 0 && conses > consLimit) 
	    timeout("much memory");
    }
    
    private static void timeout(String resource) {
	throw new EvalException("sorry, that took too " + resource, 
		ErrContext.initContext, "#time");
    }
    
    public static void countCons() { conses++; }
    
    public static void setLimits(int timeLimit, int stepLimit, int consLimit) {
	Evaluator.timeLimit = timeLimit;
	Evaluator.stepLimit = stepLimit;
	Evaluator.consLimit = consLimit;
    }
    
    public void reset() {
	steps = conses = 0;
    }
    
    /** Called when evaluation of a top-level expression is complete */
    public void exprValue(Value v) {
	answer = v;
	Name.find("it").setGlodef(v, null);
	if (display) {
	    out.print("--> ");
	    v.printOn(out);
	    out.println();
	}
    }
    
    /** Called when elaboration of a top-level definition is complete */
    public void defnValue(Name n, Value v) {
	answer = v;
	n.setGlodef(v, text);
	if (display) {
	    out.format("--- %s = ", n);
	    v.printOn(out);
	    out.println();
	}
    }
    
    public void println(Value v) {
	out.println(v);
	out.flush();
    }
    
    public void printStats(PrintWriter log) {
        log.format("(%d %s, %d %s)\n", steps, (steps == 1 ? "step" : "steps"), 
        	conses, (conses == 1 ? "cons" : "conses"));
    }

    public static abstract class Continuation {
	public abstract Result result(Value v);
	
	public Continuation copy() {
	    return this;
	}
    }
    
    /** A suspended execution */
    public interface Result {
	/** Resume the execution.  This receives the Evaluator as an
	 * argument to allow top-level printing. */
	public Result resume(Evaluator ev);
    }
    
    /** An exception raised because of a run-time error */
    public static class EvalException extends RuntimeException {
	private String errtag;
	
	public EvalException(String message, ErrContext cxt, String errtag) {
	    super(cxt != null ? cxt.format(message) : message);
	    this.errtag = errtag;
	}

	public EvalException(String message, ErrContext cxt) {
	    this(message, cxt, "#nohelp");
	}
	
	public String getErrtag() { return errtag; }
    }
}
