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

import boofcv.alg.fiducial.BaseDetectFiducialSquare;
import boofcv.alg.fiducial.StabilitySquareFiducialEstimate;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Quadrilateral_F64;

/**
 * Wrapper around {@link BaseDetectFiducialSquare} for {@link FiducialPoseDetector}
 *
 * @author Peter Abeles
 */
public abstract class BaseSquare_FiducialPoseDetector<T extends ImageSingleBand,Detector extends BaseDetectFiducialSquare<T>>
	implements FiducialPoseDetector<T>
{
	Detector alg;

	StabilitySquareFiducialEstimate stability;

	ImageType<T> type;

	public BaseSquare_FiducialPoseDetector(Detector alg) {
		this.alg = alg;
		this.type = ImageType.single(alg.getInputType());
	}

	@Override
	public void detect(T input) {
		alg.process(input);
	}

	@Override
	public void setIntrinsic(IntrinsicParameters intrinsic) {
		alg.configure(intrinsic,true);
		stability = new StabilitySquareFiducialEstimate(alg.getPoseEstimator());
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
	public int getId( int which ) {
		return alg.getFound().get(which).index;
	}

	@Override
	public ImageType<T> getInputType() {
		return type;
	}

	@Override
	public boolean computeStability(int which, double disturbance, FiducialPoseStability results) {
		Quadrilateral_F64 quad = alg.getFound().get(which).location;
		if( !this.stability.process(disturbance, quad) ) {
			return false;
		}
		results.location = stability.getLocationStability();
		results.orientation = stability.getOrientationStability();
		return true;
	}
}
