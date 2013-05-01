/*
 * Parser.java
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

import java.io.Reader;

import funbase.Scanner.Token;

/** Parser for the GeomLab language.  After parsing a paragraph with the
 *  parsePara method, you can fetch the text that has been parsed with
 *  getText, so that the defining text of each name can be saved as part
 *  of its global definition.  Subsequent paragraphs from the same file
 *  can be parsed by calling parsePara again.  ParsePara returns null on EOF,
 *  and raises Scanner.SyntaxError if it finds an error;  there's no
 *  support for subsequent error recovery. */
public class Parser {
    
    private Scanner scanner;

    public Parser(Reader r) {
	scanner = new Scanner(r);
    }
    
    public Value parsePara() {
	scanner.resetText();
	scanner.scan();
	return p_para(); 
    }
    
    public String getText() {
	return scanner.getText();
    }
    
    /** Make a node in the tree */
    private static Value node(Name op, Value... rands) {
	return Value.cons(op, Value.makeList(rands));
    }
    
    private static Value nreverse(Value xs) {
	Value ys = Value.nil;

	try {
	    while (xs.isConsValue()) {
		Value zs = xs.getTail();
		xs.setTail(ys);
		ys = xs; xs = zs;
	    }
	}
	catch (Value.WrongKindException _) {
	    throw new Error("Parser.nreverse");
	}

	return ys;
    }
    

   
    // para ::= expr [;] | DEFINE def [;]
    private Value p_para() {
	Value p;
	
	if (scanner.tok == Token.EOF)
	    return null;
	
	switch (scanner.tok) {
	    case DEFINE:
		scanner.eat(Token.DEFINE);
		p = p_defn(true);
		break;
	    default:
		p = p_expr();
	}
	
	if (scanner.tok == Token.RPAR)
	    scanner.syntax_error("Can't find matching '('", "#parenmatch");
	else if (scanner.tok == Token.KET)
	    scanner.syntax_error("Can't find matching '['", "#bramatch");
	else if (scanner.tok != Token.SEMI && scanner.tok != Token.EOF)
	    scanner.syntax_error(
		    "extra text appears after the end of the expression",
		    "#junk");
	
	return p;
    }
    
    // expr ::= LET def IN expr | LAMBDA formals expr | cond [ '>>' expr ]
    private Value p_expr() {
	switch (scanner.tok) {
	    case LET: {
		scanner.eat(Token.LET);
		Value d = p_defn(false);
		scanner.eat(Token.IN);
		Value e = p_expr();
		return node(Name.LET, d, e);
	    }

	    case LAMBDA: {
		scanner.eat(Token.LAMBDA);
		Value formals = p_formals();
		int arity = nformals;
		Value body = p_expr();
		return node(Name.LAMBDA, Value.makeNumValue(arity), 
			formals, body);
	    }

	    default: {
		Value e = p_cond();
		if (scanner.tok == Token.SEQ) {
		    scanner.eat(Token.SEQ);
		    e = node(Name.SEQ, e, p_expr());
		}
		return e;
	    }
	}
    }
    
    // cond ::= IF cond THEN cond ELSE cond | term
    private Value p_cond() {
	switch (scanner.tok) {
	case IF:
	    scanner.eat(Token.IF);
	    Value e1 = p_cond();
	    scanner.eat(Token.THEN);
	    Value e2 = p_cond();
	    scanner.eat(Token.ELSE);
	    Value e3 = p_cond();
	    return node(Name.IF, e1, e2, e3);

	default:
	    return p_term(Token.minPriority);
	}
    }
    
    // term ::= term5 { OROP term5 } (L)
    // term5 ::= term4 { ANDOP term4 } (L)
    // term4 ::= term3 { (RELOP|EQUAL) term3 } (L)
    // term3 ::= term2 { APPOP term2 } (R)
    // term2 ::= term1 { (ADDOP|PLUS|MINUS) term1 } (L)
    // term1 ::= term0 { MULOP term0 } (L)
    // term0 ::= factor { CONS factor } (R)
    private Value p_term(int min) {
	/* The naive recursive descent algorithm is repetitious, and uses lots 
	 of recursion for not much effect.  Instead, this routine builds one 
	 leftward path in the parse tree from the bottom up, calling itself 
	 recursively to parse right-hand operands.  It does not consume any 
	 binary operators with precedence less than min. The value of 
	 tok.priority decreases monotonically as we go up the 
	 tree. */
	
	Value e1 = p_factor();
	
	for (;;) {
	    Token tok = scanner.tok;
	    if (tok.priority < min  || tok.priority > Token.maxBinary) 
		return e1;
	    String s = scanner.sym;
	    scanner.eat(tok);
	    Value e2 = p_term(tok.rightPrio);
	    if (tok == Token.AND)
		e1 = node(Name.IF, e1, e2, 
			  node(Name.CONST, Value.makeBoolValue(false)));
	    else if (tok == Token.OR)
		e1 = node(Name.IF, e1, 
			node(Name.CONST, Value.makeBoolValue(true)), e2);
	    else {
		Value op = node(Name.VAR, Name.find(s));
		e1 = node(Name.APPLY, op, e1, e2);
	    }
	}
    }
    
    // factor ::= MONOP factor | primary
    private Value p_factor() {
	switch (scanner.tok) {
	case MONOP:
	    Value op = node(Name.VAR, p_sym(Token.MONOP));
	    return node(Name.APPLY, op, p_factor());
	
	case MINUS:
	case UMINUS:
	    scanner.scan();
	    if (scanner.tok == Token.NUMBER) {
		double val = p_number();
		return node(Name.CONST, Value.makeNumValue(-val));
	    }
	    else {
		return node(Name.APPLY, 
			    node(Name.VAR, Name.find("~")), p_factor());
	    }
	
	default:
	    return p_primary();
	}
    }           
    
    // primary ::= NUMBER | Name  [ ( exprs ) ] | '(' expr ')' | '[' exprs ']'
    private Value p_primary() {
	switch (scanner.tok) {
	case NUMBER:
	    return node(Name.CONST, Value.makeNumValue(p_number()));
	    
	case ATOM:
	    return node(Name.CONST, p_sym(Token.ATOM));
	
	case IDENT:
	case OP:
	    Value e1 = node(Name.VAR, p_name());
	    if (scanner.tok != Token.LPAR) 
	    	return e1;
	    else {
		scanner.eat(Token.LPAR);
		Value args = p_exprs(Token.RPAR);
		scanner.eat(Token.RPAR);
		return Value.cons(Name.find("apply"), 
			Value.cons(e1, args));
	    }

	case STRING:
	    String text = scanner.sym;
	    scanner.eat(Token.STRING);
	    return node(Name.CONST, Value.makeStringValue(text));

	case LPAR:
	    scanner.eat(Token.LPAR);
	    Value e = p_expr();
	    scanner.eat(Token.RPAR);
	    return e;
	
	case BRA:
	    scanner.eat(Token.BRA);
	    Value elements = p_exprs(Token.KET);
	    scanner.eat(Token.KET);
	    return Value.cons(Name.find("list"), elements);

	case EOF:
	    scanner.syntax_error("I expected an expression here", "#exp");
	    return null;

	default:
	    scanner.syntax_error("I don't recognise this expression", 
		    "#badexp");
	    return null;
	}
    }
    
    // name ::= ident | OP op
    private Name p_name() { return p_name(null); }

    private Name p_name(Name x) {
	Name y;

	switch (scanner.tok) {
	    case IDENT:
		break;
		   
	    case OP:
		scanner.eat(Token.OP);
		if (scanner.tok.priority <= 0) {
		    scanner.syntax_error("I expected an operator symbol here",
					 "#op");
		}
		break;

	    default:
		scanner.eat(Token.IDENT);
		return null;
	}
		
	y = Name.find(scanner.sym);

	if (x != null && y != x)
	    scanner.syntax_error(
		    "the same function name should appear in each equation",
		    "#names");

	scanner.scan();
	return y;
    }		

    // def ::= name '=' expr 
    //      |  clause { '|' clause }
    // clause ::= name formals '=' expr [ WHEN expr ]
    private Value p_defn(boolean top) {
	Name x = p_name();
	
	if (top && x.isFrozen())
	    scanner.syntax_error("can't replace built-in definition of '" + 
		    x + "'", "#redef");
	
	if (scanner.tok != Token.LPAR) {
	    // A simple value definition
	    scanner.eat(Token.EQUAL);
	    return node(Name.VAL, x, p_expr());
	}
	
	Value rules = Value.nil;
	
	Value fps = p_formals();
	int arity = nformals;

	for (;;) {
	    scanner.eat(Token.EQUAL);
	    Value e = p_expr();
	    Value rule;
	    if (scanner.tok == Token.WHEN) {
	        scanner.eat(Token.WHEN);
	        Value g = p_expr();
	        rule = Value.makeList(fps, g, e);
	    }
	    else
		rule = Value.makeList(fps, e);
	    rules = Value.cons(rule, rules);
	    
	    if (scanner.tok != Token.VBAR) break;
	    scanner.eat(Token.VBAR);
	    
	    @SuppressWarnings("unused")
	    Name y = p_name(x);
	    fps = p_formals();
	    if (nformals != arity) 
		scanner.syntax_error("each equation should have"
			+ " the same number of arguments", "#arity");
	}
	
	return Value.cons(Name.find("fun"), 
		Value.cons(x, Value.cons(Value.makeNumValue(arity), 
			nreverse(rules))));
    }
    
    // formals ::= '(' [ pattern {',' pattern} ] ')'
    private Value p_formals() {
	scanner.eat(Token.LPAR);
	Value s = p_patterns(Token.RPAR);
	scanner.eat(Token.RPAR);
	return s;
    }
    
    int nformals = 0;
    
    private Value p_patterns(Token end_tok) {
	Value v = Value.nil;
	int n = 0;
	
	if (scanner.tok != end_tok) {
	    v = Value.cons(p_pattern(), v); n = 1;
	    while (scanner.tok == Token.COMMA) {
		scanner.eat(Token.COMMA);
		v = Value.cons(p_pattern(), v); n++;
	    }
	}
	
	nformals = n;
	return nreverse(v);
    }
    
    // pattern ::= patfactor { '+' NUMBER }
    private Value p_pattern() {
	Value p = p_patfactor();
	while (scanner.tok == Token.PLUS) {
	    scanner.eat(Token.PLUS);
	    p = node(Name.PLUS, p, Value.makeNumValue(p_number()));
	}
	return p;
    }
    
    // patfactor ::= patprim { ':' patfactor }
    private Value p_patfactor() {
	Value p = p_patprim();
	if (scanner.tok == Token.CONS) {
	    Name op = p_sym(Token.CONS);
	    p = node(Name.PRIM, op, p, p_patfactor());
	}
	return p;
    }
    
    private Value p_patprim() {
	switch (scanner.tok) {
	case IDENT:
	case OP:
	    Name x = p_name();
	    if (scanner.tok != Token.LPAR)
		return node(Name.VAR, x);
	    else {
		scanner.eat(Token.LPAR);
		Value args = p_patterns(Token.RPAR);
		scanner.eat(Token.RPAR);
		return Value.cons(Name.find("prim"), 
			Value.cons(x, args));
	    }
	    
	case ATOM:
	    return node(Name.CONST, p_sym(Token.ATOM));

	case ANON:
	    scanner.eat(Token.ANON);
	    return node(Name.ANON);

	case NUMBER:
	case MINUS:
	case UMINUS:
	    return node(Name.CONST, Value.makeNumValue(p_number()));

	case STRING:
	    String text = scanner.sym;
	    scanner.eat(Token.STRING);
	    return node(Name.CONST, Value.makeStringValue(text));

	case LPAR:
	    scanner.eat(Token.LPAR);
	    Value p1 = p_pattern();
	    scanner.eat(Token.RPAR);
	    return p1;

	case BRA:
	    scanner.eat(Token.BRA);
	    Value elems = p_patterns(Token.KET);
	    scanner.eat(Token.KET);
	    return Value.cons(Name.find("list"), elems);

	default:
	    scanner.syntax_error("I don't recognise this pattern", 
	    		"#pattern");
	    return null;
	}       
    }
    
    private Value p_exprs(Token end_tok) {
	Value v = Value.nil;
	
	if (scanner.tok != end_tok && scanner.tok != Token.EOF) {
	    v = Value.cons(p_expr(), v);
	    while (scanner.tok == Token.COMMA) {
		scanner.eat(Token.COMMA);
		v = Value.cons(p_expr(), v);
	    }
	}
	
	return nreverse(v);
    }
    
    private double p_number() {
	double x = 0.0; boolean negate = false;
	if (scanner.tok == Token.MINUS || scanner.tok == Token.UMINUS) {
	    negate = true; scanner.scan();
	}
	if (scanner.tok == Token.NUMBER) 
	    x = Double.parseDouble(scanner.sym);
	scanner.eat(Token.NUMBER);
	return (negate ? -x : x);
    }
    
    private Name p_sym(Token exp) {
	String s = scanner.sym;
	scanner.eat(exp);
	return Name.find(s);
    }
}
