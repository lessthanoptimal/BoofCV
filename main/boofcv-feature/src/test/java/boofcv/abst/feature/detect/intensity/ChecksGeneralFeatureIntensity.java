/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.intensity;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for implementators of {@link GeneralFeatureIntensity}.
 *
 * @author Peter Abeles
 */
public abstract class ChecksGeneralFeatureIntensity<I extends ImageGray<I>, D extends ImageGray<D>>
		extends BoofStandardJUnit {
	public List<Class> listInputTypes = new ArrayList<>();
	public List<Class> listDerivTypes = new ArrayList<>();

	int width = 30;
	int height = 40;

	// Range of values in uniform distribution
	int imageMin = 0;
	int imageMax = 255;
	int derivMin = -100;
	int derivMax = 100;

	I input;
	D derivX, derivY, derivXX, derivYY, derivXY;

	public void addTypes( Class inputType, Class derivType ) {
		listInputTypes.add(inputType);
		listDerivTypes.add(derivType);
	}

	public abstract GeneralFeatureIntensity<I, D> createAlg( Class<I> imageType, Class<D> derivType );

	/// See if it correctly computes the response to the gradient being scaled
	@Test void gradientScale() {
		for (int i = 0; i < listInputTypes.size(); i++) {
			gradientScale(listInputTypes.get(i), listDerivTypes.get(i));
		}
	}

	public void gradientScale( Class<I> imageType, Class<D> derivType ) {
		// change RNG to avoid overflow when scaled in 8-bit images
		imageMax = 50;
		derivMin = -25;
		derivMax = 25;

		randomInit(imageType, derivType, width, height);

		// Compute intensity before the derivatives are scaled
		GeneralFeatureIntensity<I, D> alg = createAlg(imageType, derivType);
		alg.process(input, derivX, derivY, derivXX, derivYY, derivXY);

		GrayF32 before = new GrayF32().setTo(alg.getIntensity());

		// Scale the gradients
		int divisor = 2;
		GPixelMath.multiply(derivX, divisor, derivX);
		GPixelMath.multiply(derivY, divisor, derivY);
		GPixelMath.multiply(derivXX, divisor, derivXX);
		GPixelMath.multiply(derivYY, divisor, derivYY);
		GPixelMath.multiply(derivXY, divisor, derivXY);

		alg.process(input, derivX, derivY, derivXX, derivYY, derivXY);

		// floating point images will never get their gradients scaled, so skip
		if (!ImageType.single(derivType).getDataType().isInteger()) {
			assertEquals(1.0, alg.thresholdScaleByDerivative(divisor));
			return;
		}

		// If adjustment is 1 for an integer image that means it does not depend on the derivatives
		float adjustment = alg.thresholdScaleByDerivative(divisor);
		GrayF32 after = alg.getIntensity();

		for (int y = 0; y < before.height; y++) {
			for (int x = 0; x < before.width; x++) {
				assertEquals(before.get(x,y)*adjustment, after.get(x,y), 0.1);
			}
		}
	}

	/**
	 * For features which do not process the image border, the border should have a response of zero.
	 * A bug was found where if the input image size was changed the border would have "residual"
	 * values from past runs and not be zero.
	 */
	@SuppressWarnings("unchecked")
	@Test void checkReshapeBorder() {
		for (int i = 0; i < listInputTypes.size(); i++) {
			checkReshapeBorder(listInputTypes.get(i), listDerivTypes.get(i));
		}
	}

	public void checkReshapeBorder( Class<I> imageType, Class<D> derivType ) {
		randomInit(imageType, derivType, width, height);

		GeneralFeatureIntensity<I, D> alg = createAlg(imageType, derivType);
		alg.process(input, derivX, derivY, derivXX, derivYY, derivXY);

		GrayF32 intensity = alg.getIntensity();

		int r = alg.getIgnoreBorder();
		checkBorderZero(intensity, r);

		// process again with smaller images
		randomInit(imageType, derivType, width - 1, height - 1);
		alg.process(input, derivX, derivY, derivXX, derivYY, derivXY);
		intensity = alg.getIntensity();
		checkBorderZero(intensity, r);
	}

	private void randomInit( Class<I> imageType, Class<D> derivType, int width, int height ) {
		input = GeneralizedImageOps.createSingleBand(imageType, width, height);
		derivX = GeneralizedImageOps.createSingleBand(derivType, width, height);
		derivY = GeneralizedImageOps.createSingleBand(derivType, width, height);
		derivXX = GeneralizedImageOps.createSingleBand(derivType, width, height);
		derivYY = GeneralizedImageOps.createSingleBand(derivType, width, height);
		derivXY = GeneralizedImageOps.createSingleBand(derivType, width, height);

		GImageMiscOps.fillUniform(input, rand, imageMin, imageMax);
		GImageMiscOps.fillUniform(derivX, rand, derivMin, derivMax);
		GImageMiscOps.fillUniform(derivY, rand, derivMin, derivMax);
		GImageMiscOps.fillUniform(derivXX, rand, derivMin, derivMax);
		GImageMiscOps.fillUniform(derivYY, rand, derivMin, derivMax);
		GImageMiscOps.fillUniform(derivXY, rand, derivMin, derivMax);
	}

	private void checkBorderZero( GrayF32 intensity, int r ) {
		for (int y = 0; y < intensity.height; y++) {
			if (y >= r && y < intensity.height - r)
				continue;

			for (int x = 0; x < intensity.width; x++) {
				if (x >= r && x < intensity.width - r)
					continue;

				assertTrue(0 == intensity.get(x, y));
			}
		}
	}
}
