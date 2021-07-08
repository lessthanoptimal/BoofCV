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

import boofcv.misc.BoofLambdas;
import boofcv.struct.feature.TupleDesc;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

/**
 * More specialized version of {@link SceneRecognition} where it is assumed the input is composed of image features
 * that have been detected sparsely at different pixel coordinates. If supported, access is provided to words
 * that features are matched up with, which implies an internal TD-IDF style descriptor.
 *
 * This interface is of particular interest to image feature based geometric algorithms which have to compute
 * these features anyways and can make sure of the feature to word mapping to improve association between frames.
 * It also allows for image features to be precomputed and stored for later retrieval. The downside is that
 * you are stuck with an image feature based approach to recognition.
 *
 * @author Peter Abeles
 */
public interface FeatureSceneRecognition<TD extends TupleDesc<TD>> extends VerbosePrint {

	/** Learns a model using the already extracted image features */
	void learnModel( Iterator<Features<TD>> images );

	/** Removes all images from the database. The model is not modified */
	void clearDatabase();

	/**
	 * Adds a new image to the database
	 *
	 * @param id The unique ID for this image
	 * @param features All the features in this image
	 */
	void addImage( String id, Features<TD> features );

	/**
	 * Returns a list of image IDs in the database
	 *
	 * @param storage (Optional) Storage for the list of images. If null a new instance is created
	 * @return List of all the image IDs.
	 */
	List<String> getImageIds( @Nullable List<String> storage );

	/**
	 * Finds the best matches in the database to the query image. The filter can (optionally) be used to remove
	 * matches which are not of interest, i.e. too close in time.
	 *
	 * @param query (Input) Features in the query image
	 * @param filter (Input) Filter results by ID. true = keep, false = reject. Null means no filter.
	 * @param limit (Input) The maximum number of results it will return. If &le; 0 then all matches are returned.
	 * @param matches (Output) Set of matches found in best first order. List is always cleared
	 * @return true if at least one valid match was found or false if no valid matches could be found. If false
	 * that means matches is empty. This is strictly a convenience.
	 */
	boolean query( Features<TD> query,
				   @Nullable BoofLambdas.Filter<String> filter,
				   int limit, DogArray<SceneRecognition.Match> matches );

	/**
	 * Returns a single word which describes this image feature. If multiple words describe a feature in its
	 * internal implementation then there is some ambiguity resolving logic.
	 *
	 * @param featureIdx (Input) Index of the feature in the query.
	 * @return The word's ID. If no word is associated with this feature then -1 is returned.
	 */
	int getQueryWord( int featureIdx );

	/**
	 * Used to retrieve all the words a feature is associated with. If the internal implementation is hierarchical
	 * then words it passes through on the way a leaf could go in the words list.
	 *
	 * @param featureIdx (Input) Index of the feature in the query.
	 * @param words (Output) Storage for all the words the feature is associated with
	 */
	void getQueryWords( int featureIdx, DogArray_I32 words );

	int lookupWord( TD description );

	void lookupWords( TD description, DogArray_I32 word );

	/**
	 * Returns the number of unique words. it's assumed that the word ID's will occupy 0 to this value.
	 */
	int getTotalWords();

	/** The image data type which can be processed */
	Class<TD> getDescriptorType();

	/**
	 * Set of feature pixel and descriptions from a image.
	 */
	interface Features<TD extends TupleDesc<TD>> {
		Point2D_F64 getPixel( int index );

		TD getDescription( int index );

		/** Number of image features */
		int size();
	}
}
