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

import boofcv.alg.distort.AdjustmentType;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.fiducial.BaseDetectFiducialSquare;
import boofcv.core.image.border.BorderType;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import georegression.struct.se.Se3_F64;

/**
 * Wrapper around {@link BaseDetectFiducialSquare} for {@link FiducialDetector}
 *
 * @author Peter Abeles
 */
public abstract class BaseSquare_FiducialDetector<T extends ImageSingleBand,Detector extends BaseDetectFiducialSquare<T>>
	implements FiducialDetector<T>
{
	Detector alg;

	ImageType<T> type;

	boolean isDistorted;
	IntrinsicParameters intrinsicUndist = new IntrinsicParameters();
	T undistorted;
	ImageDistort<T,T> undistorter;

	public BaseSquare_FiducialDetector(Detector alg) {
		this.alg = alg;
		this.type = ImageType.single(alg.getInputType());
		undistorted = type.createImage(1,1);
	}

	@Override
	public void detect(T input) {
		if( isDistorted ) {
			undistorter.apply(input,undistorted);
			alg.process(undistorted);
		} else {
			alg.process(input);
		}
	}

	@Override
	public void setIntrinsic(IntrinsicParameters intrinsic) {
		if( intrinsic.isDistorted() ) {
			isDistorted = true;
			undistorted.reshape(intrinsic.width,intrinsic.height);
			undistorter = LensDistortionOps.removeDistortion(
					AdjustmentType.ALL_INSIDE, BorderType.EXTENDED, intrinsic, intrinsicUndist,type);
			alg.configure(intrinsicUndist);
		} else {
			isDistorted = false;
			alg.configure(intrinsic);
		}
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
}
