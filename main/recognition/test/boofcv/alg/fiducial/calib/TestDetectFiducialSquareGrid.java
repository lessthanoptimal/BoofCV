/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib;

import boofcv.alg.fiducial.square.BaseDetectFiducialSquare;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.struct.image.GrayF32;
import georegression.struct.shapes.Quadrilateral_F64;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDetectFiducialSquareGrid {



	/**
	 * Generate a fiducial and detect its corners.  Fully visible.
	 */
	@Test
	public void simple() {

		ConfigFiducialBinary configBinary = new ConfigFiducialBinary(1);
		configBinary.gridWidth = 3;

		BaseDetectFiducialSquare<GrayF32> detector =
				FactoryFiducial.squareBinary(configBinary, ConfigThreshold.fixed(125),GrayF32.class).getAlgorithm();
		DetectFiducialSquareGrid<GrayF32> alg = new DetectFiducialSquareGrid<>(3,2,
				new long[]{0,1,2,3,4,5},detector);

		RenderSquareBinaryGridFiducial render = new RenderSquareBinaryGridFiducial();
		GrayF32 image = render.generate(3, 2);

		assertTrue(alg.detect(image));

		List<DetectFiducialSquareGrid.Detection> detections = alg.detections.toList();

		int foundIds[] = new int[6];
		for (int i = 0; i < detections.size(); i++) {
			DetectFiducialSquareGrid.Detection d = detections.get(i);

			foundIds[d.gridIndex]++;

			// see if the corners are in the right location.  Order matters
			Quadrilateral_F64 expected = render.expectedCorners.get(d.gridIndex);
			Quadrilateral_F64 found = d.location;

			for (int j = 0; j < 4; j++) {
				assertTrue(expected.get(j).distance(found.get(j)) < 0.1 );
			}
		}

		// see if all the fiducials were found
		for (int i = 0; i < foundIds.length; i++) {
			assertEquals(1,foundIds[i]);
		}
	}
}
