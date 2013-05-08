/*
 * Function.java
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

import funbase.Evaluator.Continuation;
import funbase.Evaluator.EvalException;
import funbase.Evaluator.Result;


/** A value that represents a function.  Concrete subclasses are Closure,
 *  a function that is defined by bytecode, and Primitive, a built-in
 *  function.  It is these subclasses that override the apply method
 *  so that their instances can actually be applied to arguments. */
public abstract class Function extends Value {
	private static final long serialVersionUID = 1L;

    /** Name of the function: this is "<lambda>" for lambda expressions */
    protected final String name;

    /** Expected number of arguments. */
    protected final int arity;

    /** Flag for functions defined in the prelude and frozen */
    protected boolean frozen = false;

    public Function(String name, int arity) {
	this.name = name;
	this.arity = arity;
    }

    /** Complain when the wrong number of arguments are provided */
    protected void badArity(int nargs, ErrContext cxt) {
	throw new EvalException(
		"function " + name + " called with " + nargs
		+ (nargs == 1 ? " argument" : " arguments")
		+ " but needs " + arity, cxt, "#numargs");
    }

    public void freeze() { frozen = true; }

    public boolean isFrozen() { return frozen; }

    /** A (code, context) pair that represents a bytecode function */
    public static class Closure extends Function {
	private static final long serialVersionUID = 1L;
	
	/** Bytecode for the function body */
	private final Machine.ByteCode code;

	/** Values for the free variables of the closure */
	private final Value fvars[];

	/** Singleton value for error context */
	protected final ErrContext errcxt;

	public Closure(Machine.ByteCode code, Value context[]) {
	    super(code.name, code.arity);
	    this.code = code;
	    this.fvars = context;
	    this.errcxt = new ErrContext(name);
	}

	@Override
	public Result apply(Value args[], int base, int nargs, 
		ErrContext cxt0, Continuation k) {
	    assert cxt0 != null;
	    if (nargs != arity) badArity(nargs, cxt0);
	    ErrContext cxt1 = (frozen ? cxt0.freezeEnter(name) : errcxt);	
	    return new Machine(code, fvars, args, base, cxt1, k);
	}

	@Override
	public void printOn(PrintWriter out) {
	    out.printf("<function>");
	}
	
	@Override
	public void dump() {
	    if (fvars != null && fvars.length > 1)
		throw new EvalException("Can't dump closure with free variables",
			null, "#nohelp");
	    else {
		System.out.print("closure ");
		code.dump();
	    }
	}
    }

}
