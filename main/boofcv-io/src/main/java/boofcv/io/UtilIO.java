/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.io.calibration.CalibrationIO;
import boofcv.misc.BoofLambdas;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;
import boofcv.struct.calib.CameraPinholeBrown;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.RotationType;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Quaternion_F64;
import georegression.struct.so.Rodrigues_F64;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import static boofcv.io.calibration.CalibrationIO.createYmlObject;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Functions for reading and writing different data structures to a file or data stream.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"JdkObsolete", "unchecked", "ConstantConditions", "ForLoopReplaceableByForEach"})
public class UtilIO {
	public static final String UTF8 = "UTF-8";
	public static final String IMAGE_REGEX = "(.*/)*.+\\.(png|jpg|gif|bmp|jpeg|PNG|JPG|GIF|BMP|JPEG)$";

	/**
	 * Saves a list of strings as a YAML file
	 */
	public static void saveListStringYaml( List<String> list, File file ) {
		try (var output = new BufferedOutputStream(new FileOutputStream(file))) {
			Yaml yaml = createYmlObject();
			yaml.dump(list, new OutputStreamWriter(output, UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Saves a list of strings from a YAML file
	 */
	public static List<String> loadListStringYaml( File file ) {
		URL url = UtilIO.ensureURL(file.getPath());
		if (url == null)
			throw new RuntimeException("Unknown file=" + file.getPath());

		try (var reader = new BufferedInputStream(url.openStream())) {
			Yaml yaml = createYmlObject();
			return yaml.load(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Saves a list of 6-dof pose in a CSV format. Rotation matrix is converted into the specified encoding.
	 *
	 * @param list (Input) List of poses
	 * @param type (Input) Format to use for rotation matrix
	 * @param file (Output) Where it's written to.
	 */
	public static void savePoseListCsv( List<Se3_F64> list, RotationType type, File file ) {
		try (var stream = new FileOutputStream(file)) {
			savePoseListCsv(list, type, stream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Saves a list of 6-dof pose in a CSV format. Rotation matrix is converted into the specified encoding.
	 *
	 * @param list (Input) List of poses
	 * @param type (Input) Format to use for rotation matrix
	 * @param output (Output) Where it's written to.
	 */
	public static void savePoseListCsv( List<Se3_F64> list, RotationType type, OutputStream output ) {
		PrintStream out = new PrintStream(output);
		out.println("# 6-DOF pose");
		out.println("# x, y, z, (rotation)");
		out.println("rotation=" + type.name());
		out.println("count=" + list.size());
		for (int i = 0; i < list.size(); i++) {
			Se3_F64 pose = list.get(i);
			out.printf("%.8e %.8e %.8e ", pose.T.x, pose.T.y, pose.T.z);
			switch (type) {
				case EULER -> {
					double[] euler = ConvertRotation3D_F64.matrixToEuler(pose.R, EulerType.XYZ, null);
					out.printf("%.8e %.8e %.8e\n", euler[0], euler[1], euler[2]);
				}

				case QUATERNION -> {
					Quaternion_F64 quat = ConvertRotation3D_F64.matrixToQuaternion(pose.R, null);
					out.printf("%.8e %.8e %.8e %.8e\n", quat.x, quat.y, quat.z, quat.w);
				}

				case RODRIGUES -> {
					Rodrigues_F64 rod = ConvertRotation3D_F64.matrixToRodrigues(pose.R, null);
					Vector3D_F64 v = rod.unitAxisRotation;
					out.printf("%.8e %.8e %.8e %.8e\n", v.x, v.y, v.z, rod.theta);
				}
			}
		}
	}

	/**
	 * Saves a BoofCV {@link Configuration} in a YAML format to disk
	 */
	public static void saveConfig( Configuration config, File file ) {
		try (var stream = new FileOutputStream(file)) {
			var output = new BufferedOutputStream(stream);
			new SerializeConfigYaml().serialize(config, null, new OutputStreamWriter(output, UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Saves a BoofCV {@link Configuration} in a YAML format to disk. Only values which are different from
	 * the 'canonical' reference are saved. This can result in more concise and readable configurations but
	 * can cause problems in repeatability if the reference is changed in the future.
	 *
	 * @param config (Input) The configuration which is to be saved
	 * @param canonical (Input) A configuration that is compared against. Only what's not identical will be saved.
	 * IF null then a new configuration will be created using default constructor.
	 * @param file (Input) Reference to the file that the configuration will be saved at.
	 */
	public static <C extends Configuration>
	void saveConfig( C config, @Nullable C canonical, File file ) {
		try {
			if (canonical == null) {
				canonical = (C)config.getClass().getConstructor().newInstance();
			}
		} catch (InvocationTargetException | InstantiationException |
				IllegalAccessException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}

		try (var stream = new FileOutputStream(file)) {
			var output = new BufferedOutputStream(stream);
			new SerializeConfigYaml().serialize(config, canonical, new OutputStreamWriter(output, UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Loads a BoofCV {@link Configuration} in a YAML format from the disk
	 */
	public static <T extends Configuration> T loadConfig( File file ) {
		URL url = UtilIO.ensureURL(file.getPath());
		if (url == null)
			throw new RuntimeException("Unknown file=" + file.getPath());

		try (InputStream stream = url.openStream()) {
			var output = new BufferedInputStream(stream);
			return (T)new SerializeConfigYaml().deserialize(new InputStreamReader(output, UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Returns an absolute path to the file that is relative to the example directory
	 *
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
			if (pathToBase != null) {
				File pathExample = new File(pathToBase, "data/example/");
				if (pathExample.exists()) {
					return new File(pathExample.getPath(), path).getAbsoluteFile().toURI().toURL();
				}
			}

//			System.out.println("-----------------------");
			// maybe we are running inside an app and all data is stored inside as a resource
//			System.out.println("Attempting to load resource "+path);
			URL url = UtilIO.class.getClassLoader().getResource(path);

			if (url == null) {
				System.err.println(path);
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

	/**
	 * Loads camera model for an example input file. First checks to see if there's calibration specific to that file
	 * if not it looks to see if there is a directory wide one. If all fails it returns null.
	 */
	public static CameraPinholeBrown loadExampleIntrinsic( MediaManager media, File exampleFile ) {
		CameraPinholeBrown intrinsic = null;
		String specialName = FilenameUtils.getBaseName(exampleFile.getName()) + "_intrinsic.yaml";
		File specialIntrinsic = new File(exampleFile.getParent(), specialName);
		Reader reader = media.openFile(specialIntrinsic.getPath());
		if (reader != null) {
			intrinsic = CalibrationIO.load(reader);
		} else {
			reader = media.openFile(new File(exampleFile.getParent(), "intrinsic.yaml").getPath());
			if (reader != null) {
				intrinsic = CalibrationIO.load(reader);
			}
		}
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return Objects.requireNonNull(intrinsic);
	}

	public static BufferedReader openBufferedReader( String fileName ) throws FileNotFoundException {
		InputStream stream = UtilIO.openStream(fileName);
		if (stream == null)
			throw new FileNotFoundException("Can't open " + fileName);
		return new BufferedReader(new InputStreamReader(stream, UTF_8));
	}

	/**
	 * Given a path which may or may not be a URL return a URL
	 */
	public static @Nullable URL ensureURL( String path ) {
		path = systemToUnix(path);
		if (path == null)
			return null;
		URL url;
		try {
			url = new URL(path);
			if (url.getProtocol().equals("jar")) {
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

	public static URL ensureUrlNotNull( @Nullable String path ) {
		if (path == null)
			throw new RuntimeException("Null path provided");
		return Objects.requireNonNull(ensureURL(path));
	}

	public static @Nullable String ensureFilePath( String path ) {
		URL url = ensureURL(path);
		if (url == null)
			return null;
		try {
			//noinspection CharsetObjectCanBeUsed
			return URLDecoder.decode(url.getPath(), UTF8);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	/**
	 * Jar paths don't work if they include up directory. this wills trip those out.
	 */
	public static URL simplifyJarPath( URL url ) {
		try {
			String[] segments = url.toString().split(".jar!/");
			String path = simplifyJarPath(segments[1]);
			return new URL(segments[0] + ".jar!/" + path);
		} catch (IOException e) {
			return url;
		}
	}

	public static String systemToUnix( String path ) {
		if (path == null)
			throw new RuntimeException("Path can't be null");
		if (File.separatorChar == '\\') {
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
			if (!skip) {
				if (f.getName().equals("..")) {
					skip = true;
				} else {
					elements.add(f.getName());
				}
			} else {
				skip = false;
			}
			f = f.getParentFile();
		} while (f != null);

		path = "";
		for (int i = elements.size() - 1; i >= 0; i--) {
			path += elements.get(i);
			if (i > 0)
				path += '/';
		}
		return path;
	}

	public static @Nullable InputStream openStream( String path ) {
		try {
			URL url = ensureURL(path);
			if (url == null) {
				System.err.println("Unable to open " + path);
			} else {
				return url.openStream();
			}
		} catch (IOException ignore) {
		}
		return null;
	}

	/**
	 * Reads a line from an input stream.
	 */
	public static String readLine( InputStream input, StringBuilder buffer ) throws IOException {
		buffer.setLength(0);
		while (true) {
			int v = input.read();
			if (v == -1 || v == '\n')
				return buffer.toString();
			// handle windows \r\n new line
			if (v == '\r')
				continue;
			buffer.append((char)v);
		}
	}

	public static int readInt( InputStream input ) throws IOException {
		int v0 = checkEOF(input);
		int v1 = checkEOF(input);
		int v2 = checkEOF(input);
		int v3 = checkEOF(input);

		return (v0 << 24) | (v1 << 16) | (v2 << 8) | v3;
	}

	private static int checkEOF( InputStream input ) throws IOException {
		int value = input.read();
		if (value == -1)
			throw new IOException("EOF reached");
		return value;
	}

	public static void write( OutputStream output, String message ) throws IOException {
		output.write(message.getBytes(UTF_8));
	}

	public static String pathExample( String path ) {
		File fpath = new File(path);
		if (fpath.isAbsolute())
			return path;
		// Assume we are running inside of the project come
		String pathToBase = getPathToBase();
		if (pathToBase != null) {
			File pathExample = new File(pathToBase, "data/example/");
			if (pathExample.exists()) {
				return new File(pathExample.getPath(), path).getAbsolutePath();
			}
		}

		return pathExampleURL(path).toString();
	}

	public static File fileExample( String path ) {
		return new File(pathExample(path));
	}

	/**
	 * Searches for the root BoofCV directory and returns an absolute path from it.
	 *
	 * @param path File path relative to root directory
	 * @return Absolute path to file
	 */
	public static String path( String path ) {
		String pathToBase = getPathToBase();
		if (pathToBase == null)
			return path;
		return new File(pathToBase, path).getAbsolutePath();
	}

	public static File getFileToBase() {
		return new File(Objects.requireNonNull(getPathToBase()));
	}

	/**
	 * Steps back until it finds the base BoofCV directory.
	 *
	 * @return Path to the base directory.
	 */
	public static @Nullable String getPathToBase() {
		String path = new File(".").getAbsoluteFile().getParent();

		while (path != null) {
			File f = new File(path);
			if (!f.exists())
				break;

			String[] files = f.list();
			if (files == null)
				break;

			boolean foundReadme = false;
			boolean foundMain = false;
			boolean foundExamples = false;
			boolean foundIntegration = false;

			for (String s : files) {
				if (s.compareToIgnoreCase("README.md") == 0)
					foundReadme = true;
				else if (s.compareToIgnoreCase("main") == 0)
					foundMain = true;
				else if (s.compareToIgnoreCase("examples") == 0)
					foundExamples = true;
				else if (s.compareToIgnoreCase("integration") == 0)
					foundIntegration = true;
			}

			if (foundMain && foundExamples && foundIntegration && foundReadme)
				return path;

			path = f.getParent();
		}
		return null;
	}

	/**
	 * Opens up a dialog box asking the user to select a file. If the user cancels
	 * it either returns null or quits the program.
	 *
	 * @param exitOnCancel If it should quit on cancel or not.
	 * @return Name of the selected file or null if nothing was selected.
	 */
	@SuppressWarnings("NullAway")
	public static String selectFile( boolean exitOnCancel ) {
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

	public static void loadLibrarySmart( String libraryName ) {

		// see if it works the first try
		if (loadLibrary(libraryName))
			return;

		// otherwise search through the classpath for the library
		String classPath = System.getProperty("java.class.path");

		String[] stuff = classPath.split(":");

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

	public static boolean loadLibrary( String libraryName ) {
		try {
			System.out.println("tring to load: " + libraryName);
			System.loadLibrary(libraryName);
			return true;
		} catch (UnsatisfiedLinkError e) {
			return false;
		}
	}

	public static void save( Object o, String fileName ) {
		try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
			new ObjectOutputStream(fileOut).writeObject(o);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static <T> T load( String fileName ) {
		@Nullable URL url = UtilIO.ensureURL(fileName);
		if (url == null)
			throw new RuntimeException("Unknown path=" + fileName);

		try (InputStream fileIn = url.openStream()) {
			ObjectInputStream in = new ObjectInputStream(fileIn);
			return (T)in.readObject();
		} catch (IOException | ClassNotFoundException e) {
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
	 * Copies all the files and directories in 'src' into 'dst'.
	 */
	public static void copyRecursive( File src, File dst ) {
		try {
			if (src.isDirectory()) {
				if (!dst.exists() && !dst.mkdirs()) {
					throw new IOException("Cannot create dir " + dst.getAbsolutePath());
				}

				String[] children = src.list();
				for (int i = 0; i < children.length; i++) {
					copyRecursive(new File(src, children[i]), new File(dst, children[i]));
				}
			} else {
				// make sure the directory we plan to store the recording in exists
				File directory = dst.getParentFile();
				if (directory != null && !directory.exists() && !directory.mkdirs()) {
					throw new IOException("Cannot create dir " + directory.getAbsolutePath());
				}

				InputStream in = new FileInputStream(src);
				OutputStream out = new FileOutputStream(dst);

				// Copy the file in chunks
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Reads an entire file and converts it into a text string
	 */
	public static @Nullable String readAsString( String path ) {
		InputStream stream = openStream(path);
		if (stream == null) {
			System.err.println("Failed to open " + path);
			return null;
		}
		return readAsString(stream);
	}

	/**
	 * Reads an entire file and converts it into a text string
	 */
	public static @Nullable String readAsString( InputStream stream ) {
		StringBuilder code = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, UTF_8));
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
		if (nl.compareTo("\n") != 0) {
			output = output.replaceAll(nl, "\n");
		}

		return output;
	}

	/**
	 * Constructs the path for a source code file residing in the examples or demonstrations directory
	 * In the case of the file not being in either directory, an empty string is returned
	 * The expected parameters are class.getPackage().getName(), class.getSimpleName()
	 *
	 * @param pkg package containing the class
	 * @param app simple class name
	 */
	public static String getSourcePath( String pkg, String app ) {
		String path = "";
		if (pkg == null || app == null)
			return path;


		String pathToBase = getPathToBase();
		if (pathToBase != null) {
			if (pkg.contains("examples"))
				path = new File(pathToBase, "examples/src/main/java/").getAbsolutePath();
			else if (pkg.contains("demonstrations"))
				path = new File(pathToBase, "demonstrations/src/main/java/").getAbsolutePath();
			else {
				System.err.println("pkg must be to examples or demonstrations. " + pkg);
				return path;
			}
			String pathToCode = pkg.replace('.', '/') + "/" + app + ".java";
			return new File(path, pathToCode).getPath();
		} else {
			// probably running inside a jar
			String pathToCode = pkg.replace('.', '/') + "/" + app + ".java";
			URL url = UtilIO.class.getClassLoader().getResource(pathToCode);
			if (url != null)
				return url.toString();
			else
				return pathToCode;
		}
	}

	public static String getGithubURL( String pkg, String app ) {
		if (pkg == null || app == null)
			return "";

		String base;
		if (BoofVersion.VERSION.contains("SNAPSHOT")) {
			base = "https://github.com/lessthanoptimal/BoofCV/blob/" + BoofVersion.GIT_SHA + "/";
		} else {
			base = "https://github.com/lessthanoptimal/BoofCV/blob/v" + BoofVersion.VERSION + "/";
		}
		pkg = pkg.replace('.', '/') + "/";

		String dir;
		if (pkg.contains("demonstrations"))
			dir = "demonstrations/";
		else if (pkg.contains("examples"))
			dir = "examples/";
		else
			return "";

		return base + dir + "src/main/java/" + pkg + app + ".java";
	}

	/**
	 * Finds the first javadoc OR the start of the class, which ever comes first.
	 * This does require some thought. The word class can easily be inside a comment.
	 * Comments may or may not be there. Always the potential for stray //
	 */
	public static int indexOfSourceStart( String code ) {
		int state = 0;

		int indexLineStart = 0;
		char previous = 0;
		boolean justEntered = false;

		StringBuilder buffer = new StringBuilder(1024);

		for (int i = 0; i < code.length(); i++) {
			char c = code.charAt(i);

			if (state == 1) {
				if (justEntered) {
					justEntered = false;
					if (c == '*') {
						return indexLineStart;
					}
				}
				if (previous == '*' && c == '/') {
					state = 0;
				}
			} else if (state == 0) {
				if (previous == '/' && c == '/') {
					state = 2;
				} else if (previous == '/' && c == '*') {
					state = 1;
					justEntered = true;
				} else {
					buffer.append(c);
				}
			}

			if (c == '\n') {
				if (buffer.toString().contains("class")) {
					return indexLineStart;
				}
				buffer.delete(0, buffer.length());
				indexLineStart = i + 1;
				if (state == 2) {
					state = 0;
				}
			}

			previous = c;
		}
		if (buffer.toString().contains("class")) {
			return indexLineStart;
		} else {
			return 0;
		}
	}

	/**
	 * Loads a list of files with the specified prefix.
	 *
	 * @param directory Directory it looks inside of
	 * @param prefix Prefix that the file must have. Null if no prefix.
	 * @param suffix Suffix that the file must have. Null if no suffix
	 * @return List of files that are in the directory and match the prefix.
	 */
	public static List<String> listByPrefix( String directory, @Nullable String prefix, @Nullable String suffix ) {
		List<String> ret = new ArrayList<>();

		File d = new File(directory);

		if (!d.isDirectory()) {
			try {
				URL url = new URL(directory);
				if (url.getProtocol().equals("file")) {
					d = new File(url.getFile());
				} else if (url.getProtocol().equals("jar")) {
					return listJarPrefix(url, prefix, suffix);
				}
			} catch (MalformedURLException ignore) {
			}
		}
		if (!d.isDirectory())
			throw new IllegalArgumentException("Must specify an directory. " + directory);

		File[] files = d.listFiles();

		for (File f : files) {
			if (f.isDirectory() || f.isHidden())
				continue;

			if (prefix == null || f.getName().startsWith(prefix)) {
				if (suffix == null || f.getName().endsWith(suffix)) {
					ret.add(f.getAbsolutePath());
				}
			}
		}

		return ret;
	}

	/**
	 * An intelligent image file search. If the passed in string starts with "glob:" or "regex:" then it will use
	 * a glob or regex pattern to find the files. Otherwise it will see if it's a file or directory. If a file
	 * then just that file is returned in the list. If a directory then all the files in that directory are returned.
	 *
	 * @param pathPattern Either a path to the file/directory or a glob/regex pattern.
	 * @param sort If the output should be sorted first
	 * @param filter A filter that provides better flexibility in deciding what's included
	 * @return List of found files that matched all the patterns
	 */
	public static List<String> listSmart( String pathPattern, boolean sort, BoofLambdas.Filter<Path> filter ) {
		List<String> results = new ArrayList<>();

		if (pathPattern.startsWith("glob:") || pathPattern.startsWith("regex:")) {
			try {
				String baseDirectory = findBaseDirectoryInPattern(pathPattern);
				PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(pathPattern);

				Files.walkFileTree(Paths.get(baseDirectory), new SimpleFileVisitor<>() {
					@Override public FileVisitResult visitFile( Path path, BasicFileAttributes attrs ) {
						if (pathMatcher.matches(path) && filter.keep(path)) {
							results.add(path.toString());
						}
						return FileVisitResult.CONTINUE;
					}

					@Override public FileVisitResult visitFileFailed( Path file, IOException exc ) {
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else {
			File directory = new File(pathPattern);
			if (directory.isFile()) {
				if (filter.keep(directory.toPath()))
					results.add(pathPattern);
			} else if (directory.isDirectory()) {
				File[] files = directory.listFiles();
				if (files != null) {
					for (File f : files) {
						if (!filter.keep(f.toPath()))
							continue;
						results.add(f.getPath());
					}
				}
			}
		}

		if (sort)
			Collections.sort(results);

		return results;
	}

	/**
	 * This searches for a subset of the pattern which matches a valid directory. This is intended to reduce
	 * the number of files that are searched. Which is particularly important when the pattern is in reference
	 * to the root file system.
	 *
	 * It does this by creating a file from a sub string and seeing if it's a directory. Edge cases:
	 *
	 * Directory Tree:
	 * foo/aaaaa/b
	 * foo/aa/b
	 *
	 * Pattern:  foo/aa*  <-- This would only search inside of aa
	 * foo/aa/*
	 */
	static String findBaseDirectoryInPattern( String pathPattern ) {
		// Reduce the search scope and figure out if it's an absolute or relative path by looking for the
		// prefix which is a valid path
		File lastDirectory = new File("/");
		int start = pathPattern.indexOf(':') + 1;
		int end = -1;
		boolean previousDirectory = false;
		for (int i = start + 1; i <= pathPattern.length(); i++) {
			var f = new File(pathPattern.substring(start, i));
			// If the previous string was a directory and now the parent directory has changed after
			// adding another character that means the last directory must be the complete name
			// it was going for
			File parent = f.getParentFile();
			if (parent != null && previousDirectory && parent.getPath().equals(lastDirectory.getPath())) {
				end = i - 1;
			}

			if (!f.isDirectory()) {
				previousDirectory = false;
				continue;
			}

			previousDirectory = true;
			lastDirectory = f;
		}
		if (previousDirectory) {
			end = pathPattern.length();
		}
		// If it was never valid then we will assume that it's a relative pattern. Otherwise you would end
		// up searching the entire file system a lot
		return end != -1 ? pathPattern.substring(start, end) : "";
	}

	/**
	 * Same as {@link #listSmart(String, boolean, BoofLambdas.Filter)} but adds a filter that looks for images
	 * based on their extension.
	 *
	 * @param pathPattern Either a path to the file/directory or a glob/regex pattern.
	 * @param sort If the output should be sorted first
	 * @return List of matching file paths
	 */
	public static List<String> listSmartImages( String pathPattern, boolean sort ) {
		return listSmart(pathPattern, sort, ( path ) -> path.toString().matches(IMAGE_REGEX));
	}

	/**
	 * Lists all images in the directory using a regex and optionally sorts the list
	 */
	public static List<String> listImages( String directory, boolean sort ) {
		List<String> found = listByRegex(directory, IMAGE_REGEX);
		if (sort)
			Collections.sort(found);
		return found;
	}

	public static List<String> listByRegex( String directory, String regex ) {
		List<String> ret = new ArrayList<>();

		File d = new File(directory);

		if (!d.isDirectory()) {
			try {
				URL url = new URL(directory);
				if (url.getProtocol().equals("file")) {
					d = new File(url.getFile());
				} else if (url.getProtocol().equals("jar")) {
					return listJarRegex(url, regex);
				}
			} catch (MalformedURLException ignore) {
			}
		}
		if (!d.isDirectory())
			throw new IllegalArgumentException("Must specify an directory. " + directory);

		File[] files = d.listFiles();

		for (File f : files) {
			if (f.isDirectory() || f.isHidden())
				continue;

			if (f.getName().matches(regex)) {
				ret.add(f.getAbsolutePath());
			}
		}

		return ret;
	}

	/**
	 * Returns a list of all the children as a file and sorts them
	 */
	public static List<File> listFilesSorted( File directory ) {
		List<File> ret = new ArrayList<>();
		if (!directory.isDirectory())
			return ret;

		File[] files = directory.listFiles();
		if (files == null)
			return ret;

		ret.addAll(Arrays.asList(files));
		Collections.sort(ret);

		return ret;
	}

	public static List<String> listAll( String directory ) {
		List<String> ret = new ArrayList<>();

		try {
			// see if it's a URL or not
			URL url = new URL(directory);
			if (url.getProtocol().equals("file")) {
				directory = url.getFile();
			} else if (url.getProtocol().equals("jar")) {
				return listJarPrefix(url, null, null);
			} else {
				throw new RuntimeException("Not sure what to do with this url. " + url.toString());
			}
		} catch (MalformedURLException ignore) {
		}

		File d = new File(directory);

		if (!d.isDirectory())
			throw new IllegalArgumentException("Must specify an directory");

		File[] files = d.listFiles();

		for (File f : files) {
			if (f.isDirectory() || f.isHidden())
				continue;

			ret.add(f.getAbsolutePath());
		}

		return ret;
	}

	/**
	 * Lists all files in the directory with an MIME type that contains the string "type"
	 */
	public static List<String> listAllMime( String directory, String type ) {
		List<String> ret = new ArrayList<>();

		try {
			// see if it's a URL or not
			URL url = new URL(directory);
			if (url.getProtocol().equals("file")) {
				directory = url.getFile();
			} else if (url.getProtocol().equals("jar")) {
				return listJarMime(url, null, null);
			} else {
				throw new RuntimeException("Not sure what to do with this url. " + url.toString());
			}
		} catch (MalformedURLException ignore) {
		}

		File d = new File(directory);

		if (!d.isDirectory())
			throw new IllegalArgumentException("Must specify an directory");

		File[] files = d.listFiles();
		if (files == null)
			return ret;

		for (File f : files) {
			if (f.isDirectory())
				continue;
			try {
				String mimeType = Files.probeContentType(f.toPath());

				if (mimeType.contains(type))
					ret.add(f.getAbsolutePath());
			} catch (IOException ignore) {
			}
		}

		Collections.sort(ret);
		return ret;
	}

	private static List<String> listJarPrefix( URL url, @Nullable String prefix, @Nullable String suffix ) {
		List<String> output = new ArrayList<>();

		JarFile jarfile;
		try {
			JarURLConnection connection = (JarURLConnection)url.openConnection();
			jarfile = connection.getJarFile();

			String targetPath = connection.getEntryName() + "/";
			if (prefix != null) {
				targetPath += prefix;
			}

			final Enumeration<JarEntry> e = jarfile.entries();
			while (e.hasMoreElements()) {
				final ZipEntry ze = (ZipEntry)e.nextElement();
//				System.out.println("  ze.anme="+ze.getName());
				if (ze.getName().startsWith(targetPath) &&
						ze.getName().length() != targetPath.length()) {
					if (suffix == null || ze.getName().endsWith(suffix)) {
						output.add("jar:file:" + jarfile.getName() + "!/" + ze.getName());
					}
				}
			}

			jarfile.close();
			return output;
		} catch (IOException e) {
			return new ArrayList<>();
		}
	}

	private static List<String> listJarMime( URL url, @Nullable String prefix, @Nullable String type ) {
		List<String> output = new ArrayList<>();

		FileNameMap fileNameMap = URLConnection.getFileNameMap();

		JarFile jarfile;
		try {
			JarURLConnection connection = (JarURLConnection)url.openConnection();
			jarfile = connection.getJarFile();

			String targetPath = connection.getEntryName() + "/";
			if (prefix != null) {
				targetPath += prefix;
			}

			final Enumeration e = jarfile.entries();
			while (e.hasMoreElements()) {
				final ZipEntry ze = (ZipEntry)e.nextElement();
//				System.out.println("  ze.anme="+ze.getName());

				if (ze.getName().startsWith(targetPath) &&
						ze.getName().length() != targetPath.length()) {
					// TODO no idea if this will work and is fast
					String path = "jar:file:" + jarfile.getName() + "!/" + ze.getName();
					String mimeType = fileNameMap.getContentTypeFor(path);
					if (mimeType.contains(type)) {
						output.add(path);
					}
				}
			}

			jarfile.close();
			return output;
		} catch (IOException e) {
			return new ArrayList<>();
		}
	}

	private static List<String> listJarRegex( URL url, String regex ) {
		List<String> output = new ArrayList<>();

		JarFile jarfile;
		try {
			JarURLConnection connection = (JarURLConnection)url.openConnection();
			jarfile = connection.getJarFile();

			String targetPath = connection.getEntryName() + "/";

			final Enumeration<JarEntry> e = jarfile.entries();
			while (e.hasMoreElements()) {
				final ZipEntry ze = (ZipEntry)e.nextElement();
//				System.out.println("  ze.anme="+ze.getName());
				if (ze.getName().startsWith(targetPath) &&
						ze.getName().length() != targetPath.length()) {
					String shortName = ze.getName().substring(targetPath.length());
					if (shortName.matches(regex)) {
						output.add("jar:file:" + jarfile.getName() + "!/" + ze.getName());
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
	public static File[] findMatches( File directory, String regex ) {
		final Pattern p = Pattern.compile(regex); // careful: could also throw an exception!
		return directory.listFiles(file -> p.matcher(file.getName()).matches());
	}

	public static boolean validURL( URL url ) {
		try {
			url.openConnection();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public static void copyToFile( InputStream in, File file ) {
		try (FileOutputStream out = new FileOutputStream(file)) {
			if (in == null) throw new RuntimeException("Input is null");
			byte[] buffer = new byte[1024*1024];
			while (in.available() > 0) {
				int amount = in.read(buffer, 0, buffer.length);
				out.write(buffer, 0, amount);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Recursively deletes all files in path which pass the test.
	 *
	 * @param f base directory or file
	 * @param test The test
	 * @throws IOException Exception if fails to delete a file
	 */
	public static void delete( File f, FileTest test ) throws IOException {
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			if (files != null) {
				for (File c : files) {
					delete(c, test);
				}
			}
		}
		if (test.isTarget(f) && !f.delete())
			throw new IOException("Failed to delete file: " + f);
	}

	public static String checkIfJarAndCopyToTemp( String filename ) {
		// InputStream can't be seeked. This is a problem. Hack around it is to write the file
		// to a temporary file or see if it's a file  pass that in
		URL url = ensureURL(filename);
		if (url == null)
			throw new RuntimeException("Invalid: " + filename);
		switch (url.getProtocol()) {
			case "file":
				filename = url.getPath();
				// url will add %20 if there's a space in the path. This removes that and other encoding
				try {
					filename = URLDecoder.decode(filename, UTF_8.name());
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
				// the filename will include an extra / in windows, this is fine
				// in Java but FFMPEG can't handle it. So this will strip off the
				// extra character and be cross platform
				filename = new File(filename).getAbsolutePath();
				break;

			case "jar":
				System.out.println("Copying the file from the jar as a work around");
				String suffix = FilenameUtils.getExtension(filename);
				// copy the resource into a temporary file
				try {
					InputStream in = openStream(filename);
					if (in == null) throw new RuntimeException("Failed to open " + filename);
					final File tempFile = File.createTempFile("boofcv_jar_hack_", suffix);
					tempFile.deleteOnExit();
					copyToFile(in, tempFile);
					in.close();
					filename = tempFile.getAbsolutePath();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				break;
		}
		return filename;
	}

	/**
	 * Convenience function which creates a directory and everything along its path if it does not exist.
	 * Also checks to make sure the path leads to a directory if it does exist
	 *
	 * @param directory Path to the directory
	 */
	public static void mkdirs( File directory ) {
		if (directory.exists()) {
			BoofMiscOps.checkTrue(directory.isDirectory());
		} else {
			BoofMiscOps.checkTrue(directory.mkdirs());
		}
	}

	/**
	 * Checks to directory exists. If it does and delete is true it will recursively delete it and all its children.
	 * Then creates the directory again. If delete is false then it calls {@link #mkdirs(File)}.
	 *
	 * @param directory Path to the directory
	 * @param delete if true it will delete the original directory
	 */
	public static void mkdirs( File directory, boolean delete ) {
		if (!delete) {
			mkdirs(directory);
			return;
		}

		if (directory.exists()) {
			deleteRecursive(directory);
		}
		BoofMiscOps.checkTrue(directory.mkdirs());
	}

	public interface FileTest {
		boolean isTarget( File f );
	}
}
