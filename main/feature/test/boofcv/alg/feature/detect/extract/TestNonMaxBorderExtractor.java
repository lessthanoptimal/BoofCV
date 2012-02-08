/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestNonMaxBorderExtractor extends GenericNonMaxBorderTests{


	@Override
	public void findLocalMaximums(ImageFloat32 intensity,
								  float threshold, int radius,
								  boolean useStrict, QueueCorner found) {
		NonMaxBorderExtractor alg = new NonMaxBorderExtractor(radius,threshold,useStrict);
		alg.process(intensity, found);
	}

	@Test
	public void standardTests() {
		super.allStandard(true);
		super.allStandard(false);
	}


	@Test
	public void checkInputBorder() {
		checkInputBorder(true);
		checkInputBorder(false);
	}
	/**
	 * Adjust the input border and see if it has the expected behavior
	 */
	public void checkInputBorder( boolean useStrictRule ) {
		reset();

		intensity.set(1,1,20);

		// test positive case with no input border
		NonMaxBorderExtractor alg = new NonMaxBorderExtractor(3,10,useStrictRule);
		alg.process(intensity, found);
		assertEquals(1, found.size);

		// test negative case with the input border
		found.reset();
		alg.setInputBorder(3);
		alg.process(intensity, found);
		assertEquals(0,found.size);
	}

}
