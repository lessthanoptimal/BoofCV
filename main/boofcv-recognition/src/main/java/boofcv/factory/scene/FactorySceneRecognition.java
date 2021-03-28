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

package boofcv.factory.scene;

import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.scene.ConfigFeatureToSceneRecognition;
import boofcv.abst.scene.FeatureSceneRecognition;
import boofcv.abst.scene.SceneRecognition;
import boofcv.abst.scene.WrapFeatureToSceneRecognition;
import boofcv.abst.scene.nister2006.ConfigRecognitionNister2006;
import boofcv.abst.scene.nister2006.FeatureSceneRecognitionNister2006;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.misc.BoofLambdas;
import boofcv.misc.FactoryFilterLambdas;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.Factory;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating {@link SceneRecognition} and related.
 *
 * @author Peter Abeles
 */
public class FactorySceneRecognition {
	/**
	 * Creates a new {@link SceneRecognition} that is a wrapper around {@link FeatureSceneRecognition}
	 */
	public static <Image extends ImageBase<Image>, TD extends TupleDesc<TD>>
	WrapFeatureToSceneRecognition<Image, TD> createFeatureToScene( @Nullable ConfigFeatureToSceneRecognition config,
																   ImageType<Image> imageType ) {
		if (config == null)
			config = new ConfigFeatureToSceneRecognition();

		DetectDescribePoint<Image, TD> detector =
				FactoryDetectDescribe.generic(config.features, imageType.getImageClass());
		BoofLambdas.Transform<Image> downSample =
				FactoryFilterLambdas.createDownSampleFilter(config.maxImagePixels, imageType);

		FeatureSceneRecognition<TD> recognitizer =
				createSceneNister2006(config.recognizeNister2006, detector::createDescription);

		var alg = new WrapFeatureToSceneRecognition<>(detector, downSample, recognitizer);
		alg.config = config;
		return alg;
	}

	/**
	 * Creates a new {@link FeatureSceneRecognitionNister2006}.
	 */
	public static <TD extends TupleDesc<TD>>
	FeatureSceneRecognitionNister2006<TD> createSceneNister2006( @Nullable ConfigRecognitionNister2006 config,
																 Factory<TD> factory ) {
		if (config == null) {
			config = new ConfigRecognitionNister2006();
		}

		return new FeatureSceneRecognitionNister2006<>(config, factory);
	}
}
