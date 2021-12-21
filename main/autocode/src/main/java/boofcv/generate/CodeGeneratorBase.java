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

package boofcv.generate;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static boofcv.AutocodeMasterApp.findPathToProjectRoot;

/**
 * <p>Base class for code generators.</p>
 *
 * @author Peter Abeles
 */
public abstract class CodeGeneratorBase {
	public static String copyright = "/** Copyright Peter Abeles. Failed to load copyright.txt. */";

	public static final int MAX_LINE_LENGTH = 120;
	public static final int SPACE_PER_TAB = 4;

	protected PrintStream out = System.out;
	protected @Nullable String className;
	protected @Setter @Nullable String moduleName;
	/**
	 * If true the output will be in the source directory, overwriting existing code
	 */
	protected @Getter @Setter boolean overwrite = true;

	static {
		try {
			File pathCopyright = new File(findPathToProjectRoot(), "misc/copyright.txt");
			// The trim is to make we know how much white space (i.e. none) is at the end, which can be variable
			copyright = readFile(pathCopyright.getAbsolutePath(), StandardCharsets.UTF_8).trim() + "\n";
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String readFile( String path, Charset encoding ) throws IOException {
		return new String(Files.readAllBytes(Paths.get(path)), encoding);
	}

	public void parseArguments( String[] args ) {
		// todo fill this in later
	}

	public void autoSelectName() {
		className = getClass().getSimpleName();
		if (className.startsWith("Generate")) {
			int l = "Generate".length();
			className = className.substring(l);
			try {
				initFile();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		} else {
			System.out.println("Class name doesn't start with Generate");
		}
	}

	protected void printParallel( String var, String lower, String upper, String body ) {
		out.println();
		out.printf("\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(%s, %s, %s -> {\n", lower, upper, var);
		out.printf("\t\tfor (int %s = %s; %s < %s; %s++) {\n", var, lower, var, upper, var);
		out.print(body);
		out.print("\t\t}\n");
		out.print("\t\t//CONCURRENT_ABOVE });\n");
	}

	protected void printParallelBlock( String nameY0, String nameY1, String lower, String upper, @Nullable String minBlock, String body ) {
		out.println();

		if (minBlock!=null)
			out.printf("\t\t//CONCURRENT_BELOW BoofConcurrency.loopBlocks(%s, %s, %s, workspaces, (work, %s, %s)->{\n", lower, upper, minBlock, nameY0, nameY1);
		else
			out.printf("\t\t//CONCURRENT_BELOW BoofConcurrency.loopBlocks(%s, %s, workspaces, (work, %s, %s)->{\n", lower, upper, nameY0, nameY1);
		out.printf("\t\tfinal int %s = %s, %s = %s;\n", nameY0, lower, nameY1, upper);
		out.print(body);
		out.print("\t\t//CONCURRENT_INLINE });\n");
	}

	/**
	 * Creates
	 */
	public void generate() throws FileNotFoundException {
		if (className == null) {
			autoSelectName();
		}
		generateCode();
	}

	protected abstract void generateCode() throws FileNotFoundException;

	public void initFile() throws FileNotFoundException {
		File file = new File(className + ".java");
		if (overwrite) {
			if (moduleName == null)
				throw new RuntimeException("Overwrite is true but the module isn't specified");

			File path_to_root = findPathToProjectRoot();
			File path_to_file = new File(path_to_root, new File("main", moduleName).getPath());
			if (!path_to_file.exists() || !path_to_file.isDirectory()) {
				System.err.println("Can't find project root. Attempted to use " + path_to_root.getAbsolutePath());
			} else {
				file = new File(path_to_file, new File(packageToPath(getClass()), file.getName()).getPath());
				// some last sanity checks
				if (!file.getParentFile().exists()) {
					if (!file.getParentFile().mkdirs()) {
						throw new RuntimeException("Failed to create path " + file.getParentFile().getPath());
					}
				}
			}
		}

		System.out.println(file.getAbsolutePath());

		out = new PrintStream(new FileOutputStream(file));
		out.print(copyright);
		out.println();
		out.println("package " + getPackage() + ";");
		out.println();
	}

	public File packageToPath( Class c ) {
		String name = c.getCanonicalName();

		String[] words = name.split("\\.");
		String path = "src/main/java/";
		for (int i = 0; i < words.length - 1; i++) {
			path += words[i] + "/";
		}
		return new File(path);
	}

	public void setOutputFile( String className ) throws FileNotFoundException {
		if (this.className != null)
			throw new IllegalArgumentException("ClassName already set. Out of date code?");
		this.className = className;
		initFile();
	}

	public String generateDocString( String... authors ) {
		String ret = " *\n" +
				" * <p>DO NOT MODIFY. Automatically generated code created by " + getClass().getSimpleName() + "</p>\n" +
				" *\n";

		for (String author : authors) {
			ret += " * @author " + author + "\n";
		}
		ret += " */\n" +
				"@Generated(\"" + getClass().getCanonicalName() + "\")\n";

		return ret;
	}

	/**
	 * Creates a function signal and automatically formats text so that it fits within the maximum length
	 */
	public String functionSignature( int tabs, String typeReturn, String name, String... arguments ) {
		// Remove empty arguments. It can be easier to add empty strings than to filter them in advance
		List<String> realArguments = new ArrayList<>();
		for (String argument: arguments) {
			if (argument.isEmpty())
				continue;
			realArguments.add(argument);
		}

		String text = "";
		for (int i = 0; i < tabs; i++) {
			text += "\t";
		}
		int startCharacters = tabs*SPACE_PER_TAB;
		text += typeReturn+" "+name+"(" + (arguments.length==0?"":" ");
		startCharacters += text.length()-tabs;

		int lineLength = startCharacters;
		for ( int argIdx = 0; argIdx < realArguments.size(); argIdx++ ) {
			String argument = realArguments.get(argIdx);
			if (lineLength + argument.length() > MAX_LINE_LENGTH ) {
				text += "\n";
				for (int i = 0; i < startCharacters/4; i++) {
					text += "\t";
				}
				for (int i = 0; i < startCharacters%4; i++) {
					text += " ";
				}
				lineLength = startCharacters;
			}
			int lengthBefore = text.length();
			text += argument;
			if (argIdx+1 != realArguments.size()) {
				text += ", ";
			}
			lineLength += text.length()-lengthBefore;
		}
		text += " ) {\n";
		return text;
	}

//	public String generatedAnnotation() {
//		return "@Generated(\"" + getClass().getName() + "\")\n";
//	}

	public String getPackage() {
		return getClass().getPackage().getName();
	}
}
