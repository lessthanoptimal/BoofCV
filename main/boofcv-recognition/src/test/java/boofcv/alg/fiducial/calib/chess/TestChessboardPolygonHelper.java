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

package boofcv.alg.fiducial.calib.chess;

import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.GrowQueue_B;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestChessboardPolygonHelper {

	@Test
	public void filterPixelPolygon_noborder() {
		ChessboardPolygonHelper alg = new ChessboardPolygonHelper();

		assertFalse(alg.filterPixelPolygon(null,new Polygon2D_F64(3),null,false));
		assertTrue(alg.filterPixelPolygon(null,new Polygon2D_F64(4),null,false));
		assertFalse(alg.filterPixelPolygon(null,new Polygon2D_F64(5),null,false));
	}

	@Test
	public void filterPixelPolygon_border() {
		ChessboardPolygonHelper alg = new ChessboardPolygonHelper();

		Polygon2D_F64 distorted = new Polygon2D_F64(2);
		GrowQueue_B touches = new GrowQueue_B();

		// test initially with all corners inside
		touches.add(false);
		touches.add(false);
		assertFalse(alg.filterPixelPolygon(null,distorted,touches,true));
		distorted.vertexes.resize(3);
		touches.add(false);
		assertTrue(alg.filterPixelPolygon(null,distorted,touches,true));
		distorted.vertexes.resize(3);
		touches.add(false);
		assertTrue(alg.filterPixelPolygon(null,distorted,touches,true));

		// this should pass because more than one corner is inside
		for (int i = 0; i < touches.size(); i++) {
			touches.set(i,true);
		}
		for (int i = 0; i < 3; i++) {
			touches.set(i,false);
			assertTrue(alg.filterPixelPolygon(null,distorted,touches,true));
		}
	}

	@Test
	public void filterPixelPolygon_AllBorder() {
		ChessboardPolygonHelper alg = new ChessboardPolygonHelper();

		Polygon2D_F64 distorted = new Polygon2D_F64(3);
		GrowQueue_B touches = new GrowQueue_B();

		touches.add(false);
		touches.add(false);
		touches.add(false);

		// nothing is actually touching the border, should be valid
		assertTrue(alg.filterPixelPolygon(null,distorted,touches,true));

		touches.set(0,true);
		touches.set(1,true);
		touches.set(2,true);

		assertFalse(alg.filterPixelPolygon(null,distorted,touches,true));
	}
}
