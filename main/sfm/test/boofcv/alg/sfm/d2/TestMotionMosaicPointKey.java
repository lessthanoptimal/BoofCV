/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.d2;

import boofcv.struct.geo.AssociatedPair;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestMotionMosaicPointKey {

	@Test
	public void imageCoverageFraction() {
		List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
		pairs.add( new AssociatedPair(10,15,20,25));
		pairs.add( new AssociatedPair(10,15,100,30));
		pairs.add( new AssociatedPair(10,15,29,120));
		// give it a useless pair which will not contribute to the solution
		pairs.add( new AssociatedPair(10,15,50,56));

		double area = 80*95;

		int width = 200;
		int height = 250;

		double expected = area/(width*height);
		double found = MotionMosaicPointKey.imageCoverageFraction(width,height,pairs);

		assertEquals(expected, found, 1e-8);
	}
}
