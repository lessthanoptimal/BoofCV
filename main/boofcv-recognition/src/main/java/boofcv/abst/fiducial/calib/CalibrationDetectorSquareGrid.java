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
import boofcv.alg.fiducial.calib.grid.DetectSquareGridFiducial;
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
 * Implementation of {@link DetectSingleFiducialCalibration} for square grid target types.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CalibrationDetectorSquareGrid implements DetectSingleFiducialCalibration {

	DetectSquareGridFiducial<GrayF32> detector;

	List<Point2D_F64> layoutPoints;
	CalibrationObservation detected;

	public CalibrationDetectorSquareGrid( ConfigSquareGrid configDet, ConfigGridDimen configDimen ) {

		double spaceToSquareRatio = configDimen.shapeDistance/configDimen.shapeSize;

		InputToBinary<GrayF32> inputToBinary =
				FactoryThresholdBinary.threshold(configDet.thresholding, GrayF32.class);

		DetectPolygonBinaryGrayRefine<GrayF32> detectorSquare =
				FactoryShapeDetector.polygon(configDet.square, GrayF32.class);

		detector = new DetectSquareGridFiducial<>(configDimen.numRows, configDimen.numCols,
				spaceToSquareRatio, inputToBinary, detectorSquare);

		layoutPoints = createLayout(configDimen.numRows, configDimen.numCols, configDimen.shapeSize, configDimen.shapeDistance);
	}

	@Override
	public boolean process( GrayF32 input ) {
		detected = new CalibrationObservation(input.width, input.height);
		if (detector.process(input)) {
			List<PointIndex2D_F64> found = detector.getCalibrationPoints();
			for (int i = 0; i < found.size(); i++) {
				detected.add(found.get(i).p, i);
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Creates a target that is composed of squares. The squares are spaced out and each corner provides
	 * a calibration point.
	 *
	 * @param numRows Number of rows in calibration target. Must be odd.
	 * @param numCols Number of column in each calibration target. Must be odd.
	 * @param squareWidth How wide each square is. Units are target dependent.
	 * @param spaceWidth Distance between the sides on each square. Units are target dependent.
	 * @return Target description
	 */
	public static List<Point2D_F64> createLayout( int numRows, int numCols, double squareWidth, double spaceWidth ) {
		List<Point2D_F64> all = new ArrayList<>();

		double width = (numCols*squareWidth + (numCols - 1)*spaceWidth);
		double height = (numRows*squareWidth + (numRows - 1)*spaceWidth);

		double startX = -width/2;
		double startY = -height/2;

		for (int i = numRows - 1; i >= 0; i--) {
			// this will be on the top of the black in the row
			double y = startY + i*(squareWidth + spaceWidth) + squareWidth;

			List<Point2D_F64> top = new ArrayList<>();
			List<Point2D_F64> bottom = new ArrayList<>();

			for (int j = 0; j < numCols; j++) {
				double x = startX + j*(squareWidth + spaceWidth);

				top.add(new Point2D_F64(x, y));
				top.add(new Point2D_F64(x + squareWidth, y));
				bottom.add(new Point2D_F64(x, y - squareWidth));
				bottom.add(new Point2D_F64(x + squareWidth, y - squareWidth));
			}

			all.addAll(top);
			all.addAll(bottom);
		}

		return all;
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
		if (distortion == null)
			detector.getDetectorSquare().setLensDistortion(width, height, null, null);
		else {
			Point2Transform2_F32 pointDistToUndist = distortion.undistort_F32(true, true);
			Point2Transform2_F32 pointUndistToDist = distortion.distort_F32(true, true);
			PixelTransform<Point2D_F32> distToUndist = new PointToPixelTransform_F32(pointDistToUndist);
			PixelTransform<Point2D_F32> undistToDist = new PointToPixelTransform_F32(pointUndistToDist);

			detector.getDetectorSquare().setLensDistortion(width, height, distToUndist, undistToDist);
		}
	}

	public DetectSquareGridFiducial<GrayF32> getAlgorithm() {
		return detector;
	}

	/**
	 * Returns number of rows in the grid
	 *
	 * @return number of rows
	 */
	public int getGridRows() {
		return detector.getRows();
	}

	/**
	 * Returns number of columns in the grid
	 *
	 * @return number of columns
	 */
	public int getGridColumns() {
		return detector.getColumns();
	}

	/**
	 * Returns number of rows in calibration point grid
	 *
	 * @return number of rows
	 */
	public int getPointRows() {
		return detector.getCalibrationRows();
	}

	/**
	 * Returns number of columns in calibration point grid
	 *
	 * @return number of columns
	 */
	public int getPointColumns() {
		return detector.getCalibrationCols();
	}
}
