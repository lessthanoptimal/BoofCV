/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.orientation;

import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageFloat32;
import georegression.metric.UtilAngle;
import org.ddogleg.struct.GrowQueue_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestOrientationHistogramSift {

	int width = 100;
	int height = 120;

	SiftImageScaleSpace ss =
			new SiftImageScaleSpace(1.6f,5,4,false);

	// accuracy tolerance
	double tol =  1e-5;//2*Math.PI/36.0;

	public TestOrientationHistogramSift() {
		ss.constructPyramid( new ImageFloat32(width,height));
		ss.computeDerivatives();
	}

	@Test
	public void checkSinglePeak() {

		OrientationHistogramSift alg = new OrientationHistogramSift(36,2.5,1.5);

		for( int i = 0; i < 100; i++ ) {
			double theta = 2*Math.PI*i/100.0;
			theta = UtilAngle.bound(theta);

			double dx = Math.cos(theta);
			double dy = Math.sin(theta);

			setAllDerivatives(dx,dy);

			alg.setScaleSpace(ss);
			alg.process(40,42,8);

			GrowQueue_F64 found = alg.getOrientations();

			assertEquals(1,found.size);

			double error = Math.abs(UtilAngle.distHalf(theta, found.get(0)));
			assertTrue("i = "+i+" theta = "+theta+" found "+found.get(0),error <= tol);
		}
	}

	/**
	 * See if it works along the image border.
	 */
	@Test
	public void checkImageBorders() {
		OrientationHistogramSift alg = new OrientationHistogramSift(36,2.5,1.5);

		GrowQueue_F64 found = alg.getOrientations();

		double theta = 1.2;
		double dx = Math.cos(theta);
		double dy = Math.sin(theta);
		setAllDerivatives(dx,dy);

		alg.setScaleSpace(ss);

		alg.process(0,0,8);
		assertEquals(1,found.size);
		assertTrue(Math.abs(UtilAngle.distHalf(theta, found.get(0))) <= tol);

		alg.process(width-1,0,8);
		assertEquals(1,found.size);
		assertTrue(Math.abs(UtilAngle.distHalf(theta, found.get(0))) <= tol);

		alg.process(width-1,height-1,8);
		assertEquals(1,found.size);
		assertTrue(Math.abs(UtilAngle.distHalf(theta, found.get(0))) <= tol);

		alg.process(0,height-1,8);
		assertEquals(1,found.size);
		assertTrue(Math.abs(UtilAngle.distHalf(theta, found.get(0))) <= tol);

	}

	private void setAllDerivatives( double dx , double dy ) {
		int N = ss.getNumOctaves()*ss.getNumScales();

		for( int i = 0; i < N; i++ ) {
			ImageMiscOps.fill(ss.getDerivativeX(i),(float)dx);
			ImageMiscOps.fill(ss.getDerivativeY(i),(float)dy);
		}
	}

	/**
	 * Create two solutions by having two pixels with different values
	 */
	@Test
	public void checkMultipleSolutions() {

		OrientationHistogramSift alg = new OrientationHistogramSift(36,2.5,1.5);

		double theta0 = 1.2;
		double c0 = Math.cos(theta0);
		double s0 = Math.sin(theta0);
		double theta1 = 2.1;
		double c1 = Math.cos(theta1);
		double s1 = Math.sin(theta1);

		int N = ss.getNumOctaves()*ss.getNumScales();

		for( int i = 0; i < N; i++ ) {
			ImageFloat32 img = ss.getPyramidLayer(i);

			int x = img.width/2;
			int y = img.height/2;

			ss.getDerivativeX(i).set(x, y,     (float) c0);
			ss.getDerivativeY(i).set(x, y,     (float) s0);
			ss.getDerivativeX(i).set(x, y + 1, (float) c1);
			ss.getDerivativeY(i).set(x, y + 1, (float) s1);
		}

		alg.setScaleSpace(ss);
		alg.process(width/2,height/2,3);

		GrowQueue_F64 found = alg.getOrientations();
		assertEquals(2,found.size);

		// assume the order for now
		assertTrue(Math.abs(UtilAngle.distHalf(1.2, found.get(0))) <= tol);
		assertTrue(Math.abs(UtilAngle.distHalf(2.1, found.get(1))) <= tol);
	}
}
