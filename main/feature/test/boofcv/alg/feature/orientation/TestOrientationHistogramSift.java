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

package boofcv.alg.feature.orientation;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofTesting;
import georegression.metric.UtilAngle;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestOrientationHistogramSift {

	Random rand = new Random(234324);

	int width = 50;
	int height = 60;

	@Test
	public void process_easy() {
		GrayF32 derivX = new GrayF32(width, height);
		GrayF32 derivY = new GrayF32(width, height);

		process_easy(derivX,derivY);
		BoofTesting.checkSubImage(this,"process_easy",true,derivX,derivY);
	}

	public void process_easy(GrayF32 derivX, GrayF32 derivY ) {
		double theta0 = 0.5, theta1 = -1.2;
		ImageMiscOps.fill(derivX, (float) Math.cos(theta0)*1.5f);
		ImageMiscOps.fill(derivY, (float) Math.sin(theta0)*1.5f);
		GImageMiscOps.fillRectangle(derivX,(float) Math.cos(theta1),20,0,30,60);
		GImageMiscOps.fillRectangle(derivY,(float) Math.sin(theta1),20,0,30,60);

		OrientationHistogramSift<GrayF32> alg =
				new OrientationHistogramSift<>(36, 1.5,GrayF32.class);
		alg.setImageGradient(derivX, derivY);

		alg.process(20,25,4);

		assertEquals(2,alg.getOrientations().size);
		assertEquals(theta0,alg.getPeakOrientation(),1e-5);
		// the order isn't garenteed, this might need to be made more robust later on
		assertEquals(theta0,alg.getOrientations().get(0),1e-5);
		assertTrue(UtilAngle.dist(theta1,alg.getOrientations().get(1)) <= 1e-5);
	}

	/**
	 * Real basic check to see if a uniform gradient is added to the histogram.  Goes through
	 * all angles and makes sure it doesn't blow up at the border
	 */
	@Test
	public void computeHistogram() {
		GrayF32 derivX = new GrayF32(width, height);
		GrayF32 derivY = new GrayF32(width, height);
		int N = 36;
		OrientationHistogramSift<GrayF32> alg = new OrientationHistogramSift<>(N, 1.5,GrayF32.class);
		alg.setImageGradient(derivX, derivY);

		for (int degrees = 5; degrees < 360; degrees+=10) {
			double theta = UtilAngle.degreeToRadian(degrees);

			int bin = degrees * N / 360;

			ImageMiscOps.fill(derivX, (float) Math.cos(theta));
			ImageMiscOps.fill(derivY, (float) Math.sin(theta));

			alg.computeHistogram(20, 15, 3);
			checkHistogram(N, alg, theta, bin);

			alg.computeHistogram(0, 0, 3);
			checkHistogram(N, alg, theta, bin);
		}
	}

	private void checkHistogram(int n, OrientationHistogramSift alg, double theta, int index) {
		for (int i = 0; i < n; i++) {
			if (i == index) {
				double found = Math.atan2(alg.histogramY[i], alg.histogramX[i]);
				assertTrue( UtilAngle.dist(theta, found) <= 1e-5);
				assertTrue(alg.histogramMag[i] > 0);
			} else {
				assertEquals(0, alg.histogramX[i], 1e-5);
				assertEquals(0, alg.histogramY[i], 1e-5);
				assertEquals(0, alg.histogramMag[i], 1e-5);
			}
		}
	}

	@Test
	public void findHistogramPeaks() {
		OrientationHistogramSift<GrayF32> alg =
				new OrientationHistogramSift<>(36,1.5,GrayF32.class);

		int N = alg.histogramX.length;

		double expected[] = new double[N];
		for (int i = 0; i < N; i++) {
			double angle = expected[i] = UtilAngle.bound( 2.0*i*Math.PI/N );

			alg.histogramMag[i] = 1.0;
			alg.histogramX[i] = Math.cos(angle);
			alg.histogramY[i] = Math.sin(angle);
		}

		// largest local max
		alg.histogramMag[5] = 5.0;

		// should result in a single peak even though all 3 are valid
		alg.histogramMag[15] = 4.4;
		alg.histogramMag[16] = 4.5;
		alg.histogramMag[17] = 4.4;

		// not a local max since they have identical values
		alg.histogramMag[20] = 6;
		alg.histogramMag[21] = 6;

		alg.findHistogramPeaks();

		assertEquals(2,alg.getOrientations().size);
		assertEquals(expected[5],alg.getPeakOrientation(),1e-8);
		assertEquals(expected[5],alg.getOrientations().get(0),1e-8);
		assertEquals(expected[16],alg.getOrientations().get(1),1e-8);
	}

	@Test
	public void computeAngle() {
		double theta0 = 0.5, theta1 = 0.7, theta2 = 0.8;
		double dx0 = Math.cos(theta0), dx1 = Math.cos(theta1), dx2 = Math.cos(theta2);
		double dy0 = Math.sin(theta0), dy1 = Math.sin(theta1), dy2 = Math.sin(theta2);

		OrientationHistogramSift<GrayF32> alg =
				new OrientationHistogramSift<>(36,1.5,GrayF32.class);

		// repeat this test all the way around the histogram to ensure wrapping is handled correctly.
		int N = alg.histogramX.length;

		for (int i = N-2,j=N-1,k=0; k < alg.histogramX.length; i=j,j=k,k++) {
			alg.histogramX[i] = dx0; alg.histogramY[i] = dy0;
			alg.histogramX[j] = dx1; alg.histogramY[j] = dy1;
			alg.histogramX[k] = dx2; alg.histogramY[k] = dy2;

			// symmetric magnitude, no interpolation
			alg.histogramMag[i] = 1.5;
			alg.histogramMag[j] = 3.1;
			alg.histogramMag[k] = 1.5;

			// bias the results
			assertEquals(theta1,alg.computeAngle(j),1e-8);
			alg.histogramMag[i] = 2.0;
			assertTrue(alg.computeAngle(j) < theta1);
			alg.histogramMag[i] = 1.5;
			alg.histogramMag[k] = 2.0;
			assertTrue(alg.computeAngle(j) > theta1);
		}


	}

	@Test
	public void interpolateAngle() {
		double theta0 = 0.5, theta1 = 0.7, theta2 = 0.8;
		double dx0 = Math.cos(theta0), dx1 = Math.cos(theta1), dx2 = Math.cos(theta2);
		double dy0 = Math.sin(theta0), dy1 = Math.sin(theta1), dy2 = Math.sin(theta2);

		OrientationHistogramSift<GrayF32> alg =
				new OrientationHistogramSift<>(36,1.5,GrayF32.class);

		alg.histogramX[2] = dx0; alg.histogramY[2] = dy0;
		alg.histogramX[3] = dx1; alg.histogramY[3] = dy1;
		alg.histogramX[4] = dx2; alg.histogramY[4] = dy2;

		assertEquals(theta0,alg.interpolateAngle(2,3,4,-1),1e-6);
		assertEquals(theta1,alg.interpolateAngle(2,3,4, 0),1e-6);
		assertEquals(theta2,alg.interpolateAngle(2,3,4, 1),1e-6);
		assertEquals(0.6,alg.interpolateAngle(2,3,4, -0.5),1e-6);
		assertEquals(0.75,alg.interpolateAngle(2,3,4, 0.5),1e-6);
	}

	@Test
	public void computeWeight() {

		OrientationHistogramSift<GrayF32> alg = new OrientationHistogramSift<>(36,1.5,GrayF32.class);

		double sigma = 2;

		for (int i = 0; i < 50; i++) {
			double dx = (rand.nextDouble()-0.5)*3;
			double dy = (rand.nextDouble()-0.5)*3;

			double found = alg.computeWeight(dx,dy,sigma);
			double expected = exactWeight(dx,dy,sigma);

			assertEquals(expected,found,1e-3);
		}
	}

	private double exactWeight( double deltaX , double deltaY , double sigma ) {
		return Math.exp(-0.5 * ((deltaX * deltaX + deltaY * deltaY) / (sigma * sigma)));
	}

}