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

import boofcv.abst.distort.FDistort;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
public abstract class GenericDetectFindChessboardCorners extends BoofStandardJUnit {
	Random rand = new Random(234);

	int squareLength = 30;
	int w = 500;
	int h = 550;

	int offsetX = 15;
	int offsetY = 10;

	Se2_F64 transform;

	boolean showRendered = false;

	public abstract List<PointIndex2D_F64> findCorners( int numRows, int numCols, GrayF32 image );

	@BeforeEach
	public void setup() {
		offsetX = 15;
		offsetY = 10;
		transform = null;
	}

	/**
	 * Give it a simple target and see if it finds the expected number of squares
	 */
	@Test void basicTest() {
		for (int numRows = 3; numRows <= 7; numRows++) {
			for (int numCols = 3; numCols <= 7; numCols++) {
//				System.out.println("shape "+numCols+"  "+numRows);
				basicTest(numRows, numCols);
				basicTest(numRows, numCols);
			}
		}
	}

	public void basicTest( int numRows, int numCols ) {
		GrayF32 gray = renderTarget(numRows, numCols);

		ImageMiscOps.addGaussian(gray, rand, 0.1, 0, 255);

//		ShowImages.showWindow(gray,"Rendered Image");
//		try { Thread.sleep(1000); } catch (InterruptedException e) {}

		List<PointIndex2D_F64> found = findCorners(numRows, numCols, gray);
		if (found == null) {
			UtilImageIO.saveImage(gray, "savedchessboard.png");
			fail("Failed to detect target");
		}
		List<Point2D_F64> expected = calibrationPoints(numRows, numCols);

		assertEquals(expected.size(), found.size());

		// check the ordering of the points
		for (int i = 0; i < expected.size(); i++) {
			Point2D_F64 e = expected.get(i);
			Point2D_F64 f = found.get(i).p;

			if (transform != null) {
				SePointOps_F64.transform(transform, e, e);
			}

			assertEquals(e.x, f.x, 2);
			assertEquals(e.y, f.y, 2);
		}
	}

	public GrayF32 renderTarget( int numRows, int numCols ) {
		GrayF32 gray = new GrayF32(w, h);
		float backgroundValue = 150f;
		float squareValue = 20f;
		ImageMiscOps.fill(gray, backgroundValue);

		int numCols2 = numCols/2;
		int numRows2 = numRows/2;

		numCols = numCols/2 + numCols%2;
		numRows = numRows/2 + numRows%2;

		// create the grid
		for (int y = 0; y < numRows; y++) {
			for (int x = 0; x < numCols; x++) {
				int pixelY = 2*y*squareLength + offsetY;
				int pixelX = 2*x*squareLength + offsetX;

				ImageMiscOps.fillRectangle(gray, squareValue, pixelX, pixelY, squareLength, squareLength);
			}
		}
		for (int y = 0; y < numRows2; y++) {
			for (int x = 0; x < numCols2; x++) {
				int pixelY = 2*y*squareLength + offsetY + squareLength;
				int pixelX = 2*x*squareLength + offsetX + squareLength;

				ImageMiscOps.fillRectangle(gray, squareValue, pixelX, pixelY, squareLength, squareLength);
			}
		}

		if (transform != null) {
			GrayF32 distorted = new GrayF32(gray.width, gray.height);
			FDistort f = new FDistort(gray, distorted);
			f.border(backgroundValue).affine(transform.c, -transform.s, transform.s, transform.c,
					transform.T.x, transform.T.y).apply();
			gray = distorted;
		}

		if (showRendered) {
			ShowImages.showWindow(gray, "Rendered");
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return gray;
	}

	public List<Point2D_F64> calibrationPoints( int numRows, int numCols ) {

		List<Point2D_F64> ret = new ArrayList<>();

		for (int y = 0; y < numRows - 1; y++) {
			for (int x = 0; x < numCols - 1; x++) {
				int pixelY = y*squareLength + offsetY + squareLength;
				int pixelX = x*squareLength + offsetX + squareLength;

				ret.add(new Point2D_F64(pixelX, pixelY));
			}
		}

		return ret;
	}

	/**
	 * See if it can detect targets which touch the image border when thresholded using a
	 * global algorithm.
	 *
	 * This doesn't test all possible cases.
	 */
	@Test void touchesBorder_translate() {
		for (int i = 0; i < 4; i++) {
			if (i%2 == 0)
				offsetX = 0;
			else
				offsetX = 15;
			if (i/2 == 0)
				offsetY = 0;
			else
				offsetY = 10;

			basicTest(3, 4);
		}
	}
}
