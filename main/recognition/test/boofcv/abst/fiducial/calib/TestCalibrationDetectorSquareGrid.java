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

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestCalibrationDetectorSquareGrid extends GenericPlanarCalibrationDetectorChecks {

	private final static ConfigSquareGrid config = new ConfigSquareGrid(3, 2, 30,30);


	@Test
	public void createLayout() {
		List<Point2D_F64> l = CalibrationDetectorSquareGrid.createLayout(3, 2, 0.1, 0.2);

		assertEquals(4*6,l.size());

		double w = l.get(1).x - l.get(0).x;
		double h = l.get(0).y - l.get(4).y ;

		assertEquals(0.1,w,1e-8);
		assertEquals(0.1,h,1e-8);

		double s = l.get(2).x - l.get(1).x;

		assertEquals(0.2, s, 1e-8);
	}

	@Override
	public void renderTarget(GrayF32 original, List<CalibrationObservation> solutions) {
		ImageMiscOps.fill(original, 255);

		int numRows = config.numRows*2-1;
		int numCols = config.numCols*2-1;

		int square = original.getWidth() / (Math.max(numRows,numCols) + 4);

		int targetWidth = square * numCols;
		int targetHeight = square * numRows;

		int x0 = (original.width - targetWidth) / 2;
		int y0 = (original.height - targetHeight) / 2;

		for (int i = 0; i < numRows; i += 2) {
			int y = y0 + i * square;

			for (int j = 0; j < numCols; j += 2) {
				int x = x0 + j * square;
				ImageMiscOps.fillRectangle(original, 0, x, y, square, square);
			}
		}

		int pointsRow = numRows+1;
		int pointsCol = numCols+1;

		CalibrationObservation set = new CalibrationObservation();
		int gridIndex = 0;
		for (int i = 0; i < pointsRow; i++) {
			for (int j = 0; j < pointsCol; j++, gridIndex++) {
				double y = y0 + i*square;
				double x = x0 + j*square;
				set.add(new Point2D_F64(x, y), gridIndex);
			}
		}
		solutions.add(set);
	}

	@Override
	public DetectorFiducialCalibration createDetector() {
		return FactoryFiducialCalibration.squareGrid(config);
	}
}