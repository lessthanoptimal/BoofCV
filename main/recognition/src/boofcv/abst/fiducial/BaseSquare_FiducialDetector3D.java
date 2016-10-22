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

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.fiducial.square.BaseDetectFiducialSquare;
import boofcv.alg.fiducial.square.DetectFiducialSquareBinary;
import boofcv.alg.fiducial.square.FoundFiducial;
import boofcv.alg.fiducial.square.QuadPoseEstimator;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.ImageGray;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Quadrilateral_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link DetectFiducialSquareBinary} for {@link FiducialDetector3D}
 *
 * @author Peter Abeles
 */
public abstract class BaseSquare_FiducialDetector3D<T extends ImageGray>
	extends FiducialDetectorToDetector3D<T>
	implements FiducialDetector3D<T>
{
	private BaseSquare_FiducialDetector<T, ?> detector;

	// used to compute 3D pose of target
	QuadPoseEstimator poseEstimator = new QuadPoseEstimator(1e-6,200);

	Quadrilateral_F64 quad = new Quadrilateral_F64();
	List<PointIndex2D_F64> listQuad = new ArrayList<>();

	public BaseSquare_FiducialDetector3D(BaseDetectFiducialSquare<T> alg) {
		this(new BaseSquare_FiducialDetector(alg));
	}

	public BaseSquare_FiducialDetector3D(BaseSquare_FiducialDetector<T,?> detector) {
		super(detector);
		this.detector = detector;
		// add corner points in target frame.  Used to compute homography.  Target's center is at its origin
		// see comment in class JavaDoc above.  Note that the target's length is one below.  The scale factor
		// will be provided later one
		poseEstimator.setFiducial(-0.5, 0.5, 0.5, 0.5, 0.5, -0.5, -0.5, -0.5);

		for (int i = 0; i < 4; i++) {
			listQuad.add( new PointIndex2D_F64());
			listQuad.get(i).index = i;
		}
	}

	@Override
	public void setLensDistortion(LensDistortionNarrowFOV distortion) {
		super.setLensDistortion(distortion);
		poseEstimator.setLensDistoriton(distortion);
		detector.distToUndist = distortion.undistort_F64(true,true);
		detector.undistToDist = distortion.distort_F64(true,true);
	}

	@Override
	public boolean getFiducialToCamera(int which, Se3_F64 fiducialToCamera) {
		if( super.getFiducialToCamera(which, fiducialToCamera) ) {
			GeometryMath_F64.scale(fiducialToCamera.getT(), getWidth(which));
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected boolean estimatePose(List<Point2D3D> points, Se3_F64 fiducialToCamera) {
		quad.a.set( points.get(0).observation );
		quad.b.set( points.get(1).observation );
		quad.c.set( points.get(2).observation );
		quad.d.set( points.get(3).observation );

		if( !poseEstimator.process(quad) ) {
			return false;
		}

		fiducialToCamera.set( poseEstimator.getWorldToCamera() );
		return true;
	}

	@Override
	protected List<PointIndex2D_F64> getDetectedControl(int which) {
		FoundFiducial found = detector.getAlgorithm().getFound().get(which);
		listQuad.get(0).set( found.locationPixels.a );
		listQuad.get(1).set( found.locationPixels.b );
		listQuad.get(2).set( found.locationPixels.c );
		listQuad.get(3).set( found.locationPixels.d );

		return listQuad;
	}

	@Override
	protected List<Point2D3D> getControl3D(int which) {
		return poseEstimator.getPoints2D3D();
	}

	public BaseSquare_FiducialDetector<T, ?> getDetectorImage() {
		return detector;
	}
}
