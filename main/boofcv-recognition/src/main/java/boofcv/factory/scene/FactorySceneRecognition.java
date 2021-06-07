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
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.scene.ConfigFeatureToSceneRecognition;
import boofcv.abst.scene.FeatureSceneRecognition;
import boofcv.abst.scene.SceneRecognition;
import boofcv.abst.scene.WrapFeatureToSceneRecognition;
import boofcv.abst.scene.ann.ConfigRecognitionNearestNeighbor;
import boofcv.abst.scene.ann.FeatureSceneRecognitionNearestNeighbor;
import boofcv.abst.scene.nister2006.ConfigRecognitionNister2006;
import boofcv.abst.scene.nister2006.FeatureSceneRecognitionNister2006;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.tracker.ConfigPointTracker;
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
	 * Creates the default config for a {@link boofcv.abst.tracker.PointTracker} for use with scene reconstruction
	 */
	public static ConfigPointTracker createDefaultTrackerConfig() {
		var config = new ConfigPointTracker();

		config.typeTracker = ConfigPointTracker.TrackerType.KLT;
		config.klt.pruneClose = true;
		config.klt.toleranceFB = 1;
		config.klt.templateRadius = 5;
		config.klt.maximumTracks.setFixed(800);
		config.klt.config.maxIterations = 30;
		config.detDesc.typeDetector = ConfigDetectInterestPoint.Type.POINT;
		config.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		config.detDesc.detectPoint.shiTomasi.radius = 3;
		config.detDesc.detectPoint.general.radius = 6;
		config.detDesc.detectPoint.general.threshold = 0;

		return config;
	}

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

		FeatureSceneRecognition<TD> recognitizer = switch (config.typeRecognize) {
			case NISTER_2006 -> createSceneNister2006(config.recognizeNister2006, detector::createDescription);
			case NEAREST_NEIGHBOR -> createSceneNearestNeighbor(config.recognizeNeighbor, detector::createDescription);
		};

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

	/**
	 * Creates a new {@link FeatureSceneRecognitionNister2006}.
	 */
	public static <TD extends TupleDesc<TD>> FeatureSceneRecognitionNearestNeighbor<TD>
	createSceneNearestNeighbor( @Nullable ConfigRecognitionNearestNeighbor config,
								Factory<TD> factory ) {
		if (config == null) {
			config = new ConfigRecognitionNearestNeighbor();
		}

		return new FeatureSceneRecognitionNearestNeighbor<>(config, factory);
	}
}
