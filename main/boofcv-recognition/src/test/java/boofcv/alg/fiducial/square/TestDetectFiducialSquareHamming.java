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

package boofcv.alg.fiducial.square;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.factory.fiducial.ConfigHammingMarker;
import boofcv.factory.fiducial.HammingDictionary;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestDetectFiducialSquareHamming extends BoofStandardJUnit {

	static double blackBorderFraction = 0.65;
	ConfigHammingMarker config = ConfigHammingMarker.loadDictionary(HammingDictionary.ARUCO_MIP_36h12);

	private DetectPolygonBinaryGrayRefine<GrayU8> squareDetector = FactoryShapeDetector.polygon(
			new ConfigPolygonDetector(false, 4, 4), GrayU8.class);
	private InputToBinary<GrayU8> inputToBinary = FactoryThresholdBinary.globalFixed(50, true, GrayU8.class);

	/**
	 * Give it easy positive examples
	 */
	@Test void processSquare() {
		var result = new BaseDetectFiducialSquare.Result();
		var alg = new DetectFiducialSquareHamming<>(config, blackBorderFraction, inputToBinary, squareDetector, GrayU8.class);
		int markerID = 4;

		for (int i = 0; i < 4; i++) {
			GrayF32 input = create(markerID);

			for (int j = 1; j <= i; j++) {
				ImageMiscOps.rotateCCW(input.clone(), input);
			}

			assertTrue(alg.processSquare(input, result, 0, 255));

//			ShowImages.showWindow(input, "Input");
//			ShowImages.showWindow(alg.grayNoBorder, "NoBorder");
//			ShowImages.showWindow(alg.binaryInner, "Binary");
//			BoofMiscOps.sleep(10_000);

			assertEquals(markerID, result.which);
			assertEquals(i, result.rotation);
		}
	}

	private GrayF32 create( int markerID ) {
		int w = DetectFiducialSquareHamming.w;
		int side = w*(config.gridWidth + 2);
		var engine = new FiducialImageEngine();
		engine.configure(0, side, side);
		var generator = new FiducialSquareHammingGenerator(config);
		generator.setRenderer(engine);
		generator.setMarkerWidth(side);
		generator.generate(markerID);

		return engine.getGrayF32();
	}
}
