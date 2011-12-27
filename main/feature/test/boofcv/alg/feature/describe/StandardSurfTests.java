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


import boofcv.alg.feature.describe.impl.TestImplSurfDescribeOps;
import boofcv.alg.misc.ImageTestingOps;
import boofcv.alg.transform.ii.IntegralImageOps;
import boofcv.struct.deriv.SparseImageGradient;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for SURF feature descriptors.
 *
 * @author Peter Abeles
 */
public abstract class StandardSurfTests {
	int width = 60;
	int height = 70;

	int c_x = width/2;
	int c_y = height/2;

	protected SparseImageGradient<ImageFloat32,?> sparse;

	protected abstract void describe( double x , double y , double yaw , double scale ,
									  double[] features );
	/**
	 * If the image has a constant value then all the features should be zero.
	 */
	@Test
	public void features_constant() {
		ImageFloat32 img = new ImageFloat32(width,height);
		ImageTestingOps.fill(img, 50);

		ImageFloat32 ii = IntegralImageOps.transform(img, null);
		sparse = TestImplSurfDescribeOps.createGradient(ii, 1);

		double features[] = new double[64];
		describe(20, 20, 0.75, 1, features);

		for( double f : features )
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
		ImageFloat32 img = TestImplSurfDescribeOps.createGradient(width, height, 0);
		ImageFloat32 ii = IntegralImageOps.transform(img,null);
		sparse = TestImplSurfDescribeOps.createGradient(ii, 1);

		// orient the feature along the x-axis
		double features[] = new double[64];
		describe(15, 15, 0, 1, features);

		for( int i = 0; i < 64; i+= 4) {
			assertEquals(features[i],features[i+1],1e-4);
			assertTrue(features[i] > 0);
			assertEquals(0,features[i+2],1e-4);
			assertEquals(0,features[i+3],1e-4);
		}

		// now orient the feature along the y-axis
		describe(15, 15, Math.PI / 2.0, 1, features);

		for( int i = 0; i < 64; i+= 4) {
			assertEquals(-features[i+2],features[i+3],1e-4);
			assertTrue(features[i+2] < 0);
			assertEquals(0,features[i],1e-4);
			assertEquals(0,features[i+1],1e-4);
		}
	}

	/**
	 * Give it a scale factor which is a fraction and see if it blows up
	 */
	@Test
	public void features_fraction() {
				// test the gradient along the x-axis only
		ImageFloat32 img = TestImplSurfDescribeOps.createGradient(width, height, 0);
		ImageFloat32 ii = IntegralImageOps.transform(img,null);
		sparse = TestImplSurfDescribeOps.createGradient(ii,1.5);

		// orient the feature along the x-acis
		double features[] = new double[64];
		describe(25, 25, 0, 1.5, features);

		for( int i = 0; i < 64; i+= 4) {
			assertEquals(features[i],features[i+1],1e-4);
			assertTrue(features[i] > 0);
			assertEquals(0,features[i+2],1e-4);
			assertEquals(0,features[i+3],1e-4);
		}
	}
}
