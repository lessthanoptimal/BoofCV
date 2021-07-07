/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.abst.feature.orientation.RegionOrientation;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import georegression.metric.UtilAngle;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Generic tests for implementers of {@link boofcv.abst.feature.orientation.OrientationGradient}.
 *
 * @author Peter Abeles
 */
public abstract class GenericOrientationIntegralTests<T extends ImageGray<T>> extends GenericOrientationTests {

	int width = 30;
	int height = 40;

	int regionSize;

	// how accurate angle estimates are
	double angleTolerance;

	// the algorithm being tested
	OrientationIntegral<T> alg;

	// integral image
	T ii;

	protected GenericOrientationIntegralTests(double angleTolerance, int regionSize, Class imageType) {
		super(imageType);

		Class<T> integralType = GIntegralImageOps.getIntegralType(imageType);

		this.angleTolerance = angleTolerance;
		this.regionSize = regionSize;
		ii = GeneralizedImageOps.createSingleBand(integralType, width, height);
	}

	@Override
	protected void setRegionOrientation(RegionOrientation alg) {
		super.setRegionOrientation(alg);
		this.alg = (OrientationIntegral<T>)alg;
	}

	/**
	 * Tests involving the image border
	 */
	@Test
	void checkBorderExplode() {
		alg.setObjectRadius(10);

		createOrientedImage(0);
		alg.setImage(ii);

		// just see if it blows up when tracing along the image broder
		for( int y = 0; y < height; y++ ) {
			alg.compute(0,y);
			alg.compute(width-1,y);
		}
		for( int x = 0; x < width; x++ ) {
			alg.compute(x,0);
			alg.compute(x,height-1);
		}
	}

	/**
	 * Points all pixels in the surrounding region in same direction. Then sees if the found
	 * direction for the region is in the expected direction.
	 */
	@Test
	void performEasyTests() {

		alg.setObjectRadius(10);

		int N = 2*(int)(Math.PI/angleTolerance);

		int x = width/2;
		int y = height/2;

		for( int i = 0; i < N; i++ ) {
			double angle = UtilAngle.bound(i*angleTolerance);
			createOrientedImage(angle);

			alg.setImage(ii);

			double found = UtilAngle.bound(alg.compute(x,y));
			assertTrue(UtilAngle.dist(angle,found) < angleTolerance );
		}
	}

	/**
	 * Estimate the direction at a couple of different scales and see if it produces the expected results.
	 */
	@Test
	void setScale() {
		int x = width/2;
		int y = height/2;

		int N = 2*(int)(Math.PI/angleTolerance);
		double angle = UtilAngle.bound((N/2)*angleTolerance);

		createOrientedImage(angle);

		alg.setImage(ii);
		alg.setObjectRadius(10);

		double found = UtilAngle.bound(alg.compute(x,y));
		assertTrue( UtilAngle.dist(angle,found) < angleTolerance );

		alg.setObjectRadius(15);
		found = UtilAngle.bound(alg.compute(x,y));
		assertTrue( UtilAngle.dist(angle,found) < angleTolerance );

		alg.setObjectRadius(7.5);
		found = UtilAngle.bound(alg.compute(x,y));
		assertTrue( UtilAngle.dist(angle,found) < angleTolerance );
	}

	/**
	 * See if it can handle sub-images correctly
	 */
	@Test
	void checkSubImages() {
		double angle = 0.5;
		createOrientedImage(angle);
		// set the border of the image to zeros to screw up orientation estimation
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				if( j >= regionSize || i >= regionSize )
					GeneralizedImageOps.set(ii,j,i,0);
			}
		}


		T sub = (T)ii.subimage(0,0,regionSize,regionSize, null);

		alg.setImage(sub);
		alg.setObjectRadius(regionSize/2);

		double found = UtilAngle.bound(alg.compute(sub.width/2,sub.height/2));
		assertTrue(UtilAngle.dist(angle,found) < angleTolerance );
	}

	@Test
	void checkCopy() {
		// random image
		T input = (T)ii.createNew(width,height);
		GImageMiscOps.fillUniform(input,rand,0,100);
		GIntegralImageOps.transform(input,ii);

		alg.setImage(input);
		alg.setObjectRadius(regionSize/2);

		OrientationIntegral<T> algCopy = (OrientationIntegral)alg.copy();
		algCopy.setImage(input);
		algCopy.setObjectRadius(regionSize/2);

		for (int i = 0; i < 100; i++) {
			int x = rand.nextInt(width);
			int y = rand.nextInt(height);

			double expected = alg.compute(x,y);
			double found = algCopy.compute(x,y);

			assertEquals(expected,found, UtilEjml.TEST_F64);
		}
	}

	/**
	 * Creates an integral image where the whole image has a gradient in the specified direction.
	 * @param angle
	 */
	private void createOrientedImage( double angle ) {
		T input = (T)ii.createNew(width,height);

		double c = Math.cos(angle);
		double s = Math.sin(angle);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				double val = 10*(x*c + y*s);
				GeneralizedImageOps.set(input,x,y,val);
			}
		}

		GIntegralImageOps.transform(input,ii);
	}

	@Override
	protected void setImage(RegionOrientation alg, ImageGray image) {
		T input = (T)ii.createNew(width,height);
		GIntegralImageOps.transform(input,ii);
		((OrientationIntegral<T>)alg).setImage(ii);
	}
}
