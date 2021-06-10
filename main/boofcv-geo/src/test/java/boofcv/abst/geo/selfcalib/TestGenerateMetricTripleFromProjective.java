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

package boofcv.abst.geo.selfcalib;

import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.alg.geo.robust.ModelGeneratorViews;
import boofcv.alg.geo.selfcalib.MetricCameraTriple;
import boofcv.alg.geo.selfcalib.SelfCalibrationEssentialGuessAndCheck;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic;
import boofcv.alg.geo.selfcalib.SelfCalibrationPraticalGuessAndCheckFocus;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.ElevateViewInfo;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
class TestGenerateMetricTripleFromProjective extends BoofStandardJUnit {

	@Nested
	class DualQuadratic extends CommonGenerateMetricCameraTripleChecks {
		@Override
		public ModelGeneratorViews<MetricCameraTriple, AssociatedTriple, ElevateViewInfo> createGenerator() {
			Estimate1ofTrifocalTensor trifocal = FactoryMultiView.trifocal_1(null);
			var alg = new SelfCalibrationLinearDualQuadratic(1.0);
			var wrapper = new ProjectiveToMetricCameraDualQuadratic(alg);
			return new GenerateMetricTripleFromProjective(trifocal, wrapper);
		}
	}

	@Nested
	class EssentialGuessAndCheck extends CommonGenerateMetricCameraTripleChecks {
		public EssentialGuessAndCheck() {
			totalTrials = 15; // reduce number of trials to make it run faster
		}

		@Override
		public ModelGeneratorViews<MetricCameraTriple, AssociatedTriple, ElevateViewInfo> createGenerator() {
			Estimate1ofTrifocalTensor trifocal = FactoryMultiView.trifocal_1(null);
			var alg = new SelfCalibrationEssentialGuessAndCheck();
			alg.fixedFocus = false;
			alg.numberOfSamples = 200;
			alg.configure(0.3, 2.5);
			var wrapper = new ProjectiveToMetricCameraEssentialGuessAndCheck(alg);
			return new GenerateMetricTripleFromProjective(trifocal, wrapper);
		}
	}

	@Nested
	class PracticalGuessAndCheck extends CommonGenerateMetricCameraTripleChecks {
		public PracticalGuessAndCheck() {
			skewTol = 0.4;
			minimumFractionSuccess = 0.5; // more unstable than most
		}

		@Override
		public ModelGeneratorViews<MetricCameraTriple, AssociatedTriple, ElevateViewInfo> createGenerator() {
			Estimate1ofTrifocalTensor trifocal = FactoryMultiView.trifocal_1(null);
			var alg = new SelfCalibrationPraticalGuessAndCheckFocus();
			alg.setSampling(0.3, 2, 100);
			alg.setSingleCamera(false);
			var wrapper = new ProjectiveToMetricCameraPracticalGuessAndCheck(alg);
			return new GenerateMetricTripleFromProjective(trifocal, wrapper);
		}
	}
}
