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

package boofcv.abst.fiducial;

import boofcv.abst.fiducial.calib.*;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.core.image.GConvertImage;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.fitting.polygon.FitPolygon2D_F64;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ejml.UtilEjml;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper which allows a calibration target to be used like a fiducial for pose estimation. The
 * origin of the coordinate system depends on the default for the calibration target.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CalibrationFiducialDetector<T extends ImageGray<T>> extends FiducialDetectorPnP<T> {
	// detects the calibration target
	private DetectSingleFiducialCalibration detector;

	// indicates if a target was detected and the found transform
	private boolean targetDetected;

	// storage for converted input image. Detector only can process GrayF32
	private GrayF32 converted;

	// Expected type of input image
	private ImageType<T> type;

	// Known 3D location of points on calibration grid and current observations
	private List<Point2D3D> points2D3D;

	// actual width and height of the fiducial
	double sideWidth;
	double sideHeight;

	// average of width and height
	private double width;

	// The 4 corners which are selected to be the boundary
	int[] boundaryIndexes;

	@Nullable Point2Transform2_F64 pointUndistToDist;

	/**
	 * Configure it to detect chessboard style targets
	 */
	public CalibrationFiducialDetector( @Nullable ConfigChessboardBinary configDet,
										ConfigGridDimen configGrid,
										Class<T> imageType ) {
		DetectSingleFiducialCalibration detector = FactoryFiducialCalibration.chessboardB(configDet, configGrid);
		sideWidth = configGrid.numCols*configGrid.shapeSize;
		sideHeight = configGrid.numRows*configGrid.shapeSize;

		width = (sideWidth + sideHeight)/2.0;

		init(detector, width, imageType);
	}

	/**
	 * Configure it to detect chessboard style targets
	 */
	public CalibrationFiducialDetector( @Nullable ConfigChessboardX configDet,
										ConfigGridDimen configGrid,
										Class<T> imageType ) {
		DetectSingleFiducialCalibration detector = FactoryFiducialCalibration.chessboardX(configDet, configGrid);
		sideWidth = configGrid.numCols*configGrid.shapeSize;
		sideHeight = configGrid.numRows*configGrid.shapeSize;

		width = (sideWidth + sideHeight)/2.0;

		init(detector, width, imageType);
	}

	/**
	 * Configure it to detect square-grid style targets
	 */
	public CalibrationFiducialDetector( @Nullable ConfigSquareGrid configDet,
										ConfigGridDimen configGrid,
										Class<T> imageType ) {
		DetectSingleFiducialCalibration detector = FactoryFiducialCalibration.squareGrid(configDet, configGrid);
		int squareCols = configGrid.numCols;
		int squareRows = configGrid.numRows;
		sideWidth = squareCols*configGrid.shapeSize + (squareCols - 1)*configGrid.shapeDistance;
		sideHeight = squareRows*configGrid.shapeSize + (squareRows - 1)*configGrid.shapeDistance;

		double width = (sideWidth + sideHeight)/2.0;

		init(detector, width, imageType);
	}

	public CalibrationFiducialDetector( @Nullable ConfigCircleHexagonalGrid configDet,
										ConfigGridDimen configGrid,
										Class<T> imageType ) {
		CalibrationDetectorCircleHexagonalGrid detector =
				FactoryFiducialCalibration.circleHexagonalGrid(configDet, configGrid);
		int squareCols = configGrid.numCols;
		int squareRows = configGrid.numRows;
		sideWidth = squareCols*configGrid.shapeDistance/2.0;
		sideHeight = squareRows*configGrid.shapeDistance/2.0;

		double width = (sideWidth + sideHeight)/2.0;

		init(detector, width, imageType);
	}

	public CalibrationFiducialDetector( @Nullable ConfigCircleRegularGrid configDet,
										ConfigGridDimen configGrid,
										Class<T> imageType ) {
		DetectSingleFiducialCalibration detector = FactoryFiducialCalibration.circleRegularGrid(configDet, configGrid);
		sideWidth = (configGrid.numCols - 1)*configGrid.shapeDistance;
		sideHeight = (configGrid.numRows - 1)*configGrid.shapeDistance;

		double width = (sideWidth + sideHeight)/2.0;

		init(detector, width, imageType);
	}

	protected void init( DetectSingleFiducialCalibration detector, double width, Class<T> imageType ) {
		this.detector = detector;
		this.type = ImageType.single(imageType);
		this.converted = new GrayF32(1, 1);

		this.width = width;

		List<Point2D_F64> layout = detector.getLayout();
		points2D3D = new ArrayList<>();
		for (int i = 0; i < layout.size(); i++) {
			Point2D_F64 p2 = layout.get(i);
			Point2D3D p = new Point2D3D();
			p.location.setTo(p2.x, p2.y, 0);

			points2D3D.add(p);
		}

		selectBoundaryCorners();
	}

	@Override
	public double getSideWidth( int which ) {
		return sideWidth;
	}

	@Override
	public double getSideHeight( int which ) {
		return sideHeight;
	}

	/**
	 * Selects points which will be the corners in the boundary. Finds the convex hull.
	 */
	protected void selectBoundaryCorners() {
		List<Point2D_F64> layout = detector.getLayout();

		Polygon2D_F64 hull = new Polygon2D_F64();
		FitPolygon2D_F64.convexHull(layout, hull);
		UtilPolygons2D_F64.removeAlmostParallel(hull, 0.02);

		boundaryIndexes = new int[hull.size()];
		for (int i = 0; i < hull.size(); i++) {
			Point2D_F64 h = hull.get(i);
			boolean matched = false;
			for (int j = 0; j < layout.size(); j++) {
				if (h.isIdentical(layout.get(j), 1e-6)) {
					matched = true;
					boundaryIndexes[i] = j;
					break;
				}
			}
			if (!matched)
				throw new RuntimeException("Bug!");
		}
	}

	@Override
	public void detect( T input ) {
		if (input instanceof GrayF32) {
			converted = (GrayF32)input;
		} else {
			converted.reshape(input.width, input.height);
			GConvertImage.convert(input, converted);
		}

		if (!detector.process(converted)) {
			targetDetected = false;
		} else {
			targetDetected = true;

			// put detected corners back into distorted coordinates
			// Required for FiducialDetectorPnP
			if (pointUndistToDist != null) {
				CalibrationObservation detected = detector.getDetectedPoints();
				for (int i = 0; i < detected.size(); i++) {
					Point2D_F64 p = detected.get(i).p;
					pointUndistToDist.compute(p.x, p.y, p);
				}
			}
		}
	}

	@Override
	public void setLensDistortion( @Nullable LensDistortionNarrowFOV distortion, int width, int height ) {
		super.setLensDistortion(distortion, width, height);

		if (distortion == null)
			pointUndistToDist = null;
		else {
			// verify that distortion is actually applied. If not don't undistort the image while extracting features
			// this makes it run faster
			pointUndistToDist = distortion.distort_F64(true, true);
			Point2D_F64 test = new Point2D_F64();
			pointUndistToDist.compute(0, 0, test);
			if (test.norm() <= UtilEjml.TEST_F32) {
				detector.setLensDistortion(null, width, height);
				pointUndistToDist = null;
			} else {
				detector.setLensDistortion(distortion, width, height);
			}
		}
	}

	/**
	 * Returns the detection point average location. This will NOT be the same as the geometric center.
	 *
	 * @param which Fiducial's index
	 * @param location (output) Storage for the transform. modified.
	 */
	@Override
	public void getCenter( int which, Point2D_F64 location ) {
		CalibrationObservation view = detector.getDetectedPoints();

		location.setTo(0, 0);
		for (int i = 0; i < view.size(); i++) {
			Point2D_F64 p = view.get(i).p;
			location.x += p.x;
			location.y += p.y;
		}

		location.x /= view.size();
		location.y /= view.size();
	}

	@Override
	public Polygon2D_F64 getBounds( int which, @Nullable Polygon2D_F64 storage ) {
		if (storage == null)
			storage = new Polygon2D_F64();
		else
			storage.vertexes.reset();

		List<PointIndex2D_F64> control = getDetectedControl(which);
		for (int i = 0; i < boundaryIndexes.length; i++) {
			PointIndex2D_F64 p = control.get(boundaryIndexes[i]);
			if (p.index == boundaryIndexes[i])
				storage.vertexes.grow().setTo(p.p);
			else
				System.out.println("control points are out of order or not detected");
		}

		return storage;
	}

	@Override
	public int totalFound() {
		return targetDetected ? 1 : 0;
	}

	@Override
	public long getId( int which ) {
		return 0;
	}

	@Override
	public String getMessage( int which ) {return "";}

	@Override
	public ImageType<T> getInputType() {
		return type;
	}

	public List<Point2D_F64> getCalibrationPoints() {
		return detector.getLayout();
	}

	public DetectSingleFiducialCalibration getCalibDetector() {
		return detector;
	}

	public List<Point2D3D> getPoints2D3D() {
		return points2D3D;
	}

	@Override
	public double getWidth( int which ) {
		return width;
	}

	@Override
	public boolean hasID() {
		return false;
	}

	@Override
	public boolean hasMessage() {
		return false;
	}

	@Override
	public List<PointIndex2D_F64> getDetectedControl( int which ) {
		CalibrationObservation view = getCalibDetector().getDetectedPoints();
		return view.points;
	}

	@Override
	protected List<Point2D3D> getControl3D( int which ) {
		return getPoints2D3D();
	}
}
