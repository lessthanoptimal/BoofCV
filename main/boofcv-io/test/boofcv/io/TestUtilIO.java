/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Jalal on 4/30/2017.
 */
public class TestUtilIO {

	private String validDemoPackage = "boofcv.demonstrations.color";
	private String validDemoClass = "ShowColorModelApp";
	private String validExamplePackage = "boofcv.examples.enhance";
	private String validExampleClass = "ExampleImageEnhancement";
	@Test
	public void testgetSourcePath() throws MalformedURLException, ClassNotFoundException {
		File f = new File(UtilIO.getSourcePath(validDemoPackage, validDemoClass));
		Assert.assertTrue(f.exists());

		File f2 = new File(UtilIO.getSourcePath(validExamplePackage, validExampleClass));
		Assert.assertTrue(f2.exists());

		String invalidPackage = "adfs";
		String invalidClass = "asdf";
		Assert.assertEquals(UtilIO.getSourcePath(invalidPackage, invalidClass), "");

		String nullPackage = null;
		String nullClass = null;
		Assert.assertEquals(UtilIO.getSourcePath(nullPackage, nullClass), "");

	}

	@Test
	public void testgetGithubLink() throws IOException {
		String url = UtilIO.getGithubURL(validDemoPackage, validDemoClass);
		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestMethod("HEAD");
		con.setConnectTimeout(2000);
		Assert.assertEquals(con.getResponseCode(), HttpURLConnection.HTTP_OK);

		String url2 = UtilIO.getGithubURL(validExamplePackage, validExampleClass);
		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection con2 = (HttpURLConnection) new URL(url2).openConnection();
		con2.setRequestMethod("HEAD");
		con2.setConnectTimeout(2000);
		Assert.assertEquals(con2.getResponseCode(), HttpURLConnection.HTTP_OK);

		String url3 = UtilIO.getGithubURL(null, null);
		Assert.assertEquals(url3, "");
	}
}
