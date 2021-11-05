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

package boofcv.abst.scene;

import boofcv.abst.scene.ann.ConfigRecognitionNearestNeighbor;
import boofcv.abst.scene.nister2006.ConfigRecognitionNister2006;
import boofcv.alg.scene.ann.RecognitionNearestNeighborInvertedFile;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006;
import boofcv.factory.feature.describe.ConfigConvertTupleDesc;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detdesc.ConfigDetectDescribe;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.struct.Configuration;

/**
 * Generic configuration for using implementations of {@link FeatureSceneRecognition} inside
 * of {@link SceneRecognition}.
 *
 * @author Peter Abeles
 */
public class ConfigFeatureToSceneRecognition implements Configuration {
	/**
	 * Images are rescaled so that they have at most this number of pixels. To turn off set to a value &le; 0.
	 * Many of feature related tuning parameters have an implicit assumption about image resolution.
	 * Processing a much higher resolution image could require changing many other parameters for optimal
	 * performance.
	 */
	public int maxImagePixels = 640*480;

	/** Specifies which recognition algorithm to use */
	public Type typeRecognize = Type.NISTER_2006;

	/** Image feature detector */
	public final ConfigDetectDescribe features = new ConfigDetectDescribe();

	/** Configuration for {@link boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006} */
	public final ConfigRecognitionNister2006 recognizeNister2006 = new ConfigRecognitionNister2006();

	/** Configuration for {@link boofcv.alg.scene.ann.RecognitionNearestNeighborInvertedFile} */
	public final ConfigRecognitionNearestNeighbor recognizeNeighbor = new ConfigRecognitionNearestNeighbor();

	{
		// 500 features is a good trade off for memory and performance. Accuracy can be improved
		// with more features but becomes prohibitively expensive in larger datasets
		// Tuning was found auto a semi-automated process ok Holidays and UKBench datasets. See tech report.

		// Let's use SURF-FAST by default
		features.typeDescribe = ConfigDescribeRegion.Type.SURF_STABLE;
		features.typeDetector = ConfigDetectInterestPoint.Type.FAST_HESSIAN;

		// Settings a threshold degrades overall results, even if in some specific situations makes it better
		features.detectFastHessian.extract.threshold = 0.01f;
		features.detectFastHessian.extract.radius = 2;
		features.detectFastHessian.maxFeaturesAll = 500;
		features.detectFastHessian.maxFeaturesPerScale = 0;
		features.detectFastHessian.numberOfOctaves = 4;

		// Configure SIFT
		features.detectSift.extract.threshold = 0.004f;
		features.detectSift.extract.radius = 2; // best results were found with 6, but that could cause problems
		features.detectSift.edgeR = 19.1;
		features.detectSift.maxFeaturesAll = 500;
		features.detectSift.maxFeaturesPerScale = 0;
		features.scaleSpaceSift.sigma0 = 3.5f;
		features.scaleSpaceSift.lastOctave = 5;
		features.describeSift.sigmaToPixels = 1.54;
		features.describeSift.maxDescriptorElementValue = 0.25;

		// Reduce memory usage with very little loss in accuracy
		features.convertDescriptor.outputData = ConfigConvertTupleDesc.DataType.F32;
	}

	@Override public void checkValidity() {
		features.checkValidity();
		recognizeNister2006.checkValidity();
		recognizeNeighbor.checkValidity();
	}

	public ConfigFeatureToSceneRecognition setTo( ConfigFeatureToSceneRecognition src ) {
		this.maxImagePixels = src.maxImagePixels;
		this.typeRecognize = src.typeRecognize;
		this.features.setTo(src.features);
		this.recognizeNister2006.setTo(src.recognizeNister2006);
		this.recognizeNeighbor.setTo(src.recognizeNeighbor);
		return this;
	}

	/**
	 * Which type of recognition algorithm to use
	 */
	public enum Type {
		/** {@link RecognitionVocabularyTreeNister2006} */
		NISTER_2006,
		/** {@link RecognitionNearestNeighborInvertedFile} */
		NEAREST_NEIGHBOR
	}
}
