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

package boofcv.alg.scene;

import boofcv.struct.feature.TupleDesc;

/**
 * Used to construct a normalized histogram which represents the frequency of certain words in an image for use
 * in a BOW based classifier. Features are added one at a time and internally placed in the histogram. When
 * there are no more features left {@link #process()} is called and the histogram computed. The returned histogram
 * will be normalized such that it sums up to one. This normalization makes it more tolerant to images of
 * different sized and sampling frequency. Call {@link #reset} when the next image is ready.
 *
 * @author Peter Abeles
 */
public interface FeatureToWordHistogram<Desc extends TupleDesc> {
	/**
	 * Must be called before {@link #addFeature(boofcv.struct.feature.TupleDesc)} is called.
	 */
	void reset();

	/**
	 * Adds a feature to the histogram
	 *
	 * @param feature A feature which is to be matched to words. Not modified.
	 */
	void addFeature( Desc feature );

	/**
	 * No more features are being added. Normalized the computed histogram.
	 */
	void process();

	/**
	 * Histogram of word frequencies. Normalized such that the sum is equal to 1.
	 * @return histogram
	 */
	double[] getHistogram();

	/**
	 * Number of elements in the histogram. Which is the number of words the features are assigned to.
	 */
	int getTotalWords();
}
