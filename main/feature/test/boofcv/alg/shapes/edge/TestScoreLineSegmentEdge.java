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

package boofcv.alg.shapes.edge;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Peter Abeles
 */
public class TestScoreLineSegmentEdge {

	Random rand = new Random(234);

	@Test
	public void checkIsAverage() {
		int value = 200;
		GrayU8 image = new GrayU8(400,500);

		ImageMiscOps.fillRectangle(image, value, 20, 30, 40, 40);

		ScoreLineSegmentEdge<GrayU8> alg = new ScoreLineSegmentEdge<>(20,GrayU8.class);
		alg.setImage(image);

		// compute the edge along the top edge
		double found = alg.computeAverageDerivative(new Point2D_F64(22, 30), new Point2D_F64(52, 30), 0, 1);
		assertEquals(value,found,1e-8);

		// change the tangent direction
		found = alg.computeAverageDerivative(new Point2D_F64(22, 30), new Point2D_F64(52, 30), 0, -1);
		assertEquals(-value,found,1e-8);

		found = alg.computeAverageDerivative(new Point2D_F64(22, 30), new Point2D_F64(52, 30), 1, 0);
		assertEquals(0,found,1e-8);
	}

	/**
	 * makes sure it handles the image border correctly and doens't blow up
	 */
	@Test
	public void checkImageBorder() {
		GrayU8 image = new GrayU8(400,500);

		ImageMiscOps.fillUniform(image,rand,0,255);

		ScoreLineSegmentEdge<GrayU8> alg = new ScoreLineSegmentEdge<>(20,GrayU8.class);
		alg.setImage(image);

		// all along image border, check each border
		double found = alg.computeAverageDerivative(new Point2D_F64(0, 0), new Point2D_F64(10, 0), 0, 1);
		assertEquals(0, found, 1e-8);

		found = alg.computeAverageDerivative(new Point2D_F64(0, 0), new Point2D_F64(0, 10), 1, 0);
		assertEquals(0, found, 1e-8);

		found = alg.computeAverageDerivative(new Point2D_F64(image.width-1,0),
				new Point2D_F64(image.width-1, 10), 1, 0);
		assertEquals(0, found, 1e-8);

		found = alg.computeAverageDerivative(new Point2D_F64(0,image.height-1),
				new Point2D_F64(10,image.height-1), 0, 1);
		assertEquals(0, found, 1e-8);

		// check sanity check to see that it's not just aborting
		found = alg.computeAverageDerivative(new Point2D_F64(0, 0), new Point2D_F64(10, 10), 0, 1);
		assertNotEquals(0, found, 1e-8);
	}
}
