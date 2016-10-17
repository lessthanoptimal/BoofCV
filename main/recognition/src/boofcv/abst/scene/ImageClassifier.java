/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.ImageBase;

import java.util.List;

/**
 * High level interface for a classifier which assigns a single category to an image.
 *
 * @author Peter Abeles
 */
public interface ImageClassifier<T extends ImageBase<T>> extends ImageModelBase<T> {

	/**
	 * Process the image and determine which category it belongs to.  Will throw an exception if the model has
	 * not been loaded yet.
	 *
	 * @param image Image being processed.  All pixel values have a range of 0 to 255.  If a color image then the
	 *              bands will be in red, green, blue order.
	 */
	void classify( T image );

	/**
	 * Returns the category which was the best fit.
	 * @return best fit category
	 */
	int getBestResult();

	/**
	 * Returns a list of all the likely categories for the image.  What is likely is implementation dependent.
	 * Each category should be included at least once and might not be included at all.
	 * @return List of categories and scores
	 */
	List<Score> getAllResults();

	/**
	 * Returns a list of all the possible categories that a scene can be classified as
	 * @return Names of categories
	 */
	List<String> getCategories();

	/**
	 * Provides information on the score for a specific category when multiple results are requested
	 */
	class Score {
		/**
		 * The score associated with a particular category.  Score's meaning is implementation dependent.
		 */
		public double score;
		/**
		 * The category
		 */
		public int category;

		public void set( double score , int category ) {
			this.score = score;
			this.category = category;
		}
	}
}
