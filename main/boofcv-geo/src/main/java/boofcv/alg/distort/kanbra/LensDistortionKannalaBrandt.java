/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort.kanbra;

import boofcv.alg.distort.LensDistortionWideFOV;
import boofcv.struct.calib.CameraKannalaBrandt;
import boofcv.struct.distort.Point2Transform3_F32;
import boofcv.struct.distort.Point2Transform3_F64;
import boofcv.struct.distort.Point3Transform2_F32;
import boofcv.struct.distort.Point3Transform2_F64;

/**
 * Factory for creating forwards and backwards transforms using {@link CameraKannalaBrandt}
 *
 * @author Peter Abeles
 */
public class LensDistortionKannalaBrandt implements LensDistortionWideFOV {
	CameraKannalaBrandt model;
	public LensDistortionKannalaBrandt(CameraKannalaBrandt model ) {
		this.model = model;
	}

	@Override
	public Point3Transform2_F64 distortStoP_F64() {
		return new KannalaBrandtStoP_F64(model);
	}

	@Override
	public Point3Transform2_F32 distortStoP_F32() {
		return null;//new UniOmniStoP_F32(model);
	}

	@Override
	public Point2Transform3_F64 undistortPtoS_F64() {
		return new KannalaBrandtPtoS_F64(model);
	}

	@Override
	public Point2Transform3_F32 undistortPtoS_F32() {
		return null;//new UniOmniPtoS_F32(model);
	}
}
