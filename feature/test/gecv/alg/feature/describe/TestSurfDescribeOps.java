/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.feature.describe;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * @author Peter Abeles
 */
public class TestSurfDescribeOps {

	@Test
	public void gradient() {
		fail("compare to naive");
	}

	@Test
	public void features() {
		fail("compare to naive");
	}

	@Test
	public void gradient_noborder_F32() {
		fail("compare to naive");
	}

	@Test
	public void gradient_noborder_I32() {
		fail("compare to naive");
	}

	@Test
	public void isInside_aligned() {
		fail("implement");
	}

	@Test
	public void isInside_rotated() {
		fail("implement");
	}


	/**
	 * Compare against some hand computed examples
	 */
	@Test
	public void normalizeFeatures_known() {
		double features[] = new double[64];
		features[5] = 2;
		features[10] = 4;
		SurfDescribeOps.normalizeFeatures(features);
		assertEquals(0.44721,features[5],1e-3);
		assertEquals(0.89443,features[10],1e-3);
	}

	/**
	 * The descriptor is all zeros.  See if it handles this special case.
	 */
	@Test
	public void normalizeFeatures_zeros() {
		double features[] = new double[64];
		SurfDescribeOps.normalizeFeatures(features);
		for( int i = 0; i < features.length; i++ )
			assertEquals(0,features[i],1e-4);
	}
}
