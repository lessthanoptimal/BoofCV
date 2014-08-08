/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.calib.ConfigChessboard;
import boofcv.abst.calib.ConfigSquareGrid;
import boofcv.abst.calib.PlanarCalibrationDetector;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.alg.geo.calibration.Zhang99ComputeTargetHomography;
import boofcv.alg.geo.calibration.Zhang99DecomposeHomography;
import boofcv.core.image.GConvertImage;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

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
	PlanarCalibrationDetector detector;
	// describes the calibration target's geometry
	PlanarCalibrationTarget target;
	// used to compute and decompose the homography
	Zhang99ComputeTargetHomography computeH;
	Zhang99DecomposeHomography decomposeH = new Zhang99DecomposeHomography();
	// intrinsic calibration matrix
	DenseMatrix64F K = new DenseMatrix64F(3,3);
	// transform to remove lens distortion
	PointTransform_F64 distortToUndistorted;

	// indicates if a target was detected and the found transform
	boolean targetDetected;
	Se3_F64 targetToCamera;

	// storage for converted input image.  Detector only can process ImageFloat32
	ImageFloat32 converted;

	// Expected type of input image
	ImageType<T> type;

	/**
	 * Configure it to detect chessboard style targets
	 */
	public CalibrationFiducialDetector(ConfigChessboard config, double sizeOfSquares,
									   Class<T> imageType) {
		this.detector = FactoryPlanarCalibrationTarget.detectorChessboard(config);
		this.target = FactoryPlanarCalibrationTarget.gridChess(config.numCols, config.numRows, sizeOfSquares);
		this.type = ImageType.single(imageType);
		this.converted = new ImageFloat32(1,1);

		computeH = new Zhang99ComputeTargetHomography(target.points);
	}

	/**
	 * Configure it to detect square-grid style targets
	 */
	public CalibrationFiducialDetector(ConfigSquareGrid config, double sizeOfSquares,
									   Class<T> imageType) {
		this.detector = FactoryPlanarCalibrationTarget.detectorSquareGrid(config);

		double spaceWidth = sizeOfSquares*config.spaceToSquareRatio;

		this.target = FactoryPlanarCalibrationTarget.gridSquare(
				config.numCols, config.numRows,sizeOfSquares,spaceWidth);
		this.type = ImageType.single(imageType);
		this.converted = new ImageFloat32(1,1);

		computeH = new Zhang99ComputeTargetHomography(target.points);
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

		List<Point2D_F64> points = detector.getPoints();
		for( Point2D_F64 p : points ) {
			distortToUndistorted.compute(p.x,p.y,p);
		}

		// Compute the homography
		if( !computeH.computeHomography(points) ) {
			targetDetected = false;
			return;
		}

		DenseMatrix64F H = computeH.getHomography();

		// compute camera pose from the homography matrix
		decomposeH.setCalibrationMatrix(K);
		targetToCamera = decomposeH.decompose(H);
	}

	@Override
	public void setIntrinsic(IntrinsicParameters intrinsic) {
		distortToUndistorted = LensDistortionOps.transformRadialToPixel_F64(intrinsic);
		PerspectiveOps.calibrationMatrix(intrinsic, K);
	}

	@Override
	public int totalFound() {
		return targetDetected ? 1 : 0;
	}

	@Override
	public void getFiducialToWorld(int which, Se3_F64 fiducialToSensor ) {
		if( which == 0 )
			fiducialToSensor.set(targetToCamera);
	}

	@Override
	public int getId( int which ) {
		return 0;
	}

	@Override
	public ImageType<T> getInputType() {
		return type;
	}

	public List<Point2D_F64> getCalibrationPoints() {
		return target.points;
	}
}
