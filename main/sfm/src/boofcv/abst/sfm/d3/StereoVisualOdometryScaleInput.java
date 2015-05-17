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

package boofcv.abst.sfm.d3;

import boofcv.abst.distort.FDistort;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.se.Se3_F64;

/**
 * Wrapper around {@link StereoVisualOdometry} which scales the input images.
 *
 * @author Peter Abeles
 */
// TODO more efficient scaling algorithm
public class StereoVisualOdometryScaleInput<T extends ImageBase> implements StereoVisualOdometry<T>{

	double scaleFactor;

	StereoParameters scaleParameter;

	T scaleLeft;
	T scaleRight;

	StereoVisualOdometry<T> alg;

	public StereoVisualOdometryScaleInput( StereoVisualOdometry<T> alg , double scaleFactor ) {
		this.alg = alg;
      this.scaleFactor = scaleFactor;
		scaleLeft = alg.getImageType().createImage(1, 1);
		scaleRight = alg.getImageType().createImage(1, 1);
	}

	@Override
	public void setCalibration(StereoParameters parameters) {
		scaleParameter = new StereoParameters(parameters);

		PerspectiveOps.scaleIntrinsic(scaleParameter.left,scaleFactor);
		PerspectiveOps.scaleIntrinsic(scaleParameter.right,scaleFactor);

		scaleLeft.reshape(scaleParameter.left.width,scaleParameter.left.height);
		scaleRight.reshape(scaleParameter.right.width,scaleParameter.right.height);

      alg.setCalibration(scaleParameter);
	}

	@Override
	public boolean process(T leftImage, T rightImage) {

		new FDistort(leftImage,scaleLeft).scaleExt().apply();
		new FDistort(rightImage,scaleRight).scaleExt().apply();

		return alg.process(scaleLeft,scaleRight);
	}

	@Override
	public ImageType<T> getImageType() {
		return alg.getImageType();
	}

	@Override
	public void reset() {
		alg.reset();
	}

	@Override
	public boolean isFault() {
		return alg.isFault();
	}

	@Override
	public Se3_F64 getCameraToWorld() {
		return alg.getCameraToWorld();
	}
}
