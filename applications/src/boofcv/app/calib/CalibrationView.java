/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.app.calib;

import boofcv.abst.calib.PlanarCalibrationDetector;
import boofcv.abst.calib.PlanarDetectorChessboard;
import boofcv.abst.calib.PlanarDetectorSquareGrid;
import boofcv.alg.geo.calibration.CalibrationObservation;
import georegression.struct.point.Point2D_F64;

import java.util.List;

/**
 * @author Peter Abeles
 */
public interface CalibrationView {

	void initialize( PlanarCalibrationDetector detector );

	void getSides( CalibrationObservation detections , List<Point2D_F64> sides );

	int getBufferWidth( double gridPixelsWide );

	class Chessboard implements CalibrationView {

		int numRows,numCols;
		int pointRows,pointCols;

		public void initialize( PlanarCalibrationDetector detector ) {
			PlanarDetectorChessboard chessboard = (PlanarDetectorChessboard)detector;
			this.numRows = chessboard.getGridRows();
			this.numCols = chessboard.getGridColumns();

			pointRows = numRows-1;
			pointCols = numCols-1;
		}

		@Override
		public void getSides(CalibrationObservation detections, List<Point2D_F64> sides) {
			sides.clear();
			sides.add( get(0, 0, detections.observations));
			sides.add( get(0, pointCols-1, detections.observations));
			sides.add( get(pointRows-1, pointCols-1, detections.observations));
			sides.add( get(pointRows-1, 0, detections.observations));
		}

		private Point2D_F64 get( int row , int col , List<Point2D_F64> detections ) {
			return detections.get(row*pointCols+col);
		}

		@Override
		public int getBufferWidth( double gridPixelsWide ) {
			return (int)(gridPixelsWide/pointCols+0.5);
		}
	}

	class SquareGrid implements CalibrationView {

		int gridCols;
		int pointRows,pointCols;

		public void initialize( PlanarCalibrationDetector detector ) {
			PlanarDetectorSquareGrid target = (PlanarDetectorSquareGrid)detector;
			pointRows = target.getPointRows();
			pointCols = target.getPointColumns();

			gridCols = target.getGridColumns();
			gridCols += gridCols/2;
		}

		@Override
		public void getSides(CalibrationObservation detections, List<Point2D_F64> sides) {
			sides.clear();
			sides.add( get(0, 0, detections.observations));
			sides.add( get(0, pointCols-1, detections.observations));
			sides.add( get(pointRows-1, pointCols-1, detections.observations));
			sides.add( get(pointRows-1, 0, detections.observations));
		}

		private Point2D_F64 get( int row , int col , List<Point2D_F64> detections ) {
			return detections.get(row*pointCols+col);
		}

		@Override
		public int getBufferWidth( double gridPixelsWide ) {
			return (int)(0.15*(gridPixelsWide/gridCols)+0.5);
		}
	}
}
