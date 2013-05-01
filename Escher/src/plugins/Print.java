/*
 * Print.java
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
import funbase.Evaluator.Result;
import funbase.Primitive;
import funbase.Value;

/** A primitive that prints its argument on the log stream. */
public class Print {
    private static final String svnid =
	"$Id: Print.java 356 2008-05-28 13:53:15Z mike $";
    
    public static Primitive primitives[] = {
	new Primitive("print", 1) {
	    @Override
	    public Result apply(Value[] args, int base, int nargs, 
		    ErrContext cxt, final Continuation k) {
		if (nargs != arity) badArity(nargs, cxt);
		final Value v = args[base+0];
		
		/* We've no access to the log stream here.  But trickily,
		 * the resume method of a Result is passed the current
		 * Evaluator as an argument, and that does have a method
		 * for printing on the log.  So we just need to arrange
		 * a bounce on the trampoline. */
		
		return new Result() {
		    public Result resume(Evaluator ev) {
			ev.println(v);
			Thread.yield();
			return k.result(v);
		    }
		};
	    }
	}
    };
}
