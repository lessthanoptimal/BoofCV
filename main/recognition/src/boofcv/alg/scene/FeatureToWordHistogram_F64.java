/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.clustering.AssignCluster;

import java.util.Arrays;

/**
 * <p>
 * Creates a normalized histogram which represents the frequency of different visual words from the set of features.
 * Both hard and soft assignment can be used.  For hard assignment all the weight is given to the word which is the
 * best fit to the feature.  In soft the relative similarity between the words is used to assign values to the histogram.
 * </p>
 * <p>
 * With hard assignment a single word is selected.  With soft a fraction is assigned to each word based on
 * a distance metric.  See {@link AssignCluster} for the details.
 * </p>
 * @author Peter Abeles
 */
public class FeatureToWordHistogram_F64
		implements FeatureToWordHistogram<TupleDesc_F64>
{
	// Assigns a feature to a word
	private AssignCluster<double[]> assignment;

	// should it use hard or soft assignment
	private boolean hardAssignment;

	// total number of features which have been assigned to the histogram
	private int total;
	private double histogram[];

	// internal work space
	private double temp[];

	// used to catch a common bug
	private boolean processed;

	/**
	 * Assigns and configures internal algorithms.
	 *
	 * @param assignment Specifies the assignment algorithm
	 * @param hardAssignment true for hard assignment and false for soft assignment
	 */
	public FeatureToWordHistogram_F64(AssignCluster<double[]> assignment, boolean hardAssignment ) {
		this.assignment = assignment;
		this.hardAssignment = hardAssignment;

		histogram = new double[ assignment.getNumberOfClusters() ];
		if( !hardAssignment ) {
			temp = new double[ assignment.getNumberOfClusters() ];
		}
	}

	@Override
	public void reset() {
		total = 0;
		processed = false;
		Arrays.fill(histogram,0);
	}

	@Override
	public void addFeature( TupleDesc_F64 feature ) {
		if( hardAssignment ) {
			histogram[assignment.assign(feature.getValue())] += 1;
		} else {
			assignment.assign(feature.getValue(),temp);
			for (int i = 0; i < histogram.length; i++) {
				histogram[i] += temp[i];
			}
			// temp is already normalized such that the sum is equal to 1.  so total is also += 1
		}
		total += 1;
	}

	/**
	 * No more features are being added.  Normalized the computed histogram.
	 */
	@Override
	public void process() {
		processed = true;
		for (int i = 0; i < histogram.length; i++) {
			histogram[i] /= total;
		}
	}

	/**
	 * Histogram of word frequencies.  Normalized such that the sum is equal to 1.
	 * @return histogram
	 */
	@Override
	public double[] getHistogram() {
		if( !processed )
			throw new RuntimeException("Must call process first before histogram is valid");
		return histogram;
	}

	/**
	 * The total number of words used to create this histogram
	 */
	@Override
	public int getTotalWords() {
		return histogram.length;
	}
}
