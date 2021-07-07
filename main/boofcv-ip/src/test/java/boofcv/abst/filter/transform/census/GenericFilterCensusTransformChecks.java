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

package boofcv.abst.filter.transform.census;

import boofcv.BoofTesting;
import boofcv.abst.transform.census.FilterCensusTransform;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class GenericFilterCensusTransformChecks<In extends ImageGray<In>, Out extends ImageBase<Out>>
		extends BoofStandardJUnit {

	int width = 30;
	int height = 25;
	Random rand = BoofTesting.createRandom(0);
	ImageBorder<In> border;
	// The radius of the sample region. Must be set
	protected int radius;

	protected GenericFilterCensusTransformChecks( Class<In> imageType ) {
		border = FactoryImageBorder.generic(BorderType.REFLECT, ImageType.single(imageType));
	}

	public abstract FilterCensusTransform<In, Out> createAlg( ImageBorder<In> border );

	/**
	 * Make sure the border radius is set correctly. If the border is null it should have a non-zero value
	 */
	@Test void checkBorderRadius() {
		FilterCensusTransform<In, Out> alg = createAlg(border);
		assertEquals(radius, alg.getRadiusX());
		assertEquals(radius, alg.getRadiusY());
		assertEquals(0, alg.getBorderX());
		assertEquals(0, alg.getBorderY());

		alg = createAlg(null);
		assertEquals(radius, alg.getRadiusX());
		assertEquals(radius, alg.getRadiusY());
		assertEquals(radius, alg.getBorderX());
		assertEquals(radius, alg.getBorderY());
	}

	/**
	 * Makes sure the filter produces the same results as the function
	 */
	@Test void compareToFunction() {
		FilterCensusTransform<In, Out> alg = createAlg(border);

		In input = alg.getInputType().createImage(width, height);
		Out found = alg.getOutputType().createImage(width, height);
		Out expected = found.createSameShape();

		GImageMiscOps.fillUniform(input, rand, 0, 250);
		alg.process(input, found);
		callFunction(input, expected);

		BoofTesting.assertEquals(expected, found, 1e-8);
	}

	public abstract void callFunction( In input, Out output );
}
