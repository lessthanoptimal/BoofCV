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

import boofcv.alg.sfm.d3.VisOdomMonoPlaneRotTran;
import boofcv.alg.sfm.robust.DistancePlane2DToPixelSq;
import boofcv.alg.sfm.robust.GenerateSe2_PlanePtPixel;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageBase;
import georegression.struct.se.Se3_F64;

/**
 * @author Peter Abeles
 */
public class MonoMotion2D_to_MonocularPlaneVisualOdometry<T extends ImageBase>
		implements MonocularPlaneVisualOdometry<T>
{
	VisOdomMonoPlaneRotTran<T> alg;
	DistancePlane2DToPixelSq distance;
	GenerateSe2_PlanePtPixel generator;

	boolean fault;
	Se3_F64 cameraToWorld = new Se3_F64();

	public MonoMotion2D_to_MonocularPlaneVisualOdometry(VisOdomMonoPlaneRotTran<T> alg,
														DistancePlane2DToPixelSq distance ,
														GenerateSe2_PlanePtPixel generator ) {
		this.alg = alg;
		this.distance = distance;
		this.generator = generator;
	}

	@Override
	public void setExtrinsic(Se3_F64 planeToCamera) {
		alg.setExtrinsic(planeToCamera);
		generator.setExtrinsic(planeToCamera);
		distance.setExtrinsic(planeToCamera);
	}

	@Override
	public void setIntrinsic(IntrinsicParameters param) {
		alg.setIntrinsic(param);
		distance.setIntrinsic(param.fx,param.fy,param.skew);
	}

	@Override
	public boolean process(T input) {

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
