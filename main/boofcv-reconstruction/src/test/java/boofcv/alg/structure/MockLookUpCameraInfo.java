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

package boofcv.alg.structure;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.ImageDimension;

/**
 * @author Peter Abeles
 */
public class MockLookUpCameraInfo extends LookUpCameraInfo {
	CameraPinholeBrown intrinsics = new CameraPinholeBrown(2);
	boolean known = false;

	public MockLookUpCameraInfo( CameraPinholeBrown intrinsics ) {
		this.intrinsics.setTo(intrinsics);
	}

	public MockLookUpCameraInfo( CameraPinhole intrinsics ) {
		this.intrinsics.setTo(intrinsics);
	}

	public MockLookUpCameraInfo( int width, int height ) {
		PerspectiveOps.createIntrinsic(width, height, 45, intrinsics);
	}

	@Override public int totalViews() {
		throw new RuntimeException("Not supported by the mock");
	}

	@Override public int totalCameras() {
		return 1;
	}

	@Override public int viewToCamera( String viewID ) {
		return 0;
	}

	@Override public void lookupCalibration( int cameraIdx, CameraPinholeBrown calibration ) {
		calibration.setTo(this.intrinsics);
	}

	@Override public void lookupCalibration( String viewID, CameraPinholeBrown calibration ) {
		calibration.setTo(this.intrinsics);
	}

	@Override public boolean isCameraKnown( int cameraIdx ) {
		return known;
	}

	@Override public void lookupViewShape( String viewID, ImageDimension shape ) {
		shape.setTo(intrinsics.width, intrinsics.height);
	}
}
