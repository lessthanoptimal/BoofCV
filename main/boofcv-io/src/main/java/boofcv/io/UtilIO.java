/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.BoofVersion;

import javax.swing.*;
import java.io.*;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * @author Peter Abeles
 */
public class UtilIO {
	/**
	 * Returns an absolute path to the file that is relative to the example directory
	 * @param path File path relative to root directory
	 * @return Absolute path to file
	 */
	public static URL pathExampleURL( String path ) {
		try {
			File fpath = new File(path);
			if (fpath.isAbsolute())
				return fpath.toURI().toURL();
			// Assume we are running inside of the project come
			String pathToBase = getPathToBase();
			if( pathToBase != null ) {
				File pathExample = new File(pathToBase, "data/example/");
				if (pathExample.exists()) {
					return new File(pathExample.getPath(), path).getAbsoluteFile().toURL();
				}
			}

			System.out.println("-----------------------");
			// maybe we are running inside an app and all data is stored inside as a resource
			System.out.println("Attempting to load resource "+path);
			URL url = UtilIO.class.getClassLoader().getResource(path);

			if (url == null) {
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
			return url;
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static BufferedReader openBufferedReader(String fileName) throws FileNotFoundException {
		InputStream stream = UtilIO.openStream(fileName);
		if( stream == null )
			throw new FileNotFoundException("Can't open "+fileName);
		return new BufferedReader(new InputStreamReader(stream));
	}

	/**
	 * Given a path which may or may not be a URL return a URL
	 */
	public static URL ensureURL(String path ) {
		path = systemToUnix(path);
		URL url;
		try {
			url = new URL(path);
			if( url.getProtocol().equals("jar")) {
				return simplifyJarPath(url);
			}
		} catch (MalformedURLException e) {
			// might just be a file reference.
			try {
				url = new File(path).toURI().toURL(); // simplify the path. "1/2/../3" = "1/3"
			} catch (MalformedURLException e2) {
				return null;
			}
		}
		return url;
	}

	/**
	 * Jar paths don't work if they include up directory. this wills trip those out.
	 */
	public static URL simplifyJarPath( URL url ) {
		try {
			String segments[] = url.toString().split(".jar!/");
			String path = simplifyJarPath(segments[1]);
			return new URL(segments[0]+".jar!/"+path);
		} catch (IOException e) {
			return url;
		}
	}

	public static String systemToUnix(String path) {
		if (path==null) return null;
		if (File.separatorChar=='\\') {
			return path.replace('\\', '/');
		} else {
			return path;
		}
	}

	public static String simplifyJarPath( String path ) {
		List<String> elements = new ArrayList<>();
		File f = new File(path);

		boolean skip = false;
		do {
			if( !skip ) {
				if( f.getName().equals("..")) {
					skip = true;
				} else {
					elements.add(f.getName());
				}
 			} else {
				skip = false;
			}
			f = f.getParentFile();
		}while( f != null );

		path = "";
		for (int i = elements.size()-1; i >=0; i--) {
			path += elements.get(i);
			if( i > 0 )
				path+='/';
		}
		return path;
	}

	public static InputStream openStream( String path ) {
		try {
			URL url = ensureURL(path);
			if( url == null ) {
				System.err.println("Unable to open "+path);
			} else {
				return url.openStream();
			}
		} catch (IOException ignore) {}
		return null;
	}

	public static String pathExample( String path ) {
		File fpath = new File(path);
		if (fpath.isAbsolute())
			return path;
		// Assume we are running inside of the project come
		String pathToBase = getPathToBase();
		if( pathToBase != null ) {
			File pathExample = new File(pathToBase, "data/example/");
			if (pathExample.exists()) {
				return new File(pathExample.getPath(), path).getAbsolutePath();
			}
		}

		return pathExampleURL(path).toString();
	}

	/**
	 * Searches for the root BoofCV directory and returns an absolute path from it.
	 * @param path File path relative to root directory
	 * @return Absolute path to file
	 */
	public static String path( String path ) {
		String pathToBase = getPathToBase();
		if( pathToBase == null )
			return path;
		return new File(pathToBase,path).getAbsolutePath();
	}

	public static File getFileToBase() {
		return new File(getPathToBase());
	}

	/**
	 * Steps back until it finds the base BoofCV directory.
	 *
	 * @return Path to the base directory.
	 */
	public static String getPathToBase() {
		String path = new File(".").getAbsoluteFile().getParent();

		while( path != null )  {
			File f = new File(path);
			if( !f.exists() )
				break;

			String[] files = f.list();
			if( files == null )
				break;

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

			path = f.getParent();
		}
		return null;
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
		} catch(IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Deletes all the file/directory and all of its children
	 */
	public static void deleteRecursive( File f ) {
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				deleteRecursive(c);
		}
		if (!f.delete())
			throw new RuntimeException("Failed to delete file: " + f);
	}

	/**
	 * Reads an entire file and converts it into a text string
	 */
	public static String readAsString( String path ) {
		InputStream stream = openStream(path);
		if( stream == null ) {
			System.err.println("Failed to open "+path);
			return null;
		}
		return readAsString(stream);
	}

	/**
	 * Reads an entire file and converts it into a text string
	 */
	public static String readAsString( InputStream stream ) {
		StringBuilder code = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line;
			while ((line = reader.readLine()) != null)
				code.append(line).append(System.lineSeparator());
			reader.close();
		} catch (IOException e) {
			return null;
//			throw new RuntimeException(e);
		}
		String output = code.toString();

		// make windows strings appear the same as linux strings
		String nl = System.getProperty("line.separator");
		if( nl.compareTo("\n") != 0 ) {
			output = output.replaceAll(nl,"\n");
		}

		return output;
	}

	/**
	 * Constructs the path for a source code file residing in the examples or demonstrations directory
	 * In the case of the file not being in either directory, an empty string is returned
	 * The expected parameters are class.getPackage().getName(), class.getSimpleName()
	 * @param pkg package containing the class
	 * @param app simple class name
	 * @return
	 */
	public static String getSourcePath(String pkg, String app) {
		String path = "";
		if(pkg == null || app == null)
			return path;


		String pathToBase = getPathToBase();
		if( pathToBase != null ) {
			if(pkg.contains("examples"))
				path =  new File(pathToBase,"examples/src/main/java/").getAbsolutePath();
			else if(pkg.contains("demonstrations"))
				path =  new File(pathToBase,"demonstrations/src/main/java/").getAbsolutePath();
			String pathToCode = pkg.replace('.','/') + "/" + app + ".java";
			return new File(path,pathToCode).getPath();
		} else {
			// probably running inside a jar
			String pathToCode = pkg.replace('.','/') + "/" + app + ".java";
			URL url = UtilIO.class.getClassLoader().getResource(pathToCode);
			if( url != null )
				return url.toString();
			else
				return pathToCode;
		}
	}

	public static String getGithubURL(String pkg, String app) {
		if(pkg == null || app == null)
			return "";

		String base;
		if( BoofVersion.VERSION.contains("SNAPSHOT")) {
			base = "https://github.com/lessthanoptimal/BoofCV/tree/" + BoofVersion.GIT_SHA + "/";
		} else {
			base = "https://github.com/lessthanoptimal/BoofCV/blob/v" + BoofVersion.VERSION + "/";
		}
		pkg = pkg.replace('.','/') + "/";

		String dir;
		if(pkg.contains("demonstrations"))
			dir = "demonstrations/";
		else if(pkg.contains("examples"))
			dir = "examples/";
		else
			return "";

		return base + dir + "src/main/java/" + pkg + app + ".java";
	}

	/**
	 * Finds the first javadoc OR the start of the class, which ever comes first.
	 * This does require some thought.  The word class can easily be inside a comment.
	 * Comments may or may not be there.  Always the potential for stray //
	 */
	public static int indexOfSourceStart( String code ) {
		int state = 0;

		int indexLineStart = 0;
		char previous = 0;
		boolean justEntered = false;

		StringBuilder buffer = new StringBuilder(1024);

		for (int i = 0; i < code.length(); i++) {
			char c = code.charAt(i);

			if( state == 1 ) {
				if( justEntered ) {
					justEntered = false;
					if( c == '*' ) {
						return indexLineStart;
					}
				}
				if( previous == '*' && c == '/') {
					state = 0;
				}
			} else if( state == 0 ){
				if( previous == '/' && c == '/' ) {
					state = 2;
				} else if( previous == '/' && c == '*') {
					state = 1;
					justEntered = true;
				} else {
					buffer.append(c);
				}
			}

			if( c == '\n' ) {
				if( buffer.toString().contains("class")) {
					return indexLineStart;
				}
				buffer.delete(0,buffer.length());
				indexLineStart = i+1;
				if( state == 2 ) {
					state = 0;
				}
			}

			previous = c;
		}
		if( buffer.toString().contains("class")) {
			return indexLineStart;
		} else{
			return 0;
		}
	}

	/**
	 * Loads a list of files with the specified prefix.
	 *
	 * @param directory Directory it looks inside of
	 * @param prefix Prefix that the file must have
	 * @param suffix
	 * @return List of files that are in the directory and match the prefix.
	 */
	public static List<String> listByPrefix(String directory, String prefix, String suffix) {
		List<String> ret = new ArrayList<>();

		File d = new File(directory);

		if( !d.isDirectory() ) {
			try {
				URL url = new URL(directory);
				if( url.getProtocol().equals("file")) {
					d = new File(url.getFile());
				} else if( url.getProtocol().equals("jar")){
					return listJarPrefix(url,prefix,suffix);
				}
			} catch( MalformedURLException ignore){}
		}
		if( !d.isDirectory() )
			throw new IllegalArgumentException("Must specify an directory. "+directory);

		File files[] = d.listFiles();

		for( File f : files ) {
			if( f.isDirectory() || f.isHidden() )
				continue;

			if( prefix == null || f.getName().startsWith(prefix )) {
				if( suffix ==null || f.getName().endsWith(suffix)) {
					ret.add(f.getAbsolutePath());
				}
			}
		}

		return ret;
	}

	public static List<String> listByRegex(String directory, String regex ) {
		List<String> ret = new ArrayList<>();

		File d = new File(directory);

		if( !d.isDirectory() ) {
			try {
				URL url = new URL(directory);
				if( url.getProtocol().equals("file")) {
					d = new File(url.getFile());
				} else if( url.getProtocol().equals("jar")){
					return listJarRegex(url,regex);
				}
			} catch( MalformedURLException ignore){}
		}
		if( !d.isDirectory() )
			throw new IllegalArgumentException("Must specify an directory. "+directory);

		File files[] = d.listFiles();

		for( File f : files ) {
			if( f.isDirectory() || f.isHidden() )
				continue;

			if( f.getName().matches(regex) ) {
				ret.add(f.getAbsolutePath());
			}
		}

		return ret;
	}

	public static List<String> listAll(String directory ) {
		List<String> ret = new ArrayList<>();

		try {
			// see if it's a URL or not
			URL url = new URL(directory);
			if( url.getProtocol().equals("file") ) {
				directory = url.getFile();
			} else if( url.getProtocol().equals("jar") ) {
				return listJarPrefix(url,null,null);
			} else {
				throw new RuntimeException("Not sure what to do with this url. "+url.toString());
			}
		} catch (MalformedURLException ignore) {
		}

		File d = new File(directory);

		if( !d.isDirectory() )
			throw new IllegalArgumentException("Must specify an directory");

		File files[] = d.listFiles();

		for( File f : files ) {
			if( f.isDirectory() || f.isHidden() )
				continue;

			ret.add(f.getAbsolutePath());
		}

		return ret;
	}

	private static List<String> listJarPrefix(URL url , String prefix , String suffix ) {
		List<String> output = new ArrayList<>();

		JarFile jarfile;
		try {
			JarURLConnection connection = (JarURLConnection)url.openConnection();
			jarfile = connection.getJarFile();

			String targetPath = connection.getEntryName()+"/";
			if( prefix != null ) {
				targetPath += prefix;
			}

			final Enumeration e = jarfile.entries();
			while( e.hasMoreElements() ) {
				final ZipEntry ze = (ZipEntry) e.nextElement();
//				System.out.println("  ze.anme="+ze.getName());
				if( ze.getName().startsWith(targetPath) &&
						ze.getName().length() != targetPath.length()) {
					if( suffix == null || ze.getName().endsWith(suffix))  {
						output.add("jar:file:"+jarfile.getName()+"!/"+ze.getName());
					}
				}
			}

			jarfile.close();
			return output;
		} catch (IOException e) {
			return new ArrayList<>();
		}
	}

	private static List<String> listJarRegex(URL url , String regex ) {
		List<String> output = new ArrayList<>();

		JarFile jarfile;
		try {
			JarURLConnection connection = (JarURLConnection)url.openConnection();
			jarfile = connection.getJarFile();

			String targetPath = connection.getEntryName()+"/";

			final Enumeration e = jarfile.entries();
			while( e.hasMoreElements() ) {
				final ZipEntry ze = (ZipEntry) e.nextElement();
//				System.out.println("  ze.anme="+ze.getName());
				if( ze.getName().startsWith(targetPath) &&
						ze.getName().length() != targetPath.length()) {
					String shortName = ze.getName().substring(targetPath.length());
					if( shortName.matches(regex)) {
						output.add("jar:file:"+jarfile.getName()+"!/"+ze.getName());
					}
				}
			}

			jarfile.close();
			return output;
		} catch (IOException e) {
			return new ArrayList<>();
		}
	}

	/**
	 * <p>
	 * Looks for file names which match the regex in the directory.
	 * </p>
	 * <p>
	 * Example:<br>
	 * BoofMiscOps.findMatches(new File("/path/to/directory"), ".+jpg");
	 * </p>
	 *
	 * @param directory directory
	 * @param regex file name regex
	 * @return array of matching files
	 */
	public static File[] findMatches( File directory , String regex ) {
		final Pattern p = Pattern.compile(regex); // careful: could also throw an exception!
		return directory.listFiles(file -> p.matcher(file.getName()).matches());
	}

	public static boolean validURL( URL url ) {
		try {
			URLConnection c = url.openConnection();
			return true;
		} catch( IOException e ) {
			return false;
		}
	}

	public static void copyToFile( InputStream in , File file ) {
		try {
			if( in == null ) throw new RuntimeException("Input is null");
			FileOutputStream out = new FileOutputStream(file);
			byte buffer[] = new byte[1024*1024];
			while( in.available() > 0 ) {
				int amount = in.read(buffer,0,buffer.length);
				out.write(buffer,0,amount);
			}
			out.close();
			in.close();
		} catch( IOException e ) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
