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
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Wraps {@link DetectMultiFiducialCalibration} by limiting it to a single marker.
 *
 * @author Peter Abeles
 */
public class MultiToSingleFiducialCalibration implements DetectSingleFiducialCalibration {
	DetectMultiFiducialCalibration alg;

	/** Specifies which marker it will return. All other markers will be ignored */
	@Getter @Setter int targetMarker = 0;

	// which detection matches the target ID. -1 means it was not detected
	private int detectedID;

	public MultiToSingleFiducialCalibration( DetectMultiFiducialCalibration alg ) {
		this.alg = alg;
	}

	@Override public boolean process( GrayF32 input ) {
		alg.process(input);

		detectedID = -1;
		for (int i = 0; i < alg.getDetectionCount(); i++) {
			if (alg.getMarkerID(i) == targetMarker) {
				detectedID = i;
				break;
			}
		}

		return detectedID != -1;
	}

	@Override public CalibrationObservation getDetectedPoints() {
		return alg.getDetectedPoints(detectedID);
	}

	@Override public List<Point2D_F64> getLayout() {
		return alg.getLayout(targetMarker);
	}

	@Override public void setLensDistortion( LensDistortionNarrowFOV distortion, int width, int height ) {
		alg.setLensDistortion(distortion, width, height);
	}
}
