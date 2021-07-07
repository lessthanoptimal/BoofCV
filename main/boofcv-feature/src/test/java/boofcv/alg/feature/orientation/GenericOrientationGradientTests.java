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

import boofcv.abst.feature.orientation.OrientationGradient;
import boofcv.abst.feature.orientation.RegionOrientation;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import georegression.metric.UtilAngle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Generic tests for implementers of {@link boofcv.abst.feature.orientation.OrientationGradient}.
 *
 * @author Peter Abeles
 */
public abstract class GenericOrientationGradientTests<D extends ImageGray<D>> extends GenericOrientationTests {

	protected static final int r = 3;

	int width = 30;
	int height = 40;

	int regionSize;

	// how accurate angle estimates are
	double angleTolerance;

	// the algorithm being tested
	OrientationGradient<D> alg;

	// data used to store image derivatives
	D derivX,derivY;

	protected GenericOrientationGradientTests(double angleTolerance, int regionSize, Class imageType) {
		super(imageType);
		this.angleTolerance = angleTolerance;
		this.regionSize = regionSize;
	}

	@Override
	protected void setRegionOrientation(RegionOrientation alg) {
		super.setRegionOrientation(alg);
		this.alg = (OrientationGradient<D>)alg;
		Class<D> derivType = this.alg.getImageType();
		derivX = GeneralizedImageOps.createSingleBand(derivType, width, height);
		derivY = GeneralizedImageOps.createSingleBand(derivType, width, height);
	}

	/**
	 * Points all pixels in the surrounding region in same direction. Then sees if the found
	 * direction for the region is in the expected direction.
	 */
	@Test
	void performEasyTests() {

		alg.setObjectRadius(1);

		int N = 2*(int)(Math.PI/angleTolerance);

		int x = width/2;
		int y = height/2;

		for( int i = 0; i < N; i++ ) {
			double angle = UtilAngle.bound(i*angleTolerance);
			double c = Math.cos(angle);
			double s = Math.sin(angle);

			GImageMiscOps.fill(derivX,0);
			GImageMiscOps.fill(derivY, 0);
			// only fill in the around around the region so that it also checks to see if the estimate
			// is localized
			GImageMiscOps.fillRectangle(derivX,c*100,x-regionSize/2,y-regionSize/2,regionSize,regionSize);
			GImageMiscOps.fillRectangle(derivY,s*100,x-regionSize/2,y-regionSize/2,regionSize,regionSize);

			alg.setImage(derivX,derivY);

			double found = UtilAngle.bound(alg.compute(x,y));
			assertTrue( UtilAngle.dist(angle,found) < angleTolerance );
		}
	}

	/**
	 * See if it can handle sub-images correctly
	 */
	@Test void checkSubImages() {
		double angle = 0.5;
		double c = Math.cos(angle);
		double s = Math.sin(angle);

		GImageMiscOps.fill(derivX,0);
		GImageMiscOps.fill(derivY,0);
		GImageMiscOps.fillRectangle(derivX,c*100,5,0,regionSize+5,regionSize);
		GImageMiscOps.fillRectangle(derivY,s*100,5,0,regionSize+5,regionSize);

		D subX = (D)derivX.subimage(5,0,regionSize+5,regionSize, null);
		D subY = (D)derivY.subimage(5,0,regionSize+5,regionSize, null);

		alg.setImage(subX,subY);

		double found = UtilAngle.bound(alg.compute(subX.width/2,subY.height/2));
		assertTrue( UtilAngle.dist(angle,found) < angleTolerance );
	}

	/**
	 * Estimate the direction at a couple of different scales and see if it produces the expected results.
	 */
	@Test void setScale() {
		int x = width/2;
		int y = height/2;

		int N = 2*(int)(Math.PI/angleTolerance);
		double angle = UtilAngle.bound((N/2)*angleTolerance);
		double c = Math.cos(angle);
		double s = Math.sin(angle);

		GImageMiscOps.fill(derivX,c*100);
		GImageMiscOps.fill(derivY,s*100);

		alg.setImage(derivX,derivY);
		alg.setObjectRadius(10);

		double found = UtilAngle.bound(alg.compute(x,y));
		assertTrue( UtilAngle.dist(angle,found) < angleTolerance );

		alg.setObjectRadius(15);
		found = UtilAngle.bound(alg.compute(x,y));
		assertTrue( UtilAngle.dist(angle,found) < angleTolerance );

		alg.setObjectRadius(5);
		found = UtilAngle.bound(alg.compute(x,y));
		assertTrue( UtilAngle.dist(angle,found) < angleTolerance );
	}

	@Override
	protected void setImage(RegionOrientation alg, ImageGray image) {

		// The code below is a hack. S16 to S32 doesn't current exist in the code (making this test of dubious value)
		// the exact values don't matter
		GImageMiscOps.fillUniform(derivX,rand,0,100);
		GImageMiscOps.fillUniform(derivY,rand,0,100);

//		GImageDerivativeOps.gradient(DerivativeType.SOBEL,image,derivX,derivY, BorderType.EXTENDED);

		((OrientationGradient<D>)alg).setImage(derivX,derivY);
	}
}
