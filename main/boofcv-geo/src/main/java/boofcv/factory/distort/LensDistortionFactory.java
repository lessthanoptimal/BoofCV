/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.distort;

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.distort.LensDistortionWideFOV;
import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.alg.distort.kanbra.LensDistortionKannalaBrandt;
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.distort.universal.LensDistortionUniversalOmni;
import boofcv.struct.calib.*;

/**
 * Factory for lens distortion given different built-in camera models.
 *
 * @author Peter Abeles
 */
public class LensDistortionFactory {
	/**
	 * <p>
	 * Creates the {@link LensDistortionNarrowFOV lens distortion} for the specified camera parameters.
	 * </p>
	 */
	public static LensDistortionNarrowFOV narrow( CameraModel param ) {
		if (param instanceof CameraPinholeBrown) {
			CameraPinholeBrown c = (CameraPinholeBrown)param;

			if (c.isDistorted())
				return new LensDistortionBrown(c);
			else
				return new LensDistortionPinhole(c);
		} else if (param instanceof CameraPinhole) {
			CameraPinhole c = (CameraPinhole)param;

			return new LensDistortionPinhole(c);
		} else {
			throw new IllegalArgumentException("Unknown camera model " + param.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Creates the {@link LensDistortionWideFOV lens distortion} for the specified camera parameters.
	 * </p>
	 */
	public static LensDistortionWideFOV wide( CameraModel param ) {
		if (param instanceof CameraUniversalOmni) {
			return new LensDistortionUniversalOmni((CameraUniversalOmni)param);
		} else if (param instanceof CameraKannalaBrandt) {
			return new LensDistortionKannalaBrandt((CameraKannalaBrandt)param);
		} else {
			throw new IllegalArgumentException("Unknown camera model " + param.getClass().getSimpleName());
		}
	}
}
