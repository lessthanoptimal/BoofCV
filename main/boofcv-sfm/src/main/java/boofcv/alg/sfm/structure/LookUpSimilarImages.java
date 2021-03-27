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

package boofcv.alg.sfm.structure;

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;

import java.util.List;

/**
 * Interface for finding images with a similar appearance by some metric.
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
	 * @param target ID of target image
	 * @param similar Storage for IDs of similar images. Cleared upon each call
	 * @throws IllegalArgumentException If the target is not known
	 */
	void findSimilar( String target, List<String> similar );

	/**
	 * Looks up pixel observations of features in the specified view.
	 *
	 * @param target ID of target image
	 * @param features Storage for pixel observations. Cleared upon each call
	 * @throws IllegalArgumentException If the target is not known
	 */
	void lookupPixelFeats( String target, DogArray<Point2D_F64> features );

	/**
	 * Returns associated features between the two views. The set of common features will not be dependent
	 * which view is src or dst, but the the returned values in pairs will be.
	 *
	 * @param viewSrc name of view which will be the src
	 * @param viewDst name of view which will be the dst
	 * @param pairs Storage for associated features. Cleared upon each call
	 * @return true if views are similar and have known associations. False if not and results should be ignored
	 * @throws IllegalArgumentException If the one of the views is not known
	 */
	boolean lookupMatches( String viewSrc, String viewDst, DogArray<AssociatedIndex> pairs );

	/**
	 * Looks up the original images width and height
	 *
	 * @param target (Input) the image to retrieve from
	 * @param shape (Output) storage for width and height
	 * @throws IllegalArgumentException If the target is not known
	 */
	void lookupShape( String target, ImageDimension shape );
}
