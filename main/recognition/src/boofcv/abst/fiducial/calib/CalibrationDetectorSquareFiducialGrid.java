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
import boofcv.alg.fiducial.calib.DetectFiducialSquareGrid;
import boofcv.alg.fiducial.square.DetectFiducialSquareBinary;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;

import java.util.List;

/**
 * Wrapper around {@link DetectFiducialSquareGrid} for {@link DetectorFiducialCalibration}.
 *
 * @author Peter Abeles
 */
public class CalibrationDetectorSquareFiducialGrid implements DetectorFiducialCalibration {

	// number of squares along each grid axis
	int numRows;
	int numCols;

	// number of calibration points along each grid axis
	int numPointRows;
	int numPointCols;

	// layout of points in fiducial frame
	List<Point2D_F64> layoutPoints;

	// square binary fiducial detector
	DetectFiducialSquareGrid<GrayF32> detector;

	// storage for observations
	CalibrationObservation observations;

	public CalibrationDetectorSquareFiducialGrid(ConfigSquareGridBinary config) {

		DetectFiducialSquareBinary<GrayF32> fiducialDetector = FactoryFiducial.
				squareBinary(config.configDetector, config.configThreshold, GrayF32.class).getAlgorithm();
		detector = new DetectFiducialSquareGrid<>(config.numRows,config.numCols,config.ids,fiducialDetector);

		numRows = config.numRows;
		numCols = config.numCols;

		numPointRows = 2*numRows;
		numPointCols = 2*numCols;

		layoutPoints = CalibrationDetectorSquareGrid.createLayout(numRows, numCols, config.squareWidth, config.spaceWidth);
	}

	@Override
	public boolean process(GrayF32 input) {
		observations = new CalibrationObservation();
		if( !detector.detect(input) ) {
			return false;
		}

		List<DetectFiducialSquareGrid.Detection> detections = detector.getDetections();

		for (int i = 0; i < detections.size(); i++) {
			DetectFiducialSquareGrid.Detection d = detections.get(i);

			int row = d.gridIndex/numCols;
			int col = d.gridIndex%numCols;

			int pointRow = row*2;
			int pointCol = col*2;

			observations.add(d.location.a,getPointIndex(pointRow,   pointCol));
			observations.add(d.location.b,getPointIndex(pointRow,   pointCol+1));
			observations.add(d.location.c,getPointIndex(pointRow+1, pointCol+1));
			observations.add(d.location.d,getPointIndex(pointRow+1, pointCol));
		}

		observations.sort();

		return true;
	}

	private int getPointIndex( int row , int col ) {
		return row*numPointCols + col;
	}

	@Override
	public CalibrationObservation getDetectedPoints() {
		return observations;
	}

	@Override
	public List<Point2D_F64> getLayout() {
		return layoutPoints;
	}
}
