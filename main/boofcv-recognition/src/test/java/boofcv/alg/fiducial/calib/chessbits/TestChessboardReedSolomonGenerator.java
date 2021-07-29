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

package boofcv.alg.fiducial.calib.chessbits;

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.gui.image.ShowImages;
import boofcv.struct.GridShape;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
class TestChessboardReedSolomonGenerator extends BoofStandardJUnit {
	@Test void implement() {
		fail("Implement");
	}

	public static void main( String[] args ) {
		var engine = new FiducialImageEngine();
		engine.configure(10, 1200);
		var utils = new ChessBitsUtils();
		utils.markers.add( new GridShape(10, 12) );
		utils.fixate();
		var renderer = new ChessboardReedSolomonGenerator(utils);
//		renderer.multiplier = ChessboardSolomonMarkerCodec.Multiplier.LEVEL_2;
		renderer.render = engine;
		renderer.squareWidth = 100;
		renderer.render(0);

		GrayU8 image = engine.getGray();

//		GImageMiscOps.addGaussian(image, new Random(23), 4.0, 0.0, 255.0);

		ShowImages.showBlocking(image, "Foo", 200_000);
	}
}