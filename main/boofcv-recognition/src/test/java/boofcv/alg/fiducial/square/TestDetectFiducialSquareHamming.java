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

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
class TestDetectFiducialSquareHamming extends BoofStandardJUnit {
	@Test void implement() {
		fail("implement");
//		int squareWidth = 400;
//
//		var config = ConfigHammingDictionary.defineAruco_Original();
//		var engine = new FiducialImageEngine();
//		engine.configure(10, squareWidth, squareWidth);
//		var renderer = new FiducialSquareHammingGenerator(config);
//		renderer.render = engine;
//		renderer.squareWidth = squareWidth;
//		renderer.render(123);
//
//		ShowImages.showWindow(engine.getGray(), "ASDASD");
//		BoofMiscOps.sleep(10_000);
	}
}