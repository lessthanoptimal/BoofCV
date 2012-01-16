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

package boofcv.alg.feature.describe;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class BaseTestDescribeSurf<I extends ImageSingleBand> {
	Random rand = new Random(234);
	int width = 50;
	int height = 60;

	int c_x = width/2;
	int c_y = height/2;

	DescribePointSurf<I> alg = createAlg();
	I ii;

	public BaseTestDescribeSurf( Class<I> imageType ) {
		ii = GeneralizedImageOps.createSingleBand(imageType,width,height);
		GeneralizedImageOps.randomize(ii, rand, 0, 100);
	}

	public abstract DescribePointSurf<I> createAlg();

	/**
	 * Does it produce a the same features when given a subimage?
	 */
	@Test
	public void checkSubImage() {
		alg.setImage(ii);
		SurfFeature expected = alg.describe(c_x,c_y,1,0,null);

		I sub = BoofTesting.createSubImageOf(ii);

		alg.setImage(sub);
		SurfFeature found = alg.describe(c_x,c_y,1,0,null);

		assertTrue(isSimilar(expected,found));
	}

	/**
	 * Does it produce a different feature when scalled?
	 */
	@Test
	public void changeScale() {
		alg.setImage(ii);
		SurfFeature a = alg.describe(c_x,c_y,1,0,null);
		SurfFeature b = alg.describe(c_x,c_y,1.5,0,null);

		assertFalse(isSimilar(a,b));
	}

	/**
	 * Does it produce a different feature when rotated?
	 */
	@Test
	public void changeRotation() {
		alg.setImage(ii);
		SurfFeature a = alg.describe(c_x,c_y,1,0,null);
		SurfFeature b = alg.describe(c_x,c_y,1,1,null);

		assertFalse(isSimilar(a,b));
	}

	private boolean isSimilar( SurfFeature a, SurfFeature b ) {
		if( a.laplacianPositive != b.laplacianPositive )
			return false;

		for( int i = 0; i < 64; i++ ) {
			double diff = Math.abs(a.value[i] - b.value[i]);

			if( diff > 1e-4 )
				return false;
		}
		return true;
	}

	/**
	 * Can it process descriptors along the image border?  If an exception
	 * is thrown then it failed the test
	 */
	@Test
	public void checkBorder() {
		alg.setImage(ii);
	
		for( int i = 0; i < 10; i++ ) {
			double angle = (2.0*Math.PI*i)/10;
			alg.describe(0,0,1,angle,null);
			alg.describe(ii.width-1,ii.height-1,1,angle,null);
		}
	}
}
