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

import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import georegression.metric.UtilAngle;

import static org.junit.Assert.assertTrue;


/**
 * Generic tests for implementers of {@link boofcv.abst.feature.orientation.OrientationGradient}.
 *
 * @author Peter Abeles
 */
public class GenericOrientationImageTests<T extends ImageGray> {

	int width = 30;
	int height = 40;

	int regionSize;

	// how accurate angle estimates are
	double angleTolerance;

	// the algorithm being tested
	OrientationImage<T> alg;

	// integral image
	T image;

	public void setup(double angleTolerance, int regionSize ,
					  OrientationImage<T> alg , Class<T> imageType ) {
		this.angleTolerance = angleTolerance;
		this.regionSize = regionSize;
		this.alg = alg;

		image = GeneralizedImageOps.createSingleBand(imageType, width, height);
	}

	/**
	 * Performs all the tests, but the weighted test.
	 */
	public void performAll() {
		performEasyTests();
		setRadius();
		checkSubImages();
	}

	/**
	 * Points all pixels in the surrounding region in same direction.  Then sees if the found
	 * direction for the region is in the expected direction.
	 */
	public void performEasyTests() {

		alg.setObjectRadius(5);

		int N = 2*(int)(Math.PI/angleTolerance);

		int x = width/2;
		int y = height/2;

		for( int i = 0; i < N; i++ ) {
			double angle = UtilAngle.bound(i*angleTolerance);
			createOrientedImage(angle);

			alg.setImage(image);

			double found = UtilAngle.bound(alg.compute(x,y));
			assertTrue( angle+" "+found,UtilAngle.dist(angle,found) < angleTolerance );
		}
	}

	/**
	 * Estimate the direction at a couple of different scales and see if it produces the expected results.
	 */
	public void setRadius() {
		int x = width/2;
		int y = height/2;

		int N = 2*(int)(Math.PI/angleTolerance);
		double angle = UtilAngle.bound((N/2)*angleTolerance);

		createOrientedImage(angle);

		alg.setImage(image);
		alg.setObjectRadius(5);

		double found = UtilAngle.bound(alg.compute(x,y));
		assertTrue( UtilAngle.dist(angle,found) < angleTolerance );

		alg.setObjectRadius(10);
		found = UtilAngle.bound(alg.compute(x,y));
		assertTrue( UtilAngle.dist(angle,found) < angleTolerance );

		alg.setObjectRadius(2.5);
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
					GeneralizedImageOps.set(image,j,i,0);
			}
		}


		T sub = (T)image.subimage(0,0,regionSize,regionSize, null);

		alg.setObjectRadius(regionSize/3.0);
		alg.setImage(sub);

		double found = UtilAngle.bound(alg.compute(sub.width/2,sub.height/2));
		assertTrue( angle+" "+found,UtilAngle.dist(angle,found) < angleTolerance );
	}

	/**
	 * Creates an integral image where the whole image has a gradient in the specified direction.
	 * @param angle
	 */
	private void createOrientedImage( double angle ) {
		double c = Math.cos(angle);
		double s = Math.sin(angle);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				double val = 125+2*(x*c + y*s);
				if( val < 0 || val > 255) {
					throw new RuntimeException("Value is out of bounds for U8");
				}
				GeneralizedImageOps.set(image,x,y,val);
			}
		}
	}
}
