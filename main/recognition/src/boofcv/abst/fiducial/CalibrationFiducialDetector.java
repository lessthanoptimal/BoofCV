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

package boofcv.abst.fiducial;

import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.fiducial.calib.ConfigSquareGrid;
import boofcv.abst.fiducial.calib.ConfigSquareGridBinary;
import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.abst.geo.RefinePnP;
import boofcv.abst.geo.calibration.CalibrationDetector;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.core.image.GConvertImage;
import boofcv.factory.calib.FactoryCalibrationTarget;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper which allows a calibration target to be used like a fiducial for pose estimation.  The
 * origin of the coordinate system depends on the default for the calibration target.
 *
 * @author Peter Abeles
 */
public class CalibrationFiducialDetector<T extends ImageSingleBand>
		implements FiducialDetector<T>
{
	// detects the calibration target
	CalibrationDetector detector;
	// transform to remove lens distortion
	PointTransform_F64 distortToUndistorted;

	// non-linear refinement of pose estimate
	Estimate1ofPnP estimatePnP;
	RefinePnP refinePnP;

	// indicates if a target was detected and the found transform
	boolean targetDetected;
	Se3_F64 initialEstimate = new Se3_F64();
	Se3_F64 targetToCamera = new Se3_F64(); // refined

	// storage for converted input image.  Detector only can process ImageFloat32
	ImageFloat32 converted;

	// Expected type of input image
	ImageType<T> type;

	// Known 3D location of points on calibration grid and current obsrevations
	List<Point2D3D> points2D3D;
	List<Point2D3D> used2D3D = new ArrayList<Point2D3D>();

	// average of width and height
	double width;

	/**
	 * Configure it to detect chessboard style targets
	 */
	public CalibrationFiducialDetector(ConfigChessboard config,
									   Class<T> imageType) {
		CalibrationDetector detector = FactoryCalibrationTarget.detectorChessboard(config);
		double sideWidth = config.numCols*config.squareWidth;
		double sideHeight = config.numRows*config.squareWidth;

		double width = (sideWidth+sideHeight)/2.0;

		init(detector, width, imageType);
	}

	/**
	 * Configure it to detect square-grid style targets
	 */
	public CalibrationFiducialDetector(ConfigSquareGrid config,
									   Class<T> imageType) {
		CalibrationDetector detector = FactoryCalibrationTarget.detectorSquareGrid(config);
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
		CalibrationDetector detector = FactoryCalibrationTarget.detectorBinaryGrid(config);
		int squareCols = config.numCols;
		int squareRows = config.numRows;
		double sideWidth = squareCols*config.squareWidth + (squareCols-1)*config.spaceWidth;
		double sideHeight = squareRows*config.squareWidth + (squareRows-1)*config.spaceWidth;

		double width = (sideWidth+sideHeight)/2.0;

		init(detector, width, imageType);
	}

	protected void init(CalibrationDetector detector, double width, Class<T> imageType) {
		this.detector = detector;
		this.type = ImageType.single(imageType);
		this.converted = new ImageFloat32(1,1);

		this.width = width;

		List<Point2D_F64> layout = detector.getLayout();
		points2D3D = new ArrayList<Point2D3D>();
		for (int i = 0; i < layout.size(); i++) {
			Point2D_F64 p2 = layout.get(i);
			Point2D3D p = new Point2D3D();
			p.location.set(p2.x,p2.y,0);

			points2D3D.add(p);
		}

		// TODO more robust estimator that uses multiple approaches.  Similar to 4 point estimator
//		this.estimatePnP = FactoryMultiView.computePnPwithEPnP(10, 0.1);
		this.estimatePnP = FactoryMultiView.computePnPwithEPnP(10, 0.2);
		this.refinePnP = FactoryMultiView.refinePnP(1e-8,100);
	}

	@Override
	public void detect(T input) {

		if( input instanceof ImageFloat32 ) {
			converted = (ImageFloat32)input;
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

		// convert points into normalized image coord
		CalibrationObservation view = detector.getDetectedPoints();
		if( view.size() >= 3 ) {
			used2D3D.clear();
			for (int i = 0; i < view.size(); i++) {
				int gridIndex = view.get(i).index;

				Point2D3D p23 = points2D3D.get(gridIndex);
				Point2D_F64 pixel = view.get(i).pixel;

				distortToUndistorted.compute(pixel.x, pixel.y, p23.observation);

				used2D3D.add(p23);
			}

			// estimate using PNP
			targetDetected = estimatePose(targetToCamera);
		} else {
			targetDetected = false;
		}
	}

	private boolean estimatePose( Se3_F64 targetToCamera ) {
		return estimatePnP.process(used2D3D, initialEstimate) &&
				refinePnP.fitModel(used2D3D, initialEstimate, targetToCamera);
	}

	Se3_F64 referenceCameraToTarget = new Se3_F64();
	Se3_F64 targetToCameraSample = new Se3_F64();
	Se3_F64 difference = new Se3_F64();
	private Rodrigues_F64 rodrigues = new Rodrigues_F64();
	// compute metrics.
	private double maxLocation;
	private double maxOrientation;

	/**
	 * Estimates the stability by perturbing each land mark by the specified number of pixels in the distorted image.
	 */
	@Override
	public boolean computeStability(int which, double disturbance, FiducialStability results) {

		if( !targetDetected )
			return false;

		targetToCamera.invert(referenceCameraToTarget);

		CalibrationObservation view = detector.getDetectedPoints();
		Point2D_F64 workPt = new Point2D_F64();

		maxOrientation = 0;
		maxLocation = 0;
		for (int i = 0; i < view.size(); i++) {
			int gridIndex = view.get(i).index;

			Point2D3D p23 = points2D3D.get(gridIndex);
			Point2D_F64 p = view.get(i).pixel;
			workPt.set(p);

			perturb(disturbance,workPt,p,p23);
		}

		results.location = maxLocation;
		results.orientation = maxOrientation;

		return true;
	}

	private void perturb( double disturbance , Point2D_F64 pixel , Point2D_F64 original , Point2D3D p23 ) {
		pixel.x = original.x + disturbance;
		computeDisturbance(pixel, p23);
		pixel.x = original.x - disturbance;
		computeDisturbance(pixel, p23);
		pixel.y = original.y;
		pixel.y = original.y + disturbance;
		computeDisturbance(pixel, p23);
		pixel.y = original.y - disturbance;
		computeDisturbance(pixel, p23);
	}

	private void computeDisturbance(Point2D_F64 pixel, Point2D3D p23) {
		distortToUndistorted.compute(pixel.x,pixel.y,p23.observation);
		if( estimatePose(targetToCameraSample) ) {
			referenceCameraToTarget.concat(targetToCameraSample, difference);

			double d = difference.getT().norm();
			RotationMatrixGenerator.matrixToRodrigues(difference.getR(), rodrigues);
			double theta = Math.abs(rodrigues.theta);
			if (theta > maxOrientation) {
				maxOrientation = theta;
			}
			if (d > maxLocation) {
				maxLocation = d;
			}
		}
	}

	@Override
	public void setIntrinsic(IntrinsicParameters intrinsic) {
		distortToUndistorted = LensDistortionOps.transformPoint(intrinsic).undistort_F64(true,false);
	}

	@Override
	public int totalFound() {
		return targetDetected ? 1 : 0;
	}

	@Override
	public void getFiducialToCamera(int which, Se3_F64 fiducialToCamera) {
		if( which == 0 )
			fiducialToCamera.set(targetToCamera);
	}

	@Override
	public long getId( int which ) {
		return 0;
	}

	@Override
	public double getWidth(int which) {
		return width;
	}

	@Override
	public ImageType<T> getInputType() {
		return type;
	}

	@Override
	public boolean isSupportedID() {
		return false;
	}

	@Override
	public boolean isSupportedPose() {
		return true;
	}

	@Override
	public boolean isSizeKnown() {
		return true;
	}

	public List<Point2D_F64> getCalibrationPoints() {
		return detector.getLayout();
	}

	public CalibrationDetector getDetector() {
		return detector;
	}
}
