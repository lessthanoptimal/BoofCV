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

package gecv.alg.feature.describe.impl;

import gecv.alg.feature.describe.SurfDescribeOps;
import gecv.alg.misc.ImageTestingOps;
import gecv.alg.transform.ii.IntegralImageOps;
import gecv.core.image.GeneralizedImageOps;
import gecv.factory.filter.kernel.FactoryKernelGaussian;
import gecv.misc.GecvMiscOps;
import gecv.struct.convolve.Kernel2D_F64;
import gecv.struct.deriv.SparseImageGradient;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestImplSurfDescribeOps {

	Random rand = new Random(234);
	int width = 80;
	int height = 100;

	Kernel2D_F64 weight = FactoryKernelGaussian.gaussian(2,true, 64, -1,10);
	double features[] = new double[64];

	@Test
	public void gradientInner_F32() {
		ImageFloat32 ii = new ImageFloat32(width,height);
		GeneralizedImageOps.randomize(ii,rand,0,100);
		int r = 2;
		int w = r*2+1;

		double expectedX[] = new double[w*w];
		double expectedY[] = new double[w*w];
		float foundX[] = new float[w*w];
		float foundY[] = new float[w*w];

		ImplSurfDescribeOps.naiveGradient(ii,10,10,r,1,4,expectedX,expectedY);
		ImplSurfDescribeOps.gradientInner(ii,10,10,r,1,4,foundX,foundY);

		for( int i = 0; i < foundX.length; i++ ) {
			assertEquals("at "+i,expectedX[i],foundX[i],1e-4);
			assertEquals("at "+i,expectedY[i],foundY[i],1e-4);
		}

		ImplSurfDescribeOps.naiveGradient(ii,10,10,r,1.5,4,expectedX,expectedY);
		ImplSurfDescribeOps.gradientInner(ii,10,10,r,1.5,4,foundX,foundY);

		for( int i = 0; i < foundX.length; i++ ) {
			assertEquals("at "+i,expectedX[i],foundX[i],1e-4);
			assertEquals("at "+i,expectedY[i],foundY[i],1e-4);
		}
	}

	@Test
	public void gradientInner_I32() {
		ImageSInt32 ii = new ImageSInt32(width,height);
		GeneralizedImageOps.randomize(ii,rand,0,100);
		int r = 2;
		int w = r*2+1;

		double expectedX[] = new double[w*w];
		double expectedY[] = new double[w*w];
		int foundX[] = new int[w*w];
		int foundY[] = new int[w*w];

		ImplSurfDescribeOps.naiveGradient(ii,10,10,r,1,4,expectedX,expectedY);
		ImplSurfDescribeOps.gradientInner(ii,10,10,r,1,4,foundX,foundY);

		for( int i = 0; i < foundX.length; i++ ) {
			assertEquals("at "+i,expectedX[i],foundX[i],1e-4);
			assertEquals("at "+i,expectedY[i],foundY[i],1e-4);
		}

		ImplSurfDescribeOps.naiveGradient(ii,10,10,r,1.5,4,expectedX,expectedY);
		ImplSurfDescribeOps.gradientInner(ii,10,10,r,1.5,4,foundX,foundY);

		for( int i = 0; i < foundX.length; i++ ) {
			assertEquals("at "+i,expectedX[i],foundX[i],1e-4);
			assertEquals("at "+i,expectedY[i],foundY[i],1e-4);
		}
	}

	/**
	 * Create an image which has a constant slope.  See if
	 * the wavelet correctly computes that slope.
	 */
	@Test
	public void naiveGradient() {

		double scale = 1;
		int r = 6;
		checkNaiveGradient(scale, r);
	}

	/**
	 * Give it a scale factor which is a fraction and see if it blows up
	 */
	@Test
	public void naiveGradient_fraction() {
		double scale = 1.5;
		int r = 6;
		checkNaiveGradient(scale, r);
	}

	private void checkNaiveGradient(double scale, int r) {
		int w = 2*r+1;

		double derivX[] = new double[w*w];
		double derivY[] = new double[w*w];

		for( double theta = -Math.PI; theta <= Math.PI; theta += 0.15 ) {
			ImageFloat32 img = createGradient(width,height,theta);
			ImageFloat32 ii = IntegralImageOps.transform(img,null);

			ImplSurfDescribeOps.naiveGradient(ii,r*3,r*3,r,scale,4,derivX,derivY);

			double c = Math.cos(theta);
			double s = Math.sin(theta);

			for( int i = 0; i < derivX.length; i++ ) {
				double dx = derivX[i];
				double dy = derivY[i];

				double n = Math.sqrt(dx*dx + dy*dy);

				assertEquals(c,dx/n,1e-3);
				assertEquals(s,dy/n,1e-3);
			}
		}
	}

	/**
	 * If the image has a constant value then all the features should be zero.
	 */
	@Test
	public void features_constant() {
		ImageFloat32 img = new ImageFloat32(width,height);
		ImageTestingOps.fill(img,50);

		ImageFloat32 ii = IntegralImageOps.transform(img,null);
		SparseImageGradient<ImageFloat32,?> sparse = createGradient(ii,1);

		GecvMiscOps.zero(features,64);
		ImplSurfDescribeOps.features(20,20,0.75,weight,20,4,1,sparse,features);

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
		ImageFloat32 img = createGradient(width,height,0);
		ImageFloat32 ii = IntegralImageOps.transform(img,null);
		SparseImageGradient<ImageFloat32,?> sparse = createGradient(ii,1);

		// orient the feature along the x-axis
		ImplSurfDescribeOps.features(15,15,0,weight,20,4,1,sparse,features);

		for( int i = 0; i < 64; i+= 4) {
			assertEquals(features[i],features[i+1],1e-4);
			assertTrue(features[i] > 0);
			assertEquals(0,features[i+2],1e-4);
			assertEquals(0,features[i+3],1e-4);
		}

		// now orient the feature along the y-axis
		ImplSurfDescribeOps.features(15,15,Math.PI/2.0,weight,20,4,1,sparse,features);

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
		ImageFloat32 img = createGradient(width,height,0);
		ImageFloat32 ii = IntegralImageOps.transform(img,null);
		SparseImageGradient<ImageFloat32,?> sparse = createGradient(ii,1.5);

		// orient the feature along the x-acis
		ImplSurfDescribeOps.features(25,25,0,weight,20,4,1.5,sparse,features);

		for( int i = 0; i < 64; i+= 4) {
			assertEquals(features[i],features[i+1],1e-4);
			assertTrue(features[i] > 0);
			assertEquals(0,features[i+2],1e-4);
			assertEquals(0,features[i+3],1e-4);
		}
	}

	/**
	 * Creates an image with a constant gradient in the specified direction
	 */
	private ImageFloat32 createGradient( int width , int height , double theta ) {
		ImageFloat32 ret = new ImageFloat32(width,height);

		double c = Math.cos(theta);
		double s = Math.sin(theta);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				double xx = c*x;
				double yy = s*y;
				ret.set(x,y,(float)(xx+yy));
			}
		}
		return ret;
	}

	public SparseImageGradient<ImageFloat32,?> createGradient( ImageFloat32 ii , double scale) {
		SparseImageGradient<ImageFloat32,?> ret =
				SurfDescribeOps.createGradient(false,false,4,scale,ImageFloat32.class);
		ret.setImage(ii);
		return ret;
	}
}
