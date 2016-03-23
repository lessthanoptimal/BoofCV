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

package boofcv.alg.feature.describe.impl;

import boofcv.alg.feature.describe.SurfDescribeOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.ii.IntegralImageOps;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.SparseScaleGradient;
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

	@Test
	public void gradientInner_F32() {
		GrayF32 ii = new GrayF32(width,height);
		GImageMiscOps.fillUniform(ii, rand, 0, 100);
		int r = 2;
		int w = r*2+1;

		double expectedX[] = new double[w*w];
		double expectedY[] = new double[w*w];
		float foundX[] = new float[w*w];
		float foundY[] = new float[w*w];

		for( double scale = 0.2; scale <= 1.8; scale += 0.2 ) {
//			System.out.println("scale = "+scale);
			ImplSurfDescribeOps.naiveGradient(ii, 8, 9, scale, w, 4 * scale, false, expectedX, expectedY);
//			System.out.println("------------------------");
			ImplSurfDescribeOps.gradientInner(ii,8,9,scale,w, 4*scale, foundX,foundY);

			for( int i = 0; i < foundX.length; i++ ) {
				assertEquals("at "+i,expectedX[i],foundX[i],1e-4);
				assertEquals("at "+i,expectedY[i],foundY[i],1e-4);
			}

			for( int i = 0; i < foundX.length; i++ ) {
				assertTrue(foundX[i] != 0);
				assertTrue(foundY[i] != 0);
			}
		}
	}

	@Test
	public void gradientInner_I32() {
		GrayS32 ii = new GrayS32(width,height);
		GImageMiscOps.fillUniform(ii, rand, 0, 100);
		int r = 2;
		int w = r*2+1;

		double expectedX[] = new double[w*w];
		double expectedY[] = new double[w*w];
		int foundX[] = new int[w*w];
		int foundY[] = new int[w*w];

		for( double scale = 0.2; scale <= 1.8; scale += 0.2 ) {
			ImplSurfDescribeOps.naiveGradient(ii,10,9,scale,w, 4*scale, false, expectedX,expectedY);
			ImplSurfDescribeOps.gradientInner(ii,10,9,scale,w, 4*scale,foundX,foundY);

			for( int i = 0; i < foundX.length; i++ ) {
				assertEquals("at "+i,expectedX[i],foundX[i],1e-4);
				assertEquals("at "+i,expectedY[i],foundY[i],1e-4);
			}

			for( int i = 0; i < foundX.length; i++ ) {
				assertTrue(foundX[i] != 0);
				assertTrue(foundY[i] != 0);
			}
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
			GrayF32 img = new GrayF32(width,height);
			createGradient(theta,img);
			GrayF32 ii = IntegralImageOps.transform(img,null);

			ImplSurfDescribeOps.naiveGradient(ii,r*2,r*2,scale,w, 4*scale, false, derivX,derivY);

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
	 * Creates an image with a constant gradient in the specified direction
	 */
	public static <I extends ImageGray>
	void createGradient( double theta , I image ) {
		GImageGray ret = FactoryGImageGray.wrap(image);

		double c = Math.cos(theta);
		double s = Math.sin(theta);

		for( int y = 0; y < image.height; y++ ) {
			for( int x = 0; x < image.width; x++ ) {
				double xx = c*x;
				double yy = s*y;
				ret.set(x,y,(float)(xx+yy));
			}
		}
	}

	public static < II extends ImageGray>
	SparseScaleGradient<II,?> createGradient( II ii , double scale) {
		SparseScaleGradient<II,?> ret =
				SurfDescribeOps.createGradient(false,(Class<II>)ii.getClass());
		ret.setImage(ii);
		ret.setWidth(4);
		return ret;
	}
}
