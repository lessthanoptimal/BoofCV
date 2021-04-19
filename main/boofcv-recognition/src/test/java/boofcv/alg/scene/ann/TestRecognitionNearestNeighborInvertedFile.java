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

package boofcv.alg.scene.ann;

import boofcv.alg.scene.bow.BowDistanceTypes;
import boofcv.testing.BoofStandardJUnit;
import georegression.helper.KdTreePoint2D_F64;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.util.PrimitiveArrays;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestRecognitionNearestNeighborInvertedFile extends BoofStandardJUnit {
	@Test void allTogetherSimple() {
		// randomly create a set of words
		List<Point2D_F64> words = new ArrayList<>();
		for (int i = 0; i < 88; i++) {
			words.add(new Point2D_F64(rand.nextDouble()*2, rand.nextDouble()*2));
		}
		var nn = FactoryNearestNeighbor.exhaustive(new KdTreePoint2D_F64());
		nn.setPoints(words, true);

		var alg = new RecognitionNearestNeighborInvertedFile<Point2D_F64>();
		alg.initialize(nn, words.size());

		// Save randomly generated images for making queries with known answers later on
		List<List<Point2D_F64>> images = new ArrayList<>();

		for (int i = 0; i < 20; i++) {
			var image = new ArrayList<Point2D_F64>();
			int N = rand.nextInt(20) + 20;
			for (int j = 0; j < N; j++) {
				image.add(words.get(rand.nextInt(words.size())));
			}

			alg.addImage(i, image);
			images.add(image);
		}

		// Look up images given the image
		assertTrue(alg.query(images.get(5), null, 3));
		assertEquals(3, alg.getMatches().size);
		assertEquals(5, alg.getMatches().get(0).identification);

		// Look up with filter. The correct solutiuon should be excluded
		assertTrue(alg.query(images.get(5), ( index ) -> index != 5, 3));
		assertEquals(3, alg.getMatches().size);
		assertNotEquals(5, alg.getMatches().get(0).identification);

		// after clearing it shouldn't be able to find any thing
		alg.clearImages();

		assertFalse(alg.query(images.get(5), null, 3));
	}

	@Test void computeWordHistogram() {
		List<Point2D_F64> words = new ArrayList<>();
		words.add(new Point2D_F64(10, 20));
		words.add(new Point2D_F64(5, -12));
		words.add(new Point2D_F64(200, 50));
		words.add(new Point2D_F64(-10, 0));

		var alg = new RecognitionNearestNeighborInvertedFile<Point2D_F64>();
		alg.initialize(FactoryNearestNeighbor.exhaustive(new KdTreePoint2D_F64()), 99);
		alg.nearestNeighbor.setPoints(words, true);

		// randomly generate observations of words
		List<Point2D_F64> observations = new ArrayList<>();
		for (int feature = 0; feature < 105; feature++) {
			observations.add(words.get(rand.nextInt(words.size())));
		}

		alg.computeWordHistogram(observations);
		assertEquals(99, alg.wordHistogram.size);

		int total = (int)PrimitiveArrays.sumD(alg.wordHistogram.data, 0, words.size());
		assertEquals(105, total);
		assertEquals(4, alg.observedWords.size);
	}

	/**
	 * Manually construct a histogram and see if it returns the expected results
	 */
	@Test void computeImageDescriptor() {
		var alg = new RecognitionNearestNeighborInvertedFile<Point2D_F64>();
		alg.setDistanceType(BowDistanceTypes.L2);

		alg.observedWords.add(10);
		alg.observedWords.add(15);
		alg.observedWords.add(1);
		alg.wordHistogram.resetResize(20, 0);
		alg.wordHistogram.set(10, 10);
		alg.wordHistogram.set(15, 1);
		alg.wordHistogram.set(1, 40);

		alg.computeImageDescriptor(41);

		// Compute the L2 norm
		double sum = PrimitiveArrays.feedbackIdxDOp(
				alg.tmpDescWeights.data, 0, 3,
				( idx, value, prior ) -> prior + value*value);
		assertEquals(1.0, sum, UtilEjml.TEST_F32);
	}

	/**
	 * Simple test that when given a very simple set of inputs produces the expected output
	 */
	@Test void findAndScoreMatches() {
		var alg = new RecognitionNearestNeighborInvertedFile<Point2D_F64>();

		// Initialize data structures
		for (int i = 0; i < 30; i++) {
			alg.imagesDB.add(i);
		}
		alg.invertedFiles.resize(100);
		alg.observedWords.add(10);
		alg.observedWords.add(15);
		alg.tmpDescWeights.add(0.1f);
		alg.tmpDescWeights.add(0.05f);

		// add the same files to each word. give it arbitrary weights
		for (int i = 0; i < 4; i++) {
			alg.invertedFiles.get(10).addImage(i*2, (float)(0.3 - 0.05*i));
			alg.invertedFiles.get(15).addImage(i*2, (float)(0.3 - 0.05*i));
		}

		alg.findAndScoreMatches();

		// only 4 images should have been found
		assertEquals(4, alg.matches.size);
		// I know which images it should be. Order doesn't matter but that's known too
		alg.matches.forIdx(( idx, m ) -> assertEquals(idx*2, m.identification));
		// weights were  arbitrary, but I know score must be less than 2
		alg.matches.forEach(m -> assertTrue(m.error < 2.0));
		// sanity check the look up table
		alg.matches.forIdx(( idx, m ) -> assertEquals(idx, alg.imageIdx_to_match.get(m.identification)));
	}
}
