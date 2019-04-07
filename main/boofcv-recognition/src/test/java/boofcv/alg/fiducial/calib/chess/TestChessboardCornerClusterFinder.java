/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.detect.chess.ChessboardCorner;
import org.ddogleg.struct.FastQueue;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
public class TestChessboardCornerClusterFinder {
	Random rand = new Random(2334);
	final double sideLength = 40; // pixel distance between corners

	/**
	 * Perfect grids with only the exact number of expected
	 */
	@Test
	void perfect_one() {
		// test various sizes in an attempt to trigger edge cases
		perfect(2,2);
		perfect(4,2);
		perfect(3,3);
		// NN search won't find all the corners now
		perfect(4,4);
		perfect(10,10);
	}

	void perfect( int rows , int cols ) {
		List<ChessboardCorner> input = createCorners(rows,cols);
		ChessboardCornerClusterFinder alg = new ChessboardCornerClusterFinder();
		alg.maxNeighbors = 10; // this is perfect, 8 should be enough
		                       // reduced the number so that having an non-exhaustive search is stressed more
		alg.process(input);

		FastQueue<ChessboardCornerGraph> found = alg.getOutputClusters();
		assertEquals(1,found.size);
		checkCluster(found.get(0), rows,cols);
	}

	/**
	 * This contains an ambiguous corner and the algorithm needs to select the correct one
	 */
	@Test
	void perfect_ambiguous() {
		perfect_ambiguous(2,2,1);
		perfect_ambiguous(2,2,2);

		perfect_ambiguous(5,5,1);
		perfect_ambiguous(5,5,3);
	}

	void perfect_ambiguous( int rows , int cols , int numAmbiguous) {
		List<ChessboardCorner> input = createCorners(rows,cols);

		// add new corners which are near by existing ones but not part of the grid
		int N = rows*cols;
		for (int i = 0; i < numAmbiguous; i++) {
			ChessboardCorner c = input.get(rand.nextInt(N));
			ChessboardCorner d = new ChessboardCorner();
			d.set(c);
			d.x += rand.nextGaussian()*sideLength/30.0;
			d.y += rand.nextGaussian()*sideLength/30.0;
			input.add(d);
		}

		ChessboardCornerClusterFinder alg = new ChessboardCornerClusterFinder();
		alg.maxNeighbors = 10; // this is perfect, 8 should be enough
		// reduced the number so that having an non-exhaustive search is stressed more
		alg.process(input);

		FastQueue<ChessboardCornerGraph> found = alg.getOutputClusters();
		assertEquals(1,found.size);
		checkCluster(found.get(0), rows,cols);
	}

	/**
	 * There are two completely separate grids
	 */
	@Test
	void perfect_2x2_2x2() {
		fail("Implement");
	}

	/**
	 * Corners lie along a line and should be disregarded.
	 */
	@Test
	void lines() {
		fail("Implement");
	}

	// Reminder to test inner functions
	@Test
	void innerFunctions() {
		fail("Implement");
	}

	List<ChessboardCorner> createCorners( int rows , int cols ) {
		List<ChessboardCorner> corners = new ArrayList<>();

		for (int row = 0; row < rows; row++) {
			double y = sideLength*row;
			for (int col = 0; col < cols; col++) {
				double x = sideLength*col;

				ChessboardCorner c = new ChessboardCorner();
				c.intensity = 20;
				c.orientation = (((row%2)+(col%2))%2) == 0 ? Math.PI/4 : -Math.PI/4;
				c.x = x;
				c.y = y;

				corners.add(c);
			}
		}

		// randomize the list
//		Collections.shuffle(corners, new Random(345));

		return corners;
	}

	void checkCluster( ChessboardCornerGraph cluster , int rows , int cols ) {
		assertEquals(rows*cols,cluster.corners.size);

		// Checks to see there is one and only one node at each expected location
		for (int row = 0; row < rows; row++) {
			double y = sideLength * row;
			for (int col = 0; col < cols; col++) {
				double x = sideLength * col;

				int numMatches = 0;
				for (int i = 0; i < cluster.corners.size; i++) {
					if( cluster.corners.get(i).distance(x,y) < UtilEjml.TEST_F64 ) {
						numMatches++;
					}
				}

				assertEquals(1,numMatches);
			}
		}

		// check the edges
		for (int row = 0; row < rows; row++) {
			double y = sideLength * row;
			for (int col = 0; col < cols; col++) {
				double x = sideLength * col;

				int expected = 2;
				if( row > 0 && row < rows-1 ) {
					expected++;
				}
				if( col > 0 && col < cols-1 ) {
					expected++;
				}

				ChessboardCornerGraph.Node n = cluster.findClosest(x,y);

				assertEquals(expected,n.countEdges());
			}
		}
	}


}