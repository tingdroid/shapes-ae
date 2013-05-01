/*
 * Scanner.java
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
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class Scanner {
    
    public Token tok;
    public String sym;
    
    private Reader reader;
    private char push_back = '\0';
    private int line_num = 1;
    private int char_num = 0, start_char, root_char;
    
    /* The scanner keeps track of the text that has been scanned, so that
     * the defining text can be saved with each name in the global env.
     * The variable 'virgin' indicates whether we are skipping characters that
     * come before the first token of the text. */
    private StringBuilder text = new StringBuilder(200);
    private boolean virgin = true;

    public Scanner(Reader reader) {
	this.reader = reader;
    }
    
    private char readChar() {
	try {
	    int ich = reader.read(); // returns -1 on EOF
	    if (ich < 0)
		return '\0';
	    else
		return (char) ich;
	} catch (IOException e) {
	    return '\0';
	}
    }
    
    private char getChar() {
	char ch;
	
	char_num++;
	
	if (push_back == '\0')
	    ch = readChar();
	else {
	    ch = push_back; push_back = '\0';
	}

	if (ch != '\0') text.append(ch);
	return ch;
    }
    
    /** Push back one character onto the input */
    private void pushBack(char ch) {
	/* We could wrap the input in a PushbackReader, but that's overkill,
	 * as we only need to push back one character -- and anyway, we need
	 * to deal with the saved text. */
	if (ch != '\0') {
	    char_num--;
	    push_back = ch;
	    text.deleteCharAt(text.length()-1);
	}
    }
    
    public String getText() {
	return text.toString();
    }
    
    public void resetText() {
	text.setLength(0);
	virgin = true;
    }
    
    private static Map<String, Token> kwtable = 
	new HashMap<String, Token>(30);
    
    // Fill in the keyword table with all reserved words
    static {
	kwtable.put("if", Token.IF);
	kwtable.put("then", Token.THEN);
	kwtable.put("else", Token.ELSE);
	kwtable.put("let", Token.LET);
	kwtable.put("define", Token.DEFINE);
	kwtable.put("in", Token.IN);
	kwtable.put("lambda", Token.LAMBDA);
	kwtable.put("when", Token.WHEN);
	kwtable.put("op", Token.OP);
	kwtable.put("_", Token.ANON);
	
	kwtable.put("div", Token.MULOP);
	kwtable.put("mod", Token.MULOP);
	kwtable.put("and", Token.AND);
	kwtable.put("or", Token.OR);
	kwtable.put("not", Token.MONOP);

	kwtable.put("=", Token.EQUAL);
	kwtable.put("+", Token.PLUS);
	kwtable.put("-", Token.MINUS);
	kwtable.put("$", Token.MULOP);
	kwtable.put("*", Token.MULOP);
	kwtable.put("/", Token.MULOP);
	kwtable.put("&", Token.ADDOP);
	kwtable.put("~", Token.UMINUS);
	kwtable.put(":", Token.CONS);
	kwtable.put("@", Token.APPOP);
	kwtable.put("<", Token.RELOP);
	kwtable.put("<=", Token.RELOP);
	kwtable.put("<>", Token.RELOP);
	kwtable.put(">", Token.RELOP);
	kwtable.put(">=", Token.RELOP);
	kwtable.put(">>", Token.SEQ);
    }
    
    public static void addOperator(String symbol, Token token) {
	kwtable.put(symbol, token);
    }
    
    public static void addOperator(String symbol, String proto) {
	Token tok = kwtable.get(symbol);
	Token protok = kwtable.get(proto);
	    
	if (protok == null || protok.priority == 0
		|| (tok != null && tok != Token.prototype[protok.priority]))
	    throw new Evaluator.EvalException("bad arguments to _opdef", null);
	    
	kwtable.put(symbol, Token.prototype[protok.priority]);		    
    }
    
    @SuppressWarnings("unchecked")
    public static void readSyntax(ObjectInputStream in) 
    		throws IOException, ClassNotFoundException {
	kwtable = (Map<String, Token>) in.readObject();
    }
    
    public static void writeSyntax(ObjectOutputStream out)
    		throws IOException {
	out.writeObject(kwtable);
    }
    
    private boolean isOpChar(char ch) {
	final String opchars = "!#$%&*+-/:<=>?@^~";
	return opchars.indexOf(ch) != -1;
    }
    
    public void scan() {
	start_char = char_num;
	char ch = getChar();
	tok = null; sym = null;
	while (tok == null) {
	    switch (ch) {
		case '\0': // EOF
		    tok = Token.EOF; break;
		case ' ':
		case '\t':
		case '\r':
		    start_char = char_num; 
		    if (virgin) resetText();
		    ch = getChar(); 
		    break;
		case '\n':
		    line_num++; start_char = char_num; 
		    if (virgin) resetText();
		    ch = getChar(); 
		    break;
		case '{': {
		    int depth = 0;
		    do {
			if (ch == '{')
			    depth++;
			else if (ch == '}')
			    depth--;
			else if (ch == '\n')
			    line_num++;
			else if (ch == '\0') {
			    start_char = char_num; tok = Token.EOF;
			    syntax_error("unterminated comment", "#comment");
			}
			
			ch = getChar();
		    } while (depth > 0);
		    break; 
		}
		case '}':
		    tok = Token.RBRACE;
		    syntax_error("Can't find matching '{'", "#bracematch");
		    break;
		case '(':
		    tok = Token.LPAR; break;
		case ')': 
		    tok = Token.RPAR; break;
		case '[':
		    tok = Token.BRA; break;
		case ']':
		    tok = Token.KET; break;
		case ',': 
		    tok = Token.COMMA; break;
		case ';':
		    tok = Token.SEMI; break;
		case '|':
		    tok = Token.VBAR; break;
		    
		case '"': {
		    StringBuilder string = new StringBuilder(80);
		    ch = getChar();
		    while (ch != '"' && ch != '\n' && ch != '\0') {
			string.append(ch); ch = getChar();
		    }
		    if (ch == '"') {
			tok = Token.STRING;
			sym = string.toString();
		    }
		    else {
			pushBack(ch);
			start_char = char_num;
			tok = (ch == '\n' ? Token.EOL : Token.EOF);
			syntax_error("unterminated string constant", "#string");
		    }
		    break;
		}
		
		case '#': {
		    StringBuilder buf = new StringBuilder(10);
		    ch = getChar();
		    while (Character.isLetter(ch)) {
			buf.append(ch); ch = getChar();
		    }
		    pushBack(ch);
		    sym = buf.toString();
		    tok = Token.ATOM;
		    break;
		}
		
		default:
		    if (Character.isLetter(ch) || ch == '_') {
			// An identifier
			StringBuilder buf = new StringBuilder(10);
			while (Character.isLetterOrDigit(ch) || ch == '_') {
			    buf.append(ch); ch = getChar();
			}
			pushBack(ch);
			sym = buf.toString();
			tok = kwtable.get(sym);
			if (tok == null) tok = Token.IDENT;
		    } else if (Character.isDigit(ch)) {
			// A numeric constant
			StringBuilder buf = new StringBuilder(10);
			tok = Token.NUMBER; 
			while (Character.isDigit(ch)) {
			    buf.append(ch); ch = getChar();
			}
			if (ch == '.') {
			    buf.append(ch); ch = getChar();
			    while (Character.isDigit(ch)) {
				buf.append(ch); ch = getChar();
			    }
			}
			if (ch == 'E') {
			    buf.append(ch); ch = getChar();
			    if (ch == '+' || ch == '-') {
				buf.append(ch); ch = getChar();
			    }
			    if (! Character.isDigit(ch)) {
				sym = buf.toString();
				badToken();
			    }
			    else {
				do {
				    buf.append(ch); ch = getChar();
				} while (Character.isDigit(ch));
			    }
			}
			pushBack(ch);
			sym = buf.toString();
		    } else if (isOpChar(ch)) {
			// A symbolic operator
			StringBuilder buf = new StringBuilder(10);
			while (isOpChar(ch)) {
			    buf.append(ch); ch = getChar();
			}
			pushBack(ch);
			sym = buf.toString();
			tok = kwtable.get(sym);
			if (tok == null) badToken();
		    } else {
			sym = new String(new char[] { ch });
			badToken();
		    }
	    }
	}
	
	if (virgin) {
	    root_char = start_char;
	    virgin = false;
	}
    }

    private void badToken() {
	tok = Token.BADTOK;
	syntax_error("unknown symbol", "#badtok");
    }
    
    /** Consume a specified token, or complain if it is not there */
    public void eat(Token expected) {
        if (tok == expected)
            scan();
        else
            syntax_error("I expected " + expected.spelling + " here", "#eat");
    }

    /** Report a syntax error at the current token */
    void syntax_error(String msg, String errtag) {
	String chars = 
	    (tok == Token.EOF ? "end of input" :
		tok == Token.EOL ? "end of line" :
		"'" + text.substring(start_char - root_char) + "'");
	throw new SyntaxException(msg, line_num, start_char, char_num, 
		chars, errtag);
    }
    
    public static class SyntaxException extends RuntimeException {
	private int line, start, end;
	private String errtok, errtag;
	
	public SyntaxException(String msg, int line, int start, int end, 
		String etok, String errtag) {
	    super(msg);
	    this.line = line;
	    this.start = start;
	    this.end = end;
	    this.errtok = etok;
	    this.errtag = errtag;
	}
	
	public String shortMessage() {
	    return getMessage() + " (at " + errtok + ")";
	}
	
	public String toString() {
	    return getMessage() + " (at " + errtok + " on line " + line + ")";
	}
	
	/** Get the start position of the token where the error was detected */
	public int getStart() { return start; }
	
	/** Get the end position of the token where the error was detected */
	public int getEnd() { return end; }
	
	/** Get the help file tag for the error */
	public String getErrtag() { return errtag; }
    }

    public static enum Token {    
        IDENT("an identifier"),
        NUMBER("a number"),
        ATOM("an atom"),
        LPAR("'('"),
        RPAR("')'"),
        COMMA("','"),
        EQUAL("'='", 3, 4),
        SEMI("';'"),
        BRA("'['"),
        KET("']'"),
        MINUS("'-'", 5, 6),
        PLUS("'+'", 5, 6),
        VBAR("'|'"),
        SEQ(">>"),
        STRING("a string constant"),
        UMINUS("'~'", 8, 8),
        OROP("a binary operator", 1, 2),
        ANDOP("a binary operator", 2, 3),
        RELOP("a binary operator", 3, 4),
        APPOP("a binary operator", 4, 4),
        ADDOP("a binary operator", 5, 6),
        MULOP("a binary operator", 6, 7),
        CONSOP("a binary operator", 7, 7),
        MONOP("a unary operator", 8, 8),
        IF("'if'"),
        THEN("'then'"),
        ELSE("'else'"),
        LET("'let'"),
        DEFINE("'define'"),
        IN("'in'"),
        LAMBDA("'lambda'"),
        WHEN("'when'"),
        OP("'op'"),
        CONS("':'", 7, 7),
        OR("'or'", 1, 2),
        AND("'and'", 2, 3),
        ANON("'_'"),
        RBRACE("'}'"),
        EOL("end of line"),
        EOF("end of input"),
        BADTOK("an unrecognised token");
        
        public final int priority;
        public final int rightPrio;
        public static final int minPriority = 1;  // Must be > 0
        public static final int maxBinary = 7;

        public final String spelling;
        
        private Token(String spelling) {
            this(spelling, 0, 0);
        }

        private Token(String spelling, int priority, int rightPrio) {
            this.priority = priority;
            this.rightPrio = rightPrio;
            this.spelling = spelling;
        }

        public static Token prototype[] = new Token[] {
            null, OROP, ANDOP, RELOP, APPOP, ADDOP, MULOP, CONSOP, MONOP
        };
    }
}

