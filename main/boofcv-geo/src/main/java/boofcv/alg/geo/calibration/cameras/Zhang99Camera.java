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

package boofcv.alg.geo.calibration.cameras;

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.struct.calib.CameraModel;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Wrapper that converts a camera model into a format understood by Zhang99.
 *
 * @author Peter Abeles
 */
public interface Zhang99Camera {
	/**
	 * Provide an initial estimate for the camera parameters given 1) estimated pinhole camera parameters,
	 * set of found homographies, and observed calibration targets.
	 *
	 * @param K (Input) Estimated pinhole camera parameters
	 * @param homographies (Input) Homographies
	 * @param observations (Input) Target observations
	 * @return Initial estimate of camera model
	 */
	BundleAdjustmentCamera initializeCamera( DMatrixRMaj K,
											 List<DMatrixRMaj> homographies,
											 List<CalibrationObservation> observations );

	CameraModel getCameraModel( BundleAdjustmentCamera bundleCam );
}
