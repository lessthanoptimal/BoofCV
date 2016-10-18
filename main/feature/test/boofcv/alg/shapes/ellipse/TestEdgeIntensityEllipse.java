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

package boofcv.alg.shapes.ellipse;

import boofcv.struct.image.GrayU8;
import georegression.struct.shapes.EllipseRotated_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestEdgeIntensityEllipse {
	@Test
	public void fullyInside() {
		check(new EllipseRotated_F64(50,60,10,5,0.1));
	}

	@Test
	public void partiallyInside() {
		check(new EllipseRotated_F64(5,60,10,5,0.1));
	}

	/**
	 * Basic check to see if the score is higher when dead on.
	 */
	private void check(EllipseRotated_F64 ellipse) {
		List<EllipseRotated_F64> list = new ArrayList<>();
		list.add( ellipse );

		GrayU8 image = TestBinaryEllipseDetectorPixel.renderEllipses(200,210, list, 0);

		EdgeIntensityEllipse<GrayU8> alg = new EdgeIntensityEllipse<>(1.5,20,10.0,GrayU8.class);
		alg.setImage(image);

		assertTrue(alg.process(ellipse));
		double score0 = alg.getEdgeIntensity();

		// now try it again with it offset a little.  score should go down
		ellipse.center.x += 0.75;
		assertTrue(alg.process(ellipse));
		double score1 = alg.getEdgeIntensity();

		assertTrue( score1 < score0 );

		assertTrue( score0 >= 0 && score0 <= 255);
		assertTrue( score1 >= 0 && score1 <= 255);
	}

	/**
	 * Makes sure the score stays about the same when it is inside and partially outside
	 */
	@Test
	public void scoreInsideAndOutside() {
		EllipseRotated_F64 ellipse = new EllipseRotated_F64(50,60,10,5,0.1);
		List<EllipseRotated_F64> list = new ArrayList<>();
		list.add( ellipse );

		GrayU8 image = TestBinaryEllipseDetectorPixel.renderEllipses(200,210, list, 0);

		EdgeIntensityEllipse<GrayU8> alg = new EdgeIntensityEllipse<>(1.5,20,10.0,GrayU8.class);
		alg.setImage(image);

		assertTrue(alg.process(ellipse));
		double score0 = alg.getEdgeIntensity();

		// Move it outside the image and render again
		ellipse.center.x = 5;
		image = TestBinaryEllipseDetectorPixel.renderEllipses(200,210, list, 0);
		alg.setImage(image);
		assertTrue(alg.process(ellipse));

		double score1 = alg.getEdgeIntensity();

		assertEquals( score0 , score1 , 10 );
	}
}