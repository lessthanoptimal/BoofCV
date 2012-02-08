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


import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.testing.BoofTesting;

import static org.junit.Assert.assertEquals;

/**
 * Standard tests for  non-maximum suppression algorithms
 *
 * @author Peter Abeles
 */
// todo add optional tests for features inside image border
public abstract class GenericNonMaxTests {

	int width = 30;
	int height = 40;

	private QueueCorner found = new QueueCorner(100);
	private ImageFloat32 intensity = new ImageFloat32(width,height);

	public abstract void findLocalMaximums(ImageFloat32 intensity,
										   float threshold, int radius, boolean useStrict,
										   QueueCorner found);


	public void reset() {
		found.reset();
		ImageTestingOps.fill(intensity,0);
	}

	public void allStandard( boolean useStrictRule ) {
		if( useStrictRule )
			testStrictRule();
		testRadius(useStrictRule);
		testThreshold(useStrictRule);
		exclude_MAX_VALUE(useStrictRule);
		testSubimage(useStrictRule);
	}

	public void testSubimage( boolean useStrict ) {
		reset();

		intensity.set(5, 5, 30);
		intensity.set(5, 8, 35);
		intensity.set(5, 12, 31);

		// see how many features it finds at various sizes
		findLocalMaximums(intensity, 5, 2, useStrict,found);
		assertEquals(3,found.size);

		found.reset();
		ImageFloat32 sub = BoofTesting.createSubImageOf(intensity);
		findLocalMaximums(sub, 5, 2, useStrict,found);
		assertEquals(3,found.size);
	}
	public void testThreshold( boolean useStrict ) {
		reset();

		intensity.set(5, 5, 30);
		intensity.set(5, 8, 35);
		intensity.set(5, 12, 31);

		// see how many features it finds at various sizes
		findLocalMaximums(intensity, 5, 2, useStrict,found);
		assertEquals(3,found.size);

		found.reset();
		findLocalMaximums(intensity, 35, 2, useStrict,found);
		assertEquals(1,found.size);

	}

	public void testRadius( boolean useStrict ) {
		reset();

		intensity.set(5, 5, 30);
		intensity.set(5, 8, 35);
		intensity.set(5, 12, 31);

		// see how many features it finds at various sizes
		findLocalMaximums(intensity, 5, 2,useStrict,found);
		assertEquals(3,found.size);

		found.reset();
		findLocalMaximums(intensity, 5, 3,useStrict,found);
		assertEquals(2,found.size);

		found.reset();
		findLocalMaximums(intensity, 5, 4,useStrict,found);
		assertEquals(1,found.size);
	}

	public void testStrictRule() {
		reset();

		intensity.set(3, 5, 30);
		intensity.set(5, 7, 30);
		intensity.set(7, 7, 30);

		// none of these points are a strict maximum
		findLocalMaximums(intensity, 5, 2,true,found);
		assertEquals(0,found.size);
	}

	public void testRelaxedRule() {
		reset();

		intensity.set(3, 5, 30);
		intensity.set(5, 7, 30);
		intensity.set(7, 7, 30);

		// three points should be returned when the strict rule is not used
		found.reset();
		findLocalMaximums(intensity, 5, 2,false,found);
		assertEquals(3,found.size);
	}

	public void exclude_MAX_VALUE( boolean useStrict ) {
		reset();

		intensity.set(15, 20, Float.MAX_VALUE);
		intensity.set(20,21,Float.MAX_VALUE);
		intensity.set(10,25,Float.MAX_VALUE);
		intensity.set(11,24,10);
		intensity.set(25,35,10);

		findLocalMaximums(intensity, 5, 2, useStrict, found);

		// only one feature should be found.  The rest should be MAX_VALUE or too close to MAX_VALUE
		assertEquals(1, found.size);
		assertEquals(25, found.data[0].x);
		assertEquals(35, found.data[0].y);
	}



}
