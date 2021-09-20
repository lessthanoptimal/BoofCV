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
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeBrown;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.geo.calibration.RadialDistortionEstimateLinear;
import boofcv.struct.calib.CameraModel;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Camera parameters for model {@link boofcv.struct.calib.CameraPinholeBrown}.
 *
 * @author Peter Abeles
 */
public class Zhang99CameraBrown implements Zhang99Camera {
	boolean assumeZeroSkew;
	boolean includeTangential;

	private final RadialDistortionEstimateLinear computeRadial;

	public Zhang99CameraBrown( List<Point2D_F64> layout,
							   boolean assumeZeroSkew, boolean includeTangential, int numRadial ) {
		this.assumeZeroSkew = assumeZeroSkew;
		this.includeTangential = includeTangential;
		computeRadial = new RadialDistortionEstimateLinear(layout, numRadial);
	}

	@Override public BundleAdjustmentCamera initializeCamera(
			DMatrixRMaj K, List<DMatrixRMaj> homographies, List<CalibrationObservation> observations ) {
		computeRadial.process(K, homographies, observations);

		BundlePinholeBrown cam = new BundlePinholeBrown(assumeZeroSkew, includeTangential);
		cam.radial = computeRadial.getParameters().clone();
		cam.setK(K);
		return cam;
	}

	@Override
	public CameraModel getCameraModel( BundleAdjustmentCamera bundleCam ) {
		BundlePinholeBrown cam = (BundlePinholeBrown)bundleCam;
		return BundleAdjustmentOps.convert(cam, 0, 0, null);
	}
}
