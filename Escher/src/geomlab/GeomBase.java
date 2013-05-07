/*
 * GeomBase.java
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

package geomlab;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Application;
import android.content.Context;

import funbase.Evaluator;
import funbase.Name;
import funbase.Parser;
import funbase.Primitive;
import funbase.Scanner;
import funbase.Value;
import geomlab.Command.CommandException;

/** Common superclass for classes that provide a read-eval-print loop */
public class GeomBase extends Application {

	protected boolean statsFlag = false;
	protected Value last_val = null;
	protected String errtag = "";
	protected int status = 0;
	protected PrintWriter log;
	private File currentFile = null;

	@Override
	public void onCreate() {
		super.onCreate();
		registerApp(this);
	}

	public static Context context() {
		return theApp.getApplicationContext();
	}	
	
	public void setLog(PrintWriter log) {
		this.log = log;
	}

	public void logWrite(String s) {
		log.println(s);
		log.flush();
	}

	public void logMessage(String msg) {
		logWrite("[" + msg + "]");
	}

	public void errorMessage(String msg, String errtag) {
		logMessage(msg);
		if (status < 1)
			status = 1;
		this.errtag = errtag;
	}

	public void evalError(String prefix, String message, String errtag) {
		log.print(prefix);
		log.println(message);
		log.flush();
		if (status < 2)
			status = 2;
		this.errtag = errtag;
	}

	protected boolean eval_loop(Reader reader, boolean display,
			/* AppFrame */ Object errframe) {
		Parser parser = new Parser(reader);
		errtag = "";

		while (true) {
			try {
				Value p = parser.parsePara();
				if (p == null)
					return true; // End of input

				last_val = null;

				Evaluator ev = new Evaluator(p, parser.getText(), display, log);

				try {
					last_val = ev.execute();
				} catch (Evaluator.EvalException e) {
					evalError("Aargh: ", e.getMessage(), e.getErrtag());
					return false;
				} catch (Throwable e) {
					evalError("Failure: ", e.toString(), "#failure");
					return false;
				} finally {
					if (display) {
						if (statsFlag)
							ev.printStats(log);
						log.flush();
					}
				}
			} catch (Scanner.SyntaxException e) {
				//if (errframe == null)
					evalError("Oops: ", e.toString(), e.getErrtag());
				//else {
				//	evalError("Oops: ", e.shortMessage(), e.getErrtag());
				//	errframe.showError(e.getStart(), e.getEnd());
				//}
				return false;
			}
		}
	}

	/** Global method of accessing resource streams */
	public static InputStream getResourceAsStream(String name) throws IOException, CommandException {
		InputStream stream = theApp.getResources().getAssets().open(name);
		if (stream == null)
			throw new CommandException("Can't read resource " + name,
					"#noresource");
		
		return stream;
	}

	/** Global method of accessing resource URLs */
	public static URL getResource(String name) throws MalformedURLException {
		URL url = new URL("file:///android_asset/" + name);
		return url;
	}

	/** Load from a file */
	protected void loadFromFile(File file, boolean display) {
		File save_currentFile = currentFile;
		try {
			Reader reader = new BufferedReader(new FileReader(file));
			currentFile = file;
			eval_loop(reader, display, null);
			logMessage("Loaded " + file.getName());
			try {
				reader.close();
			} catch (IOException e) {
			}
		} catch (FileNotFoundException e) {
			errorMessage("Can't read " + file.getName(), "#nofile");
		} finally {
			currentFile = save_currentFile;
		}
	}

	protected void loadFromStream(InputStream in) {
		Reader reader = new InputStreamReader(in);
		eval_loop(reader, false, null);
	}

	public File getCurrentFile() {
		return currentFile;
	}

	public void exit() {
		System.exit(0);
	}

	public boolean getStatsFlag() {
		return statsFlag;
	}

	public void setStatsFlag(boolean statsFlag) {
		this.statsFlag = statsFlag;
	}

	public String getErrtag() {
		return errtag;
	}

	public int getStatus() {
		return status;
	}

	protected static GeomBase theApp;

	public static void registerApp(GeomBase app) {
		theApp = app;
	}

	public static final Primitive primitives[] = {
	/* A few system-oriented primitives */
	new Primitive("primitive", 1) {
		/* Look up a primitive */
		public Value invoke(Value args[], int base) {
			return Primitive.find(cxt.string(args[base + 0]));
		}
	},

	new Primitive("install", 1) {
		/* Install a plug-in class with primitives. */
		public Value invoke(Value args[], int base) {
			String name = cxt.string(args[base + 0]);
			try {
				Class<?> plugin;
				try {
					plugin = Class.forName("plugins." + name);
				} catch (ClassNotFoundException e) {
					plugin = Class.forName(name);
				}

				Session.installPlugin(plugin);
			} catch (Exception e) {
				cxt.primFail(
						"install failure for " + name + " - " + e.getMessage(),
						"#install");
			}
			return Value.nil;
		}
	},

	new Primitive("freeze", 0) {
		public Value invoke(Value args[], int base) {
			Name.freezeGlobals();
			return Value.nil;
		}
	},

	new Primitive("error", 2) {
		public Value invoke(Value args[], int base) {
			cxt.primFail(cxt.string(args[base + 0]), cxt.string(args[base + 1]));
			return null;
		}
	},

	new Primitive("opdef", 2) {
		public Value invoke(Value args[], int base) {
			Scanner.addOperator(cxt.string(args[base + 0]),
					cxt.string(args[base + 1]));
			return Value.nil;
		}
	},

	new Primitive("load", 1) {
		public Value invoke(Value args[], int base) {
			String name = cxt.string(args[base + 0]);
			File current = theApp.getCurrentFile();
			File file = (current == null ? new File(name) : new File(
					current.getParentFile(), name));
			theApp.loadFromFile(file, false);
			return Value.nil;
		}
	},

	new Primitive("limit", 3) {
		public Value invoke(Value args[], int base) {
			Evaluator.setLimits((int) cxt.number(args[base + 0]),
					(int) cxt.number(args[base + 1]),
					(int) cxt.number(args[base + 2]));
			return Value.nil;
		}
	},

	new Primitive("quit", 0) {
		public Value invoke(Value args[], int base) {
			theApp.exit();
			return Value.nil;
		}
	},

	new Primitive("dump", 1) {
		public Value invoke(Value args[], int base) {
			try {
				Session.saveSession(new File(cxt.string(args[base + 0])));
				return Value.nil;
			} catch (Command.CommandException e) {
				throw new Evaluator.EvalException(e.toString(), cxt, "#nohelp");
			}
		}
	},

	new Primitive("xdump", 0) {
		public Value invoke(Value args[], int base) {
			Name.dumpNames();
			return Value.nil;
		}
	} };
}
