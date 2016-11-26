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
import boofcv.abst.fiducial.calib.CalibrationDetectorCircleAsymmGrid;
import boofcv.abst.fiducial.calib.CalibrationDetectorSquareGrid;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.geometry.algs.AndrewMonotoneConvexHull_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

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
	void initialize( DetectorFiducialCalibration detector );

	/**
	 * Get the sidesCollision for collision detection with points
	 */
	void getSidesCollision(CalibrationObservation detections , List<Point2D_F64> sides );

	/**
	 * Returns a quadrilateral that's used when checking focus and if the target is being held straight
	 */
	void getQuadFocus( CalibrationObservation detections , List<Point2D_F64> sides );


	/**
	 * Returns how wide the image border should be given the visual width of the canonical fiducial
	 */
	int getBufferWidth( double gridPixelsWide );

	class Chessboard implements CalibrationView {

		int numRows,numCols;
		int pointRows,pointCols;

		public void initialize( DetectorFiducialCalibration detector ) {
			CalibrationDetectorChessboard chessboard = (CalibrationDetectorChessboard)detector;
			this.numRows = chessboard.getGridRows();
			this.numCols = chessboard.getGridColumns();

			pointRows = numRows-1;
			pointCols = numCols-1;
		}

		@Override
		public void getSidesCollision(CalibrationObservation detections, List<Point2D_F64> sides) {
			sides.clear();
			sides.add( get(0, 0, detections.points));
			sides.add( get(0, pointCols-1, detections.points));
			sides.add( get(pointRows-1, pointCols-1, detections.points));
			sides.add( get(pointRows-1, 0, detections.points));
		}

		@Override
		public void getQuadFocus(CalibrationObservation detections, List<Point2D_F64> sides) {
			getSidesCollision(detections, sides);
		}

		private Point2D_F64 get(int row , int col , List<PointIndex2D_F64> detections ) {
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

		public void initialize( DetectorFiducialCalibration detector ) {
			CalibrationDetectorSquareGrid target = (CalibrationDetectorSquareGrid)detector;
			pointRows = target.getPointRows();
			pointCols = target.getPointColumns();

			gridCols = target.getGridColumns();
			gridCols += gridCols/2;
		}

		@Override
		public void getSidesCollision(CalibrationObservation detections, List<Point2D_F64> sides) {
			sides.clear();
			sides.add( get(0, 0, detections.points));
			sides.add( get(0, pointCols-1, detections.points));
			sides.add( get(pointRows-1, pointCols-1, detections.points));
			sides.add( get(pointRows-1, 0, detections.points));
		}

		@Override
		public void getQuadFocus(CalibrationObservation detections, List<Point2D_F64> sides) {
			getSidesCollision(detections, sides);
		}

		private Point2D_F64 get( int row , int col , List<PointIndex2D_F64> detections ) {
			return detections.get(row*pointCols+col);
		}

		@Override
		public int getBufferWidth( double gridPixelsWide ) {
			return (int)(0.15*(gridPixelsWide/gridCols)+0.5);
		}
	}

	class CircleAsymmGrid implements CalibrationView {

		int gridRows, gridCols;

		double spaceToRadius;

		AndrewMonotoneConvexHull_F64 alg = new AndrewMonotoneConvexHull_F64();
		Point2D_F64 []pts = new Point2D_F64[0];
		Polygon2D_F64 poly = new Polygon2D_F64();

		public void initialize( DetectorFiducialCalibration detector ) {
			CalibrationDetectorCircleAsymmGrid target = (CalibrationDetectorCircleAsymmGrid)detector;
			gridRows = target.getRows();
			gridCols = target.getColumns();

			spaceToRadius = target.getSpaceToRadius();
		}

		@Override
		public void getSidesCollision(CalibrationObservation detections, List<Point2D_F64> sides) {
			if( pts.length != detections.size() ) {
				pts = new Point2D_F64[detections.size()];
			}
			for (int i = 0; i < pts.length; i++) {
				pts[i] = detections.get(i);
			}

			sides.clear();

			alg.process(pts,pts.length,poly);

			for (int i = 0; i < poly.size(); i++) {
				sides.add( poly.get(i));
			}

			// TODO remove a point if it's almost a line
			for (int i = 0; i < sides.size(); i++) {

			}

		}

		@Override
		public void getQuadFocus(CalibrationObservation detections, List<Point2D_F64> sides) {
			sides.clear();

			int outerCols=gridCols - (1-gridCols%2);
			int outerRows=gridRows - (1-gridRows%2);

			sides.add( detections.get(0) );
			sides.add( detections.get(getIndex(0,outerCols-1)) );
			sides.add( detections.get(getIndex(outerRows-1,outerCols-1)) );
			sides.add( detections.get(getIndex(outerRows-1,0)) );
		}

		private int getIndex( int row , int col ) {
			int evenCols = gridCols/2 + gridCols%2;
			int oddCols = gridCols/2;

			int index = (row/2+row%2)*evenCols + (row/2)*oddCols;
			if( row%2 == 0 )
				index += col/2;
			else
				index += col/2+col%2;

			return index;
		}

		@Override
		public int getBufferWidth( double gridPixelsWide ) {
			int outerCols=gridCols/2+gridCols%2;

			double spaceWidth = gridPixelsWide/(outerCols-1);
			System.out.println("grid width "+gridPixelsWide+" spaceWidth "+spaceWidth+" GRID COLS "+gridCols+"  outerCols "+outerCols);
			return (int)(1.25*spaceWidth/spaceToRadius+0.5);
		}
	}
}
