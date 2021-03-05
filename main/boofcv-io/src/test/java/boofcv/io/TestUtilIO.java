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
import boofcv.testing.BoofStandardJUnit;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 * @author Jalal
 */
class TestUtilIO extends BoofStandardJUnit {

	private final String validDemoPackage = "boofcv.demonstrations.color";
	private final String validDemoClass = "ShowColorModelApp";
	private final String validExamplePackage = "boofcv.examples.enhance";
	private final String validExampleClass = "ExampleImageEnhancement";

	/**
	 * See if it can get the URL for a resource correctly
	 */
	@Test void pathExampleURL_resource() {
		URL found = UtilIO.pathExampleURL("boofcv/io/image/wrapper/images/dummy01.png");

		assertNotNull(found);
		assertTrue(found.toString().endsWith("dummy01.png"));
	}

	/**
	 * See if it handles the URL after it has been messed up by being passed through File
	 */
	@Test void ensureURL_mangled() {
		String input = "jar:file:/home/person/BoofApplications/demonstrations.jar!/fiducial/image/video/patterns/chicken.png";
		URL url = UtilIO.ensureURL(new File(input).getPath());

		assertNotNull(url);
		assertEquals(input,url.toString());
	}

	@Test void simplifyJarPath() throws MalformedURLException {
		String input = "jar:file:/home/person/BoofApplications/demonstrations.jar!/fiducial/image/video/../patterns/chicken.png";
		String expected = "jar:file:/home/person/BoofApplications/demonstrations.jar!/fiducial/image/patterns/chicken.png";

		URL a = new URL(input);
		URL b = UtilIO.simplifyJarPath(a);
		assertEquals(expected,b.toString());
	}

	@Test void readAsString() throws IOException {
		String expected = "This is\na string\n";
		File tmp = File.createTempFile("readAsString",null);

		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tmp),UTF_8);
		writer.write(expected);
		writer.close();

		String found = UtilIO.readAsString(tmp.getAbsolutePath());
		assertTrue(tmp.delete());

		assertEquals(expected,found);
	}

	@Test void getSourcePath() {
		File f1 = new File(UtilIO.getSourcePath(validDemoPackage, validDemoClass));
		assertTrue(f1.exists(),f1.getPath());
		assertTrue(f1.isAbsolute());

		File f2 = new File(UtilIO.getSourcePath(validExamplePackage, validExampleClass));
		assertTrue(f2.exists());
		assertTrue(f2.isAbsolute());

		String invalidPackage = "adfs";
		String invalidClass = "asdf";
		assertEquals("" ,UtilIO.getSourcePath(invalidPackage, invalidClass));

		String nullPackage = null;
		String nullClass = null;
		assertEquals("",UtilIO.getSourcePath(nullPackage, nullClass));

		// Avoid a false positive on the stderr check
		err.used = false;
	}

	@SuppressWarnings("ConstantConditions")
	@Test void getGithubLink() throws IOException {
		// Don't run If this is a snapshot since it will allways fail
		if(BoofVersion.VERSION.contains("SNAPSHOT"))
			return;

		String url = UtilIO.getGithubURL(validDemoPackage, validDemoClass);
		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestMethod("HEAD");
		con.setConnectTimeout(2000);
//		System.out.println(con.getResponseMessage());
		assertEquals(HttpURLConnection.HTTP_OK,con.getResponseCode());

		String url2 = UtilIO.getGithubURL(validExamplePackage, validExampleClass);
		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection con2 = (HttpURLConnection) new URL(url2).openConnection();
		con2.setRequestMethod("HEAD");
		con2.setConnectTimeout(2000);
		assertEquals(HttpURLConnection.HTTP_OK,con2.getResponseCode());

		String url3 = UtilIO.getGithubURL(null, null);
		assertEquals(url3, "");
	}

	@Test void indexOfSourceStart() {
		String example0 = "// asdfasdf\n\nimport foo;\npublic class stuff{\n/** Yo Dog*/public void class()}";

		assertEquals(25,UtilIO.indexOfSourceStart(example0));

		String example1 = "public class stuff{\n/** Yo Dog*/public void class()}";

		assertEquals(0,UtilIO.indexOfSourceStart(example1));

		String example2 = "/* copyright*/\nimport stuff;\n/** comments */public class stuff{\n/** Yo Dog*/public void class()}";

		assertEquals(29,UtilIO.indexOfSourceStart(example2));
	}

	@Test void findMatches() {
		String f = TestUtilIO.class.getResource(".").getFile();
		String w = "resources";
		File[] matches = UtilIO.findMatches(new File(f),"\\w*.class");
		if( matches.length < 3 ) {
			// depending on how the IDE is configured it might not point it where one might think it should be
			if( f.contains(w) ) {
				int l = f.lastIndexOf(w);
				f = f.substring(0,l)+"classes"+f.substring(l+w.length());
			}
			matches = UtilIO.findMatches(new File(f),"\\w*.class");
		}
		assertTrue(matches.length>=3);
	}

	@Test void delete() throws IOException {
		File tmp = Files.createTempDirectory("delete").toFile();
		assertTrue(new File(tmp,"boo.txt").createNewFile());
		assertTrue(new File(tmp,"moo.txt").createNewFile());
		assertTrue(new File(tmp,"moo_2.txt").createNewFile());
		assertEquals(3,tmp.list().length);

		UtilIO.delete(tmp,f->f.getName().startsWith("moo"));
		assertEquals(1,tmp.list().length);
		FileUtils.deleteDirectory(tmp);
	}
	
	@Test void findBaseDirectoryInPattern() throws IOException {
		// Create a file path with some pathological issues
		File base = Files.createTempDirectory("delete").toFile();
		String bp = base.getPath(); // short variable bame since this will be used a lot
		String gbp = "glob:"+bp;
		assertTrue(new File(base,"foo/aaaa/bbb").mkdirs());
		assertTrue(new File(base,"foo/aa/bbb").mkdirs());

		assertEquals(bp+"/foo/aaaa/",UtilIO.findBaseDirectoryInPattern(gbp+"/foo/aaaa/**"));
		assertEquals(bp+"/foo/",UtilIO.findBaseDirectoryInPattern(gbp+"/foo/aaaa**/*"));
		assertEquals(bp+"/foo/",UtilIO.findBaseDirectoryInPattern(gbp+"/foo/aa**/*"));
		assertEquals(bp+"/foo/aa",UtilIO.findBaseDirectoryInPattern(gbp+"/foo/aa"));

		// clean up
		FileUtils.deleteDirectory(base);
	}
}
