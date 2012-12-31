/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import javax.swing.*;
import java.io.File;


/**
 * @author Peter Abeles
 */
public class UtilIO {

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
			boolean foundLib = false;

			for( String s : files ) {
				if( s.compareToIgnoreCase("main") == 0 )
					foundMain = true;
				else if( s.compareToIgnoreCase("lib") == 0 )
					foundLib = true;
			}

			if( foundMain && foundLib )
				return path;

			path = "../"+path;
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

}
