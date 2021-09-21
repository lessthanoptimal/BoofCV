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

public class TestCameraCalibrationMono extends BoofStandardJUnit {
	public void scan( String args ) {
		CameraCalibrationMono.main(args.split("\\s+"));
	}

	@Test void justDetect() throws IOException {
		String examplePath = UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess");
		Path outputPath = Files.createTempDirectory("calibration");

		scan(String.format("--Output=%s/intrinsics.yaml --JustDetect --Input=%s/frame12.jpg CHESSBOARD --Grid=7:5",outputPath.toString(),examplePath));

		// Just make sure it ran and generated files
		assertTrue(outputPath.resolve("intrinsics_landmarks/frame12.csv").toFile().exists());
		assertTrue(outputPath.resolve("intrinsics_landmarks/summary_detection.csv").toFile().exists());
	}
}
