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

package boofcv.alg.fiducial.calib.chess;

import boofcv.alg.feature.detect.chess.ChessboardCorner;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("WeakerAccess")
@Disabled // TODO enable again, but might require a rewrite. Was commented out under develop and never written again
class TestChessboardCornerClusterFinder extends BoofStandardJUnit {
	final double sideLength = 40; // pixel distance between corners
	double offsetX;
	double offsetY;

	GrayU8 image = new GrayU8(1, 1);

	@BeforeEach
	void setup() {
		offsetX = 0;
		offsetY = 0;
	}

	/**
	 * Perfect grids with only the exact number of expected
	 */
	@Test
	void perfect_one() {
		// test various sizes in an attempt to trigger edge cases
		perfect(2, 2);
		perfect(4, 2);
		perfect(3, 3);
		// NN search won't find all the corners now
		perfect(4, 4);
		perfect(10, 10);
	}

	void perfect( int rows, int cols ) {
		List<ChessboardCorner> input = createCorners(rows, cols);
		ChessboardCornerClusterFinder<GrayU8> alg = createAlg();
		alg.setThresholdEdgeIntensity(0.0); // turn off this check
		alg.setMaxNeighbors(10); // this is perfect, 8 should be enough
		// reduced the number so that having an non-exhaustive search is stressed more
		alg.process(image, input, 1);

		DogArray<ChessboardCornerGraph> found = alg.getOutputClusters();
		assertEquals(1, found.size);
		checkClusterPerfect(found.get(0), rows, cols);
	}

	/**
	 * This contains an ambiguous corner and the algorithm needs to select the correct one
	 */
	@Test
	void perfect_ambiguous() {
		for (int i = 0; i < 10; i++) {
			perfect_ambiguous(2, 2, 1);
			perfect_ambiguous(5, 5, 1);
			perfect_ambiguous(10, 8, 1);
		}
	}

	void perfect_ambiguous( int rows, int cols, int numAmbiguous ) {
		List<ChessboardCorner> input = createCorners(rows, cols);

		// add new corners which are near by existing ones but not part of the grid
		int N = rows*cols;
		for (int i = 0; i < numAmbiguous; i++) {
			ChessboardCorner c = input.get(rand.nextInt(N));
			ChessboardCorner d = new ChessboardCorner();
			d.setTo(c);
			d.x += rand.nextGaussian()*sideLength/30.0;
			d.y += rand.nextGaussian()*sideLength/30.0;
			input.add(d);
		}

		ChessboardCornerClusterFinder<GrayU8> alg = createAlg();
		alg.setMaxNeighbors(10); // this is perfect, 8 should be enough
		// reduced the number so that having an non-exhaustive search is stressed more
		alg.process(image, input, 1);

		DogArray<ChessboardCornerGraph> found = alg.getOutputClusters();
		if (numAmbiguous == 0) {
			assertEquals(1, found.size);
			checkClusterPerfect(found.get(0), rows, cols);
		} else {
			assertTrue(found.size > 0);
			for (int i = 0; i < found.size; i++) {
				checkClusterAmbiguous(found.get(i), rows, cols);
			}
		}
	}

	/**
	 * There are two completely separate grids which are parallel to each other.
	 */
	@Test
	void perfect_2x2_3x2() {
		List<ChessboardCorner> input = createCorners(2, 2);
		offsetX = 500;
		input.addAll(createCorners(3, 2));

		ChessboardCornerClusterFinder<GrayU8> alg = createAlg();
		alg.setMaxNeighborDistance(200);
		alg.process(image, input, 1);
		DogArray<ChessboardCornerGraph> found = alg.getOutputClusters();

		assertEquals(2, found.size);

		boolean found2x2 = false;
		boolean found3x2 = false;

		for (int i = 0; i < found.size; i++) {
			ChessboardCornerGraph g = found.get(i);
			if (g.corners.size == 4)
				found2x2 = true;
			else if (g.corners.size == 6)
				found3x2 = true;
		}

		assertTrue(found2x2);
		assertTrue(found3x2);
	}

	/**
	 * Actual corner observations from a fisheye image
	 */
	@Test
	void fisheye() {
		ChessboardCornerClusterFinder<GrayU8> alg = createAlg();
		alg.setThresholdEdgeIntensity(-1);
		alg.process(new GrayU8(480, 480), createFisheye(), 1);
		DogArray<ChessboardCornerGraph> found = alg.getOutputClusters();
		assertEquals(1, found.size);
		assertEquals(24, found.get(0).corners.size);
	}

	List<ChessboardCorner> createFisheye() {
		double[] data = new double[]{
				901.730, 19.788, -0.916481,
				776.088, 36.243, 0.800666,
				659.545, 67.488, -1.141423,
				556.513, 110.203, 0.534072,
				568.884, 159.993, -1.193452,
				587.523, 219.934, 0.483544,
				609.680, 284.868, -1.216572,
				699.776, 255.642, 0.630314,
				799.915, 234.210, -1.047695,
				906.694, 223.450, 0.866288,
				1016.448, 224.564, -0.848513,
				1122.507, 237.118, 1.029737,
				1135.740, 163.016, -0.661328,
				1146.909, 94.946, 1.022470,
				1154.920, 40.350, -0.735298,
				1026.851, 77.206, -0.746636,
				1021.864, 147.066, 0.870077,
				904.144, 146.284, -0.927371,
				789.445, 159.577, 0.700495,
				682.746, 185.030, -1.134515,
				668.750, 120.472, 0.577675,
				780.841, 91.222, -0.959134,
				902.360, 75.823, 0.804160,
				1030.534, 21.252, 0.903699};

		// Input image was 1920x1920
		// let's scale that down to 480x480
		double scale = 4.0;

		List<ChessboardCorner> out = new ArrayList<>();
		for (int i = 0; i < data.length; i += 3) {
			ChessboardCorner c = new ChessboardCorner();
			c.x = data[i]/scale;
			c.y = data[i + 1]/scale;
			c.orientation = data[i + 2];
			c.intensity = 100;
			out.add(c);
		}

		return out;
	}

	List<ChessboardCorner> createCorners( int rows, int cols ) {
		List<ChessboardCorner> corners = new ArrayList<>();

		for (int row = 0; row < rows; row++) {
			double y = offsetY + sideLength*row;
			for (int col = 0; col < cols; col++) {
				double x = offsetX + sideLength*col;

				ChessboardCorner c = new ChessboardCorner();
				c.intensity = 20;
				c.orientation = (((row%2) + (col%2))%2) == 0 ? Math.PI/4 : -Math.PI/4;
				c.x = x;
				c.y = y;

				corners.add(c);
			}
		}

		// randomize the list
		Collections.shuffle(corners, rand);

		return corners;
	}

	void checkClusterPerfect( ChessboardCornerGraph cluster, int rows, int cols ) {
		assertEquals(rows*cols, cluster.corners.size);

		// Checks to see there is one and only one node at each expected location
		for (int row = 0; row < rows; row++) {
			double y = sideLength*row;
			for (int col = 0; col < cols; col++) {
				double x = sideLength*col;

				int numMatches = 0;
				for (int i = 0; i < cluster.corners.size; i++) {
					if (cluster.corners.get(i).corner.distance(x, y) < UtilEjml.TEST_F64) {
						numMatches++;
					}
				}

				assertEquals(1, numMatches);
			}
		}

		// check the edges
		for (int row = 0; row < rows; row++) {
			double y = sideLength*row;
			for (int col = 0; col < cols; col++) {
				double x = sideLength*col;

				int expected = 2;
				if (row > 0 && row < rows - 1) {
					expected++;
				}
				if (col > 0 && col < cols - 1) {
					expected++;
				}

				ChessboardCornerGraph.Node n = cluster.findClosest(x, y);

				assertEquals(expected, n.countEdges());
			}
		}
	}

	void checkClusterAmbiguous( ChessboardCornerGraph cluster, int rows, int cols ) {
		assertEquals(rows*cols, cluster.corners.size);

		// Checks to see there is one and only one node at each expected location
		for (int i = 0; i < cluster.corners.size; i++) {
			Point2D_F64 a = cluster.corners.get(i).corner;

			int matches = 0;
			for (int j = i + 1; j < cluster.corners.size; j++) {
				Point2D_F64 b = cluster.corners.get(j).corner;

				if (a.distance(b) < 0.0001) {
					matches++;
				}
			}

			assertEquals(0, matches);
		}

		// check the edges
		for (int row = 0; row < rows; row++) {
			double y = sideLength*row;
			for (int col = 0; col < cols; col++) {
				double x = sideLength*col;

				int expected = 2;
				if (row > 0 && row < rows - 1) {
					expected++;
				}
				if (col > 0 && col < cols - 1) {
					expected++;
				}

				ChessboardCornerGraph.Node n = cluster.findClosest(x, y);

				assertEquals(expected, n.countEdges());
			}
		}
	}

	public ChessboardCornerClusterFinder<GrayU8> createAlg() {
		return new ChessboardCornerClusterFinder<>(new DummyIntensity());
	}

	private static class DummyIntensity extends ChessboardCornerEdgeIntensity<GrayU8> {

		public DummyIntensity() {
			super(GrayU8.class);
		}

		@Override
		public float process( ChessboardCorner ca, ChessboardCorner cb, double direction_a_to_b ) {
			return 100.0f;
		}
	}
}
