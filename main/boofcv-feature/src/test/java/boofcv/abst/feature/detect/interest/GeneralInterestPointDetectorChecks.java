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

package boofcv.abst.feature.detect.interest;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic high level tests for {@link InterestPointDetector}
 *
 * @author Peter Abeles
 */
public abstract class GeneralInterestPointDetectorChecks<T extends ImageGray<T>> extends BoofStandardJUnit {

	private InterestPointDetector<T> detector;

	private boolean hasOrientation;
	private boolean hasRadius;

	private T image;

	protected GeneralInterestPointDetectorChecks(InterestPointDetector<T> detector,
											  boolean hasOrientation, boolean hasRadius,
											  Class<T> imageType ) {
		configure(detector,hasOrientation, hasRadius,imageType);
	}

	protected GeneralInterestPointDetectorChecks() {}

	public void configure(InterestPointDetector<T> detector,
						  boolean hasOrientation, boolean hasRadius,
						  Class<T> imageType ) {
		this.detector = detector;
		this.hasOrientation = hasOrientation;
		this.hasRadius = hasRadius;

		// create a random input image
		image = GeneralizedImageOps.createSingleBand(imageType,60,80);
		GImageMiscOps.fillUniform(image, rand, 0, 100);
	}

	public void performAllTests() {
		checkExpectedCharacteristics();
		checkDetect();
		checkSubImage();
	}

	@Test void checkExpectedCharacteristics() {
		assertEquals(hasOrientation, detector.hasOrientation());
		assertEquals(hasRadius, detector.hasScale());
	}

	/**
	 * Detect features and see if all the expected parameters have been set
	 */
	@Test void checkDetect() {
		detector.detect(image);

		assertTrue(detector.getNumberOfFeatures() > 0);

		// number of times each of these are not zero
		int numPoint = 0;
		int numRadius = 0;
		int numYaw = 0;

		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			Point2D_F64 p = detector.getLocation(i);
			double radius = detector.getRadius(i);
			double yaw = detector.getOrientation(i);

			if( p.x != 0 && p.y != 0 )
				numPoint++;

			if( radius != 1 )
				numRadius++;

			if( yaw != 0 )
				numYaw++;
		}

		assertTrue(numPoint > 0 );

		if(hasRadius)
			assertTrue(numRadius > 0 );
		else
			assertEquals(0, numRadius);

		if( hasOrientation )
			assertTrue(numYaw > 0 );
		else
			assertEquals(0, numYaw);
	}

	/**
	 * Does it handle sub-images correctly?
	 */
	@Test void checkSubImage() {
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
			boolean matched = false;
			for (int j = 0; j < original.size(); j++) {
				Point2D_F64 f = found.get(j);
				if( f.isIdentical(o.x,o.y)) {
					matched = true;
				}
			}
			assertTrue(matched);
		}
	}


}
