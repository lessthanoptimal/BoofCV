/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.structure.score3d;

import boofcv.alg.structure.EpipolarScore3D;
import boofcv.factory.geo.ConfigFundamental;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.struct.geo.AssociatedPair;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

public class TestScoreFundamentalHomographyCompatibility extends CommonEpipolarScore3DChecks {

	// Can't detect translational motion for planar scenes. See code comments
	@Test @Override void planar_translate_xy() {}
	@Test @Override void planar_translate_z_twoCameras() {}
	@Test @Override void planar_translate_z_oneCamera() {}

	@Override public EpipolarScore3D createAlg() {
		ConfigRansac configRansac = new ConfigRansac();
		configRansac.iterations = 20;
		configRansac.inlierThreshold = 2.0;

		ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D =
				FactoryMultiViewRobust.fundamentalRansac(new ConfigFundamental(), configRansac);

		var alg = new ScoreFundamentalHomographyCompatibility(ransac3D);
		alg.ratio3D = 1.2;
		return alg;
	}
}
