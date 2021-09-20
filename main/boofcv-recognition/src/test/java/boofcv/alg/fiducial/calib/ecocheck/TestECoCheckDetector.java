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

package boofcv.alg.fiducial.calib.ecocheck;

import boofcv.abst.fiducial.calib.ConfigChessboardX;
import boofcv.alg.distort.AbstractInterpolatePixelS;
import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.feature.detect.chess.ChessboardCorner;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid.GridElement;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerGraph;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckDetector.Transform;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.GridShape;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.FastAccess;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestECoCheckDetector extends BoofStandardJUnit {

	// Reduce boilerplate by pre-declaring the algorithm being tested
	ConfigChessboardX config = new ConfigChessboardX();
	ECoCheckUtils utils = new ECoCheckUtils();
	ECoCheckDetector<GrayU8> alg;

	List<Point2D_F64> truthCorners;

	// Quite zone around rendered marker
	int borderPixels = 10;

	@BeforeEach void setup() {
		setUp(5, 6);
	}

	void setUp( int rows, int cols ) {
		utils = new ECoCheckUtils();
		utils.addMarker(rows, cols);
		utils.fixate();
		alg = new ECoCheckDetector<>(utils, config, GrayU8.class);
	}

	/**
	 * Test the entire pipeline on a very simple synthetic scenario with different shaped targets
	 */
	@Test void oneTargetShapes() {
		oneTargetShapes(5, 6);
		oneTargetShapes(5, 5);
		oneTargetShapes(4, 6);
	}

	void oneTargetShapes( int squareRows, int squareCols ) {
		setUp(squareRows, squareCols);

		GrayU8 image = createMarker(utils, 0);

		int numEncodedSquares = (squareCols - 2)*(squareRows - 2)/2;

		alg.process(image);

		FastAccess<ECoCheckFound> found = alg.getFound();
		assertEquals(1, found.size);

		ECoCheckFound marker = found.get(0);
		assertEquals(0, marker.markerID);
		assertEquals(squareRows, marker.squareRows);
		assertEquals(squareCols, marker.squareCols);
		assertEquals(numEncodedSquares, marker.decodedCells.size);
		assertEquals(truthCorners.size(), marker.corners.size);
		assertEquals(truthCorners.size(), marker.metadata.size);

		// see if the corners are at all the correct locations and have the correct ID
		for (int i = 0; i < marker.corners.size; i++) {
			PointIndex2D_F64 c = marker.corners.get(i);
			Point2D_F64 truth = truthCorners.get(c.index);

			// Check that their locations are the same and compensate for the border added to the marker
			assertTrue(truth.isIdentical(c.p.x - borderPixels, c.p.y - borderPixels, UtilEjml.TEST_F64));
		}
	}

	/**
	 * Take one target and rotate it. See if anything goes horribly wrong
	 */
	@Test void simpleOneTarget_Rotate() {
		GrayU8 image = createMarker(utils, 0);
		GrayU8 rotated = new GrayU8(1, 1);

		for (int i = 0; i < 3; i++) {
			ImageMiscOps.rotateCCW(image, rotated);

			alg.process(rotated);
			image.setTo(rotated);

			FastAccess<ECoCheckFound> found = alg.getFound();
			assertEquals(1, found.size);
			ECoCheckFound marker = found.get(0);
			assertEquals(0, marker.markerID);
			assertEquals(5, marker.squareRows);
			assertEquals(6, marker.squareCols);
			assertEquals(truthCorners.size(), marker.corners.size);
			assertEquals(truthCorners.size(), marker.metadata.size);
		}
	}

	/**
	 * Part of the target is obstructed.
	 */
	@Test void partialTarget() {
		GrayU8 image = createMarker(utils, 0);

		// block part of the left side
		ImageMiscOps.fillRectangle(image, 255, 0, 0, 80, image.height);

		alg.process(image);

		FastAccess<ECoCheckFound> found = alg.getFound();
		assertEquals(1, found.size);

		// number of squares and corners is found through manual inspection
		ECoCheckFound marker = found.get(0);
		assertEquals(0, marker.markerID);
		assertEquals(5, marker.squareRows);
		assertEquals(6, marker.squareCols);
		assertEquals(5, marker.decodedCells.size);
		assertEquals(16, marker.corners.size);
		assertEquals(marker.corners.size, marker.metadata.size);
	}

	/**
	 * See if it will return an anonymous chessboard
	 */
	@Test void chessboard_NoData() {
		GrayU8 image = createMarker(utils, 0);

		// zap all the encoded data
		for (int row = 1; row < 4; row++) {
			for (int cols = row%2 + 1; cols < 5; cols += 2) {
				ImageMiscOps.fillRectangle(image, 255, 50 + (cols - 1)*80, 50 + (row - 1)*80, 80, 80);
			}
		}

		alg.process(image);

		FastAccess<ECoCheckFound> found = alg.getFound();
		assertEquals(1, found.size);

		ECoCheckFound marker = found.get(0);
		assertEquals(-1, marker.markerID);
		assertEquals(5, marker.squareRows);
		assertEquals(6, marker.squareCols);
		assertEquals(truthCorners.size(), marker.corners.size);
		assertEquals(truthCorners.size(), marker.metadata.size);
		assertEquals(0, marker.decodedCells.size);
		assertEquals(0, marker.touchBinary.size);
	}

	/**
	 * Two patterns are so close their grids will overlap. See if this correctly splits them up
	 */
	@Test void touchingTargets() {
		utils = new ECoCheckUtils();
		utils.addMarker(3, 4); // two targets with the same shape
		utils.addMarker(3, 4);
		utils.fixate();
		alg = new ECoCheckDetector<>(utils, config, GrayU8.class);

		GrayU8 image0 = createMarker(utils, 0);
		GrayU8 image1 = createMarker(utils, 1);

		GrayU8 combined = new GrayU8(image0.width, image0.height*2);

		ImageMiscOps.copy(0, 0, 0, 0, image0.width, image0.height, image0, combined);
		ImageMiscOps.copy(0, 10, 0, image0.height - 10,
				image1.width, image1.height - 10, image1, combined);

		alg.process(combined);

		FastAccess<ECoCheckFound> found = alg.getFound();
		assertEquals(2, found.size);

		found.forEach(t -> {
			assertEquals(3, t.squareRows);
			assertEquals(4, t.squareCols);
			assertEquals(truthCorners.size(), t.corners.size);
		});
	}

	/**
	 * Multiple patterns are visible at once. Targets have enough space that they shouldn't be joined.
	 */
	@Test void multipleTargets() {
		utils = new ECoCheckUtils();
		utils.addMarker(3, 4);
		utils.addMarker(4, 3);
		utils.fixate();
		alg = new ECoCheckDetector<>(utils, config, GrayU8.class);

		GrayU8 image0 = createMarker(utils, 0);
		GrayU8 image1 = createMarker(utils, 1);

		int space = 40;
		GrayU8 combined = new GrayU8(Math.max(image0.width, image1.width), image0.height + image1.height + space);

		ImageMiscOps.copy(0, 0, 0, 0, image0.width, image0.height, image0, combined);
		ImageMiscOps.copy(0, 0, 0, image0.height + space,
				image1.width, image1.height, image1, combined);

		alg.process(combined);

		FastAccess<ECoCheckFound> found = alg.getFound();
		assertEquals(2, found.size);
	}

	private GrayU8 createMarker( ECoCheckUtils utils, int markerID ) {
		int squareWidth = 80;
		GridShape shape = utils.markers.get(markerID);
		var engine = new FiducialImageEngine();
		engine.configure(10, squareWidth*(shape.cols - 1), squareWidth*(shape.rows - 1));
		var renderer = new ECoCheckGenerator(utils);
		renderer.render = engine;
		renderer.squareWidth = squareWidth;
		renderer.render(markerID);

		truthCorners = renderer.corners;

		return engine.getGray();
	}

	/**
	 * It will fail to create a target because a grid element has been marked, indicating that it has already been
	 * used. This is a rare situation not tested in image based tests above.
	 */
	@Test void createCorrectedTarget_Marked() {
		Transform transform = new Transform();
		transform.marker = 0;

		// make this large enough so that it doesn't blow up by trying to access a pixel out of bounds
		alg.cornersAroundBinary.reshape(10, 10);

		// Create a grid with a few arbitrary elements that will have no issues
		DogArray<GridElement> sparseGrid = alg.clusterToGrid.getSparseGrid();
		for (int i = 0; i < 5; i++) {
			GridElement e = sparseGrid.grow();
			e.row = i;
			e.col = 0;
			e.node = new ChessboardCornerGraph.Node();
			e.node.corner = new ChessboardCorner();
		}

		// Pass the first time
		assertTrue(alg.createCorrectedTarget(transform, new ECoCheckFound()));

		// Second time it will blow up because they have been marked!
		assertFalse(alg.createCorrectedTarget(transform, new ECoCheckFound()));
	}

	@Test void findMatching() {
		alg.transforms.grow().setTo(1, 2, 3, 4, 0);
		alg.transforms.grow().setTo(1, 2, 4, 3, 0);
		alg.transforms.grow().setTo(2, 2, 3, 4, 0);

		// feed it transforms that are in the array
		assertSame(alg.transforms.get(0), alg.findMatching(1, 2, 3, 4));
		assertSame(alg.transforms.get(1), alg.findMatching(1, 2, 4, 3));
		assertSame(alg.transforms.get(2), alg.findMatching(2, 2, 3, 4));

		// Some that are not in the list
		assertNull(alg.findMatching(1, 2, 3, 1));
		assertNull(alg.findMatching(1, 2, 2, 4));
	}

	@Test void sampleInnerWhite() {
		Point2D_F64 a = new Point2D_F64(10, 30);
		Point2D_F64 b = new Point2D_F64(40, 30);

		// If it samples between the two points it should have a known value
		// It should not sample exactly on the line or right next to the end points
		alg.interpolate = new AbstractInterpolatePixelS<>() {
			@Override public float get( float x, float y ) {
				if (x < 12 || x > 38)
					return 0;
				if (y > 30)
					return 200;
				else if (y < 30)
					return 120;
				else
					return 0;
			}
		};

		float found = alg.sampleInnerWhite(a, b);
		assertEquals(160, found, UtilEjml.TEST_F32);
	}

	@Test void isBorderWhite() {
		// give it the transform. t=85,85 scale=30
		alg.utils.squareToPixel.data = new double[]{30, 0, 85, 0, 30, 85, 0, 0, 1};

		// return black unless inside the hollow square
		// inner square = (90, 90, 110, 110)
		// outer square = (85, 85, 115, 115)
		alg.interpolate = new AbstractInterpolatePixelS<>() {
			@Override public float get( float x, float y ) {
				x = Math.abs(x - 100);
				y = Math.abs(y - 100);

				if (x < 10 && y < 10)
					return 0;
				if (x < 15 && y < 15)
					return 255;
				return 0;
			}
		};

		var a = new ChessboardCorner();
		var b = new ChessboardCorner();
		var c = new ChessboardCorner();
		var d = new ChessboardCorner();

		a.setTo(85, 85);
		b.setTo(110, 85);
		c.setTo(110, 110);
		d.setTo(85, 110);

		// order should not matter because the line it's sampled along is determined by squareToPixel transform
		assertTrue(alg.isBorderWhite(a, b, c, d));
		assertTrue(alg.isBorderWhite(d, c, b, a));

		// changing the transform will cause it to fail
		alg.utils.squareToPixel.data = new double[]{30, 0, 95, 0, 30, 85, 0, 0, 1};
		assertFalse(alg.isBorderWhite(a, b, c, d));
	}

	/**
	 * Create a simple scenario where the bits follow a known pattern.
	 */
	@Test void sampleBitsGray() {
		var points = new ArrayList<Point2D_F64>();
		var values = new DogArray_F32();

		int rows = 5;
		int cols = 6;
		int blockSize = 4;

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				for (int k = 0; k < blockSize; k++) {
					points.add(new Point2D_F64(j*20, i*20));
				}
			}
		}

		alg.bitImage.reshape(cols, rows);
		alg.interpolate = new AbstractInterpolatePixelS<>() {
			@Override public float get( float x, float y ) {
				int i = (int)(y/20 + 0.5);
				int j = (int)(x/20 + 0.5);

				return (i + j)%3 == 0 ? 10 : 200;
			}
		};
		alg.samplePixelGray(points, values);
		alg.graySamplesToBits(values, blockSize, 100);

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				int expected = (i + j)%3 == 0 ? 1 : 0;
				assertEquals(expected, alg.bitImage.get(j, i));
			}
		}
	}
}
