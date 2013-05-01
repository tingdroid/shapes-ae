/*
 * BootLoader.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import funbase.Scanner.Token;

/** Part of the process of building a working GeomLab system is to 
 *  input code for the compiler that was prepared using the previous
 *  incarnation of the system.  The code is written out using the
 *  method Name.dumpNames and the dump methods of various Value
 *  classes.  This class contains methods for reading it back.  Once
 *  the GeomLab system is working, this class is no longer needed, so
 *  the error handling is deliberately spartan. */
public class BootLoader {
    private Scanner scanner;
    
    public BootLoader(Reader r) {
	scanner = new Scanner(r);
    }
    
    /** Read a sequence of global value definitions. */
    public void boot() {
	scanner.scan();
	
	while (true) {
	    String t = get(Token.IDENT);
	    if (t.equals("global")) {
		String x = get(Token.STRING);
		Value v = value();
		Name xx = Name.find(x);
		xx.setGlodef(v, null);
	    }
	    else if (t.equals("quit"))
		return;
	    else
		throw new Error("BootLoader.boot " + t);
	}
    }
    
    /** Read a value from the bootfile */
    private Value value() {
	String t = get(Token.IDENT);
	if (t.equals("boolean"))
	    return Value.makeBoolValue(getInt() != 0);
	else if (t.equals("name"))
	    return Name.find(get(Token.STRING));
	else if (t.equals("string"))
	    return Value.makeStringValue(get(Token.STRING));
	else if (t.equals("integer"))
	    return Value.makeNumValue(getInt());
	else if (t.equals("primitive"))
	    return Primitive.find(get(Token.STRING));
	else if (t.equals("nil"))
	    return Value.nil;
	else if (t.equals("bytecode")) {
	    /* Bytecode for a function. */
	    String name = get(Token.STRING);
	    int arity = getInt();
	    int fsize = getInt();
	    int ssize = getInt();
	    int nops = getInt();
	    int nconsts = getInt();
	    int ops[] = new int[nops], rands[] = new int[nops];
	    Value consts[] = new Value[nconsts];
	    for (int i = 0; i < nops; i++) {
		ops[i] = getInt();
		rands[i] = getInt();
	    }
	    for (int j = 0; j < nconsts; j++)
		consts[j] = value();
	    return new Machine.ByteCode(name, arity, fsize, ssize,
		    ops, rands, consts);
	}
	else if (t.equals("closure")) {
	    /* A closure.  We don't deal with closures that have free
	       variables, but the compiler is written to avoid these. */
	    Machine.ByteCode c = (Machine.ByteCode) value();
	    return new Function.Closure(c, null);
	}
	else
	    throw new Error("BootLoader.value " + t);
    }
    
    /** Return the next token as a string */
    private String get(Scanner.Token t) {
	String s = scanner.sym;
	scanner.eat(t);
	return s;
    }
    
    /** Scan and return an integer */
    private int getInt() {
	String s = "";
	if (scanner.tok == Token.MINUS) {
	    scanner.eat(Token.MINUS);
	    s = "-";
	}
	s += get(Token.NUMBER);
	return Integer.parseInt(s);
    }
    
    /** Bootstrap the system from a file. */
    public static void bootstrap(File file) {
        try {
            Reader reader = new BufferedReader(new FileReader(file));
            BootLoader loader = new BootLoader(reader);
            loader.boot();
            try { reader.close(); } catch (IOException e) { }
        }
        catch (FileNotFoundException e) {
            throw new Error("Can't read " + file.getName());
        }
    }
}
