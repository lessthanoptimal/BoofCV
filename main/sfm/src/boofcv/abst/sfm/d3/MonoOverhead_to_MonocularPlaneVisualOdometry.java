/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.sfm.d3.VisOdomMonoOverheadMotion2D;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageBase;
import georegression.struct.se.Se3_F64;

/**
 * Wrapper around {@link VisOdomMonoOverheadMotion2D} for {@link MonocularPlaneVisualOdometry}.
 *
 * @author Peter Abeles
 */
public class MonoOverhead_to_MonocularPlaneVisualOdometry<T extends ImageBase>
		implements MonocularPlaneVisualOdometry<T>
{
	// motion estimation algorithm
	VisOdomMonoOverheadMotion2D<T> alg;

	// indicates if camera parameters have changed
	boolean configChanged = false;

	// camera parameters
	Se3_F64 planeToCamera;
	IntrinsicParameters param;

	boolean fault;
	Se3_F64 cameraToWorld = new Se3_F64();

	public MonoOverhead_to_MonocularPlaneVisualOdometry(VisOdomMonoOverheadMotion2D<T> alg) {
		this.alg = alg;
	}

	@Override
	public void setExtrinsic(Se3_F64 planeToCamera) {
		this.planeToCamera = planeToCamera;
		configChanged = true;
	}

	@Override
	public void setIntrinsic(IntrinsicParameters param) {
		this.param = param;
		configChanged = true;
	}

	@Override
	public boolean process(T input) {
		if( configChanged ) {
			configChanged = false;
			if( planeToCamera == null || param == null )
				throw new IllegalArgumentException("Both extrinsic and intrinsic parameters need to be specified");
			alg.configureCamera(param, planeToCamera);
		}

		fault = alg.process(input);
		return fault;
	}

	@Override
	public void reset() {
		alg.reset();
	}

	@Override
	public boolean isFault() {
		return fault;
	}

	@Override
	public Se3_F64 getCameraToWorld() {

		Se3_F64 worldToCamera = alg.getWorldToCurr3D();
		worldToCamera.invert(cameraToWorld);

		return cameraToWorld;
	}
}
