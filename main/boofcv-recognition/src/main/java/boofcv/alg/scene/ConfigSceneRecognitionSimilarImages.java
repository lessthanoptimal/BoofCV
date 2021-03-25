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

package boofcv.alg.scene;

import boofcv.abst.scene.nister2006.ConfigRecognitionNister2006;
import boofcv.factory.feature.associate.ConfigAssociate;
import boofcv.factory.feature.describe.ConfigConvertTupleDesc;
import boofcv.factory.feature.describe.ConfigDescribeRegionPoint;
import boofcv.factory.feature.detdesc.ConfigDetectDescribe;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link SceneRecognitionSimilarImages}
 *
 * @author Peter Abeles
 */
public class ConfigSceneRecognitionSimilarImages implements Configuration {

	/** Number of images which will be considered as matches when using the recognizer */
	public int limitMatchesConsider = 30;

	/** Fraction of features in a single image which must be associated for them to be considered similar */
	public double minimumRatio = 0.5;

	/** Image feature detector */
	public final ConfigDetectDescribe features = new ConfigDetectDescribe();

	/** Configuration for {@link boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006} */
	public final ConfigRecognitionNister2006 recognizeNister2006 = new ConfigRecognitionNister2006();

	/** Feature association */
	public final ConfigAssociate associate = new ConfigAssociate();

	{
		// Let's use SURF-FAST by default
		features.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.SURF_STABLE;
		features.typeDetector = ConfigDetectInterestPoint.DetectorType.FAST_HESSIAN;
		// Settings a threshold degrades overall results, even if in some specific situations makes it better
		features.detectFastHessian.extract.threshold = 0;
		features.detectFastHessian.extract.radius = 2;
		// 500 features is a good trade off for memory and performance. Accuracy can be improved
		// with more features but becomes prohibitively expensive in larger datasets
		features.detectFastHessian.maxFeaturesAll = 500;
		features.detectFastHessian.maxFeaturesPerScale = 0;

		// Reduce memory usage with very little loss in accuracy
		features.convertDescriptor.outputData = ConfigConvertTupleDesc.DataType.F32;
	}

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(limitMatchesConsider >= 1, "Must consider at least 1 match");
		BoofMiscOps.checkTrue(minimumRatio >= 0.0, "Negative ratios make no sense");

		features.checkValidity();
		recognizeNister2006.checkValidity();
		associate.checkValidity();
	}

	public void setTo( ConfigSceneRecognitionSimilarImages src ) {
		this.limitMatchesConsider = src.limitMatchesConsider;
		this.minimumRatio = src.minimumRatio;
	}
}
