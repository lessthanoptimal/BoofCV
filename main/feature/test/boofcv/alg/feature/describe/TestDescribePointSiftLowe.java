/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageFloat32;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDescribePointSiftLowe {

	Random rand = new Random(234);

	/**
	 * Tests to see if it blows up and not much more.  Random image.  Compute descriptor along border and image
	 * center.
	 */
	@Test
	public void process() {
		ImageFloat32 derivX = new ImageFloat32(200,200);
		ImageFloat32 derivY = new ImageFloat32(200,200);

		GImageMiscOps.fillUniform(derivX,rand,-100,100);
		GImageMiscOps.fillUniform(derivY,rand,-100,100);

		DescribePointSiftLowe<ImageFloat32> alg =
				new DescribePointSiftLowe<ImageFloat32>(4,4,8,1.5,0.5,0.2,ImageFloat32.class);
		alg.setImageGradient(derivX,derivY);

		List<Point2D_I32> testPoints = new ArrayList<Point2D_I32>();
		testPoints.add( new Point2D_I32(100,0));
		testPoints.add( new Point2D_I32(100,199));
		testPoints.add( new Point2D_I32(0,100));
		testPoints.add( new Point2D_I32(199,100));
		testPoints.add( new Point2D_I32(100,100));

		TupleDesc_F64 desc = new TupleDesc_F64(alg.getDescriptorLength());
		for( Point2D_I32 where : testPoints ) {
			alg.process(where.x,where.y,2,0.5,desc);
		}
	}

	@Test
	public void massageDescriptor() {
		TupleDesc_F64 descriptor = new TupleDesc_F64(128);
		descriptor.value[5] = 100;
		descriptor.value[20] = 120;
		descriptor.value[60] = 20;

		DescribePointSiftLowe<ImageFloat32> alg =
				new DescribePointSiftLowe<ImageFloat32>(4,4,8,1.5,0.5,0.2,ImageFloat32.class);
		alg.descriptor = descriptor;
		alg.massageDescriptor();

		assertEquals(1,normL2(alg.descriptor),1e-8);

		// cropping should make 5 and 20 the same
		assertEquals(descriptor.value[5],descriptor.value[20],1e-8);
	}

	private double normL2( TupleDesc_F64 desc ) {
		double total = 0;
		for( double d : desc.value) {
			total += d*d;
		}
		return Math.sqrt(total);
	}

	/**
	 * Only put gradient inside a small area that fills the descriptor.  Then double the scale and see if
	 * only a 1/4 of the original image is inside
	 */
	@Test
	public void computeRawDescriptor_scale() {
		ImageFloat32 derivX = new ImageFloat32(200,200);
		ImageFloat32 derivY = new ImageFloat32(200,200);

		DescribePointSiftLowe<ImageFloat32> alg =
				new DescribePointSiftLowe<ImageFloat32>(4,4,8,1.5,0.5,0.2,ImageFloat32.class);
		int r = alg.getCanonicalRadius();
		ImageMiscOps.fillRectangle(derivX,5.0f,60,60,2*r,2*r);

		alg.setImageGradient(derivX,derivY);
		alg.descriptor = new TupleDesc_F64(128);
		alg.computeRawDescriptor(60+r, 60+r, 1, 0);

		int numHit = computeInside(alg);
		assertEquals(4*4,numHit);

		alg.descriptor = new TupleDesc_F64(128);
		alg.computeRawDescriptor(60+r, 60+r, 2, 0);
		numHit = computeInside(alg);
		assertEquals(2*2,numHit);

	}

	private int computeInside(DescribePointSiftLowe alg) {
		int numHit = 0;
		for (int j = 0; j < 128; j++) {
			if (j % 8 == 0) {
				if(alg.descriptor.value[j] > 0)
					numHit++;
			} else {
				assertEquals(0, alg.descriptor.value[j], 1e-4);
			}
		}
		return numHit;
	}

	/**
	 * Have a constant gradient, which has an easy to understand descriptor, then rotate the feature and see
	 * if the description changes as expected.
	 */
	@Test
	public void computeRawDescriptor_rotation() {
		ImageFloat32 derivX = new ImageFloat32(60,55);
		ImageFloat32 derivY = new ImageFloat32(60,55);

		ImageMiscOps.fill(derivX,5.0f);
		
		DescribePointSiftLowe<ImageFloat32> alg =
				new DescribePointSiftLowe<ImageFloat32>(4,4,8,1.5,0.5,0.2,ImageFloat32.class);
		alg.setImageGradient(derivX,derivY);

		for( int i = 0; i < 8; i++ ) {
			double angle = UtilAngle.bound(i*Math.PI/4);
			alg.descriptor = new TupleDesc_F64(128);
			alg.computeRawDescriptor(20, 21, 1, angle);

			int bin = (int) (UtilAngle.domain2PI(-angle) * 8 / (2 * Math.PI));
			for (int j = 0; j < 128; j++) {
				if (j % 8 == bin) {
					assertTrue(alg.descriptor.value[j] > 0);
				} else {
					assertEquals(0, alg.descriptor.value[j], 1e-4);
				}
			}
		}
		
	}

	/**
	 * Tests trilinear interpolation by checking out some of its properties instead of its value
	 * exactly
	 */
	@Test
	public void trilinearInterpolation() {
		DescribePointSiftLowe<ImageFloat32> alg =
				new DescribePointSiftLowe<ImageFloat32>(4,4,8,1.5,0.5,0.2,ImageFloat32.class);

		alg.descriptor = new TupleDesc_F64(128);

		// in the middle of the feature, the total amount added to the descriptor should equal the input weight
		// upper edges will have a value less than the input weight
		alg.trilinearInterpolation(2.0f,1.25f,2.0f,0.5);

		double sum = 0;
		int count = 0;
		for (int i = 0; i < alg.descriptor.size(); i++) {
			sum += alg.descriptor.value[i];
			if( alg.descriptor.value[i] != 0 )
				count++;
		}
		assertEquals(2.0,sum,1e-6);
		assertTrue(count>1);

		// try an edge case
		sum = 0;
		alg.descriptor.fill(0);
		alg.trilinearInterpolation(2.0f,3.25f,3.25f,0.5);
		for (int i = 0; i < alg.descriptor.size(); i++) {
			sum += alg.descriptor.value[i];
		}
		assertEquals( 2.0*0.75*0.75*1.0, sum, 1e-8 );

		// now have something exactly at the start of a bin.  all the weight should be in one location
		alg.descriptor.fill(0);
		alg.trilinearInterpolation(2.0f,3f,3f,2*Math.PI/8);
		count = 0;
		for (int i = 0; i < alg.descriptor.size(); i++) {
			double weight = alg.descriptor.value[i];
			if( weight > 0 ) {
				assertEquals(2.0,weight,1e-8);
				count++;
			}
		}
		assertEquals(1,count);
	}

}