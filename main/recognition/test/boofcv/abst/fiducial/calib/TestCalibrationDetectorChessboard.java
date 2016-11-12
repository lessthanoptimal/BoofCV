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

package boofcv.abst.fiducial.calib;

import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCalibrationDetectorChessboard extends GenericPlanarCalibrationDetectorChecks {

	private final static ConfigChessboard config = new ConfigChessboard(5, 4, 30);

	@Test
	public void createLayout() {
		List<Point2D_F64> layout = createDetector().getLayout();

		// first control points should be the top left corner then work it's way down in a
		// grid pattern
		assertTrue(layout.get(0).y == layout.get(2).y);
		assertTrue(layout.get(0).x <  layout.get(2).x);
		assertTrue(layout.get(0).y >  layout.get(3).y);

	}

	@Override
	public void renderTarget(GrayF32 original, List<CalibrationObservation> solutions) {

		ImageMiscOps.fill(original, 255);

		int square = original.getWidth()/(Math.max(config.numCols,config.numRows)+4);

		int targetWidth  = square * config.numCols;
		int targetHeight = square * config.numRows;

		int x0 = (original.width - targetWidth) / 2;
		int y0 = (original.height- targetHeight) / 2;

		for (int i = 0; i < config.numRows; i++) {
			int y = y0 + i*square;

			int startJ = i%2 == 0 ? 0 : 1;
			for (int j = startJ; j < config.numCols; j += 2) {
				int x = x0 + j * square;
				ImageMiscOps.fillRectangle(original,0,x,y,square,square);
			}
		}

		int pointsRow = 2*(config.numRows/2) - (1 - config.numRows % 2);
		int pointsCol = 2*(config.numCols/2) - (1 - config.numCols % 2);

		CalibrationObservation set = new CalibrationObservation();
		int gridIndex = 0;
		for (int i = 0; i < pointsRow; i++) {
			for (int j = 0; j < pointsCol; j++,gridIndex++) {
				double y = y0+(i+1)*square;
				double x = x0+(j+1)*square;
				set.add(new Point2D_F64(x, y), gridIndex);
			}
		}
		solutions.add(set);
	}

	@Override
	public DetectorFiducialCalibration createDetector() {
		return FactoryFiducialCalibration.chessboard(config);
	}
}