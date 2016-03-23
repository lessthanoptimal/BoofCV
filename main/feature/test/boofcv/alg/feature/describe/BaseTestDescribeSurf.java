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

package boofcv.alg.feature.describe;

import boofcv.alg.feature.describe.impl.TestImplSurfDescribeOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.SparseImageGradient;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public abstract class BaseTestDescribeSurf<I extends ImageGray,II extends ImageGray> {
	Random rand = new Random(234);
	int width = 50;
	int height = 60;

	int c_x = width/2;
	int c_y = height/2;

	DescribePointSurf<II> alg = createAlg();
	protected SparseImageGradient<II,?> sparse;
	II ii;
	I input;

	public BaseTestDescribeSurf( Class<I> inputType , Class<II> integralType ) {
		input = GeneralizedImageOps.createSingleBand(inputType,width,height);
		ii = GeneralizedImageOps.createSingleBand(integralType,width,height);
	}

	public abstract DescribePointSurf<II> createAlg();

	/**
	 * Does it produce a the same features when given a subimage?
	 */
	@Test
	public void checkSubImage() {
		GImageMiscOps.fillUniform(ii, rand, 0, 100);
		alg.setImage(ii);
		BrightFeature expected = alg.createDescription();
		alg.describe(c_x,c_y, 0, 1, expected);

		II sub = BoofTesting.createSubImageOf(ii);

		alg.setImage(sub);
		BrightFeature found = alg.createDescription();
		alg.describe(c_x,c_y, 0, 1, found);

		assertTrue(isSimilar(expected,found));
	}

	/**
	 * Does it produce a different feature when scalled?
	 */
	@Test
	public void changeScale() {
		GImageMiscOps.fillUniform(ii, rand, 0, 100);
		alg.setImage(ii);
		BrightFeature a = alg.createDescription();
		BrightFeature b = alg.createDescription();
		alg.describe(c_x,c_y, 0, 1, a);
		alg.describe(c_x,c_y, 0, 1.5, b);

		assertFalse(isSimilar(a,b));
	}

	/**
	 * Does it produce a different feature when rotated?
	 */
	@Test
	public void changeRotation() {
		GImageMiscOps.fillUniform(ii, rand, 0, 100);
		alg.setImage(ii);
		BrightFeature a = alg.createDescription();
		BrightFeature b = alg.createDescription();
		alg.describe(c_x,c_y, 0, 1, a);
		alg.describe(c_x,c_y, 1, 1, b);

		assertFalse(isSimilar(a,b));
	}

	private boolean isSimilar(BrightFeature a, BrightFeature b ) {
		if( a.white != b.white)
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
			alg.describe(0,0, angle, 1, alg.createDescription());
			alg.describe(ii.width-1,ii.height-1, angle, 1, alg.createDescription());
		}
	}

	/**
	 * If the image has a constant value then all the features should be zero.
	 */
	@Test
	public void features_constant() {
		GImageMiscOps.fill(input, 50);

		GIntegralImageOps.transform(input, ii);
		sparse = TestImplSurfDescribeOps.createGradient(ii, 1);
		alg.setImage(ii);
		
		BrightFeature feat = alg.createDescription();
		alg.describe(20,20, 0.75, 1, feat);

		for( double f : feat.value )
			assertEquals(0,f,1e-4);
	}

	/**
	 * Create an image which has a constant slope.  The features aligned along that
	 * direction should be large.  This also checks that the orientation parameter
	 * is being used correctly and that absolute value is being done.
	 */
	@Test
	public void features_increasing() {
		// test the gradient along the x-axis only
		TestImplSurfDescribeOps.createGradient(0,input);
		GIntegralImageOps.transform(input, ii);
		sparse = TestImplSurfDescribeOps.createGradient(ii, 1);

		// orient the feature along the x-axis
		alg.setImage(ii);
		BrightFeature feat = alg.createDescription();
		alg.describe(15,15, 0, 1, feat);

		for( int i = 0; i < 64; i+= 4) {
			assertEquals(feat.value[i],feat.value[i+1],1e-4);
			assertTrue(feat.value[i] > 0);
			assertEquals(0,feat.value[i+2],1e-4);
			assertEquals(0,feat.value[i+3],1e-4);
		}

		// now orient the feature along the y-axis
		alg.describe(15,15, Math.PI / 2.0, 1, feat);

		for( int i = 0; i < 64; i+= 4) {
			assertEquals(-feat.value[i+2],feat.value[i+3],1e-4);
			assertTrue(feat.value[i+2] < 0);
			assertEquals(0,feat.value[i],1e-4);
			assertEquals(0,feat.value[i+1],1e-4);
		}
	}

	/**
	 * Give it a scale factor which is a fraction and see if it blows up
	 */
	@Test
	public void features_fraction() {
		// test the gradient along the x-axis only
		TestImplSurfDescribeOps.createGradient( 0,input);
		GIntegralImageOps.transform(input,ii);
		sparse = TestImplSurfDescribeOps.createGradient(ii,1.5);

		// orient the feature along the x-axis
		alg.setImage(ii);
		BrightFeature feat = alg.createDescription();
		alg.describe(25,25, 0, 1.5, feat);

		for( int i = 0; i < 64; i+= 4) {
			assertEquals(feat.value[i],feat.value[i+1],1e-4);
			assertTrue(feat.value[i] > 0);
			assertEquals(0,feat.value[i+2],1e-4);
			assertEquals(0,feat.value[i+3],1e-4);
		}
	}
}
