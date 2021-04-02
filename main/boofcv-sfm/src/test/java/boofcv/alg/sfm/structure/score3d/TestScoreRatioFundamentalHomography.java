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

package boofcv.alg.sfm.structure.score3d;

import boofcv.alg.sfm.structure.EpipolarScore3D;
import boofcv.factory.geo.ConfigFundamental;
import boofcv.factory.geo.ConfigHomography;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.homography.Homography2D_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestScoreRatioFundamentalHomography extends CommonEpipolarScore3DChecks {

	// it can't handle the planar case since a homography and fundamental matrix both fit well here
	@Test @Override void planar() {}

	/**
	 * Specialized test to make sure the score function used here has the desired properties
	 */
	@Test void checkScore() {
		var m = new ScoreRatioFundamentalHomography();
		m.countF = 100;
		m.countH = 100;
		double score0 = m.getScore();

		// fewer points found to match homography so it should be more 3D
		m.countH = 50;
		double score1 = m.getScore();
		assertTrue(score1 > score0);

		// more points should result in a better score too
		m.countF *= 2;
		m.countH *= 2;
		double score2 = m.getScore();
		assertTrue(score2 > score1);
	}

	@Override public EpipolarScore3D createAlg() {
		ConfigRansac configRansac = new ConfigRansac();
		configRansac.iterations = 20;
		configRansac.inlierThreshold = 2.0;
		configRansac.randSeed = 3454345; // TODO improve test/alg. It's sensitive to random seed

		ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D =
				FactoryMultiViewRobust.fundamentalRansac(new ConfigFundamental(), configRansac);
		ModelMatcher<Homography2D_F64, AssociatedPair> ransacH =
				FactoryMultiViewRobust.homographyRansac(new ConfigHomography(), configRansac);

		return new ScoreRatioFundamentalHomography(ransac3D, ransacH);
	}
}
