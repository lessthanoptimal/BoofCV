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

package boofcv.alg.similar;

import boofcv.abst.scene.nister2006.ConfigRecognitionNister2006;
import boofcv.factory.feature.associate.ConfigAssociate;
import boofcv.factory.feature.describe.ConfigConvertTupleDesc;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detdesc.ConfigDetectDescribe;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link SimilarImagesSceneRecognition}
 *
 * @author Peter Abeles
 */
public class ConfigSimilarImagesSceneRecognition implements Configuration {

	/** Number of images which will be considered as matches when using the recognizer */
	public int limitMatchesConsider = 30;

	/**
	 * Specifies how many features need to be matched for an image to be considered similar. Absolute
	 * is the number of matches. Fraction is relative to the number of images in each image.
	 */
	public final ConfigLength minimumSimilar = ConfigLength.relative(0.2, 100);

	/** Image feature detector */
	public final ConfigDetectDescribe features = new ConfigDetectDescribe();

	/** Configuration for {@link boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006} */
	public final ConfigRecognitionNister2006 recognizeNister2006 = new ConfigRecognitionNister2006();

	/** Feature association */
	public final ConfigAssociate associate = new ConfigAssociate();

	{
		recognizeNister2006.learningMinimumPointsForChildren.setFixed(20);

		// Let's use SURF-FAST by default
		features.typeDescribe = ConfigDescribeRegion.Type.SURF_STABLE;
		features.typeDetector = ConfigDetectInterestPoint.Type.FAST_HESSIAN;
		// Settings a threshold degrades overall results, even if in some specific situations makes it better
		features.detectFastHessian.extract.threshold = 0.5f;
		features.detectFastHessian.extract.radius = 5;
		features.detectFastHessian.numberOfOctaves = 7;
		// 500 features is a good trade off for memory and performance. Accuracy can be improved
		// with more features but becomes prohibitively expensive in larger datasets
		features.detectFastHessian.maxFeaturesAll = 1000;
		features.detectFastHessian.extract.radius = 6; // tuned for 800x600 image
		features.detectFastHessian.maxFeaturesPerScale = 0;

		// Also give SIFT reasonable parameters
		features.describeSift.sigmaToPixels = 2.0f;
		features.detectSift.extract.threshold = 0.5f;
		features.detectSift.extract.radius = 6;
		features.detectSift.maxFeaturesAll = 800;
		features.detectSift.maxFeaturesPerScale = 0;

		// Reduce memory usage with very little loss in accuracy
		features.convertDescriptor.outputData = ConfigConvertTupleDesc.DataType.F32;
	}

	/**
	 * The default makes implicit assumptions about the input image. This relaxes those but will perform worse
	 * in most situations. Only use this if the default completely fails.
	 */
	public static ConfigSimilarImagesSceneRecognition createFailSafe() {
		var config = new ConfigSimilarImagesSceneRecognition();

		// Feature threshold makes assumptions about how bright the image is
		// Feature radius might be too large for very small images
		config.features.detectFastHessian.extract.threshold = 0.0f;
		config.features.detectFastHessian.extract.radius = 2;
		config.features.detectFastHessian.numberOfOctaves = 4;
		config.features.detectSift.extract.threshold = 0.0f;
		config.features.detectSift.extract.radius = 2;
		config.features.describeSift.sigmaToPixels = 1.0f;

		// If you have extremely small dataset the default value could cause problems
		config.recognizeNister2006.learningMinimumPointsForChildren.setFixed(0);
		config.recognizeNister2006.minimumDepthFromRoot = 0;

		// If you have next to no image features to work with the default might be too small
		config.minimumSimilar.setFraction(0.4);

		return config;
	}

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(limitMatchesConsider >= 1, "Must consider at least 1 match");

		features.checkValidity();
		recognizeNister2006.checkValidity();
		associate.checkValidity();
	}

	public ConfigSimilarImagesSceneRecognition setTo( ConfigSimilarImagesSceneRecognition src ) {
		this.limitMatchesConsider = src.limitMatchesConsider;
		this.minimumSimilar.setTo(src.minimumSimilar);
		this.features.setTo(src.features);
		this.recognizeNister2006.setTo(src.recognizeNister2006);
		this.associate.setTo(src.associate);
		return this;
	}
}
