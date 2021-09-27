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

package boofcv;

import boofcv.io.UtilIO;
import boofcv.testing.BoofStandardJUnit;
import com.peterabeles.lang.CheckForbiddenHelper;
import com.peterabeles.lang.CheckForbiddenLanguage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Makes sure all Configuration classes are compliant
 *
 * @author Peter Abeles
 */
@SuppressWarnings("StringConcatenationInLoop")
public class TestEnforceCodeStandards {
	// Skip over these directories since they don't contain library code
	String[] blacklistConfig = new String[]{"autocode", "boofcv-core", "checks"};
	// Only run this on performance critical code
	String[] blackListLanguage = new String[]{"boofcv-io", "checks", "autocode"};

	/**
	 * This code needs to run fast and minimize memory usage. Lint check for certain illegal operations.
	 */
	@Test void performanceCodeLanguageApiCheck() throws FileNotFoundException {
		String pathToMain = UtilIO.path("main");

		var checker = new CheckForbiddenLanguage();
		CheckForbiddenHelper.forbidNonExplicitVar(checker, true);
		CheckForbiddenHelper.forbidForEach(checker);

		File[] moduleDirectories = new File(pathToMain).listFiles();
		assertNotNull(moduleDirectories);

		for (File module : moduleDirectories) {
//			System.out.println("module "+module.getPath());
			File dirCode = new File(module, "src/main");

			if (!dirCode.exists())
				continue;

			boolean inBlackList = Arrays.asList(blackListLanguage).contains(module.getName());
			if (inBlackList)
				continue;

			Collection<File> files = FileUtils.listFiles(dirCode,
					new RegexFileFilter("\\S*.java"), DirectoryFileFilter.DIRECTORY);

			boolean failed = false;
			for (File classFile : files) {
				String text = UtilIO.readAsString(new FileInputStream(classFile));
				assertNotNull(text);

				if (!checker.process(text)) {
					failed = true;
					System.err.println();
					for (var failure : checker.getFailures()) {
						System.err.println(classFile.getAbsolutePath()+":"+failure.line);
						System.err.println("  code:   " + failure.code.trim());
						System.err.println("  reason: " + failure.check.reason);
						System.err.println();
					}
				}
			}

			if (failed)
				System.err.println(
						"\nIf this is a false positive, the error can be turned off for the file or specific lines. E.g.\n" +
						"    Ignore current line: \"// lint:forbidden ignore_line\"\n" +
						"    Disable line below:  \"// lint:forbidden ignore_below\"\n" +
						"    See documentation for more options.\n");

			assertFalse(failed);
		}
	}

	/**
	 * Makes sure all unit tests extend BoofStandardJUnit
	 */
	@Test void unitTestsMustExtendBoofStandardJUnit() {
		String pathToMain = UtilIO.path("main");

		File[] moduleDirectories = new File(pathToMain).listFiles();
		assertNotNull(moduleDirectories);

		for (File module : moduleDirectories) {
//			System.out.println("module "+module.getPath());
			File dirTest = new File(module, "src/test");

			if (!dirTest.exists())
				continue;

			boolean inBlackList = Arrays.asList(blacklistConfig).contains(module.getName());
			if (inBlackList)
				continue;

			Collection<File> files = FileUtils.listFiles(dirTest,
					new RegexFileFilter("Test[A-Z]\\S*.java"),
					DirectoryFileFilter.DIRECTORY);

			boolean failed = false;
			for (File classFile : files) {
				Path f = new File(dirTest, "java").toPath().relativize(classFile.toPath());
				String classPath = f.toString().replace(File.separatorChar, '.').replace(".java", "");

				// This will be moved to a different project eventually. Update this line in the future
				if (classPath.startsWith("boofcv.concurrency"))
					continue;

				// Load the class
				Class<?> c;
				try {
					c = Class.forName(classPath);
				} catch (NoClassDefFoundError e) {
					System.err.println("loading " + classPath);
					e.printStackTrace(System.err);
					fail(e.getMessage());
					return;
				} catch (ClassNotFoundException e) {
					System.err.println("Not Found: " + classPath);
					failed = true;
					continue;
				}

				// See if it extends BoofStandardJUnit
				boolean found = false;
				while (c != null) {
					c = c.getSuperclass();
					if (c == BoofStandardJUnit.class) {
						found = true;
						break;
					}
				}
				if (!found) {
					System.err.println("Does not extend BoofStandardJUnit");
					System.err.println(classFile.getAbsolutePath()+":1"); // todo real line number
					failed = true;
				}
			}

			assertFalse(failed, "All tests must extend BoofStandardJUnit. See stderr.");
		}
	}

	@Test void configsAllHaveValidUnitTests() throws IOException {
		List<File> missing = new ArrayList<>();
		List<File> invalid = new ArrayList<>();

		String pathToMain = UtilIO.path("main");

		File[] moduleDirectories = new File(pathToMain).listFiles();
		assertNotNull(moduleDirectories);

		for (File module : moduleDirectories) {
			File dirSrc = new File(module, "src/main");

			if (!dirSrc.exists())
				continue;

			boolean inBlackList = Arrays.asList(blacklistConfig).contains(module.getName());
			if (inBlackList)
				continue;

			File dirTest = new File(module, "src/test");

			Collection<File> files = FileUtils.listFiles(dirSrc,
					new RegexFileFilter("Config[A-Z]\\S*.java"),
					DirectoryFileFilter.DIRECTORY);

			for (File classFile : files) {
				String text = UtilIO.readAsString(new FileInputStream(classFile));
				assertNotNull(text);

				// There is a weird situation that I decided to keep. A Config* was only an interface
				// but you don't want to skip over situations where they forgot to implement Configuration or
				// it extends a class and you can't see directly that it was an instance of Configuration
				if (text.contains("extends Configuration") && !text.contains("implements Configuration"))
					continue;

				Path f = dirSrc.toPath().relativize(classFile.toPath());
				File testFile = dirTest.toPath().resolve(f).toFile();
				testFile = new File(testFile.getParentFile(), "Test" + testFile.getName());
				if (!testFile.isFile()) {
					missing.add(testFile);
					continue;
				}

				text = UtilIO.readAsString(new FileInputStream(testFile));
				assertNotNull(text);

				if (!text.contains("extends StandardConfigurationChecks"))
					invalid.add(testFile);
			}
		}

		// Print out the problems to make it easier to fix
		if (!missing.isEmpty())
			System.err.println("Missing:");
		for (File f : missing) {
			System.out.println(f.getAbsolutePath());
			// commented out since a unit test really shouldn't auto generate code and commit it to git. Plus
			// the commit part won't work on all architectures
//			generateDefaultConfigTest(f);
		}
		if (!invalid.isEmpty())
			System.err.println("Invalid:");
		for (File f : invalid) {
			System.out.println(f.getAbsolutePath()+":1"); // TODO real line number
		}

		assertEquals(0, missing.size());
		assertEquals(0, invalid.size());
	}

//	private void generateDefaultConfigTest( File file ) throws IOException {
//		String[] dirs = file.getAbsolutePath().split(Pattern.quote(File.separator));
//
//		int start = dirs.length - 1;
//		while (start >= 0 && !dirs[start].equals("java")) {
//			start--;
//		}
//		if (start < 0)
//			fail("Couldn't find src");
//
//		String path = dirs[start + 1];
//		for (int i = start + 2; i < dirs.length - 1; i++) {
//			path += "." + dirs[i];
//		}
//
//		String className = FilenameUtils.getBaseName(file.getName());
//
//		if (!file.getParentFile().exists())
//			assertTrue(file.getParentFile().mkdirs());
//		assertTrue(file.createNewFile());
//		var out = new PrintStream(new FileOutputStream(file, false), true, "UTF-8");
//		out.println(CodeGeneratorBase.copyright);
//		out.println();
//		out.println("package " + path + ";");
//		out.println();
//		out.println("import boofcv.struct.StandardConfigurationChecks;");
//		out.println();
//		out.println("public class " + className + " extends StandardConfigurationChecks {}");
//		out.println();
//		out.close();
//
//		Runtime rt = Runtime.getRuntime();
//		try {
//			Process pr = rt.exec("git add " + file.getAbsolutePath());
//			if (!pr.waitFor(10000, TimeUnit.MILLISECONDS)) {
//				fail("Couldn't add new file to git");
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//	}
}
