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

import boofcv.alg.fiducial.square.BaseDetectFiducialSquare;
import boofcv.alg.fiducial.square.FoundFiducial;
import boofcv.alg.fiducial.square.StabilitySquareFiducialEstimate;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilLine2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Quadrilateral_F64;

/**
 * Wrapper around {@link BaseDetectFiducialSquare} for {@link FiducialDetector}
 *
 * @author Peter Abeles
 */
public abstract class BaseSquare_FiducialDetector<T extends ImageGray,Detector extends BaseDetectFiducialSquare<T>>
	implements FiducialDetector<T>
{
	Detector alg;

	StabilitySquareFiducialEstimate stability;

	ImageType<T> type;

	CameraPinholeRadial intrinsic;

	// Used for finding the center of the square
	LineGeneral2D_F64 line02 = new LineGeneral2D_F64();
	LineGeneral2D_F64 line13 = new LineGeneral2D_F64();

	public BaseSquare_FiducialDetector(Detector alg) {
		this.alg = alg;
		this.type = ImageType.single(alg.getInputType());
	}

	@Override
	public void detect(T input) {
		alg.process(input);
	}

	@Override
	public void setIntrinsic(CameraPinholeRadial intrinsic) {
		this.intrinsic = intrinsic;
		alg.configure(intrinsic,true);
		stability = new StabilitySquareFiducialEstimate(alg.getPoseEstimator());
	}

	@Override
	public CameraPinholeRadial getIntrinsics() {
		return intrinsic;
	}

	/**
	 * Return the intersection of two lines defined by opposing corners.  This should also be the geometric center
	 * @param which Fiducial's index
	 * @param location (output) Storage for the transform. modified.
	 */
	@Override
	public void getImageLocation(int which, Point2D_F64 location) {
		FoundFiducial f = alg.getFound().get(which);

		// compute intersection in undistorted pixels so that the intersection is the true
		// geometric center of the square
		UtilLine2D_F64.convert(f.locationUndist.a,f.locationUndist.c,line02);
		UtilLine2D_F64.convert(f.locationUndist.b,f.locationUndist.d,line13);

		Intersection2D_F64.intersection(line02,line13,location);

		// apply lens distortion to the point so that it appears in the correct location
		alg.getPointUndistToDist().compute(location.x,location.y, location);
	}

	@Override
	public int totalFound() {
		return alg.getFound().size;
	}

	@Override
	public void getFiducialToCamera(int which, Se3_F64 fiducialToCamera) {
		fiducialToCamera.set(alg.getFound().get(which).targetToSensor);
	}

	@Override
	public long getId( int which ) {
		return alg.getFound().get(which).id;
	}

	@Override
	public ImageType<T> getInputType() {
		return type;
	}

	@Override
	public boolean computeStability(int which, double disturbance, FiducialStability results) {
		Quadrilateral_F64 quad = alg.getFound().get(which).locationDist;
		if( !this.stability.process(disturbance, quad) ) {
			return false;
		}
		results.location = stability.getLocationStability();
		results.orientation = stability.getOrientationStability();
		return true;
	}

	@Override
	public boolean isSupportedID() {
		return true;
	}

	@Override
	public boolean isSupportedPose() {
		return intrinsic != null;
	}

	@Override
	public boolean isSizeKnown() {
		return true;
	}

	public Detector getAlgorithm() {
		return alg;
	}
}
