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

package boofcv.alg.filter.derivative;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.SparseImageOperator;

import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Standard tests for implementers of {@link boofcv.struct.sparse.SparseImageGradient}.
 *
 * @author Peter Abeles
 */
public abstract class GeneralSparseOperatorTests
<T extends ImageGray>
{
	protected Random rand = new Random(12342);
	protected int width = 30;
	protected int height = 40;

	public Class<T> inputType;

	protected T input;

	// Define a relative box in which the sampled region is contained inside
	protected int sampleBoxX0;
	protected int sampleBoxX1;
	protected int sampleBoxY0;
	protected int sampleBoxY1;

	protected GeneralSparseOperatorTests(Class<T> inputType ,
										 int sampleBoxX0 , int sampleBoxY0 ,
										 int sampleBoxX1 , int sampleBoxY1 ) {
		this.inputType = inputType;
		this.sampleBoxX0 = sampleBoxX0;
		this.sampleBoxY0 = sampleBoxY0;
		this.sampleBoxX1 = sampleBoxX1;
		this.sampleBoxY1 = sampleBoxY1;

		input = GeneralizedImageOps.createSingleBand(inputType, width, height);

		GImageMiscOps.fillUniform(input, rand, 0, 100);
	}


	/**
	 * See if it recognizes that it's along the border
	 */
	public void isInBounds( SparseImageOperator<T> alg ) {
		alg.setImage(input);

		// should always be inside the image center
		assertTrue(alg.isInBounds(width/2,height/2));

		// Extreme points around the image should be outside
		assertFalse(alg.isInBounds(0, 0));
		assertFalse(alg.isInBounds(width / 2, 0));
		assertFalse(alg.isInBounds(width / 2, height - 1));
		assertFalse(alg.isInBounds(0, height / 2));
		assertFalse(alg.isInBounds(width - 1, height / 2));

		// now check points that should be just outside
		assertFalse(alg.isInBounds(-sampleBoxX0 - 1, -sampleBoxY0 - 1));
		assertFalse(alg.isInBounds(width-sampleBoxX1, height-sampleBoxY1));

		// now check points that should be just inside
		assertTrue(alg.isInBounds(-sampleBoxX0, -sampleBoxY0));
		assertTrue(alg.isInBounds(width - sampleBoxX1 - 1, height - sampleBoxY1 - 1));
	}


}
