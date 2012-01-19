/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.sparse.SparseImageOperator;

import java.util.Random;

import static org.junit.Assert.assertTrue;


/**
 * Standard tests for implementers of {@link boofcv.struct.sparse.SparseImageGradient}.
 *
 * @author Peter Abeles
 */
public abstract class GeneralSparseOperatorTests
<T extends ImageSingleBand>
{
	protected Random rand = new Random(12342);
	protected int width = 30;
	protected int height = 40;

	public Class<T> inputType;

	protected T input;


	protected GeneralSparseOperatorTests(Class<T> inputType) {
		this.inputType = inputType;

		input = GeneralizedImageOps.createSingleBand(inputType, width, height);

		GeneralizedImageOps.randomize(input,rand,0,100);
	}


	/**
	 * See if it recognizes that it's along the border
	 */
	public void isInBounds( SparseImageOperator<T> alg ) {
		alg.setImage(input);

		assertTrue(alg.isInBounds(width/2,height/2));

		assertTrue(!alg.isInBounds(0,0));
		assertTrue(!alg.isInBounds(width/2,0));
		assertTrue(!alg.isInBounds(width/2,height-1));
		assertTrue(!alg.isInBounds(0,height/2));
		assertTrue(!alg.isInBounds(width-1,height/2));
	}


}
