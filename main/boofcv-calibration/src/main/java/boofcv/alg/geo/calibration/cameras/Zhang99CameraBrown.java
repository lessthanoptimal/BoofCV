/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.geo.bundle.cameras.BundlePinholeBrown;
import boofcv.struct.calib.CameraModel;
import boofcv.struct.calib.CameraPinholeBrown;
import org.ejml.data.DMatrixRMaj;

/**
 * @author Peter Abeles
 */
public class Zhang99CameraBrown implements Zhang99Camera {
	boolean assumeZeroSkew;
	boolean includeTangential;
	int numRadial;

	public Zhang99CameraBrown(boolean assumeZeroSkew, boolean includeTangential, int numRadial) {
		this.assumeZeroSkew = assumeZeroSkew;
		this.includeTangential = includeTangential;
		this.numRadial = numRadial;
	}

	@Override
	public BundleAdjustmentCamera initalizeCamera(DMatrixRMaj K, double[] radial) {
		BundlePinholeBrown cam = new BundlePinholeBrown(assumeZeroSkew,includeTangential);
		cam.radial = radial.clone();
		if( cam.radial.length != numRadial )
			throw new RuntimeException("BUGW!");
		cam.setK(K);
		return cam;
	}

	@Override
	public CameraModel getCameraModel(BundleAdjustmentCamera bundleCam) {
		BundlePinholeBrown cam = (BundlePinholeBrown)bundleCam;
		CameraPinholeBrown out = new CameraPinholeBrown();
		cam.convert(out);
		return out;
	}

	@Override
	public boolean isZeroSkew() {
		return assumeZeroSkew;
	}

	@Override
	public int numRadial() {
		return numRadial;
	}
}
