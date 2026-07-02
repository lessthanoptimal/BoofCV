/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.geo.calibration.ConfigCalibrateUniversalOmni;
import boofcv.alg.geo.bundle.cameras.BundleUniversalOmni;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.geo.calibration.RadialDistortionEstimateLinear;
import boofcv.struct.calib.CameraModel;
import boofcv.struct.calib.CameraUniversalOmni;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Camera parameters for model {@link CameraUniversalOmni}.
 *
 * @author Peter Abeles
 */
public class Zhang99CameraUniversalOmni implements Zhang99Camera {
	ConfigCalibrateUniversalOmni config;

	private final RadialDistortionEstimateLinear computeRadial;

	public Zhang99CameraUniversalOmni( ConfigCalibrateUniversalOmni config ) {
		this.config = new ConfigCalibrateUniversalOmni().setTo(config);
		computeRadial = new RadialDistortionEstimateLinear(config.numRadial);
	}

	@Override public void setLayouts( List<List<Point2D_F64>> layouts ) {
		computeRadial.setLayouts(layouts);
	}

	@Override public BundleAdjustmentCamera initializeCamera(
			DMatrixRMaj K, List<DMatrixRMaj> homographies, List<CalibrationObservation> observations ) {
		computeRadial.process(K, homographies, observations);
		double[] radial = computeRadial.getParameters();

		BundleUniversalOmni cam = new BundleUniversalOmni(config);
		System.arraycopy(radial, 0, cam.radial, 0, radial.length);
		cam.setK(K);
		cam.t1 = cam.t2 = 0;
		return cam;
	}

	@Override
	public CameraModel getCameraModel( BundleAdjustmentCamera bundleCam ) {
		var cam = (BundleUniversalOmni)bundleCam;
		var out = new CameraUniversalOmni(cam.radial.length);
		cam.convert(out);
		return out;
	}
}
