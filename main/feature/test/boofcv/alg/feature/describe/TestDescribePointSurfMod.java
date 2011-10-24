/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.ImageFloat32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestDescribePointSurfMod {

	Random rand = new Random(234);
	int width = 50;
	int height = 60;

	int c_x = width/2;
	int c_y = height/2;

	DescribePointSurfMod<ImageFloat32> alg = new DescribePointSurfMod<ImageFloat32>();
	ImageFloat32 ii = new ImageFloat32(width,height);

	public TestDescribePointSurfMod() {
		GeneralizedImageOps.randomize(ii,rand,0,100);
	}

	/**
	 * Does it produce a the same features when given a subimage?
	 */
	@Test
	public void checkSubImage() {
		alg.setImage(ii);
		SurfFeature expected = alg.describe(c_x,c_y,1,0,null);

		ImageFloat32 sub = BoofTesting.createSubImageOf(ii);

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
			double diff = Math.abs(a.features.value[i] - b.features.value[i]);

			if( diff > 1e-4 )
				return false;
		}
		return true;
	}
}
