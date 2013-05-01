/*
 * Machine.java
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
import funbase.Value.WrongKindException;


/** An instance of this class represents an activation of a function
 *  that has been compiled to 'bytecode'.  The class contains the
 *  interpreter loop for bytecode instructions.
 *  
 *  This class implements both Continuation and Result.  As a Continuation,
 *  an instance is able to accept a value -- the result of a function
 *  that the machine has called --  and push it onto its evaluation
 *  stack.  It then yields itself as a Result, and when resumed, it 
 *  continues the execution of the code. */
public class Machine extends Continuation implements Result {

    /* Opcodes used to be an enumerated type: but (I guess) that
       entails a method call as part of any switch statement, and
       speed is of the essence in the main interpreter loop. */

    /** Opcode values */
    public static final int GLOBAL = 1, LOCAL = 2, ARG = 3, 
	FVAR = 4, BIND = 5, POP = 6, CONST = 7, INT = 8, 
	LIST = 9, CLOSURE = 10, TRAP = 11, GUARD = 12, 
	JFALSE = 13, JUMP = 14, CALL = 15, TCALL = 16, 
	RETURN = 17, MCONST = 18, MINT = 19, MPLUS = 20, 
	MEQ = 21, MLIST = 22, MPRIM = 23, TOPVAL = 24, 
	TOPDEF = 25;

    /** Instructions */
    private final ByteCode code;
    
    /** Free variables of the running closure */
    private final Value fvars[];
    
    /** Arguments */
    private final Value args[];
    
    /** Base for arguments in args array */
    private final int base;
    
    /** Context for reporting errors */
    private final ErrContext cxt;
    
    /** Return address */
    private final Continuation retcont;

    /** Stack frame and evaluation stack */
    private final Value stack[];

    private int pc = 0, sp, trap = -1;
    
    /* The function uses the following storage locations in its own frame:
     * 
     *   args[base .. base+nargs) are the arguments
     *     (We use offsets from base so that arguments do not have to be
     *     copied into a fresh array before a function call)
     *     
     *   stack[0 .. fsize) are the local variables, with values established by
     *     BIND instructions during pattern matching or evaluation of a
     *     let-expression
     *     
     *   stack[fsize .. sp) is the evalation stack.
     *   
     *   fvars[0..) are the values of free variables of the function,
     *   captured when the closure was created.
     */
    
    public Machine(ByteCode code, Value fvars[], Value args[], 
	    int base, ErrContext cxt, Continuation k) {
	this.code = code;
	this.fvars = fvars;
	this.args = args;
	this.base = base;
	this.cxt = cxt;
	this.retcont = k;

	this.stack = new Value[code.fsize+code.ssize];	    
	sp = code.fsize;
    }
    
    /** Constuct a top-level machine */
    public Machine(ByteCode code) {
	this(code, null, null, 0, ErrContext.initContext, 
		new Continuation() {
	    public Result result(Value k) {
		throw new EvalException("The machine died", 
			ErrContext.initContext, "#nohelp");
	    }
	});
    }
    
    public Result result(Value v) {
	stack[sp++] = v;
	return this;
    }

    @Override
    public Continuation copy() {
	Machine fresh = 
	    new Machine(code, fvars, args, base, cxt, retcont.copy());
	fresh.pc = pc; fresh.sp = sp; fresh.trap = trap;
	System.arraycopy(stack, 0, fresh.stack, 0, sp);
	return fresh;
    }

    public Result resume(Evaluator ev) {
	while (true) {
	    int op = code.instrs[pc];
	    int rand = code.rands[pc];
	    pc++;

	    switch (op) {
		case CONST:
		    // Push a value from the constant table
		    stack[sp++] = code.consts[rand];
		    break;
		case INT:
		    // Push an integer
		    stack[sp++] = Value.makeNumValue(rand);
		    break;
		case BIND:
		    // Pop a value and bind a slot in the frame
		    stack[rand] = stack[--sp];
		    break;
		case GLOBAL: {
		    // Push the global definition of a name
		    Name x = (Name) code.consts[rand];
		    Value v = x.getGlodef();
		    if (v == null)
			throw new EvalException(x + " is not defined", 
				cxt, "#undef");
		    stack[sp++] = v;
		    break;
		}
		case ARG:
		    // Push an argument
		    stack[sp++] = args[base+rand];
		    break;
		case LOCAL:
		    // Push a value from the frame
		    stack[sp++] = stack[rand];
		    break;
		case FVAR:
		    // Push the value of a free variable
		    stack[sp++] = fvars[rand];
		    break;
		case CLOSURE: {
		    // Form a closure
		    Value newcxt[] = new Value[rand+1];
		    sp -= rand;
		    System.arraycopy(stack, sp, newcxt, 1, rand);
		    stack[sp-1] = newcxt[0] =
			new Function.Closure((ByteCode) stack[sp-1], newcxt);
		    break;
		}
		case POP:
		    // Pop and discard a value
		    sp--;
		    break;
		case JUMP:
		    // Unconditional jump
		    pc = rand;
		    break;
		case JFALSE:
		    // Pop a boolean and jump on false
		    try {
			if (! stack[--sp].asBoolean()) pc = rand;
		    }
		    catch (WrongKindException e) {
			throw new EvalException(
				"boolean expected in conditional expression",
				cxt, "#condexp");
		    }
		    break;
		case GUARD:
		    // Pop a boolean and trap on false
		    try {
			if (! stack[--sp].asBoolean()) trap();
		    }
		    catch (WrongKindException e) {
			throw new EvalException(
				"boolean expected after 'when'", 
				cxt, "#condexp");
		    }
		    break;
		case TRAP:
		    // Set the trap handler
		    trap = rand;
		    break;
		case MLIST:
		    // Be careful to avoid stack overflow if the argument is
		    // a list that is longer than expected.
		    try {
			Value v = stack[--sp]; 
			for (int i = 0; i < rand; i++) {
			    stack[sp++] = v.getHead();
			    v = v.getTail();
			}
			if (! v.isNilValue()) trap();
		    }
		    catch (Value.WrongKindException _) {
			trap();
		    }
		    break;
		case MPRIM: {
		    // Pop a primitive and an instance of it; push the args
		    Value cons = stack[--sp];
		    Value obj = stack[--sp];
		    if (cons.pattMatch(obj, stack, sp, rand, cxt))
			sp += rand;
		    else
			trap();
		    break;
		}
		case MCONST:
		    // Pop a value and match with a constant, or trap
		    if (! stack[--sp].equals(code.consts[rand]))
			trap();
		    break;
		case MINT:
		    // Pop a value and match with an integer, or trap
		    if (! stack[--sp].equals(Value.makeNumValue(rand)))
			trap();
		    break;
		case MEQ: {
		    // Pop two values and trap if not equal 
		    Value v = stack[--sp];
		    if (! stack[--sp].equals(v)) 
			trap();
		    break;
		}
		case MPLUS:
		    // Match an n+k pattern, or trap
		    try {
			double inc = code.consts[rand].asNumber();
			double y = stack[--sp].asNumber();
			double x = y - inc;
			if (inc > 0 && x >= 0 && x == (int) x)
			    stack[sp++] = Value.makeNumValue(x);
			else
			    trap();
		    }
		    catch (WrongKindException e) {
			trap();
		    }
		    break;
		case CALL:
		case TCALL: {
		    // Call a function, as a tail call if TCALL
		    final Value fun = stack[--sp];
		    int argp = (sp -= rand);
		    return fun.apply(stack, argp, rand, cxt, 
			    (op == CALL ? this : retcont));
		}
		case RETURN:
		    // Pop and return a value
		    return retcont.result(stack[--sp]);
		case LIST:
		    // Make a list of constant length from value stack
		    sp -= (rand-1);
		    stack[sp-1] = Value.makeList(stack, sp-1, rand);
		    break;
		case TOPVAL: {
		    final Value v = stack[--sp];
		    return new Result() {
			public Result resume(Evaluator ev) {
			    ev.exprValue(v);
			    return null;
			}
		    };
		}
		case TOPDEF: {
		    final Name lhs = (Name) code.consts[rand];
		    final Value v = stack[--sp];
		    return new Result() {
			public Result resume(Evaluator ev) {
			    ev.defnValue(lhs, v);
			    return null;
			}
		    };
		}
		default:
		    throw new Error("illegal opcode " + code.instrs[pc-1]);
	    }
	}
    }

    /** Trap on failure of pattern matching */
    private void trap() {
	if (trap >= 0) {
	    pc = trap; trap = -1; sp = code.fsize;
	}
	else {
	    StringBuilder buf = new StringBuilder();
	    if (code.arity > 0) {
		buf.append(args[base+0]);
		for (int i = 1; i < code.arity; i++)
		    buf.append(", " + args[base+i]);
	    }

	    throw new EvalException("no pattern matches "
		    + (code.arity == 1 ? "argument" : "arguments")
		    + " (" + buf + ")", cxt, "#match");
	}
    }
    
    /** Code for a function body.  Though this is called 'bytecode',
     *  in reality each instruction is encoded as two words, an
     *  opcode instrs[pc] and an integer operand rands[pc].  In some 
     *  cases, the operand is an index into the table consts of
     *  constants used in the function. */
    public static class ByteCode extends Value {
	private static final long serialVersionUID = 1L;
	
        /** Name of the function (used for error messages) */
        protected final String name;
        
        /** Number of arguments */
        protected final int arity;
        
        /** Size of frame for variable bindings */
        protected final int fsize;
        
        /** Size of evaluation stack */
        protected final int ssize;
        
        /* Opcodes for the instructions */
        protected final int instrs[];
        
        /* Operands for the instructions */
        protected final int rands[];
        
        /* Pool of constant values */
        protected final Value consts[];

        public ByteCode(String name, int arity, int fsize, int ssize,
        	int instrs[], int rands[], Value consts[]) {
            this.name = name;
            this.arity = arity;
            this.fsize = fsize;
            this.ssize = ssize;
            this.instrs = instrs; 
            this.rands = rands;
            this.consts = consts;
        }
        
        @Override
        public void printOn(PrintWriter out) {
            out.print("<bytecode>");
        }
        
	@Override
        public void dump() {
            System.out.printf("bytecode \"%s\" %d %d %d %d %d", 
        	    name, arity, fsize, ssize, instrs.length, consts.length);
            for (int i = 0; i < instrs.length; i++) {
        	if (i % 10 == 0) System.out.println();
        	System.out.printf(" %d %d", instrs[i], rands[i]);
            }
	    System.out.println();
            for (int j = 0; j < consts.length; j++)
        	consts[j].dump();
            System.out.printf("{ end of %s }\n", name);
        }
    }
}
