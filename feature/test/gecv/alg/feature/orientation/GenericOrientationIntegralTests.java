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

package gecv.alg.feature.orientation;

import gecv.alg.transform.ii.GIntegralImageOps;
import gecv.core.image.GeneralizedImageOps;
import gecv.struct.image.ImageBase;
import jgrl.metric.UtilAngle;

import static org.junit.Assert.assertTrue;


/**
 * Generic tests for implementers of {@link OrientationGradient}.
 *
 * @author Peter Abeles
 */
public class GenericOrientationIntegralTests<T extends ImageBase> {

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

		ii = GeneralizedImageOps.createImage(imageType,width,height);
	}

	/**
	 * Performs all the tests, but the weighted test.
	 */
	public void performAll() {
		performEasyTests();
		setScale();
	}

	/**
	 * Points all pixels in the surrounding region in same direction.  Then sees if the found
	 * direction for the region is in the expected direction.
	 */
	public void performEasyTests() {

		alg.setScale(1);

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
		alg.setScale(1);

		double found = UtilAngle.bound(alg.compute(x,y));
		assertTrue( UtilAngle.dist(angle,found) < angleTolerance );

		alg.setScale(1.5);
		found = UtilAngle.bound(alg.compute(x,y));
		assertTrue( UtilAngle.dist(angle,found) < angleTolerance );

		alg.setScale(0.5);
		found = UtilAngle.bound(alg.compute(x,y));
		assertTrue( UtilAngle.dist(angle,found) < angleTolerance );
	}

	/**
	 * Creates an integral image where the whole image has a gradient in the specified direction.
	 * @param angle
	 */
	private void createOrientedImage( double angle ) {
		T input = (T)ii._createNew(width,height);

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
