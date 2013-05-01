/*
 * ContValue.java
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

import funbase.ErrContext;
import funbase.Evaluator;
import funbase.Evaluator.Continuation;
import funbase.Evaluator.Result;
import funbase.Function;
import funbase.Primitive;
import funbase.Value;


/** First-class continuations.
 *  Correct working of a captured continuation depends on its
 *  implementing the copy method properly; otherwise the first
 *  invocation of a continuation will spoil it for later invocations. */
public class ContValue extends Function {
    private static final String svnid =
	"$Id: ContValue.java 365 2008-06-11 17:11:29Z mike $";
    private static final long serialVersionUID = 1L;
    
    private Continuation cont;
    
    public ContValue(Continuation cont) {
	super("<continuation>", 1);
	Evaluator.countCons();
	this.cont = cont;
    }

    @Override
    public Result apply(Value[] args, int base, int nargs, 
	    ErrContext cxt, Continuation k) {
	if (nargs != arity) badArity(nargs, cxt);
	Continuation k1 = cont.copy();
	return k1.result(args[base+0]);
    }
    
    public void printOn(PrintWriter out) {
	out.print("<continuation>");
    }    
    
    public static final Primitive primitives[] = {
	new Primitive("callcc", 1) {
	    /** Capture the current continuation as an instance of
	     *  ContValue, and pass it as a parameter to the argument. */
	    @Override
	    public Result apply(Value args[], int base, int nargs, 
		    ErrContext cxt, Continuation k) {
		this.cxt = cxt;
		Value f = args[base+0];
		Value argv[] = new Value[] { new ContValue(k.copy()) };
		return f.apply(argv, 0, 1, cxt, k);
	    }
	}
    };
}