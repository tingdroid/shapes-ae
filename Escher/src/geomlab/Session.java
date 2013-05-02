/*
 * Session.java
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

import funbase.Name;
import funbase.Primitive;
import funbase.Scanner;
import geomlab.Command.CommandException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import android.app.Application;
import android.content.Context;

/**
 * This class provides static methods for serializing the GeomLab session state
 * into a file, and reloading a saved session state. The saved state consists of
 * the set of loaded plugins and all global definitions, also the syntax table
 * from the lexer, but does not include the command history.
 * 
 * Other bits of global state that are not saved: the time and space limits in
 * Evaluator, the palette of colours in Picture.
 */

public class Session extends Application {
	/** Signature for saved sessions (spells "GEOM") */
	private static final int SIG = 0x47454f4d;

	/** Version ID for saved sessions */
	private static final int VERSION = 10000;

	/** Table of loaded plugins */
	private static Set<String> plugins = new LinkedHashSet<String>(10);

	private static Session session = null;

	@Override
	public void onCreate() {
		super.onCreate();
		session = this;
	}

	public static Context context() {
		return session.getApplicationContext();
	}
	
	public static void installPlugin(Class<?> plugin) throws CommandException {
		if (plugins.contains(plugin.getName()))
			return;

		plugins.add(plugin.getName());
		try {
			Field primField = plugin.getField("primitives");
			Primitive prims[] = (Primitive[]) primField.get(null);
			for (Primitive p : prims)
				Primitive.register(p);
		} catch (Exception e) {
			throw new CommandException(e.toString(), "#nohelp");
		}
	}

	/** Load saved session state from a file */
	public static void loadSession(File file) throws CommandException {
		String name = file.getName();
		try {
			InputStream inraw = new BufferedInputStream(new FileInputStream(
					file));
			loadSession(name, inraw);
		} catch (FileNotFoundException e) {
			throw new CommandException("Can't read " + name, "#nofile");
		}
	}

	/** Load from a resource in the classpath (e.g. the prelude file) */
	protected static void loadResource(String name) throws CommandException, IOException {
		InputStream stream = session.getResources().getAssets().open(name);
		if (stream == null)
			throw new CommandException("Can't read resource " + name,
					"#noresource");

		loadSession(name, stream);
	}

	@SuppressWarnings("unchecked")
	private static void loadSession(String name, InputStream inraw)
			throws CommandException {
		try {
			ObjectInputStream in = new ObjectInputStream(inraw);
			int sig = in.readInt();
			if (sig != SIG)
				throw new CommandException("Sorry, file " + name
						+ " is not a saved session", "#badformat");
			int version = in.readInt();
			if (version != VERSION)
				throw new CommandException("Sorry, file " + name
						+ "was saved by a different version of GeomLab",
						"#badversion");

			plugins.clear();
			Primitive.clearPrimitives();

			Set<String> sessionPlugins = (Set<String>) in.readObject();
			for (String x : sessionPlugins) {
				Class<?> plugin = Class.forName(x);
				installPlugin(plugin);
			}

			Scanner.readSyntax(in);
			Name.readNameTable(in);
		} catch (IOException e) {
			throw new CommandException("I/O failed while reading " + name
					+ " - " + e, "#readfail");
		} catch (ClassNotFoundException e) {
			throw new CommandException("Couldn't find class " + e.getMessage(),
					"#missingclass");
		} finally {
			try {
				inraw.close();
			} catch (IOException e) {
			}
		}
	}

	/** Save the session state on a file */
	public static void saveSession(File file) throws CommandException {
		try {
			OutputStream outraw = new BufferedOutputStream(
					new FileOutputStream(file));
			try {
				ObjectOutputStream out = new ObjectOutputStream(outraw);
				out.writeInt(SIG);
				out.writeInt(VERSION);
				out.flush();
				out.writeObject(plugins);
				Scanner.writeSyntax(out);
				Name.writeNameTable(out);
				out.flush();
			} catch (IOException e) {
				throw new CommandException("I/O failed while writing "
						+ file.getName() + " - " + e, "#writefail");
			} finally {
				try {
					outraw.close();
				} catch (IOException e) {
				}
			}
		} catch (FileNotFoundException e) {
			throw new CommandException("Couldn't write " + file.getName(),
					"#nowrite");
		}
	}

}
