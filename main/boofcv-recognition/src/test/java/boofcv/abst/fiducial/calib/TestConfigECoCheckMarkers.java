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

package boofcv.abst.fiducial.calib;

import boofcv.struct.StandardConfigurationChecks;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestConfigECoCheckMarkers extends StandardConfigurationChecks {
	@Test void parse() {
		ConfigECoCheckMarkers config = ConfigECoCheckMarkers.parse("10x4e6n5", 2.0);
		assertEquals(5, config.firstTargetDuplicated);
		assertEquals(6, config.errorCorrectionLevel);
		assertEquals(10, config.markerShapes.get(0).numRows);
		assertEquals(4, config.markerShapes.get(0).numCols);
		assertEquals(2.0, config.markerShapes.get(0).squareSize, UtilEjml.TEST_F64);
	}

	@Test void compactName() {
		ConfigECoCheckMarkers config = ConfigECoCheckMarkers.singleShape(4, 5, 6, 2.0);
		config.errorCorrectionLevel = 7;
		String found = config.compactName();
		assertEquals("4x5e7n6", found);
	}
}
