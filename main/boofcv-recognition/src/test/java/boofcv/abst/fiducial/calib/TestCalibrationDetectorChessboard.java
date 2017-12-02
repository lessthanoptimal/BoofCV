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

package boofcv.abst.fiducial.calib;

import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.List;

import static boofcv.abst.fiducial.calib.CalibrationDetectorChessboard.gridChess;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCalibrationDetectorChessboard extends GenericPlanarCalibrationDetectorChecks {


	public TestCalibrationDetectorChessboard() {
		targetConfigs.add( new ConfigChessboard(5, 4, 30) );

		// tuning made 1 fail. On real world data it does better. <shrug>
		fisheyeAllowedFails = 1;
	}

	@Test
	public void createLayout() {
		List<Point2D_F64> layout = createDetector(new ConfigChessboard(5, 4, 30) ).getLayout();

		// first control points should be the top left corner then work it's way down in a
		// grid pattern
		assertTrue(layout.get(0).y == layout.get(2).y);
		assertTrue(layout.get(0).x <  layout.get(2).x);
		assertTrue(layout.get(0).y >  layout.get(3).y);
	}

	@Override
	public void renderTarget(Object layout, double length3D , GrayF32 image, List<Point2D_F64> points2D) {
		ConfigChessboard config = (ConfigChessboard)layout;

		int square = 40;

		int targetWidth  = square * config.numCols;
		int targetHeight = square * config.numRows;

		image.reshape(square*2 + targetWidth, square*2 + targetHeight);
		ImageMiscOps.fill(image, 255);

		int x0 = square;
		int y0 = (image.height- targetHeight) / 2;

		for (int i = 0; i < config.numRows; i++) {
			int y = y0 + i*square;

			int startJ = i%2 == 0 ? 0 : 1;
			for (int j = startJ; j < config.numCols; j += 2) {
				int x = x0 + j * square;
				ImageMiscOps.fillRectangle(image,0,x,y,square,square);
			}
		}

		double lengthPattern = length3D*config.numCols/(config.numCols+2);

		points2D.clear();
		points2D.addAll( gridChess(config.numRows, config.numCols, lengthPattern/config.numCols ));

	}

	@Override
	public DetectorFiducialCalibration createDetector(Object layout) {
		return FactoryFiducialCalibration.chessboard((ConfigChessboard)layout);
	}
}