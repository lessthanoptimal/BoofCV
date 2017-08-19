/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestChessboardPolygonHelper {

	@Test
	public void implement() {
		fail("Implement");
	}

//	@Test
//	public void filterPixelPolygon_noborder() {
//		ChessboardPolygonHelper alg = new ChessboardPolygonHelper();
//
//		List<Point2D_I32> externalUndist = new ArrayList<>();
//		List<Point2D_I32> externalDist = new ArrayList<>();
//		GrowQueue_I32 splits = new GrowQueue_I32();
//
//		splits.add(0);
//		splits.add(10);
//		splits.add(20);
//		assertFalse(alg.filterPixelPolygon(externalUndist,externalDist,splits,false));
//		splits.add(30);
//		assertTrue(alg.filterPixelPolygon(externalUndist,externalDist,splits,false));
//		splits.add(40);
//		assertFalse(alg.filterPixelPolygon(externalUndist,externalDist,splits,false));
//	}
//
//	@Test
//	public void filterPixelPolygon_border() {
//		ChessboardPolygonHelper alg = new ChessboardPolygonHelper(null,null,null);
//		alg.width = 50;
//		alg.height = 60;
//
//		List<Point2D_I32> externalUndist = new ArrayList<>();
//		List<Point2D_I32> externalDist = new ArrayList<>();
//		GrowQueue_I32 splits = new GrowQueue_I32();
//
//		for (int i = 0; i < 100; i++) {
//			externalUndist.add(new Point2D_I32(10,10));
//			externalDist.add(new Point2D_I32(10,10));
//		}
//
//		// test initially with all corners inside
//		splits.add(0);
//		splits.add(10);
//		assertFalse(alg.filterPixelPolygon(externalUndist,externalDist,splits,true));
//		splits.add(20);
//		assertTrue(alg.filterPixelPolygon(externalUndist,externalDist,splits,true));
//		splits.add(30);
//		assertTrue(alg.filterPixelPolygon(externalUndist,externalDist,splits,true));
//		// these should all fail because there are too many corners inside not touching the border
//		for (int i = 0; i < 3; i++) {
//			splits.add(40+i*10);
//			assertFalse(alg.filterPixelPolygon(externalUndist,externalDist,splits,true));
//		}
//
//		// this should pass because only 1 or 3 corners are inside
//		for (int i = 0; i < splits.size(); i++) {
//			externalDist.get(splits.get(i)).set(0,0);
//		}
//		for (int i = 0; i < 3; i++) {
//			externalDist.get(i).set(10,10);
//			assertTrue(alg.filterPixelPolygon(externalUndist,externalDist,splits,true));
//		}
//	}
//
//	@Test
//	public void filterPixelPolygon_AllBorder() {
//		ChessboardPolygonHelper alg = new ChessboardPolygonHelper(null,null,null);
//		alg.width = 50;
//		alg.height = 60;
//
//		List<Point2D_I32> externalUndist = new ArrayList<>();
//		List<Point2D_I32> externalDist = new ArrayList<>();
//		GrowQueue_I32 splits = new GrowQueue_I32();
//
//		for (int i = 0; i < 100; i++) {
//			externalUndist.add(new Point2D_I32(10,10));
//			externalDist.add(new Point2D_I32(10,10));
//		}
//
//		splits.add(0);
//		splits.add(10);
//		splits.add(20);
//
//		// nothing is actually touching the border, should be valid
//		assertTrue(alg.filterPixelPolygon(externalUndist,externalDist,splits,true));
//
//		for (int i = 0; i < splits.size(); i++) {
//			externalDist.get(splits.get(i)).set(0,0);
//		}
//		assertFalse(alg.filterPixelPolygon(externalUndist,externalDist,splits,true));
//	}
}
