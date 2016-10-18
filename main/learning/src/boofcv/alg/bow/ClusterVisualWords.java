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

package boofcv.alg.bow;

import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.clustering.AssignCluster;
import org.ddogleg.clustering.ComputeClusters;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds clusters of {@link TupleDesc_F64} which can be used to identify frequent features, a.k.a words.
 * Internally it uses {@link org.ddogleg.clustering.ComputeClusters} and simply extracts the inner array
 * from the tuple.
 *
 * @author Peter Abeles
 */
// TODO make completely generic?  Not F64 specific?
public class ClusterVisualWords {

	// cluster finding algorithm
	ComputeClusters<double[]> computeClusters;

	// inner arrays extracted from the input features
	List<double[]> tuples = new ArrayList<>();

	/**
	 * Constructor which configures the cluster finder.
	 *
	 * @param computeClusters Cluster finding algorithm.
	 * @param featureDOF Number of elements in the feature
	 * @param randomSeed Seed for random number generator
	 */
	public ClusterVisualWords(ComputeClusters<double[]> computeClusters, int featureDOF, long randomSeed) {
		this.computeClusters = computeClusters;

		computeClusters.init(featureDOF,randomSeed);
	}

	/**
	 * Add a feature to the list.
	 *
	 * @param feature image feature. Reference to inner array is saved.
	 */
	public void addReference(TupleDesc_F64 feature) {
		tuples.add(feature.getValue());
	}

	/**
	 * Clusters the list of features into the specified number of words
	 * @param numberOfWords Number of words/clusters it should find
	 */
	public void process( int numberOfWords ) {
		computeClusters.process(tuples,numberOfWords);
	}

	/**
	 * Returns a transform from point to cluster.
	 */
	public AssignCluster<double[]> getAssignment() {
		return computeClusters.getAssignment();
	}

}
