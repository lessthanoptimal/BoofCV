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

package boofcv.alg.flow;

import boofcv.struct.image.GrayF32;
import boofcv.struct.pyramid.ImagePyramid;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestUtilDenseOpticalFlow {

	@Test
	public void standardPyramid_sigma_05() {
		ImagePyramid pyr = UtilDenseOpticalFlow.standardPyramid(100,200,0.5,1.5,25,100, GrayF32.class);

		assertEquals(3,pyr.getNumLayers());

		assertEquals(1,pyr.getScale(0),1e-8);
		assertEquals(2,pyr.getScale(1),1e-8);
		assertEquals(4,pyr.getScale(2),1e-8);

		double sigma = 1.5*Math.sqrt(Math.pow(0.5,-2)-1);

		assertEquals(sigma,pyr.getSigma(0),1e-8);
		for( int i = 1; i < 3; i++ )
			assertTrue(pyr.getSigma(i)>sigma);
	}

	@Test
	public void standardPyramid_sigma_1() {
		ImagePyramid pyr = UtilDenseOpticalFlow.standardPyramid(100,200,1,1.5,25,100, GrayF32.class);

		assertEquals(1,pyr.getNumLayers());

		assertEquals(1,pyr.getScale(0),1e-8);
		assertEquals(0,pyr.getSigma(0),1e-8);
	}

	@Test
	public void standardPyramid_sigma_0() {
		ImagePyramid pyr = UtilDenseOpticalFlow.standardPyramid(100,200,0,1.5,25,30, GrayF32.class);

		assertEquals(30,pyr.getNumLayers());

		assertEquals(1,pyr.getScale(0),1e-8);
		assertEquals(100/25,pyr.getScale(29),1e-8);
	}

	@Test
	public void standardPyramid_justScale() {
		ImagePyramid pyr = UtilDenseOpticalFlow.standardPyramid(100,200,0.5,0,25,100, GrayF32.class);

		assertEquals(3,pyr.getNumLayers());

		assertEquals(1,pyr.getScale(0),1e-8);
		assertEquals(2,pyr.getScale(1),1e-8);
		assertEquals(4,pyr.getScale(2),1e-8);

		for( int i = 0; i < 3; i++ )
			assertEquals(0, pyr.getSigma(i), 1e-8);
	}

}
