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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.StringReader;

import javax.swing.JFrame;

import plugins.Drawable;

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
	private static final String svnid = "$Id: GeomLab.java 365 2008-06-11 17:11:29Z mike $";

	// public final AppFrame frame = new AppFrame();
	// public final GraphBox arena = new GraphBox("Picture", frame);
	// public final PhoneHome phoneHome = new PhoneHome();

	public boolean antialiased = false;

	public GeomLab() {
		setLog(frame.getLogWriter());

		frame.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				evaluate();
			}
		});
	}

	protected void prompt() {
		log.print("> ");
		log.flush();
		frame.setFocus();
	}

	/** Print an error or information message into the log */
	public void logMessage(String msg) {
		log.print("\n[" + msg + "]\n");
	}

	/** Insert "> " after each newline in a command string */
	private String fix(String cmd) {
		StringBuilder res = new StringBuilder();
		int i = 0;
		for (;;) {
			int j = cmd.indexOf("\n", i);
			if (j < 0)
				break;
			res.append(cmd.substring(i, j + 1));
			res.append("> ");
			i = j + 1;
		}
		res.append(cmd.substring(i));
		return res.toString();
	}

	/** Update the picture display */
	protected void displayUpdate(Value val) {
		if (val == null || !(val instanceof Drawable))
			arena.setPicture(null);
		else {
			arena.setPicture((Drawable) val);
			if (!arena.isVisible())
				arena.setVisible(true);
			if (arena.getExtendedState() == JFrame.ICONIFIED)
				arena.setExtendedState(JFrame.NORMAL);

			// Keep main window active -- no idea whether invokeLater helps
			// here or not.
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					frame.setVisible(true);
					frame.toFront();
					frame.input.requestFocus();
				}
			});
		}
	}

	public void loadFileCommand(File file) {
		log.println();
		loadFromFile(file, true);
		prompt();
	}

	/** Command -- evaluate expressions */
	public void evaluate() {
		/*
		 * This runs the commands in another thread to allow display updates
		 * during evaluation
		 */

		String command = frame.input.getText();

		frame.setEnabled(false);
		log.println(fix(command));
		log.flush();

		final StringReader reader = new StringReader(command);

		Thread evalThread = new Thread() {
			public void run() {
				boolean done = eval_loop(reader, true, frame);
				displayUpdate(last_val);
				prompt();
				if (done)
					frame.input.setText("");
				frame.setEnabled(true);
			}
		};

		evalThread.start();
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

	/** Command -- paste the defining text for a name into the input area */
	public void findDefinition() {
		String x = frame.input.getText();
		if (x.equals(""))
			return;

		Name name = Name.find(x);
		String def = name.getDeftext();
		if (def == null) {
			frame.input.setText("\"No definition found\"");
			return;
		}

		frame.input.setText(def);
	}

	public static void main(String args[]) {
		GeomLab app = new GeomLab();
		GeomBase.registerApp(app);
		// app.phoneHome.request();

		app.frame.setJMenuBar(Command.makeAppMenuBar(app));
		app.frame.setVisible(true);

		app.log.println("Welcome to GeomLab");

		try {
			Session.loadResource("geomlab.gls");
		} catch (CommandException e) {
			app.errorMessage(e.getMessage(), e.getErrtag());
		}

		for (String f : args)
			app.loadFromFile(new File(f), false);
		app.log.println();
		app.prompt();
	}
}