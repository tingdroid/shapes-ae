/*
 * GeomLab.java
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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import plugins.Drawable;

import com.ting.escher.Console;
import com.ting.escher.EvalListener;

import funbase.Name;
import funbase.Value;
import geomlab.Command.CommandException;

/**
 * The main application class for GeomLab.
 * 
 * The Geomlab application is made up of three parts: (i) an interpreter for the
 * core of the GeomLab language, (ii) a graphical interface where you can enter
 * GeomLab expressions and have them submitted to the interpreter, and (iii) a
 * collection of classes that implement primitives that are included in the
 * initial environment of the interpreter. These pieces are quite independent of
 * each other: for example, the interpreter knows nothing about the data type of
 * pictures that's implemented by the Picture class. The GUI knows how to
 * display pictures that satisfy the {@link plugins.Drawable} interface (as
 * instances of {@link plugins.Picture Picture} do), but does not know any
 * details of how pictures are made up.
 */
public class GeomLab extends GeomBase {

	public Console frame; // = new AppFrame();
	public GraphBox arena; // = new GraphBox("Picture", frame);
	// public final PhoneHome phoneHome = new PhoneHome();

	public GeomLab() {
	}
	
	protected void prompt() {
		log.print("> ");
		log.flush();
	}

	/** Print an error or information message into the log */
	public void logMessage(String msg) {
		log.print("\n[" + msg + "]\n");
	}

	/** Update the picture display */
	protected void displayUpdate(Value val) {
		if (arena == null) {
			// TODO create arena
			return;
		}
		if (val == null || !(val instanceof Drawable))
			arena.setPicture(null);
		else {
			arena.setPicture((Drawable) val);
		}
	}

	public void loadFileCommand(File file) {
		log.println();
		loadFromFile(file, true);
		prompt();
	}

	/** Command -- evaluate expressions */
	public String evaluate(String command) {
		final StringReader reader = new StringReader(command);

		boolean done = eval_loop(reader, true, frame);
		displayUpdate(last_val);
		if (last_val != null && !(last_val instanceof Drawable)) {
			return last_val.toString(); 
		}
		return null;
	}

	/** Command -- paste a list of global names into the log */
	public void listNames() {
		java.util.List<String> names = Name.getGlobalNames();

		log.println();
		if (names.size() == 0)
			log.print("(no global definitions)");
		else {
			final int MAX = 40;
			String s = names.get(0);
			int w = s.length();
			log.print(s);
			for (int i = 1; i < names.size(); i++) {
				s = names.get(i);
				if (w + s.length() + 2 < MAX) {
					w += 2;
					log.print(", ");
				} else {
					w = 0;
					log.println(",");
				}
				w += s.length();
				log.print(s);
			}
		}
		log.println();
		prompt();
	}

	public static void withFrame(Console frame) throws IOException {
		final GeomLab app = (GeomLab)theApp;
		app.frame = frame;
		app.setLog(frame.getPrintWriter());

		frame.setEvalListener(new EvalListener() {
			public String evalPerformed(String input) {
				return app.evaluate(input);
			}
		});

		app.log.println("Welcome to GeomLab");
		app.log.flush();

		try {
			Session.loadResource("geomlab.gls");
		} catch (CommandException e) {
			app.errorMessage(e.getMessage(), e.getErrtag());
		}

		//for (String f : args)
		//	app.loadFromFile(new File(f), false);
		//app.log.println();
		//app.prompt();
	}
}