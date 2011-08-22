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

import gecv.alg.feature.describe.RegionOrientation;
import gecv.core.image.GeneralizedImageOps;
import gecv.struct.image.ImageBase;
import jgrl.metric.UtilAngle;

import static org.junit.Assert.assertTrue;


/**
 * Generic tests for implementers of {@link gecv.alg.feature.describe.RegionOrientation}.
 *
 * @author Peter Abeles
 */
public class GenericOrientationTests<D extends ImageBase> {

	int width = 30;
	int height = 40;

	int regionSize;

	// how accurate angle estimates are
	double angleTolerance;

	// the algorithm being tested
	RegionOrientation<D> alg;

	// data used to store image derivatives
	D derivX,derivY;

	public void setup(double angleTolerance, int regionSize , RegionOrientation<D> alg) {
		this.angleTolerance = angleTolerance;
		this.alg = alg;
		this.regionSize = regionSize;

		Class<D> imageType = alg.getImageType();

		derivX = GeneralizedImageOps.createImage(imageType,width,height);
		derivY = GeneralizedImageOps.createImage(imageType,width,height);
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
			double angle = UtilAngle.bound(N*angleTolerance);
			double c = Math.cos(angle);
			double s = Math.sin(angle);

			GeneralizedImageOps.fill(derivX,0);
			GeneralizedImageOps.fill(derivY,0);
			// only fill in the around around the region so that it also checks to see if the estimate
			// is localized
			GeneralizedImageOps.fillRectangle(derivX,c*100,x-regionSize/2,y-regionSize/2,regionSize,regionSize);
			GeneralizedImageOps.fillRectangle(derivY,s*100,x-regionSize/2,y-regionSize/2,regionSize,regionSize);

			alg.setImage(derivX,derivY);

			double found = UtilAngle.bound(alg.compute(x,y));
			assertTrue( UtilAngle.dist(angle,found) < angleTolerance );
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
		double c = Math.cos(angle);
		double s = Math.sin(angle);

		GeneralizedImageOps.fill(derivX,c*100);
		GeneralizedImageOps.fill(derivY,s*100);

		alg.setImage(derivX,derivY);
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
}
