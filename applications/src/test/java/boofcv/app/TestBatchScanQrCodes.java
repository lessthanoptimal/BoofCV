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

package boofcv.app;

import boofcv.io.UtilIO;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestBatchScanQrCodes extends BoofStandardJUnit {
	public void scan( String args ) {
		BatchScanQrCodes.main(args.split("\\s+"));
	}

	@Test void pathDirectory() throws IOException {
		String examplePath = UtilIO.pathExample("fiducial/qrcode");
		Path outputPath = Files.createTempDirectory("myFile");

		scan(String.format("-i %s -o %s/results.txt",examplePath,outputPath.toString()));

		// Just make sure it ran and generated a file
		Path resultsPath = outputPath.resolve("results.txt");
		assertTrue(resultsPath.toFile().exists());
	}

	@Test void pathGlob() throws IOException {
		// Create the path and handle window file paths
		String examplePath = UtilIO.pathExample("fiducial/qrcode").replace("\\","/");
		Path outputPath = Files.createTempDirectory("myFile");

		scan(String.format("-i glob:%s/*.jpg -o %s/results.txt",examplePath,outputPath.toString()));

		// Just make sure it ran and generated a file
		Path resultsPath = outputPath.resolve("results.txt");
		assertTrue(resultsPath.toFile().exists());
	}
}
