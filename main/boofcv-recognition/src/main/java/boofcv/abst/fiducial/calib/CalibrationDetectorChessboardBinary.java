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

package boofcv.abst.fiducial.calib;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.fiducial.calib.chess.DetectChessboardBinaryPattern;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.PointToPixelTransform_F32;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link DetectChessboardBinaryPattern} for {@link DetectSingleFiducialCalibration}
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CalibrationDetectorChessboardBinary implements DetectSingleFiducialCalibration {

	DetectChessboardBinaryPattern<GrayF32> alg;

	List<Point2D_F64> layoutPoints;
	CalibrationObservation detected;

	public CalibrationDetectorChessboardBinary( ConfigChessboardBinary configDet, ConfigGridDimen configGrid ) {

		DetectPolygonBinaryGrayRefine<GrayF32> detectorSquare =
				FactoryShapeDetector.polygon(configDet.square, GrayF32.class);

		InputToBinary<GrayF32> inputToBinary =
				FactoryThresholdBinary.threshold(configDet.thresholding, GrayF32.class);

		alg = new DetectChessboardBinaryPattern<>(
				configGrid.numRows, configGrid.numCols, configDet.maximumCornerDistance, detectorSquare, inputToBinary);

		layoutPoints = gridChess(configGrid.numRows, configGrid.numCols, configGrid.shapeSize);
	}

	@Override
	public boolean process( GrayF32 input ) {
		detected = new CalibrationObservation(input.width, input.height);
		if (alg.process(input)) {
			List<PointIndex2D_F64> found = alg.getCalibrationPoints();
			for (int i = 0; i < found.size(); i++) {
				detected.add(found.get(i).p, i);
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public CalibrationObservation getDetectedPoints() {
		return detected;
	}

	@Override
	public List<Point2D_F64> getLayout() {
		return layoutPoints;
	}

	@Override
	public void setLensDistortion( @Nullable LensDistortionNarrowFOV distortion, int width, int height ) {
		if (distortion == null) {
			alg.getFindSeeds().getDetectorSquare().setLensDistortion(width, height, null, null);
		} else {
			Point2Transform2_F32 pointDistToUndist = distortion.undistort_F32(true, true);
			Point2Transform2_F32 pointUndistToDist = distortion.distort_F32(true, true);
			PixelTransform<Point2D_F32> distToUndist = new PointToPixelTransform_F32(pointDistToUndist);
			PixelTransform<Point2D_F32> undistToDist = new PointToPixelTransform_F32(pointUndistToDist);

			alg.getFindSeeds().getDetectorSquare().setLensDistortion(width, height, distToUndist, undistToDist);
		}
	}

	/**
	 * Returns number of rows in the chessboard grid
	 *
	 * @return number of rows
	 */
	public int getGridRows() {
		return alg.getRows();
	}

	/**
	 * Returns number of columns in the chessboard grid
	 *
	 * @return number of columns
	 */
	public int getGridColumns() {
		return alg.getColumns();
	}

	public DetectChessboardBinaryPattern<GrayF32> getAlgorithm() {
		return alg;
	}

	/**
	 * This target is composed of a checkered chess board like squares. Each corner of an interior square
	 * touches an adjacent square, but the sides are separated. Only interior square corners provide
	 * calibration points.
	 *
	 * @param numRows Number of grid rows in the calibration target
	 * @param numCols Number of grid columns in the calibration target
	 * @param squareWidth How wide each square is. Units are target dependent.
	 * @return Target description
	 */
	public static List<Point2D_F64> gridChess( int numRows, int numCols, double squareWidth ) {
		List<Point2D_F64> all = new ArrayList<>();

		// convert it into the number of calibration points
		numCols = numCols - 1;
		numRows = numRows - 1;

		// center the grid around the origin. length of a size divided by two
		double startX = -((numCols - 1)*squareWidth)/2.0;
		double startY = -((numRows - 1)*squareWidth)/2.0;

		for (int i = numRows - 1; i >= 0; i--) {
			double y = startY + i*squareWidth;
			for (int j = 0; j < numCols; j++) {
				double x = startX + j*squareWidth;
				all.add(new Point2D_F64(x, y));
			}
		}

		return all;
	}
}
