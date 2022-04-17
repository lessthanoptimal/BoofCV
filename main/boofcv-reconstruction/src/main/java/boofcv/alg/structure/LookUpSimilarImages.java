/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.structure;

import boofcv.misc.BoofLambdas;
import boofcv.struct.feature.AssociatedIndex;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Interface for finding images with a similar appearance and identifying point features which are related
 * between the two images. This is typically used as an initial step when performing
 * scene reconstruction. For scene reconstruction a geometric constraint is applied afterwards to remove false
 * positives matches and associated features.
 *
 * @author Peter Abeles
 */
public interface LookUpSimilarImages {
	/**
	 * Returns a list of all images by ID that it has in its data base
	 */
	List<String> getImageIDs();

	/**
	 * Given an imageID, it will find list of similar images. Similarity is defined from
	 * the perspective of the 'target', so viewA might think viewB is similar to it, but viewB might think
	 * viewA is not similar to it.
	 *
	 * @param target (Input) ID of target image
	 * @param filter (Input) Filter results by ID. true = keep, false = reject. Null means no filter.
	 * @param similarImages (Output) Storage for IDs of similar images. Cleared upon each call
	 * @throws IllegalArgumentException If the target is not known
	 */
	void findSimilar( String target, @Nullable BoofLambdas.Filter<String> filter, List<String> similarImages );

	/**
	 * Looks up pixel observations of features in the specified view.
	 *
	 * @param target ID of target image
	 * @param features Storage for pixel observations. Cleared upon each call
	 * @throws IllegalArgumentException If the target is not known
	 */
	void lookupPixelFeats( String target, DogArray<Point2D_F64> features );

	/**
	 * Looks up pairs of associated features from a similar image which was returned in the most
	 * recent call to {@link #findSimilar}. The src will be the target in 'findSimilar' and the dst
	 * will be the requested similar image.
	 *
	 * @param similarID ID of a similar image to the target when calling {@link #findSimilar}.
	 * @param pairs Storage for associated features. Cleared upon each call
	 * @return true if views are similar and have known associations. False if not and results should be ignored
	 * @throws IllegalArgumentException If the one of the views is not known
	 */
	boolean lookupAssociated( String similarID, DogArray<AssociatedIndex> pairs );
}
