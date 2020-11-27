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

package boofcv.alg.feature.describe.llah;

import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.combinatorics.Combinations;
import org.ddogleg.sorting.QuickSort_F64;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestLlahOperations extends BoofStandardJUnit {

	int neighborsN = 7;
	int comboM = 5;

	List<List<Point2D_F64>> documents = new ArrayList<>();

	long totalCalls;

	public TestLlahOperations() {
		for (int i = 0; i < 10; i++) {
			documents.add(UtilPoint2D_F64.random(-5, 5, 20, rand));
		}
	}

	/**
	 * See if it can look up documents when given perfect information. Then it should fail when given documents
	 * not in the list.
	 */
	@Test
	void simple_test_everything() {
		simple_test_everything(LlahInvariant.AFFINE);
		simple_test_everything(LlahInvariant.CROSS_RATIO);
	}

	void simple_test_everything(LlahInvariant type) {
		LlahOperations llahOps = createLlahOps(type);

		for (int docID = 0; docID < 5; docID++) {
			llahOps.createDocument(documents.get(docID));
		}

		var found = new ArrayList<LlahOperations.FoundDocument>();

		int maxDotHits = (int) llahOps.computeMaxUniqueHashPerPoint();

		// See if the requested document is the best match
		for (int docID = 0; docID < 5; docID++) {
//			System.out.println("TESTING "+docID);
			llahOps.lookupDocuments(documents.get(docID), 8, found);
			assertFalse(found.isEmpty());

			LlahOperations.FoundDocument best = null;
			int bestHits = 0;
			for (var result : found) {
//				System.out.println("doc "+result.document.documentID+" hits "+result.countHits());
				if (result.countHits() > bestHits) {
					best = result;
					bestHits = result.countHits();
				}
			}

			assertNotNull(best);
			assertEquals(best.document.documentID, docID);

			// there is a one-to-one relationship between dots and markers here. Make sure that's true
			final int N = documents.get(docID).size();
			for (int landmarkIdx = 0; landmarkIdx < N; landmarkIdx++) {
				int dotIdx = best.landmarkToDots.get(landmarkIdx);
				int hits = best.landmarkHits.get(landmarkIdx);

				assertEquals(landmarkIdx, dotIdx);
				assertTrue(hits > maxDotHits * 0.8);
				// won't always be the max since it stops on the first hash match
			}
		}

		// If given a document not in the list all the matches should be very poor
		for (int docID = 5; docID < documents.size(); docID++) {
			llahOps.lookupDocuments(documents.get(docID), 8, found);
			if (found.isEmpty())
				continue;

			LlahOperations.FoundDocument best = null;
			int bestHits = 0;
			for (var result : found) {
				if (result.countHits() > bestHits) {
					bestHits = result.countHits();
					best = result;
				}
			}
			assertNotNull(best);

			// Number of features is the maximum number of hits
			assertTrue(bestHits < best.document.features.size() / 5);
		}
	}

	private LlahOperations createLlahOps(LlahInvariant invariantType) {
		switch (invariantType) {
			case AFFINE:
				return new LlahOperations(neighborsN, comboM, new LlahHasher.Affine(8, 1000));
			case CROSS_RATIO:
				return new LlahOperations(neighborsN + 1, comboM + 1, new LlahHasher.CrossRatio(16, 10000));
			default:
				throw new IllegalArgumentException("Unknown");
		}
	}

	@Test
	void learnHashing() {
		LlahOperations llahOps = createLlahOps(LlahInvariant.AFFINE);

		int numDiscrete = 30;
		double maxValue = 100;
		llahOps.learnHashing(documents, numDiscrete, 100_000, maxValue);
		int[] counts = new int[numDiscrete];
		for (int i = 0; i < 1000; i++) {
			double value = maxValue * i / 1000.0;
			int d = llahOps.hasher.discretize(value);
			counts[d]++;
		}
		int totalNotZero = 0;
		for (int i = 0; i < numDiscrete; i++) {
			if (counts[i] != 0)
				totalNotZero++;
		}

//		for (int i = 0; i < llahOps.hasher.samples.length; i++) {
//			if( i != 0 && i%5 == 0 )
//				System.out.println();
//			System.out.print(llahOps.hasher.samples[i]+", ");
//		}
//		System.out.println();

		assertTrue(totalNotZero > numDiscrete * 0.5);
	}


	/**
	 * Sees if all the neighbors are found for the specified point. The specified point should not be
	 * included in the neighbor list
	 */
	@Test
	void findNeighbors() {
		LlahOperations llahOps = createLlahOps(LlahInvariant.AFFINE);

		List<Point2D_F64> list = UtilPoint2D_F64.random(-1, 1, 20, rand);
		List<Point2D_F64> expected = new ArrayList<>();
		llahOps.nn.setPoints(list, false);

		Point2D_F64 target = list.get(10);
		var distances = new double[list.size()];
		for (int i = 0; i < list.size(); i++) {
			expected.add(list.get(i));
			distances[i] = target.distance(list.get(i));
		}
		new QuickSort_F64().sort(distances, list.size(), expected);

		llahOps.findNeighbors(target);

		for (int i = 0; i < neighborsN; i++) {
			assertTrue(llahOps.neighbors.contains(expected.get(i + 1)));
		}
	}

	@Test
	void computeAllFeatures() {
		List<Point2D_F64> list = UtilPoint2D_F64.random(-1, 1, 20, rand);
		long nCm = Combinations.computeTotalCombinations(neighborsN, comboM);
		long expected = nCm * comboM; // cyclical permutations
		expected *= list.size(); // once for each point

		LlahOperations llahOps = createLlahOps(LlahInvariant.AFFINE);

		totalCalls = 0;
		llahOps.computeAllFeatures(list, (targetIndex, points) -> totalCalls++);

		assertEquals(expected, totalCalls);

		assertEquals(nCm * comboM, llahOps.computeMaxUniqueHashPerPoint());
	}

	@Test
	void checkListSize() {
		LlahOperations llahOps = createLlahOps(LlahInvariant.AFFINE);

		// Requires N neighbors + 1
		var list = new ArrayList<Point2D_F64>();
		for (int i = 0; i < neighborsN + 1; i++) {
			list.add(new Point2D_F64(1, 1));
		}
		llahOps.checkListSize(list);

		list.remove(0);

		try {
			llahOps.checkListSize(list);
			fail("Should have thrown an exception");
		} catch (Exception ignore) {
		}
	}

	/**
	 * Rotating the points should not affect the results.
	 */
	@Test
	void invariantToRotation() {
		for (var invariant : LlahInvariant.values()) {
			invariantToRotation(createLlahOps(invariant));
		}
	}

	private void invariantToRotation(LlahOperations llahOps) {
		for (int docID = 0; docID < documents.size(); docID++) {
			llahOps.createDocument(documents.get(docID));
		}

		List<Point2D_F64> target = documents.get(2);
		List<LlahOperations.FoundDocument> foundDocuments = new ArrayList<>();
		llahOps.lookupDocuments(target, 8, foundDocuments);
		assertEquals(1, foundDocuments.size());
		DogArray_I32 expected = foundDocuments.get(0).landmarkToDots;

		for (int angleIdx = 0; angleIdx < 12; angleIdx++) {
			var se = new Se2_F64(0, 0, angleIdx * Math.PI / 4);

			// rotate and save into a new list of points
			var rotated = new ArrayList<Point2D_F64>();
			for (var p : target) {
				var d = new Point2D_F64();
				SePointOps_F64.transform(se, p, d);
				rotated.add(d);
			}
			llahOps.lookupDocuments(rotated, 8, foundDocuments);
			assertEquals(1, foundDocuments.size());
			DogArray_I32 found = foundDocuments.get(0).landmarkToDots;

			// Use the number of matches as a finger print
			for (int i = 0; i < expected.size; i++) {
				assertEquals(expected.data[i], found.data[i]);
			}
		}
	}

	/**
	 * The input can be shuffled and it won't change the results
	 */
	@Test
	void invariantToOrder() {
		for (var invariant : LlahInvariant.values()) {
			invariantToOrder(createLlahOps(invariant));
		}
	}

	private void invariantToOrder(LlahOperations llahOps) {
		for (int docID = 0; docID < documents.size(); docID++) {
			llahOps.createDocument(documents.get(docID));
		}

		List<Point2D_F64> target = documents.get(2);
		List<LlahOperations.FoundDocument> foundDocuments = new ArrayList<>();
		llahOps.lookupDocuments(target, 8, foundDocuments);
		assertEquals(1, foundDocuments.size());
		DogArray_I32 expected = foundDocuments.get(0).landmarkToDots;

		for (int trial = 0; trial < 12; trial++) {
			// Change the input's order
			var shuffled = new ArrayList<>(target);
			Collections.shuffle(shuffled, rand);

			// Look up the document
			llahOps.lookupDocuments(shuffled, 8, foundDocuments);
			assertEquals(1, foundDocuments.size());
			DogArray_I32 found = foundDocuments.get(0).landmarkToDots;

			// Use the number of matches as a finger print
			for (int i = 0; i < expected.size; i++) {
				assertEquals(expected.data[i], found.data[i]);
			}
		}
	}

	@Test
	void invariantToNoise() {
		for (var invariant : LlahInvariant.values()) {
			invariantToNoise(createLlahOps(invariant));
		}
	}

	private void invariantToNoise(LlahOperations llahOps) {
		for (int docID = 0; docID < documents.size(); docID++) {
			llahOps.createDocument(documents.get(docID));
		}

		List<Point2D_F64> target = documents.get(2);
		List<LlahOperations.FoundDocument> foundDocuments = new ArrayList<>();

		for (int trial = 0; trial < 10; trial++) {
			// add noise
			var noised = new ArrayList<Point2D_F64>();
			for( var p : target ) {
				p = p.copy();
				p.x += rand.nextGaussian()*0.03;
				p.y += rand.nextGaussian()*0.03;
				noised.add(p);
			}

			// Look up the document
			llahOps.lookupDocuments(noised,8,foundDocuments);
			assertEquals(1, foundDocuments.size());
			LlahOperations.FoundDocument doc = foundDocuments.get(0);
			assertEquals(2,doc.document.documentID);

			DogArray_I32 found = doc.landmarkToDots;

			// The order should be the same, but a few misses are allowed
			int totalMatched = 0;
			int totalWrong = 0;
			for (int i = 0; i < found.size; i++) {
				if( found.data[i] < 0 )
					continue;
				if( i != found.data[i])
					totalWrong++;
				totalMatched++;
			}

			assertTrue( totalWrong <= 2 );
			assertTrue(totalMatched>=found.size*3/4);
		}
	}
}
