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

import gecv.abst.filter.derivative.ImageGradient;
import gecv.core.image.GeneralizedImageOps;
import gecv.factory.feature.describe.FactoryDescribePointAlgs;
import gecv.factory.filter.derivative.FactoryDerivative;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.feature.TupleFeature_F64;
import gecv.struct.image.ImageFloat32;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertFalse;


/**
 * @author Peter Abeles
 */
public class TestDescribePointSteerable2D {

	Random rand = new Random(234);
	int width = 50;
	int height = 60;

	int c_x = width/2;
	int c_y = height/2;

	int radius = 10;

	DescribePointSteerable2D<ImageFloat32, Kernel2D_F32> alg;
	ImageFloat32 image = new ImageFloat32(width,height);

	public TestDescribePointSteerable2D() {
		GeneralizedImageOps.randomize(image,rand,0,100);

		ImageGradient<ImageFloat32,ImageFloat32> gradient = FactoryDerivative.sobel(ImageFloat32.class,ImageFloat32.class);
		alg = FactoryDescribePointAlgs.steerableGaussian(false,-1,radius,ImageFloat32.class);

	}

	/**
	 * Does it produce a the same features when given a subimage?
	 */
	@Test
	public void checkSubImage()
	{
		alg.setImage(image);
		TupleFeature_F64 expected = alg.describe(c_x,c_y,0);

		ImageFloat32 sub = GecvTesting.createSubImageOf(image);
		alg.setImage(sub);
		TupleFeature_F64 found = alg.describe(c_x,c_y,0);

		GecvTesting.assertEquals(expected.value,found.value,1e-8);
	}

	/**
	 * Does it produce a different feature when rotated?
	 */
	@Test
	public void changeRotation() {
		alg.setImage(image);
		TupleFeature_F64 a = alg.describe(c_x,c_y,0);
		TupleFeature_F64 b = alg.describe(c_x,c_y,1);

		boolean equals = true;
		for( int i = 0; i < a.value.length; i++ ) {
			double diff = Math.abs(a.value[i]-b.value[i]);
			if( diff > 1e-8 ) {
				equals = false;
				break;
			}
		}
		assertFalse(equals);
	}
}
