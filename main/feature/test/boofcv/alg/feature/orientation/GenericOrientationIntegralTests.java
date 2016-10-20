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

import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import georegression.metric.UtilAngle;

import static org.junit.Assert.assertTrue;


/**
 * Generic tests for implementers of {@link boofcv.abst.feature.orientation.OrientationGradient}.
 *
 * @author Peter Abeles
 */
public class GenericOrientationIntegralTests<T extends ImageGray> {

	int width = 30;
	int height = 40;

	int regionSize;

	// how accurate angle estimates are
	double angleTolerance;

	// the algorithm being tested
	OrientationIntegral<T> alg;

	// integral image
	T ii;

	public void setup(double angleTolerance, int regionSize ,
					  OrientationIntegral<T> alg , Class<T> imageType ) {
		this.angleTolerance = angleTolerance;
		this.regionSize = regionSize;
		this.alg = alg;

		ii = GeneralizedImageOps.createSingleBand(imageType, width, height);
	}

	/**
	 * Performs all the tests, but the weighted test.
	 */
	public void performAll() {
		performEasyTests();
		setScale();
		checkSubImages();
		checkBorderExplode();
	}

	/**
	 * Tests involving the image border
	 */
	public void checkBorderExplode() {
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
	 * Points all pixels in the surrounding region in same direction.  Then sees if the found
	 * direction for the region is in the expected direction.
	 */
	public void performEasyTests() {

		alg.setObjectRadius(10);

		int N = 2*(int)(Math.PI/angleTolerance);

		int x = width/2;
		int y = height/2;

		for( int i = 0; i < N; i++ ) {
			double angle = UtilAngle.bound(i*angleTolerance);
			createOrientedImage(angle);

			alg.setImage(ii);

			double found = UtilAngle.bound(alg.compute(x,y));
			assertTrue( angle+" "+found,UtilAngle.dist(angle,found) < angleTolerance );
		}
	}

	/**
	 * Estimate the direction at a couple of different scales and see if it produces the expected results.
	 */
	public void setScale() {
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
	public void checkSubImages() {
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
		assertTrue( angle+" "+found,UtilAngle.dist(angle,found) < angleTolerance );
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
}
