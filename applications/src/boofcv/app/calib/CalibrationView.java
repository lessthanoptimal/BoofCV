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

package boofcv.app.calib;

import boofcv.abst.fiducial.calib.CalibrationDetectorChessboard;
import boofcv.abst.fiducial.calib.CalibrationDetectorSquareGrid;
import boofcv.abst.geo.calibration.CalibrationDetector;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;

import java.util.List;

/**
 * Interface used to provide calibration target specific information
 *
 * @author Peter Abeles
 */
public interface CalibrationView {

	/**
	 * Intialize by providing it a reference to the detector.  This is then used to determine the appearance
	 * of the target
	 */
	void initialize( CalibrationDetector detector );

	/**
	 * Returns the 4 sides around the fiducial
	 */
	void getSides( CalibrationObservation detections , List<Point2D_F64> sides );

	/**
	 * Returns how wide the image border should be given the visual width of the canonical fiducial
	 */
	int getBufferWidth( double gridPixelsWide );

	class Chessboard implements CalibrationView {

		int numRows,numCols;
		int pointRows,pointCols;

		public void initialize( CalibrationDetector detector ) {
			CalibrationDetectorChessboard chessboard = (CalibrationDetectorChessboard)detector;
			this.numRows = chessboard.getGridRows();
			this.numCols = chessboard.getGridColumns();

			pointRows = numRows-1;
			pointCols = numCols-1;
		}

		@Override
		public void getSides(CalibrationObservation detections, List<Point2D_F64> sides) {
			sides.clear();
			sides.add( get(0, 0, detections.points));
			sides.add( get(0, pointCols-1, detections.points));
			sides.add( get(pointRows-1, pointCols-1, detections.points));
			sides.add( get(pointRows-1, 0, detections.points));
		}

		private Point2D_F64 get( int row , int col , List<PointIndex2D_F64> detections ) {
			return detections.get(row*pointCols+col);
		}

		@Override
		public int getBufferWidth( double gridPixelsWide ) {
			return (int)(0.2*gridPixelsWide/(pointCols-1)+0.5);
		}
	}

	class SquareGrid implements CalibrationView {

		int gridCols;
		int pointRows,pointCols;

		public void initialize( CalibrationDetector detector ) {
			CalibrationDetectorSquareGrid target = (CalibrationDetectorSquareGrid)detector;
			pointRows = target.getPointRows();
			pointCols = target.getPointColumns();

			gridCols = target.getGridColumns();
			gridCols += gridCols/2;
		}

		@Override
		public void getSides(CalibrationObservation detections, List<Point2D_F64> sides) {
			sides.clear();
			sides.add( get(0, 0, detections.points));
			sides.add( get(0, pointCols-1, detections.points));
			sides.add( get(pointRows-1, pointCols-1, detections.points));
			sides.add( get(pointRows-1, 0, detections.points));
		}

		private Point2D_F64 get( int row , int col , List<PointIndex2D_F64> detections ) {
			return detections.get(row*pointCols+col);
		}

		@Override
		public int getBufferWidth( double gridPixelsWide ) {
			return (int)(0.15*(gridPixelsWide/gridCols)+0.5);
		}
	}
}
