/*
 * Name.java
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Names in the Fun program are represented by unique Name objects.
 *  These contain a shallow binding to their value in the global
 *  environment. */
public class Name extends Value 
	implements Comparable<Name>, java.io.Serializable {
    private static final long serialVersionUID = 1L;
	
    /** The name as a string */
    private final String tag;
    
    /** The definition in the global environment, or null */
    private Value glodef = null;
    
    /** For user-defined names, the text of the definition */
    private String deftext = null;
    
    /** True for names that are system-defined and may not be changed */
    private boolean frozen = false;
    
    private Name(String tag) {
	this.tag = tag;
	nameTable.put(this.tag, this);
    }
    
    /** Set the global definition and defining text */
    public void setGlodef(Value v, String text) { 
	this.glodef = v;
	this.deftext = text;
    }
    
    /** Get the global definition of a name */
    public Value getGlodef() { return glodef; }
    
    /** Get the defining text */
    public String getDeftext() { return deftext; }
    
    /** Test if the global definition is unmodifiable */
    public boolean isFrozen() { return frozen; }
    
    public boolean equals(Object a) { return this == a; }
    
    public int compareTo(Name other) {
	if (this == other) return 0;
	return this.tag.compareTo(other.tag);
    }
    
    public String toString() { return tag; }
    
    @Override
    public void printOn(PrintWriter out) {
	out.printf("#%s", tag);
    }
    
    @Override
    public void dump() {
	System.out.printf("name \"%s\"\n", tag);
    }

    /** A global mapping of strings to Name objects */
    private static Map<String, Name> nameTable = 
	new HashMap<String, Name>(200);
    
    /** Find or create the unique Name with a given spelling */
    public static Name find(String tag) {
	Name name = nameTable.get(tag);
	if (name == null) name = new Name(tag);
	return name;
    }
    
    /** Find or create a name and install its global definition */
    public static void define(String tag, Value value) {
	Name name = find(tag);
	name.setGlodef(value, null);
    }

    /** Tag for abstract syntax tree */
    public static Name ANON, APPLY, CONST, IF, LAMBDA, LET,
	PLUS, PRIM, SEQ, VAL, VAR;

    private static void setNames() {
	ANON = find("anon"); APPLY = find("apply");
	CONST = find("const"); IF = find("if");
	LAMBDA = find("lambda"); LET = find("let");
	PLUS = find("plus"); PRIM = find("prim");
	SEQ = find("seq"); VAL = find("val");
	VAR = find("var");
    }

    static {
	setNames();
    }

    /** Read the global name table from a serialized stream */
    @SuppressWarnings("unchecked")
    public static void readNameTable(ObjectInputStream in) 
    		throws IOException, ClassNotFoundException {
	nameTable = (Map<String,Name>) in.readObject();
	setNames();
    }

    /** Write the global name table in a serialized stream */
    public static void writeNameTable(ObjectOutputStream out) 
    		throws IOException {
	out.writeObject(nameTable);
    }

    /** Freeze all global definitions made so far */
    public static void freezeGlobals() {
	find("it").glodef = null;
	for (Name x : nameTable.values()) {
	    if (x.glodef != null) {
		x.frozen = true;
		x.deftext = null;
		x.glodef.freeze();
	    }
	}
    }
    
    /** Get alphabetical list of globally defined names */
    public static List<String> getGlobalNames() {
        ArrayList<String> names = new ArrayList<String>(100);
        for (Name x : nameTable.values()) {
            String xx = x.toString();
            if (x.getGlodef() != null && !xx.startsWith("_"))
        	names.add(xx);	    
        }
	Collections.sort(names);
        return names;
    }
    
    public static void dumpNames() {
	// Sort the entries to help us reach a fixpoint
	ArrayList<String> names = new ArrayList<String>(nameTable.size());
	names.addAll(nameTable.keySet());
	Collections.sort(names);
	for (String k : names) {
	    Name x = find(k);
	    if (x.glodef != null) {
		System.out.printf("global \"%s\" ", x.tag);
		x.glodef.dump();
	    }
	}
	// The zero allows the scanner to read one token beyond the quit.
	System.out.println("quit 0");
    }
}
