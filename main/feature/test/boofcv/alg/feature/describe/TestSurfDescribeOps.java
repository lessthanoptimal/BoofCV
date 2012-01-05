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

import boofcv.alg.feature.describe.impl.ImplSurfDescribeOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;


/**
 * @author Peter Abeles
 */
public class TestSurfDescribeOps {

	Random rand = new Random(2342);

	int width = 60;
	int height = 70;

	int c_x = width/2;
	int c_y = height/2;

	ImageFloat32 inputF32;
	ImageSInt32 inputI32;

	public TestSurfDescribeOps() {
		inputF32 = new ImageFloat32(width,height);
		inputI32 = new ImageSInt32(width,height);
		GeneralizedImageOps.randomize(inputF32,rand,0,100);
		GeneralizedImageOps.randomize(inputI32,rand,0,100);
	}

	@Test
	public void gradient() {
		int radiusRegions = 4;
		int w = radiusRegions*2+1;
		double expectedX[] = new double[ w*w ];
		double expectedY[] = new double[ w*w ];
		double foundX[] = new double[ w*w ];
		double foundY[] = new double[ w*w ];

		// check inside the image
		SurfDescribeOps.gradient(inputF32,c_x,c_y,radiusRegions,4,1.5, false, foundX,foundY);
		ImplSurfDescribeOps.naiveGradient(inputF32,c_x,c_y,radiusRegions, 4, 1.5, false, expectedX,expectedY);

		BoofTesting.assertEquals(foundX,expectedX,1e-4);
		BoofTesting.assertEquals(foundY,expectedY,1e-4);

		// check the border
		SurfDescribeOps.gradient(inputF32,0,0,radiusRegions,4,1.5, false, foundX,foundY);
		ImplSurfDescribeOps.naiveGradient(inputF32,0,0,radiusRegions, 4, 1.5, false, expectedX,expectedY);

		BoofTesting.assertEquals(foundX,expectedX,1e-4);
		BoofTesting.assertEquals(foundY,expectedY,1e-4);
	}

	@Test
	public void gradient_noborder_F32() {
		int radiusRegions = 4;
		int w = radiusRegions*2+1;
		double expectedX[] = new double[ w*w ];
		double expectedY[] = new double[ w*w ];
		float foundX[] = new float[ w*w ];
		float foundY[] = new float[ w*w ];

		// check inside the image
		SurfDescribeOps.gradient_noborder(inputF32,c_x,c_y,radiusRegions, 4,1.5,foundX,foundY);
		ImplSurfDescribeOps.naiveGradient(inputF32,c_x,c_y,radiusRegions, 4, 1.5, false, expectedX,expectedY);

		BoofTesting.assertEquals(expectedX,foundX,1e-4);
		BoofTesting.assertEquals(expectedY,foundY,1e-4);
	}

	@Test
	public void gradient_noborder_I32() {
		int radiusRegions = 4;
		int w = radiusRegions*2+1;
		double expectedX[] = new double[ w*w ];
		double expectedY[] = new double[ w*w ];
		int foundX[] = new int[ w*w ];
		int foundY[] = new int[ w*w ];

		// check inside the image
		SurfDescribeOps.gradient_noborder(inputI32,c_x,c_y,radiusRegions, 4,1.5,foundX,foundY);
		ImplSurfDescribeOps.naiveGradient(inputI32,c_x,c_y,radiusRegions, 4, 1.5, false, expectedX,expectedY);

		BoofTesting.assertEquals(expectedX,foundX);
		BoofTesting.assertEquals(expectedY,foundY);
	}


	@Test
	public void isInside_aligned() {
		int regionRadius = 10;
		int kernelSize = 3;

		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,c_y,regionRadius,kernelSize,1, 0));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,c_y,regionRadius,kernelSize,2, 0));
		// check lower boundary
		for( int i = 0; i < 2; i++ ) {
			boolean swap = i != 0;
			checkInside(regionRadius+1+1,c_y,swap,regionRadius,kernelSize,1,true);
			checkInside(2*regionRadius+3+1,c_y,swap,regionRadius,kernelSize,2,true);
			checkInside(regionRadius+1,c_y,swap,regionRadius,kernelSize,1,false);
			checkInside(2*regionRadius+3,c_y,swap,regionRadius,kernelSize,2,false);
		}
		// check upper boundary
		checkInside(width-regionRadius-1-1,c_y,false,regionRadius,kernelSize,1,true);
		checkInside(c_x,height-regionRadius-1-1,false,regionRadius,kernelSize,1,true);
		checkInside(width-regionRadius-1,c_y,false,regionRadius,kernelSize,1,false);
		checkInside(c_x,height-regionRadius-1,false,regionRadius,kernelSize,1,false);
	}

	private void checkInside( double x , double y , boolean swap , int regionRadius, int kernelSize ,
							  double scale , boolean result )
	{
		if( swap ) {
			double temp = x;
			x = y;
			y = temp;
		}
		assertTrue(result == SurfDescribeOps.isInside(inputF32,x,y,regionRadius,kernelSize,scale, 0));
	}

	@Test
	public void isInside_rotated() {
		int regionRadius = 10;
		int kernelSize = 3;
		double d90 = Math.PI/2.0;
		double d45 = Math.PI/4.0;
		 // it will round up the odd kernel size and take in account the extra offset for integral image sampling
		int fullRadius = regionRadius + (kernelSize/2 + (kernelSize%2) + 1);
		double r = Math.sqrt(2*fullRadius*fullRadius);
		int e = (int)Math.ceil(r)-fullRadius;

		// some positive examples
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,c_y,regionRadius,kernelSize,1,0));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,c_y,regionRadius,kernelSize,2,0));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,c_y,regionRadius,kernelSize,1,d90));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,c_y,regionRadius,kernelSize,2,d90));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,c_y,regionRadius,kernelSize,1,0.5));

		// boundary test cases
		// +2 = kernel radius, + 1 = needing to sample x-1,y-1 below in the integral image
		assertFalse(SurfDescribeOps.isInside(inputF32,regionRadius+2,c_y,regionRadius,kernelSize,1,0));
		assertFalse(SurfDescribeOps.isInside(inputF32,c_x,regionRadius+2,regionRadius,kernelSize,1,0));
		assertFalse(SurfDescribeOps.isInside(inputF32,width-regionRadius-1-1,c_y,regionRadius,kernelSize,1,0));
		assertFalse(SurfDescribeOps.isInside(inputF32,c_x,height-regionRadius-1-1,regionRadius,kernelSize,1,0));

		assertTrue(SurfDescribeOps.isInside(inputF32,regionRadius+2+1,c_y,regionRadius,kernelSize,1,0));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,regionRadius+2+1,regionRadius,kernelSize,1,0));
		assertTrue(SurfDescribeOps.isInside(inputF32,width-regionRadius-2-1,c_y,regionRadius,kernelSize,1,0));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,height-regionRadius-2-1,regionRadius,kernelSize,1,0));

		// boundary with a rotation
		assertFalse(SurfDescribeOps.isInside(inputF32,regionRadius+2+1,c_y,regionRadius,kernelSize,1,0.5));
		assertFalse(SurfDescribeOps.isInside(inputF32,c_x,regionRadius+2+1,regionRadius,kernelSize,1,0.5));
		assertFalse(SurfDescribeOps.isInside(inputF32,width-regionRadius-2-1,c_y,regionRadius,kernelSize,1,0.5));
		assertFalse(SurfDescribeOps.isInside(inputF32,c_x,height-regionRadius-2-1,regionRadius,kernelSize,1,0.5));

		assertFalse(SurfDescribeOps.isInside(inputF32,regionRadius+2+e,c_y,regionRadius,kernelSize,1,d45));
		assertFalse(SurfDescribeOps.isInside(inputF32,c_x,regionRadius+2+e,regionRadius,kernelSize,1,d45));
		assertTrue(SurfDescribeOps.isInside(inputF32,regionRadius+2+1+e,c_y,regionRadius,kernelSize,1,d45));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,regionRadius+2+1+e,regionRadius,kernelSize,1,d45));
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
