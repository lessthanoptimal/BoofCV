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

package boofcv.abst.geo.calibration;

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class MultiToSingleFiducialCalibration implements DetectSingleFiducialCalibration {
	DetectMultiFiducialCalibration alg;

	public MultiToSingleFiducialCalibration( DetectMultiFiducialCalibration alg ) {
		if (alg.getTotalUniqueMarkers() > 1)
			throw new IllegalArgumentException("Must be able to only detect a single target. " +
					"Won't know which layout to use.");
		this.alg = alg;
	}

	@Override public boolean process( GrayF32 input ) {
		alg.process(input);
		return alg.getCount() == 1;
	}

	@Override public CalibrationObservation getDetectedPoints() {
		return alg.getDetectedPoints(0);
	}

	@Override public List<Point2D_F64> getLayout() {
		return alg.getLayout(0);
	}

	@Override public void setLensDistortion( LensDistortionNarrowFOV distortion, int width, int height ) {
		alg.setLensDistortion(distortion, width, height);
	}
}
