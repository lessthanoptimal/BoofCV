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

package boofcv.alg.fiducial.calib.chess;

import boofcv.abst.distort.FDistort;
import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class TestDetectChessboardFiducial {

	Random rand = new Random(234);

	int squareLength = 30;
	int w = 500;
	int h = 550;

	int offsetX = 15;
	int offsetY = 10;

	Se2_F64 transform;

	@Before
	public void setup() {
		offsetX = 15;
		offsetY = 10;
		transform = null;
	}

	/**
	 * Give it a simple target and see if it finds the expected number of squares
	 */
	@Test
	public void basicTest() {
		for( int numRows = 3; numRows <= 7; numRows++ ) {
			for( int numCols = 3; numCols <= 7; numCols++ ) {
//				System.out.println("shape "+numCols+"  "+numRows);
				basicTest(numRows, numCols, true);
			}
		}
	}

	public void basicTest(int numRows, int numCols , boolean localThreshold ) {
		GrayF32 gray = renderTarget(numRows, numCols);

		ImageMiscOps.addGaussian(gray,rand,0.1,0,255);

//		ShowImages.showWindow(gray,"Rendered Image");
//		try { Thread.sleep(1000); } catch (InterruptedException e) {}

		ConfigChessboard configChess = new ConfigChessboard(5, 5, 1);

		BinaryPolygonDetector<GrayF32> detectorSquare =
				FactoryShapeDetector.polygon(configChess.square, GrayF32.class);
//		detectorSquare.setVerbose(true);

		InputToBinary<GrayF32> inputToBinary;
		if( localThreshold )
			inputToBinary = FactoryThresholdBinary.localSquareBlockMinMax(10,0.90,true,10,GrayF32.class);
		else
			inputToBinary = FactoryThresholdBinary.globalFixed(50,true,GrayF32.class);

		DetectChessboardFiducial alg =
				new DetectChessboardFiducial(numRows, numCols, 4,detectorSquare,null,null,inputToBinary);

		assertTrue(alg.process(gray));

		List<Point2D_F64> found = alg.getCalibrationPoints();
		List<Point2D_F64> expected = calibrationPoints(numRows, numCols);

		assertEquals(expected.size(), found.size());

		// check the ordering of the points
		for( int i = 0; i < expected.size(); i++ ) {
			Point2D_F64 e = expected.get(i);
			Point2D_F64 f = found.get(i);

			if( transform != null ) {
				SePointOps_F64.transform(transform,e,e);
			}

			assertEquals("i = " + i,e.x,f.x,2);
			assertEquals("i = " + i,e.y,f.y,2);
		}
	}

	public GrayF32 renderTarget(int numRows, int numCols) {
		GrayF32 gray = new GrayF32(w,h);
		ImageMiscOps.fill(gray,80f);

		int numCols2 = numCols/2;
		int numRows2 = numRows/2;

		numCols = numCols/2 + numCols%2;
		numRows = numRows/2 + numRows%2;

		// create the grid
		for( int y = 0; y < numRows; y++) {
			for( int x = 0; x < numCols; x++ ) {
				int pixelY = 2*y*squareLength+offsetY;
				int pixelX = 2*x*squareLength+offsetX;

				ImageMiscOps.fillRectangle(gray, 20, pixelX, pixelY, squareLength, squareLength);
			}
		}
		for( int y = 0; y < numRows2; y++) {
			for( int x = 0; x < numCols2; x++ ) {
				int pixelY = 2*y*squareLength+offsetY+squareLength;
				int pixelX = 2*x*squareLength+offsetX+squareLength;

				ImageMiscOps.fillRectangle(gray, 20, pixelX, pixelY, squareLength, squareLength);
			}
		}

		if( transform != null ) {
			GrayF32 distorted = new GrayF32(gray.width,gray.height);
			FDistort f = new FDistort(gray,distorted);
			f.border(80f).affine(transform.c,-transform.s,transform.s,transform.c,
					transform.T.x,transform.T.y).apply();
			gray = distorted;
		}

		return gray;
	}

	public List<Point2D_F64> calibrationPoints(int numRows, int numCols) {

		List<Point2D_F64> ret = new ArrayList<>();

		for( int y = 0; y < numRows-1; y++) {
			for( int x = 0; x < numCols-1; x++ ) {
				int pixelY = y*squareLength+offsetY+squareLength;
				int pixelX = x*squareLength+offsetX+squareLength;

				ret.add( new Point2D_F64(pixelX,pixelY));
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
	@Test
	public void touchesBorder_translate() {
		for (int i = 0; i < 4; i++) {
			if( i%2 == 0 )
				offsetX = 0;
			else
				offsetX = 15;
			if( i/2 == 0 )
				offsetY = 0;
			else
				offsetY = 10;

			basicTest(3,4, false);
		}
	}

	@Test
	public void touchedBorder_rotate() {
		List<Se2_F64> transforms = new ArrayList<>();

		transforms.add( new Se2_F64(0,0,0.1));
		transforms.add( new Se2_F64(w-100,0,0.1));
		transforms.add( new Se2_F64(w/2,-5,0.4));
		transforms.add( new Se2_F64(w/2,h-105,0.4));
		transforms.add( new Se2_F64(w/2,h-100,0.4));
		transforms.add( new Se2_F64(35,40,Math.PI/4));

		transforms.add( new Se2_F64(50,-5,0.05));
		transforms.add( new Se2_F64(50,-25,0.05));
		transforms.add( new Se2_F64(50,h-65,-0.05));
		transforms.add( new Se2_F64(50,h-90,0.05));


		offsetX = 0;
		offsetY = 0;

		for(Se2_F64 t : transforms ) {
			transform = t;
			basicTest(3,4, false);
			basicTest(3,4, true);
		}
	}
}
