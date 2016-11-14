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

package boofcv.abst.fiducial;

import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.fiducial.calib.ConfigCircleAsymmetricGrid;
import boofcv.abst.fiducial.calib.ConfigSquareGrid;
import boofcv.abst.fiducial.calib.ConfigSquareGridBinary;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.core.image.GConvertImage;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper which allows a calibration target to be used like a fiducial for pose estimation.  The
 * origin of the coordinate system depends on the default for the calibration target.
 *
 * @author Peter Abeles
 */
public class CalibrationFiducialDetector<T extends ImageGray>
		extends FiducialDetectorPnP<T>
{
	// detects the calibration target
	private DetectorFiducialCalibration detector;

	// indicates if a target was detected and the found transform
	private boolean targetDetected;

	// storage for converted input image.  Detector only can process GrayF32
	private GrayF32 converted;

	// Expected type of input image
	private ImageType<T> type;

	// Known 3D location of points on calibration grid and current observations
	private List<Point2D3D> points2D3D;

	// average of width and height
	private double width;

	/**
	 * Configure it to detect chessboard style targets
	 */
	public CalibrationFiducialDetector(ConfigChessboard config,
									   Class<T> imageType) {
		DetectorFiducialCalibration detector = FactoryFiducialCalibration.chessboard(config);
		double sideWidth = config.numCols*config.squareWidth;
		double sideHeight = config.numRows*config.squareWidth;

		width = (sideWidth+sideHeight)/2.0;

		init(detector, width, imageType);
	}

	/**
	 * Configure it to detect square-grid style targets
	 */
	public CalibrationFiducialDetector(ConfigSquareGrid config,
									   Class<T> imageType) {
		DetectorFiducialCalibration detector = FactoryFiducialCalibration.squareGrid(config);
		int squareCols = config.numCols;
		int squareRows = config.numRows;
		double sideWidth = squareCols* config.squareWidth + (squareCols-1)*config.spaceWidth;
		double sideHeight = squareRows*config.squareWidth + (squareRows-1)*config.spaceWidth;

		double width = (sideWidth+sideHeight)/2.0;

		init(detector, width, imageType);
	}

	/**
	 * Configure it to detect square-grid style targets
	 */
	public CalibrationFiducialDetector(ConfigSquareGridBinary config,
									   Class<T> imageType) {
		DetectorFiducialCalibration detector = FactoryFiducialCalibration.binaryGrid(config);
		int squareCols = config.numCols;
		int squareRows = config.numRows;
		double sideWidth = squareCols*config.squareWidth + (squareCols-1)*config.spaceWidth;
		double sideHeight = squareRows*config.squareWidth + (squareRows-1)*config.spaceWidth;

		double width = (sideWidth+sideHeight)/2.0;

		init(detector, width, imageType);
	}

	public CalibrationFiducialDetector(ConfigCircleAsymmetricGrid config,
									   Class<T> imageType) {
		DetectorFiducialCalibration detector = FactoryFiducialCalibration.circleAsymmGrid(config);
		int squareCols = config.numCols;
		int squareRows = config.numRows;
		double sideWidth = squareCols*config.centerDistance/2.0;
		double sideHeight = squareRows*config.centerDistance/2.0;

		double width = (sideWidth+sideHeight)/2.0;

		init(detector, width, imageType);
	}

	protected void init(DetectorFiducialCalibration detector, double width, Class<T> imageType) {
		this.detector = detector;
		this.type = ImageType.single(imageType);
		this.converted = new GrayF32(1,1);

		this.width = width;

		List<Point2D_F64> layout = detector.getLayout();
		points2D3D = new ArrayList<>();
		for (int i = 0; i < layout.size(); i++) {
			Point2D_F64 p2 = layout.get(i);
			Point2D3D p = new Point2D3D();
			p.location.set(p2.x,p2.y,0);

			points2D3D.add(p);
		}
	}

	@Override
	public void detect(T input) {
		if( input instanceof GrayF32) {
			converted = (GrayF32)input;
		} else {
			converted.reshape(input.width,input.height);
			GConvertImage.convert(input, converted);
		}

		if( !detector.process(converted) ) {
			targetDetected = false;
			return;
		} else {
			targetDetected = true;
		}
	}

	/**
	 * Returns the detection point average location.  This will NOT be the same as the geometric center.
	 *
	 * @param which Fiducial's index
	 * @param location (output) Storage for the transform. modified.
	 */
	@Override
	public void getImageLocation(int which, Point2D_F64 location) {
		CalibrationObservation view = detector.getDetectedPoints();

		location.set(0,0);
		for (int i = 0; i < view.size(); i++) {
			PointIndex2D_F64 p = view.get(i);
			location.x += p.x;
			location.y += p.y;
		}

		location.x /= view.size();
		location.y /= view.size();
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
	public String getMessage(int which) {
		return null;
	}

	@Override
	public ImageType<T> getInputType() {
		return type;
	}

	public List<Point2D_F64> getCalibrationPoints() {
		return detector.getLayout();
	}

	public DetectorFiducialCalibration getCalibDetector() {
		return detector;
	}

	public List<Point2D3D> getPoints2D3D() {
		return points2D3D;
	}

	@Override
	public double getWidth(int which) {
		return width;
	}

	@Override
	public boolean hasUniqueID() {
		return false;
	}

	@Override
	public boolean hasMessage() {
		return false;
	}

	@Override
	protected List<PointIndex2D_F64> getDetectedControl(int which) {
		CalibrationObservation view = getCalibDetector().getDetectedPoints();
		return view.points;
	}

	@Override
	protected List<Point2D3D> getControl3D(int which) {
		return getPoints2D3D();
	}
}
