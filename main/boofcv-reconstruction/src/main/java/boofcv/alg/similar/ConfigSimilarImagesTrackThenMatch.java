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
import boofcv.factory.feature.describe.ConfigDescribePoint;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link SimilarImagesSceneRecognition}
 *
 * @author Peter Abeles
 */
public class ConfigSimilarImagesTrackThenMatch implements Configuration {

	/** Number of images which will be considered as matches when using the recognizer */
	public int limitQuery = 20;

	/**
	 * Minimum number of frames (by ID) away two frames need to be for loop closure logic to connect them
	 */
	public int minimumRecognizeDistance = 30;

	/**
	 * {@link SimilarImagesFromTracks#searchRadius}
	 */
	public int sequentialSearchRadius = 8;

	/**
	 * {@link SimilarImagesFromTracks#minimumCommonTracks}
	 */
	public final ConfigLength sequentialMinimumCommonTracks = ConfigLength.relative(0.4, 200.0);

	/**
	 * Specifies how many features need to be matched for an image to be considered similar. Absolute
	 * is the number of matches. Fraction is relative to the number of images in each image.
	 *
	 * The default minimum number of matches is probably set too low.
	 */
	public final ConfigLength minimumSimilar = ConfigLength.relative(0.35, 150);

	/** Image feature descriptions */
	public final ConfigDescribePoint descriptions = new ConfigDescribePoint();

	/** Configuration for {@link boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006} */
	public final ConfigRecognitionNister2006 recognizeNister2006 = new ConfigRecognitionNister2006();

	/** Feature association */
	public final ConfigAssociate associate = new ConfigAssociate();

	{
		recognizeNister2006.learningMinimumPointsForChildren.setFixed(20);

		descriptions.descriptors.type = ConfigDescribeRegion.Type.SURF_STABLE;
		descriptions.radius = 20;

		// Reduce memory usage with very little loss in accuracy
		descriptions.convert.outputData = ConfigConvertTupleDesc.DataType.F32;
	}

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(limitQuery >= 1, "Must consider at least 1 match");

		descriptions.checkValidity();
		recognizeNister2006.checkValidity();
		associate.checkValidity();
		sequentialMinimumCommonTracks.checkValidity();
	}

	public ConfigSimilarImagesTrackThenMatch setTo( ConfigSimilarImagesTrackThenMatch src ) {
		this.sequentialSearchRadius = src.sequentialSearchRadius;
		this.sequentialMinimumCommonTracks.setTo(src.sequentialMinimumCommonTracks);
		this.limitQuery = src.limitQuery;
		this.minimumRecognizeDistance = src.minimumRecognizeDistance;
		this.minimumSimilar.setTo(src.minimumSimilar);
		this.descriptions.setTo(src.descriptions);
		this.recognizeNister2006.setTo(src.recognizeNister2006);
		this.associate.setTo(src.associate);
		return this;
	}
}
