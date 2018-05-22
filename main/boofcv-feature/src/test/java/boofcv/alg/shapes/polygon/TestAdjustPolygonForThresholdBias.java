/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.shapes.polygon;

import georegression.struct.shapes.Polygon2D_F64;
import org.ejml.UtilEjml;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestAdjustPolygonForThresholdBias {
	@Test
	public void adjustForThresholdBias() {
		AdjustPolygonForThresholdBias alg = new AdjustPolygonForThresholdBias();

		Polygon2D_F64 original = new Polygon2D_F64(10,10, 10,40, 35,40, 35,10);
		Polygon2D_F64 shape = original.copy();
		alg.process(shape,true);

		assertTrue( shape.get(0).distance(10,10) <= UtilEjml.EPS );
		assertTrue( shape.get(1).distance(10,41) <= UtilEjml.EPS );
		assertTrue( shape.get(2).distance(36,41) <= UtilEjml.EPS );
		assertTrue( shape.get(3).distance(36,10) <= UtilEjml.EPS );

		shape = original.copy();
		shape.flip();
		alg.process(shape,false);

		assertTrue( shape.get(0).distance(10,10) <= UtilEjml.EPS );
		assertTrue( shape.get(3).distance(10,41) <= UtilEjml.EPS );
		assertTrue( shape.get(2).distance(36,41) <= UtilEjml.EPS );
		assertTrue( shape.get(1).distance(36,10) <= UtilEjml.EPS );
	}

	/**
	 * Create a situation where a point will be shifted on top of an existing one. See if that is caught
	 * and removed. Not trivial to reproduce with real data but does happen.
	 */
	@Test
	public void removeDuplicatePoint() {

		AdjustPolygonForThresholdBias alg = new AdjustPolygonForThresholdBias();

		Polygon2D_F64 original = new Polygon2D_F64(10,10, 10,9, 9,9, 20,9, 20,0, 11,0);
		original.flip();
		Polygon2D_F64 shape = original.copy();
		alg.process(shape,true);

		assertEquals(original.size()-1,shape.size());
	}
}
