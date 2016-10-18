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

package boofcv.alg.shapes.polyline;

import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestMinimizeEnergyPrune {

	/**
	 * Perfect case.  See if it does nothing
	 */
	@Test
	public void prine_no_change() {
		List<Point2D_I32> contours = createSquare(10,12,20,30);
		GrowQueue_I32 corners = createSquareCorners(10, 12, 20, 30);

		MinimizeEnergyPrune alg = new MinimizeEnergyPrune(1);

		GrowQueue_I32 output = new GrowQueue_I32();
		alg.prune(contours, corners, output);

		assertEquals(corners.size(),output.size());
		for (int i = 0; i < corners.size(); i++) {
			assertEquals(corners.get(i),output.get(i));
		}
	}

	/**
	 * Adds an obviously redundant corner and see if it gets removed
	 */
	@Test
	public void prune_obvious() {
		List<Point2D_I32> contours = createSquare(10,12,20,30);
		GrowQueue_I32 corners = createSquareCorners(10, 12, 20, 30);
		corners.add(corners.get(3)+4);

		MinimizeEnergyPrune alg = new MinimizeEnergyPrune(1);

		GrowQueue_I32 output = new GrowQueue_I32();
		alg.prune(contours, corners, output);

		assertEquals(4, output.size());

		// see if the two sets of corners are equivalent, taking in account the possibility of a rotation
		checkMatched(corners, output);

	}

	@Test
	public void energyRemoveCorner() {
		List<Point2D_I32> contours = createSquare(10,12,20,30);
		GrowQueue_I32 corners = createSquareCorners(10, 12, 20, 30);

		MinimizeEnergyPrune alg = new MinimizeEnergyPrune(1);
		alg.contour = contours;

		alg.computeSegmentEnergy(corners);

		// compute the energy with the skipped corner
		double expected = 0;
		for (int i = 0; i < 4; i++) {
			expected += alg.energySegment[i];
		}

		// add the corner which is going to be skipped
		corners.add(corners.get(3)+4);
		alg.computeSegmentEnergy(corners);

		double found = alg.energyRemoveCorner(4,corners);
		assertEquals(expected,found,1e-8);

		// add it in another location
		corners.removeTail();
		corners.insert(3,corners.get(2)+3);
		alg.computeSegmentEnergy(corners);
		found = alg.energyRemoveCorner(3,corners);
		assertEquals(expected,found,1e-8);

		// skip a different corner and the energy should go up
		corners = createSquareCorners(10, 12, 20, 30);
		for (int i = 0; i < 4; i++) {
			assertTrue(expected<alg.energyRemoveCorner(i,corners));
		}
	}

	@Test
	public void computeSegmentEnergy() {

		List<Point2D_I32> contours = createSquare(10,12,20,30);
		GrowQueue_I32 corners = createSquareCorners(10, 12, 20, 30);

		// test with everything perfectly lining up
		MinimizeEnergyPrune alg = new MinimizeEnergyPrune(1);
		alg.contour = contours;

		double split = alg.splitPenalty;

		double expected[] = new double[]{split/100.0,split/(18.0*18.0),split/100.0,split/(18.0*18.0)};

		for (int i = 0, j = corners.size()-1; i < corners.size(); j=i,i++) {
			double found = alg.computeSegmentEnergy(corners,j,i);
			assertEquals(expected[j],found,1e-8);
		}

		// Now make the corners less than perfect and see if the energy increases
		corners.set(1,corners.get(1)+1);
		corners.set(3,corners.get(3)+1);

		for (int i = 0, j = corners.size()-1; i < corners.size(); j=i,i++) {
			double found = alg.computeSegmentEnergy(corners,j,i);
			assertTrue(expected[j] < found);
		}
	}

	@Test
	public void removeDuplicates() {
		List<Point2D_I32> contours = createSquare(10,12,20,30);

		GrowQueue_I32 corners = createSquareCorners(10, 12, 20, 30);

		corners.add(corners.get(0));
		corners.add(corners.get(2));

		MinimizeEnergyPrune alg = new MinimizeEnergyPrune(4);
		alg.contour = contours;
		alg.removeDuplicates(corners);

		checkMatched(createSquareCorners(10, 12, 20, 30),corners);
	}

	private List<Point2D_I32> createSquare( int x0 , int y0 , int x1 , int y1 ) {
		List<Point2D_I32> output = new ArrayList<>();

		for (int x = x0; x < x1; x++) {
			output.add( new Point2D_I32(x,y0));
		}

		for (int y = y0; y < y1; y++) {
			output.add( new Point2D_I32(x1,y));
		}

		for (int x = x1; x > x0; x--) {
			output.add( new Point2D_I32(x,y1));
		}

		for (int y = y1; y > y0; y--) {
			output.add( new Point2D_I32(x0,y));
		}

		return output;
	}

	private GrowQueue_I32 createSquareCorners( int x0 , int y0 , int x1 , int y1 ) {
		GrowQueue_I32 corners = new GrowQueue_I32();

		int c0 = 0;
		int c1 = c0 + x1-x0;
		int c2 = c1 + y1-y0;
		int c3 = c2 + x1-x0;

		corners.add(c0);
		corners.add(c1);
		corners.add(c2);
		corners.add(c3);

		return corners;
	}

	private void checkMatched(GrowQueue_I32 corners, GrowQueue_I32 output) {
		boolean foundMatch = false;
		for (int offset = 0; offset < 4; offset++) {
			boolean matched = true;
			for (int i = 0; i < 4; i++) {
				if (corners.get(i) != output.get((offset+i)%4)) {
					matched = false;
					break;
				}
			}
			if( matched ) {
				foundMatch = true;
				break;
			}
		}
		assertTrue(foundMatch);
	}
}