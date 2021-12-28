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
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

/**
 * Implementations of this interface seek to solve the problem of "have I seen this scene before, but from the
 * same or different perspective? If so find those images". For example, this should be used if you want to find
 * images of a mountain taken from a different angle or zoom. Implementations may or may not apply geometric
 * constraints to ensure that it is the same scene.
 *
 * Usage Example:
 * <ol>
 *     <li>Learn a model from a set of images by calling {@link #learnModel}</li>
 *     <li>Add images for later retrieval by calling {@link #addImage}</li>
 *     <li>Call {@link #query} to find the set of N images which are most similar to the passed in image</li>
 * </ol>
 *
 * You can also save models and your image database by using functions in RecognitionIO.
 *
 * @author Peter Abeles
 */
public interface SceneRecognition<T extends ImageBase<T>> extends VerbosePrint {
	/**
	 * Learns a model by finding the most distinctive features in the provided set of images. Images are not
	 * added to the database.
	 */
	void learnModel( Iterator<T> images );

	/** Removes all images from the database. The model is not modified */
	void clearDatabase();

	/**
	 * Adds a new image to the database
	 *
	 * @param id The unique ID for this image
	 * @param image The image
	 */
	void addImage( String id, T image );

	/**
	 * Finds the best matches in the database to the query image.
	 *
	 * @param queryImage (Input) image being processed
	 * @param filter (Input) Used to filter results so that known matches don't pollute the results.
	 * @param limit (Input) The maximum number of results it will return. If &le; 0 then all matches are returned.
	 * @param matches (Output) Set of matches found in best first order. List is always cleared
	 * @return true if at least one valid match was found or false if no valid matches could be found. If false
	 * that means matches is empty. This is strictly a convenience.
	 */
	boolean query( T queryImage, @Nullable BoofLambdas.Filter<String> filter, int limit, DogArray<Match> matches );

	/**
	 * Returns a list of image IDs in the database
	 *
	 * @param storage (Optional) Storage for the list of images. If null a new instance is created
	 * @return List of all the image IDs.
	 */
	List<String> getImageIds( @Nullable List<String> storage );

	/** The image data type which can be processed */
	ImageType<T> getImageType();

	/** References a match in the database to the query image */
	@SuppressWarnings({"NullAway.Init"})
	class Match {
		/** ID of matching image */
		public String id;
		/** Error. Larger the value less similar it is to the original. Meaning is implementation dependent. */
		public double error;
	}
}
