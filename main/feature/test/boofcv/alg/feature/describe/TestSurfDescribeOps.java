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

import boofcv.alg.feature.describe.impl.ImplSurfDescribeOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
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

	GrayF32 inputF32;
	GrayS32 inputI32;

	public TestSurfDescribeOps() {
		inputF32 = new GrayF32(width,height);
		inputI32 = new GrayS32(width,height);
		GImageMiscOps.fillUniform(inputF32, rand, 0, 100);
		GImageMiscOps.fillUniform(inputI32, rand, 0, 100);
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
		SurfDescribeOps.gradient(inputF32,c_x,c_y,1.5,w,4*1.5, false, foundX,foundY);
		ImplSurfDescribeOps.naiveGradient(inputF32,c_x,c_y,1.5,w, 4*1.5, false, expectedX,expectedY);

		BoofTesting.assertEquals(foundX,expectedX,1e-4);
		BoofTesting.assertEquals(foundY,expectedY,1e-4);

		// check the border
		SurfDescribeOps.gradient(inputF32,-2,-1,1.5,w,4*1.5, false, foundX,foundY);
		ImplSurfDescribeOps.naiveGradient(inputF32, -2, -1, 1.5, w, 4 * 1.5, false, expectedX, expectedY);

		BoofTesting.assertEquals(foundX, expectedX, 1e-4);
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
		SurfDescribeOps.gradient_noborder(inputF32,c_x,c_y,1.5,w, 4*1.5,foundX,foundY);
		ImplSurfDescribeOps.naiveGradient(inputF32,c_x,c_y,1.5,w, 4*1.5, false, expectedX,expectedY);

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
		SurfDescribeOps.gradient_noborder(inputI32,c_x,c_y,1.5,w, 4*1.5,foundX,foundY);
		ImplSurfDescribeOps.naiveGradient(inputI32,c_x,c_y,1.5,w, 4*1.5, false, expectedX,expectedY);

		BoofTesting.assertEquals(expectedX,foundX);
		BoofTesting.assertEquals(expectedY,foundY);
	}


	@Test
	public void isInside_aligned() {
		int regionRadius = 10;
		int kernelSize = 2;

		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,c_y,regionRadius,kernelSize,1, 0,0));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,c_y,regionRadius,kernelSize,2, 0,0));
		// check lower boundary
		for( int i = 0; i < 2; i++ ) {
			boolean swap = i != 0;
			checkInside(regionRadius+1+1,c_y,swap,regionRadius,kernelSize,1,true);
			checkInside(2*regionRadius+2+1,c_y,swap,regionRadius,kernelSize,2,true);
			checkInside(regionRadius+1,c_y,swap,regionRadius,kernelSize,1,false);
			checkInside(2*regionRadius+2,c_y,swap,regionRadius,kernelSize,2,false);
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
		assertTrue(result == SurfDescribeOps.isInside(inputF32,x,y,regionRadius,kernelSize,scale, 0,0));
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
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,c_y,regionRadius,kernelSize,1,0,0));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,c_y,regionRadius,kernelSize,2,0,0));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,c_y,regionRadius,kernelSize,1,Math.cos(d90),Math.sin(d90)));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,c_y,regionRadius,kernelSize,2,Math.cos(d90),Math.sin(d90)));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,c_y,regionRadius,kernelSize,1,Math.cos(0.5),Math.sin(0.5)));

		// boundary test cases
		// +2 = kernel radius, + 1 = needing to sample x-1,y-1 below in the integral image
		assertFalse(SurfDescribeOps.isInside(inputF32,regionRadius+2,c_y,regionRadius,kernelSize,1,0,0));
		assertFalse(SurfDescribeOps.isInside(inputF32,c_x,regionRadius+2,regionRadius,kernelSize,1,0,0));
		assertFalse(SurfDescribeOps.isInside(inputF32,width-regionRadius-1-1,c_y,regionRadius,kernelSize,1,0,0));
		assertFalse(SurfDescribeOps.isInside(inputF32,c_x,height-regionRadius-1-1,regionRadius,kernelSize,1,0,0));

		assertTrue(SurfDescribeOps.isInside(inputF32,regionRadius+2+1,c_y,regionRadius,kernelSize,1,0,0));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,regionRadius+2+1,regionRadius,kernelSize,1,0,0));
		assertTrue(SurfDescribeOps.isInside(inputF32,width-regionRadius-2-1,c_y,regionRadius,kernelSize,1,0,0));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,height-regionRadius-2-1,regionRadius,kernelSize,1,0,0));

		// boundary with a rotation
		double c5 = Math.cos(0.5);
		double s5 = Math.sin(0.5);
		assertFalse(SurfDescribeOps.isInside(inputF32,regionRadius+2+1,c_y,regionRadius,kernelSize,1,c5,s5));
		assertFalse(SurfDescribeOps.isInside(inputF32,c_x,regionRadius+2+1,regionRadius,kernelSize,1,c5,s5));
		assertFalse(SurfDescribeOps.isInside(inputF32,width-regionRadius-2-1,c_y,regionRadius,kernelSize,1,c5,s5));
		assertFalse(SurfDescribeOps.isInside(inputF32,c_x,height-regionRadius-2-1,regionRadius,kernelSize,1,c5,s5));

		double c45 = Math.cos(d45);
		double s45 = Math.sin(d45);
		assertFalse(SurfDescribeOps.isInside(inputF32,regionRadius+2+e,c_y,regionRadius,kernelSize,1,c45,s45));
		assertFalse(SurfDescribeOps.isInside(inputF32,c_x,regionRadius+2+e,regionRadius,kernelSize,1,c45,s45));
		assertTrue(SurfDescribeOps.isInside(inputF32,regionRadius+2+1+e,c_y,regionRadius,kernelSize,1,c45,s45));
		assertTrue(SurfDescribeOps.isInside(inputF32,c_x,regionRadius+2+1+e,regionRadius,kernelSize,1,c45,s45));
	}

	@Test
	public void isInside_rect() {
		// test a positive example
		assertTrue(SurfDescribeOps.isInside(20,30,2.3,4.5,8.7,2.0));

		// test positive border conditions
		assertTrue(SurfDescribeOps.isInside(20,30,2,2,8.7,2.0));
		assertTrue(SurfDescribeOps.isInside(20,30,8,18,10,2.0));

		// test negative border conditions
		assertFalse(SurfDescribeOps.isInside(20,30,1,1,8.7,2.0));
		assertFalse(SurfDescribeOps.isInside(20,30,9,19,10,2.0));
	}
	
	@Test
	public void rotatedWidth() {
		// test rotations by 90 degrees because these cases are simple
		assertEquals(10,SurfDescribeOps.rotatedWidth(10,1,0),1e-8);
		assertEquals(10,SurfDescribeOps.rotatedWidth(10,0,1),1e-8);
		assertEquals(10,SurfDescribeOps.rotatedWidth(10,-1,0),1e-8);
		assertEquals(10,SurfDescribeOps.rotatedWidth(10,0,-1),1e-8);
		
		// test the maximum increase at 45 degrees
		double w = 2*Math.sqrt(2*25);
		double theta = Math.PI/4.0;
		assertEquals(w,SurfDescribeOps.rotatedWidth(10,Math.cos(theta),Math.sin(theta)),1e-8);
		theta += Math.PI;
		assertEquals(w,SurfDescribeOps.rotatedWidth(10,Math.cos(theta),Math.sin(theta)),1e-8);
		theta -= Math.PI*2;
		assertEquals(w,SurfDescribeOps.rotatedWidth(10,Math.cos(theta),Math.sin(theta)),1e-8);
	}
}
