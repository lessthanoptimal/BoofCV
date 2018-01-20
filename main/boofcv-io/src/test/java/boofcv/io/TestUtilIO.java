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

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 * @author Jalal
 */
public class TestUtilIO {

	private String validDemoPackage = "boofcv.demonstrations.color";
	private String validDemoClass = "ShowColorModelApp";
	private String validExamplePackage = "boofcv.examples.enhance";
	private String validExampleClass = "ExampleImageEnhancement";


	@Test
	public void readAsString() throws IOException {
		String expected = "This is\na string\n";
		File tmp = File.createTempFile("readAsString",null);

		FileWriter writer = new FileWriter(tmp);
		writer.write(expected);
		writer.close();

		String found = UtilIO.readAsString(tmp.getAbsolutePath());
		assertTrue(tmp.delete());

		assertEquals(expected,found);
	}

	@Test
	public void getSourcePath() throws MalformedURLException, ClassNotFoundException {
		File f1 = new File(UtilIO.getSourcePath(validDemoPackage, validDemoClass));
		assertTrue(f1.exists());
		assertTrue(f1.isAbsolute());

		File f2 = new File(UtilIO.getSourcePath(validExamplePackage, validExampleClass));
		assertTrue(f2.exists());
		assertTrue(f2.isAbsolute());

		String invalidPackage = "adfs";
		String invalidClass = "asdf";
		assertEquals(UtilIO.getSourcePath(invalidPackage, invalidClass), "");

		String nullPackage = null;
		String nullClass = null;
		assertEquals(UtilIO.getSourcePath(nullPackage, nullClass), "");
	}

	@Test
	public void getGithubLink() throws IOException {
		String url = UtilIO.getGithubURL(validDemoPackage, validDemoClass);
		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestMethod("HEAD");
		con.setConnectTimeout(2000);
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

	@Test
	public void indexOfSourceStart() {
		String example0 = "// asdfasdf\n\nimport foo;\npublic class stuff{\n/** Yo Dog*/public void class()}";

		assertEquals(25,UtilIO.indexOfSourceStart(example0));

		String example1 = "public class stuff{\n/** Yo Dog*/public void class()}";

		assertEquals(0,UtilIO.indexOfSourceStart(example1));

		String example2 = "/* copyright*/\nimport stuff;\n/** comments */public class stuff{\n/** Yo Dog*/public void class()}";

		assertEquals(29,UtilIO.indexOfSourceStart(example2));
	}

	@Test
	public void findMatches() {
		String f = TestUtilIO.class.getResource(".").getFile();
		String w = "resources";
		File[] matches = UtilIO.findMatches(new File(f),"\\w*.class");
		if( matches.length < 3 ) {
			// depending on how the IDE is configured it might not point it where one might think it should be
			if( f.contains(w) ) {
				int l = f.lastIndexOf(w);
				f = f.substring(0,l)+"classes"+f.substring(l+w.length(),f.length());
			}
			matches = UtilIO.findMatches(new File(f),"\\w*.class");
		}
		assertTrue(matches.length>=3);
	}
}
