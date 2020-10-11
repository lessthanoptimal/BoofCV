/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.AutocodeMasterApp;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * <p>Base class for code generators.</p>
 *
 * @author Peter Abeles
 */
public abstract class CodeGeneratorBase {
	public static String copyright = "/*\n" +
			" * Copyright (c) 2020, Peter Abeles. All Rights Reserved.\n" +
			" *\n" +
			" * This file is part of BoofCV (http://boofcv.org).\n" +
			" *\n" +
			" * Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
			" * you may not use this file except in compliance with the License.\n" +
			" * You may obtain a copy of the License at\n" +
			" *\n" +
			" *   http://www.apache.org/licenses/LICENSE-2.0\n" +
			" *\n" +
			" * Unless required by applicable law or agreed to in writing, software\n" +
			" * distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
			" * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
			" * See the License for the specific language governing permissions and\n" +
			" * limitations under the License.\n" +
			" */\n";

	protected PrintStream out;
	protected @Nullable String className;
	protected @Setter @Nullable String moduleName;
	/**
	 * If true the output will be in the source directory, overwriting existing code
	 */
	protected @Getter @Setter boolean overwrite = true;

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
		out.printf("\t\tfor( int %s = %s; %s < %s; %s++ ) {\n", var, lower, var, upper, var);
		out.print(body);
		out.print("\t\t}\n");
		out.print("\t\t//CONCURRENT_ABOVE });\n");
	}

	protected void printParallelBlock( String var0, String var1, String lower, String upper, String minBlock, String body ) {
		out.println();

		out.printf("\t\t//CONCURRENT_BELOW BoofConcurrency.loopBlocks(%s, %s, %s,(%s,%s)->{\n", lower, upper, minBlock, var0, var1);
		out.printf("\t\tfinal int %s = %s, %s = %s;\n", var0, lower, var1, upper);
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

			File path_to_root = AutocodeMasterApp.findPathToProjectRoot();
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
			throw new IllegalArgumentException("ClassName already set.  Out of date code?");
		this.className = className;
		initFile();
	}

	public String generateDocString( String... authors ) {
		String ret = " *\n" +
				" * <p>DO NOT MODIFY.  Automatically generated code created by " + getClass().getSimpleName() + "</p>\n" +
				" *\n";

		for (String author : authors) {
			ret += " * @author " + author + "\n";
		}
		ret += " */\n" +
				"@Generated(\"" + getClass().getCanonicalName() + "\")\n";

		return ret;
	}

//	public String generatedAnnotation() {
//		return "@Generated(\"" + getClass().getName() + "\")\n";
//	}

	public String getPackage() {
		return getClass().getPackage().getName();
	}
}
