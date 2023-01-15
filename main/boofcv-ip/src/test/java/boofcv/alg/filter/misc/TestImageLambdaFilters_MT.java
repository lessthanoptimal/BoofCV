/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.misc;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

@SuppressWarnings("rawtypes")
class TestImageLambdaFilters_MT extends CompareIdenticalFunctions {

	protected TestImageLambdaFilters_MT() {
		super(ImageLambdaFilters_MT.class, ImageLambdaFilters.class);
	}

	@Test
	void performTests() {
		performTests(6);
	}

	@Override
	protected boolean isTestMethod( Method m ) {
		return m.getName().equals("filterRectCenterInner");
	}

	// Sub-images are problematic because we don't have access to the image inside the lambda.
	// This will effectively disable the  check.
	@Override
	protected Object[] createSubImageInputs( Object[] param ) {
		return param;
	}

	@Override protected Object[][] createInputParam( Method candidate, Method validation ) {
		Class[] paramTypes = candidate.getParameterTypes();

		var src = GeneralizedImageOps.createSingleBand(paramTypes[0], 300, 200);
		var dst = src.createSameShape();

		// Noise up the input image
		GImageMiscOps.fillUniform(src, rand, 0, 200);

		// Create the appropriate lambda
		Object lambda;
		int bits = src.getImageType().getDataType().getNumBits();
		if (src.getImageType().getDataType().isInteger()) {
			if (bits <= 32) {
				lambda = (ImageLambdaFilters.RectCenter_S32)( indexPixel, workspace ) -> {
					int x = indexPixel%src.stride;
					int y = indexPixel/src.stride;
					return (int)GeneralizedImageOps.get((ImageGray)src, x, y) + 5;
				};
			} else {
				lambda = (ImageLambdaFilters.RectCenter_S64)( indexPixel, workspace ) -> {
					int x = indexPixel%src.stride;
					int y = indexPixel/src.stride;
					return (long)GeneralizedImageOps.get((ImageGray)src, x, y) + 5;
				};
			}
		} else if (bits == 32) {
			lambda = (ImageLambdaFilters.RectCenter_F32)( indexPixel, workspace ) -> {
				int x = indexPixel%src.width;
				int y = indexPixel/src.width;
				return (float)GeneralizedImageOps.get((ImageGray)src, x, y) + 5;
			};
		} else {
			lambda = (ImageLambdaFilters.RectCenter_F64)( indexPixel, workspace ) -> {
				int x = indexPixel%src.width;
				int y = indexPixel/src.width;
				return (double)GeneralizedImageOps.get((ImageGray)src, x, y) + 5;
			};
		}

		return new Object[][]{{src, 4, 6, dst, null, lambda}};
	}
}

