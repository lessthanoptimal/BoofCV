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

package boofcv.alg.geo.bundle.cameras;

import boofcv.struct.calib.CameraPinholeBrown;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
public class TestBundlePinholeBrown {
	@Test
	void withSkew() {
		double[][]parameters = new double[][]{{300,200,400,400,0.01,0.009,-0.001,0.001,0.1},{400,600,1000,1000,0.01,0.009,-0.001,0.001,2}};
		new GenericChecksBundleAdjustmentCamera(new BundlePinholeBrown(false, true),0.02){}
				.setParameters(parameters)
				.checkAll();
	}

	@Test
	void withoutSkew() {
		double[][]parameters = new double[][]{{300,200,400,400,0.01,0.009,-0.001,0.001},{400,600,1000,1000,0.01,0.009,-0.001,0.001}};
		new GenericChecksBundleAdjustmentCamera(new BundlePinholeBrown(true, true),0.02){}
				.setParameters(parameters)
//				.setPrint(true)
				.checkAll();
	}

	@Test
	void variousRadialLengths() {
		for (int i = 0; i <= 2; i++) {
			CameraPinholeBrown cam = new CameraPinholeBrown(i);
			cam.fx = 300;cam.fy = 200;
			cam.cx = cam.cy = 400;
			for (int j = 0; j < i; j++) {
				cam.radial[j] = 0.01 - j*0.001;
			}
			cam.t1 = -0.001;cam.t2 = 0.001;

			BundlePinholeBrown alg = new BundlePinholeBrown(cam);
			double parameters[][] = new double[1][alg.getIntrinsicCount()];
			alg.getIntrinsic(parameters[0],0);
			new GenericChecksBundleAdjustmentCamera(alg,0.02){}
					.setParameters(parameters)
//					.setPrint(true)
					.checkAll();
		}
	}

	@Test
	void zeroTangential() {
		CameraPinholeBrown cam = new CameraPinholeBrown(1);
		cam.fx = 300;cam.fy = 200;
		cam.cx = cam.cy = 400;
		cam.radial[0] = 0.01;
		// since t1 and t2 are zero it will automatically turn off tangential

		BundlePinholeBrown alg = new BundlePinholeBrown(cam);
		double parameters[][] = new double[1][alg.getIntrinsicCount()];
		alg.getIntrinsic(parameters[0],0);
		new GenericChecksBundleAdjustmentCamera(alg,0.02){}
				.setParameters(parameters)
//					.setPrint(true)
				.checkAll();
	}
}