/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.gui;

import boofcv.io.image.UtilImageIO;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestBoofSwingUtil {

	@Test
	void isImage() throws IOException {
		// this file should be around no matter what the module root is
		File f = new File("build.gradle");
		assertTrue(f.exists());
		assertFalse(BoofSwingUtil.isImage(f));

		// create an image to test the positive case
		f = File.createTempFile("booftest",".jpg");

		BufferedImage image = new BufferedImage(100,200,BufferedImage.TYPE_INT_RGB);
		UtilImageIO.saveImage(image,f.getAbsolutePath());
		assertTrue(BoofSwingUtil.isImage(f));

		// clean up
		assertTrue(f.delete());
	}
}
