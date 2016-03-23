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

package boofcv.factory.transform.pyramid;

import boofcv.alg.transform.pyramid.PyramidFloatGaussianScale;
import boofcv.struct.image.GrayF32;
import boofcv.struct.pyramid.PyramidFloat;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestFactoryPyramid {
	/**
	 * Makes sure the correct amount of blur is applied to each layer.  The scale and getSigma() should be the same
	 */
	@Test
	public void scaleSpace() {
		double ss[] = new double[]{1,2,4,6,8,10};

		PyramidFloat<GrayF32> pyramid = FactoryPyramid.scaleSpacePyramid(ss, GrayF32.class);

		for( int i = 0; i < ss.length; i++ ) {
			assertEquals(ss[i],pyramid.getSigma(i),1e-8);
			assertEquals(ss[i],pyramid.getScale(i),1e-8);
			// the amount of blur applied to the previous layer should be different
			if( i > 1)
				assertTrue(Math.abs(ss[i] - ((PyramidFloatGaussianScale)pyramid).getSigmaLayers()[i])>0.1);
		}
	}
}
