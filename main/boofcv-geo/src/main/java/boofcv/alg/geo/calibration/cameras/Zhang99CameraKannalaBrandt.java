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
import boofcv.alg.geo.bundle.cameras.BundleKannalaBrandt;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.struct.calib.CameraKannalaBrandt;
import boofcv.struct.calib.CameraModel;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Implementation of Kannala-Brandt for {@link Zhang99Camera}.
 *
 * @author Peter Abeles
 */
public class Zhang99CameraKannalaBrandt implements Zhang99Camera {

	boolean assumeZeroSkew;
	int numSymmetric, numAsymmetric;

	public Zhang99CameraKannalaBrandt( boolean assumeZeroSkew, int numSymmetric, int numAsymmetric ) {
		this.assumeZeroSkew = assumeZeroSkew;
		this.numSymmetric = numSymmetric;
		this.numAsymmetric = numAsymmetric;
	}

	@Override
	public BundleAdjustmentCamera initializeCamera( DMatrixRMaj K,
													List<DMatrixRMaj> homographies,
													List<CalibrationObservation> observations ) {
		var ret = new BundleKannalaBrandt();
		ret.configure(assumeZeroSkew, numSymmetric, numAsymmetric);
		ret.model.fsetK(K);

		// all zeros are a pathological case. Fill it with arbitrary small values.
		for (int i = 0; i < numAsymmetric; i++) {
			ret.model.radial[i] = (i%2 == 0) ? 0.005 : -0.005;
			ret.model.tangent[i] = (i%2 == 0) ? 0.005 : -0.005;
		}

		// Distortion terms are all initialized to zero or nearly zero. Stability could be improved if an initial
		// estimate was provided. Maybe adjust the initial K since the pin-hole model and this model don't
		// treat the two parameters as the same

		// Could also try initializing with different noise and see if it does better?

		return ret;
	}

	@Override public CameraModel getCameraModel( BundleAdjustmentCamera bundleCam ) {
		BundleKannalaBrandt cam = (BundleKannalaBrandt)bundleCam;
		return BundleAdjustmentOps.convert(cam, 0, 0, (CameraKannalaBrandt)null);
	}
}
