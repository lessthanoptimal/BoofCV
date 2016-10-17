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

package boofcv.alg.transform.pyramid;

import boofcv.abst.distort.FDistort;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestPyramidFloatScale extends GenericPyramidTests<GrayF32> {

	public TestPyramidFloatScale() {
		super(GrayF32.class);
	}

	/**
	 * Compares update to a convolution and sub-sampling of upper layers.
	 */
	@Test
	public void update() {

		GrayF32 input = new GrayF32(width,height);
		BoofTesting.checkSubImage(this, "_update", true, input);
	}

	public void _update(GrayF32 input) {

		InterpolatePixelS<GrayF32> interp = FactoryInterpolation.bilinearPixelS(input, BorderType.EXTENDED);
		PyramidFloatScale<GrayF32> alg = new PyramidFloatScale<>(interp,new double[]{3,5},imageType);
		alg.process(input);

		// test the first layer
		GrayF32 expected = new GrayF32((int)Math.ceil(width/3.0),(int)Math.ceil(height/3.0));
		new FDistort(input,expected).scale().apply();
		GrayF32 found = alg.getLayer(0);

		BoofTesting.assertEquals(expected,found,1e-4);

		// test the second layer
		GrayF32 next = new GrayF32((int)Math.ceil(width/5.0),(int)Math.ceil(height/5.0));
		new FDistort(expected,next).scale().apply();
		found = alg.getLayer(1);

		BoofTesting.assertEquals(next,found,1e-4);
	}



	@Override
	protected ImagePyramid<GrayF32> createPyramid(int... scales) {
		InterpolatePixelS<GrayF32> interp = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED);
		double a[] = BoofMiscOps.convertTo_F64(scales);
		return new PyramidFloatScale<>(interp,a,imageType);
	}

	/**
	 * Well no blur is applied at any level so this should all be zero
	 */
	@Test
	public void checkSigmas() {
		InterpolatePixelS<GrayF32> interp = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED);
		PyramidFloatScale<GrayF32> alg = new PyramidFloatScale<>(interp,new double[]{3,5},imageType);

		for( int i = 0; i < 2; i++ ) {
			assertEquals(0,alg.getSigma(0),1e-8);
		}
	}
}
