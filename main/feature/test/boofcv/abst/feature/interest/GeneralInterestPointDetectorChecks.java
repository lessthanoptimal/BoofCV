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

package boofcv.abst.feature.interest;

import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Basic high level tests for {@link InterestPointDetector}
 *
 * @author Peter Abeles
 */
public abstract class GeneralInterestPointDetectorChecks<T extends ImageGray> {

	private Random rand = new Random(234);

	private InterestPointDetector<T> detector;

	private boolean hasOrientation;
	private boolean hasScale;

	private T image;

	public GeneralInterestPointDetectorChecks(InterestPointDetector<T> detector,
											  boolean hasOrientation, boolean hasScale,
											  Class<T> imageType ) {
		configure(detector,hasOrientation,hasScale,imageType);
	}

	public GeneralInterestPointDetectorChecks() {
	}

	public void configure(InterestPointDetector<T> detector,
						  boolean hasOrientation, boolean hasScale,
						  Class<T> imageType ) {
		this.detector = detector;
		this.hasOrientation = hasOrientation;
		this.hasScale = hasScale;

		// create a random input image
		image = GeneralizedImageOps.createSingleBand(imageType,60,80);
		GImageMiscOps.fillUniform(image, rand, 0, 100);
	}

	public void performAllTests() {
		checkExpectedCharacteristics();
		checkDetect();
		checkSubImage();
	}

	@Test
	public void checkExpectedCharacteristics() {
		assertTrue(hasOrientation == detector.hasOrientation());
		assertTrue(hasScale == detector.hasScale());
	}

	/**
	 * Detect features and see if all the expected parameters have been set
	 */
	@Test
	public void checkDetect() {
		detector.detect(image);

		assertTrue(detector.getNumberOfFeatures() > 0);

		// number of times each of these are not zero
		int numPoint = 0;
		int numScale = 0;
		int numYaw = 0;

		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			Point2D_F64 p = detector.getLocation(i);
			double radius = detector.getRadius(i);
			double yaw = detector.getOrientation(i);

			if( p.x != 0 && p.y != 0 )
				numPoint++;

			if( radius != 1 )
				numScale++;

			if( yaw != 0 )
				numYaw++;
		}

		assertTrue(numPoint > 0 );

		if( hasScale )
			assertTrue(numScale > 0 );
		else
			assertTrue(numScale == 0);

		if( hasOrientation )
			assertTrue(numYaw > 0 );
		else
			assertTrue(numYaw == 0);
	}

	/**
	 * Does it handle sub-images correctly?
	 */
	@Test
	public void checkSubImage() {
		List<Point2D_F64> original = new ArrayList<>();
		List<Point2D_F64> found = new ArrayList<>();

		detector.detect(image);

		assertTrue(detector.getNumberOfFeatures() > 0);

		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			Point2D_F64 p = detector.getLocation(i);

			original.add( p.copy() );
		}

		T subimage = BoofTesting.createSubImageOf(image);

		detector.detect(subimage);

		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			Point2D_F64 p = detector.getLocation(i);

			found.add( p.copy() );
		}

		// see if processing the two images produces the same results
		assertEquals(original.size(),found.size());
		for( int i = 0; i < original.size(); i++ ) {
			Point2D_F64 o = original.get(i);
			Point2D_F64 f = found.get(i);

			assertTrue(o.x == f.x);
			assertTrue(o.y == f.y);
		}
	}


}
