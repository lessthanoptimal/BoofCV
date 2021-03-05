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

package boofcv.struct.kmeans;

import boofcv.struct.PackedArray;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.clustering.ComputeMeanClusters;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Common checks for implementations of {@link ComputeMeanClusters}
 *
 * @author Peter Abeles
 **/
public abstract class GenericComputeMeanClustersChecks<T> extends BoofStandardJUnit {

	// If a tuple descriptor this is the number of elements
	protected int DOF = 4;

	protected double tol;

	// Number of clusters/labels
	int numLabels = 3;
	// Storage for input points
	PackedArray<T> packed = createArray();
	// Which points have been assigned to which labels
	DogArray_I32 assignments = new DogArray_I32();
	// List of randomly genered points
	List<T> list = new ArrayList<>();

	/** Creates the algorithm being tested */
	public abstract ComputeMeanClusters<T> createAlg();

	/** Creates the correct array data type */
	public abstract PackedArray<T> createArray();

	/** Convert a point in to a generic format */
	public abstract void pointToCommonArray( T src, DogArray_F64 dst );

	/** Creates a random point */
	public abstract T randomPoint();

	/** Compares found solution against a generic way of computing the mean clusters */
	@Test public void computeMean() {
		// determine DOF and create work space
		var tmp = new DogArray_F64();
		pointToCommonArray(randomPoint(), tmp);
		int DOF = tmp.size;

		// Create random points and assignments
		createRandomPoints();

		// Compute the means of the labeled clusters
		DogArray<T> clusters = new DogArray<>(this::randomPoint);
		clusters.resize(numLabels);

		ComputeMeanClusters<T> alg = createAlg();
		alg.process(packed, assignments, clusters);

		// Compute solution
		List<double[]> expected = computeExpected(tmp, DOF, numLabels, list, assignments);

		// Compare to expected
		for (int label = 0; label < numLabels; label++) {
			double[] e = expected.get(label);
			pointToCommonArray(clusters.get(label), tmp);

			for (int i = 0; i < DOF; i++) {
				assertEquals(e[i], tmp.get(i), tol);
			}
		}
	}

	/** Call it more than once and make sure it produces the same answer */
	@Test public void multipleCalls() {
		// Create random points and assignments
		createRandomPoints();

		// Call it twice
		DogArray<T> clusters1 = new DogArray<>(this::randomPoint);
		DogArray<T> clusters2 = new DogArray<>(this::randomPoint);

		clusters1.resize(numLabels);
		clusters2.resize(numLabels);

		ComputeMeanClusters<T> alg = createAlg();
		alg.process(packed, assignments, clusters1);
		alg.process(packed, assignments, clusters2);

		// See if it got identical answers
		checkIdentical(clusters1, clusters2);
	}

	/** Makes sure the newInstance returns the same result as the original */
	@Test public void newInstanceThread() {
		createRandomPoints();

		// Call it twice
		DogArray<T> clusters1 = new DogArray<>(this::randomPoint);
		DogArray<T> clusters2 = new DogArray<>(this::randomPoint);

		clusters1.resize(numLabels);
		clusters2.resize(numLabels);

		ComputeMeanClusters<T> alg = createAlg();
		alg.process(packed, assignments, clusters1);
		alg.newInstanceThread().process(packed, assignments, clusters2);

		// See if it got identical answers
		checkIdentical(clusters1, clusters2);
	}

	private void checkIdentical( DogArray<T> clusters1, DogArray<T> clusters2 ) {
		DogArray_F64 dog1 = new DogArray_F64();
		DogArray_F64 dog2 = new DogArray_F64();

		for (int label = 0; label < numLabels; label++) {
			pointToCommonArray(clusters1.get(label), dog1);
			pointToCommonArray(clusters2.get(label), dog2);

			for (int element = 0; element < dog1.size; element++) {
				assertEquals(dog1.get(element), dog2.get(element), UtilEjml.TEST_F64);
			}
		}
	}

	private List<double[]> computeExpected( DogArray_F64 tmp, int DOF, int numLabels, List<T> list, DogArray_I32 assignments ) {
		int[] counts = new int[numLabels];
		List<double[]> expected = new ArrayList<>();
		for (int i = 0; i < numLabels; i++) {
			expected.add(new double[DOF]);
		}

		for (int i = 0; i < list.size(); i++) {
			int label = assignments.get(i);
			T point = list.get(i);
			pointToCommonArray(point, tmp);

			counts[label]++;
			double[] cluster = expected.get(label);
			for (int j = 0; j < tmp.size; j++) {
				cluster[j] += tmp.get(j);
			}
		}

		for (int i = 0; i < numLabels; i++) {
			for (int j = 0; j < DOF; j++) {
				expected.get(i)[j] /= counts[i];
			}
		}
		return expected;
	}

	private void createRandomPoints() {
		for (int i = 0; i < 30; i++) {
			T point = randomPoint();
			list.add(point);
			packed.append(point);
			assignments.add(rand.nextInt(numLabels));
		}
	}
}
