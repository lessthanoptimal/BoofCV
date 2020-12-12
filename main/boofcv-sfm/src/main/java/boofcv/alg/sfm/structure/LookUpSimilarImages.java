/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

	List<String> getImageIDs();

	/**
	 * @param target ID of target image
	 * @param similar Storage for IDs of similar images. Cleared upon each call
	 */
	void findSimilar( String target, List<String> similar );

	/**
	 * Looks up pixel observations of features in the specified view.
	 *
	 * @param target ID of target image
	 * @param features Storage for pixel observations. Cleared upon each call
	 */
	void lookupPixelFeats( String target, DogArray<Point2D_F64> features );

	/**
	 * Looks up associated features between the two views. Which view
	 *
	 * @param viewA name of view A
	 * @param viewB name of view B
	 * @param pairs Storage for associated features. Cleared upon each call
	 * @return true if views are similar and have known associations. False if not and results should be ignored
	 */
	boolean lookupMatches( String viewA, String viewB, DogArray<AssociatedIndex> pairs );

	/**
	 * Looks up the original images width and height
	 *
	 * @param target (Input) the image to retrieve from
	 * @param shape (Output) storage for width and height
	 */
	void lookupShape( String target, ImageDimension shape );
}
