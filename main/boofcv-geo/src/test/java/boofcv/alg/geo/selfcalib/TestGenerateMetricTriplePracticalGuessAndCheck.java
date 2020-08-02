/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.selfcalib;

import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.alg.geo.robust.ModelGeneratorViews;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.image.ImageDimension;

/**
 * @author Peter Abeles
 */
class TestGenerateMetricTriplePracticalGuessAndCheck extends CommonGenerateMetricCameraTripleChecks {
	public TestGenerateMetricTriplePracticalGuessAndCheck() {
		skewTol = 0.1;
		minimumFractionSuccess = 0.9; // more unstable than most
	}

	@Override
	public ModelGeneratorViews<MetricCameraTriple, AssociatedTriple, ImageDimension> createGenerator() {
		Estimate1ofTrifocalTensor trifocal = FactoryMultiView.trifocal_1(null);
		var alg = new SelfCalibrationGuessAndCheckFocus();
		alg.setSampling(0.3,2,100);
		alg.setSingleCamera(false);

		return new GenerateMetricTriplePracticalGuessAndCheck(trifocal,alg);
	}
}