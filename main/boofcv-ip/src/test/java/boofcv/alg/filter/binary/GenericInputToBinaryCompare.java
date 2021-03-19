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

package boofcv.alg.filter.binary;

import boofcv.BoofTesting;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

/**
 * Compares two implementations of the same algorithm to see if they produce identical results
 *
 * @author Peter Abeles
 */
public abstract class GenericInputToBinaryCompare<T extends ImageGray<T>> extends BoofStandardJUnit {

	protected int width = 100, height = 110;

	// algorithm being tested
	InputToBinary<T> test;
	// algorithm which it is being tested against
	InputToBinary<T> validation;

	public void initialize( InputToBinary<T> test, InputToBinary<T> validation ) {
		this.test = test;
		this.validation = validation;
	}

	@Test void compare() {
		T input = (T)GeneralizedImageOps.createSingleBand(test.getInputType().getImageClass(), width, height);

		GImageMiscOps.fillUniform(input, rand, 0, 100);

		GrayU8 expected = new GrayU8(width, height);
		GrayU8 found = new GrayU8(width, height);

		validation.process(input, expected);
		test.process(input, found);

		BoofTesting.assertEquals(expected, found, 0);
	}
}
