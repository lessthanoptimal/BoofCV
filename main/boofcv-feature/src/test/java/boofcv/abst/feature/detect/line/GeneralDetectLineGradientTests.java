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

package boofcv.abst.feature.detect.line;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F32;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public abstract class GeneralDetectLineGradientTests extends BoofStandardJUnit {

	int width = 40;
	int height = 50;

	double toleranceLocation = 1.5;
	double toleranceAngle = 0.1;

	int lineLocation = 10;

	Class[] imageTypes;

	protected GeneralDetectLineGradientTests(Class... imageTypes) {
		this.imageTypes = imageTypes;
	}

	public abstract <T extends ImageGray<T>> DetectLine<T> createAlg(Class<T> imageType );

	/**
	 * See if it can detect an obvious line in an image.
	 */
	@Test void obviousLine() {
		for( Class c : imageTypes ) {
			obviousLine(c);
		}
	}

	/**
	 * Check to see if an subimage produces the same result as a regular image.
	 */
	@Test void subImages() {
		for( Class c : imageTypes ) {
			subImages(c);
		}
	}

	private <T extends ImageGray<T>> void obviousLine(Class<T> imageType ) {
		T input = GeneralizedImageOps.createSingleBand(imageType, width, height);

		GImageMiscOps.fillRectangle(input, 30, 0, 0, lineLocation, height);

		DetectLine<T> alg = createAlg(imageType);

		List<LineParametric2D_F32> found = alg.detect(input);

		assertTrue(found.size() >= 1);
		// see if at least one of the lines is within tolerance

		boolean foundMatch = false;
		for( LineParametric2D_F32 l : found ) {
			Point2D_F32 p = l.getPoint();
			double angle = l.getAngle();

			if( Math.abs(p.x-lineLocation) < toleranceLocation &&
				((UtilAngle.dist(Math.PI/2, angle) <= toleranceAngle) ||
					(UtilAngle.dist(-Math.PI/2, angle) <= toleranceAngle)) )
			{
				foundMatch = true;
				break;
			}
		}

		assertTrue(foundMatch);
	}

	private <T extends ImageGray<T>> void subImages(Class<T> imageType ) {
		T input = GeneralizedImageOps.createSingleBand(imageType, width, height);

		GImageMiscOps.fillRectangle(input,30,0,0,lineLocation,height);
		T sub = BoofTesting.createSubImageOf(input);

		List<LineParametric2D_F32> foundA = createAlg(imageType).detect(input);
	    List<LineParametric2D_F32> foundB = createAlg(imageType).detect(sub);

		// the output should be exactly identical
		assertEquals(foundA.size(),foundB.size());

		for( int i = 0; i < foundA.size(); i++ ) {
			LineParametric2D_F32 a = foundA.get(i);

			// When run in concurrent mode the order might be different
			boolean matched = false;
			for (int j = 0; j < foundB.size(); j++) {
				LineParametric2D_F32 b = foundB.get(j);

				if( a.slope.x == b.slope.x && a.slope.y == b.slope.y && a.p.x == b.p.x && a.p.y == b.p.y ) {
					matched = true;
					break;
				}
			}
			if( !matched )
				throw new RuntimeException();
			assertTrue(matched);
		}
	}
}
