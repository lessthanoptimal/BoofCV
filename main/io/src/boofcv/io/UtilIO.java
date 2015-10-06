/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.io;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.javabean.JavaBeanConverter;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.io.xml.XppDriver;

import javax.swing.*;
import java.io.*;
import java.net.URL;


/**
 * @author Peter Abeles
 */
public class UtilIO {

	/**
	 * Returns an absolute path to the file that is relative to the example directory
	 * @param path File path relative to root directory
	 * @return Absolute path to file
	 */
	public static String pathExample( String path ) {
		File pathExample = new File(getPathToBase(),"data/example/");
		if( !pathExample.exists() ) {
			System.err.println();
			System.err.println("Can't find data/example directory!  There are three likely causes for this problem.");
			System.err.println();
			System.err.println("1) You checked out the source code from git and did not pull the data submodule too.");
			System.err.println("2) You are trying to run an example from outside the BoofCV directory tree.");
			System.err.println("3) You are trying to pass in your own image.");
			System.err.println();
			System.err.println("Solutions:");
			System.err.println("1) Follow instructions in the boofcv/readme.md file to grab the data directory.");
			System.err.println("2) Launch the example from inside BoofCV's directory tree!");
			System.err.println("3) Don't use this function and just pass in the path directly");
			System.exit(1);
		}

		File f = new File(pathExample.getPath(),path);
		if( f.isDirectory() )
			return f.getAbsolutePath()+"/";
		else
			return f.getAbsolutePath();
	}

	/**
	 * Searches for the root BoofCV directory and returns an absolute path from it.
	 * @param path File path relative to root directory
	 * @return Absolute path to file
	 */
	public static String path( String path ) {
		return new File(getPathToBase(),path).getAbsolutePath();
	}

	/**
	 * Steps back until it finds the base BoofCV directory.
	 *
	 * @return Path to the base directory.
	 */
	public static String getPathToBase() {
		String path = "./";


		for( int i = 0; i < 3; i++ ) {
			File f = new File(path);
			if( !f.exists() )
				throw new RuntimeException("Failed");
			String[] files = f.list();

			boolean foundMain = false;
			boolean foundExamples = false;
			boolean foundIntegration = false;

			for( String s : files ) {
				if( s.compareToIgnoreCase("main") == 0 )
					foundMain = true;
				else if( s.compareToIgnoreCase("examples") == 0 )
					foundExamples = true;
				else if( s.compareToIgnoreCase("integration") == 0 )
					foundIntegration = true;
			}

			if( foundMain && foundExamples && foundIntegration)
				return path;

			if( i > 0 )
				path = "../"+path;
			else
				path = "../";
		}
		throw new RuntimeException("Base not found");
	}

	/**
	 * Opens up a dialog box asking the user to select a file.  If the user cancels
	 * it either returns null or quits the program.
	 *
	 * @param exitOnCancel If it should quit on cancel or not.
	 * @return Name of the selected file or null if nothing was selected.
	 */
	public static String selectFile(boolean exitOnCancel) {
		String fileName = null;
		JFileChooser fc = new JFileChooser();

		int returnVal = fc.showOpenDialog(null);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			fileName = fc.getSelectedFile().getAbsolutePath();
		} else if (exitOnCancel) {
			System.exit(0);
		}

		return fileName;
	}

	public static void loadLibrarySmart(String libraryName) {

		// see if it works the first try
		if (loadLibrary(libraryName))
			return;

		// otherwise search through the classpath for the library
		String classPath = System.getProperty("java.class.path");

		String stuff[] = classPath.split(":");

		for (String s : stuff) {
			File f = new File(s);
			if (!f.isDirectory())
				continue;
			f = new File(s + "/" + libraryName);
			if (f.exists()) {
				String libraryPath = System.getProperty("java.library.path");
				libraryPath += ":" + s;
				System.setProperty("java.library.path", libraryPath);
				if (!loadLibrary(libraryName))
					throw new RuntimeException("Shouldn't have failed to load this time");
				return;
			}

		}

		System.out.println("classPath");
	}

	public static boolean loadLibrary(String libraryName) {
		try {
			System.out.println("tring to load: " + libraryName);
			System.loadLibrary(libraryName);
			return true;
		} catch (UnsatisfiedLinkError e) {
			return false;
		}
	}

	public static void saveXML( Object o , String fileName ) {
		XStream xstream = createXStream();

		try {
			xstream.toXML(o, new FileOutputStream(fileName));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T loadXML( URL url ) {
		return loadXML(url.getPath());
	}

	public static <T> T loadXML( String directory , String fileName ) {
		return loadXML(new File(directory,fileName).getPath());
	}
	public static <T> T loadXML( String fileName ) {
		try {
			return (T)loadXML(new FileReader(fileName));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T loadXML( Reader r ) {
		XStream xstream = createXStream();

		return (T)xstream.fromXML(r);
	}

	private static XStream createXStream() {
		XStream xstream = new XStream(new PureJavaReflectionProvider(),new XppDriver(),
				new ClassLoaderReference(Thread.currentThread().getContextClassLoader()));
		xstream.registerConverter(new JavaBeanConverter(xstream.getMapper()));
		return xstream;
	}

	public static void save( Object o , String fileName ) {
		try {
			FileOutputStream fileOut = new FileOutputStream(fileName);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(o);
			out.close();
			fileOut.close();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T load( String fileName ) {
		try {
			FileInputStream fileIn = new FileInputStream(fileName);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			T obj = (T)in.readObject();
			in.close();
			fileIn.close();
			return obj;
		} catch(IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}
