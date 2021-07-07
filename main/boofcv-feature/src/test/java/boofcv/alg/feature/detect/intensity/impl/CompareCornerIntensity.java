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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.BoofTesting;
import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * Sees if the output from two corner intensity calculations are the same
 *
 * @author Peter Abeles
 */
public abstract class CompareCornerIntensity<D extends ImageGray<D>> extends BoofStandardJUnit {

	protected GradientCornerIntensity<D> algA, algB;

	protected D derivX,derivY;
	protected double tol = UtilEjml.TEST_F64_SQ; // not sure why it isn't exact

	protected CompareCornerIntensity(Class<D> imageType ) {
		derivX = GeneralizedImageOps.createSingleBand(imageType,50,60);
		derivY = derivX.createSameShape();


		Random rand = new Random(234);
		GImageMiscOps.fillUniform(derivX,rand,0,100);
		GImageMiscOps.fillUniform(derivY,rand,0,100);
	}

	public void initialize(GradientCornerIntensity<D> algA , GradientCornerIntensity<D> algB ) {
		this.algA = algA;
		this.algB = algB;
	}

	@Test void compare() {
		GrayF32 outputA = new GrayF32(derivX.width,derivX.height);
		GrayF32 outputB = new GrayF32(derivX.width,derivX.height);

		algA.process(derivX,derivY, outputA);
		algB.process(derivX,derivY, outputB);

		BoofTesting.assertEqualsRelative(outputA,outputB,tol);
	}
}
