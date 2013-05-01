/*
 * BasicPrims.java
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

import funbase.Primitive;
import funbase.Value;

/** Basic primitives for handling numbers, booleans and lists */
public class BasicPrims {
    private static final String svnid =
	"$Id: BasicPrims.java 356 2008-05-28 13:53:15Z mike $";

    public static final Primitive primitives[] = {
	new Primitive("=", 2) {
	    public Value invoke(Value args[], int base) {
		return Value.makeBoolValue(
			args[base+0].equals(args[base+1]));
	    }
	},
	
	new Primitive("<>", 2) {
	    public Value invoke(Value args[], int base) {
		return Value.makeBoolValue(
			! args[base+0].equals(args[base+1]));
	    }
	},
	
	new Primitive("+", 2) {
	    public Value invoke(Value args[], int base) {
		return Value.makeNumValue(
			cxt.number(args[base+0]) + cxt.number(args[base+1]));
	    }
	},
	
	new Primitive("-", 2) {
	    public Value invoke(Value args[], int base) {
		return Value.makeNumValue(
			cxt.number(args[base+0]) - cxt.number(args[base+1]));
	    }
	},
	
	new Primitive("*", 2) {
	    public Value invoke(Value args[], int base) {
		return Value.makeNumValue(
			cxt.number(args[base+0]) * cxt.number(args[base+1]));
	    }
	},
	
	new Primitive("/", 2) {
	    public Value invoke(Value args[], int base) {
		if (cxt.number(args[base+1]) == 0.0) 
		    cxt.primFail("division by zero", "#divzero");
		return Value.makeNumValue(
			cxt.number(args[base+0]) / cxt.number(args[base+1]));
	    }
	},
	
	new Primitive("~", 1) {
	    public Value invoke(Value args[], int base) {
		return Value.makeNumValue(- cxt.number(args[base+0]));
	    }
	},
	
	new Primitive("<", 2) {
	    public Value invoke(Value args[], int base) {
		return Value.makeBoolValue(
			cxt.number(args[base+0]) < cxt.number(args[base+1]));
	    }
	},
	
	new Primitive("<=", 2) {
	    public Value invoke(Value args[], int base) {
		return Value.makeBoolValue(
			cxt.number(args[base+0]) <= cxt.number(args[base+1]));
	    }
	},
	
	new Primitive(">", 2) {
	    public Value invoke(Value args[], int base) {
		return Value.makeBoolValue(
			cxt.number(args[base+0]) > cxt.number(args[base+1]));
	    }
	},
	
	new Primitive(">=", 2) {
	    public Value invoke(Value args[], int base) {
		return Value.makeBoolValue(
			cxt.number(args[base+0]) >= cxt.number(args[base+1]));
	    }
	},
	
	new Primitive("numeric", 1) {
	    public Value invoke(Value args[], int base) {
		return Value.makeBoolValue(args[base+0].isNumValue());
	    }
	},
	
	new Primitive("int", 1) {
	    public Value invoke(Value args[], int base) {
		return Value.makeNumValue(Math.floor(cxt.number(args[base+0])));
	    }
	},
	
	new Primitive("sqrt", 1) {
	    public Value invoke(Value args[], int base) {
		if (cxt.number(args[base+0]) < 0.0) 
		    cxt.primFail("taking square root of a negative number", 
				 "#sqrt");
		return Value.makeNumValue(Math.sqrt(cxt.number(args[base+0])));
	    }
	},
	
	new Primitive("sin", 1) {
	    public Value invoke(Value args[], int base) {
		return Value.makeNumValue(
			Math.sin(cxt.number(args[base+0]) * Math.PI / 180));
	    }
	},
	
	new Primitive("cos", 1) {
	    public Value invoke(Value args[], int base) {
		return Value.makeNumValue(
			Math.cos(cxt.number(args[base+0]) * Math.PI / 180));
	    }
	},
	
	new Primitive("tan", 1) {
	    public Value invoke(Value args[], int base) {
		return Value.makeNumValue(
			Math.tan(cxt.number(args[base+0]) * Math.PI / 180));
	    }
	},
	
	new Primitive("random", 0) {
	    public Value invoke(Value args[], int base) {
		return Value.makeNumValue(Math.random());
	    }
	},
	
	new Primitive("head", 1) {
	    public Value invoke(Value args[], int base) {
		return cxt.head(args[base+0]);
	    }
	},
	
	new Primitive("tail", 1) {
	    public Value invoke(Value args[], int base) {
		return cxt.tail(args[base+0]);
	    }
	},
	
	new Primitive.Constructor(":", 2) {
	    public Value invoke(Value args[], int base) {
		Value tl = args[base+1];
		if (! tl.isConsValue() && ! tl.isNilValue()) cxt.expect("list");
		return cons(args[base+0], tl);
	    }
	    
	    public boolean match(Value obj, Value args[], int base) {
		try {
		    args[base+0] = obj.getHead();
		    args[base+1] = obj.getTail();
		    return true;
		}
		catch (Value.WrongKindException _) {
		    return false;
		}
	    }
	}
    };
}
