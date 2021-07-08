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

import boofcv.struct.feature.TupleDesc;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public abstract class GenericFeatureSceneRecognitionChecks<TD extends TupleDesc<TD>> extends BoofStandardJUnit {

	/**
	 * New instance of the algorithm being tested
	 */
	public abstract FeatureSceneRecognition<TD> createAlg();

	/**
	 * Create a descriptor. The description needs to be identical when given the same seed
	 */
	public abstract TD createDescriptor( int seed );

	/**
	 * Test everything all together in a simple example
	 */
	@Test void simpleAll() {
		// Create descriptions for all the "images". Vary the number of descriptions but make sure
		// some of them overlap so images have similarities
		List<List<TD>> images = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			List<TD> descriptions = new ArrayList<>();
			int N = 10 + i;
			for (int j = 0; j < N; j++) {
				descriptions.add(createDescriptor(i + j));
			}
			images.add(descriptions);
		}

		FeatureSceneRecognition<TD> alg = createAlg();

		// learn the model from the pre-generated images
		alg.learnModel(new Iterator<>() {
			int index = 0;

			@Override public boolean hasNext() {return index < images.size();}

			@Override public FeatureSceneRecognition.Features<TD> next() {
				int imageIdx = index++;
				return getFeatures(imageIdx, images);
			}
		});

		// Now add the images and query the words found in the image just added
		var wordZeroInImages = new DogArray_I32();
		var words = new DogArray_I32();
		for (int imageIdx = 0; imageIdx < images.size(); imageIdx++) {
			alg.addImage("" + imageIdx, getFeatures(imageIdx, images));

			// sanity check the found words using precomputed information from the just added image
			int word0 = alg.getQueryWord(0);
			assertTrue(word0 > 0);


			alg.getQueryWords(0, words);
			assertTrue(words.size > 0);
			// Even if there are multiple words for this feature the singular word should be in this larger list
			assertTrue(words.contains(word0));

			// Save the word for testing later on
			wordZeroInImages.add(word0);
		}
		assertEquals(images.size(), alg.getImageIds(null).size());

		// It should have some matches and make sure the limit is respected
		var matches = new DogArray<>(SceneRecognition.Match::new);
		assertTrue(alg.query(getFeatures(1, images), ( id ) -> true, 3, matches));
		assertTrue(matches.size > 0 && matches.size <= 3);

		// See if it handles a null filter correctly. Should produce the same results
		var matchesNull = new DogArray<>(SceneRecognition.Match::new);
		assertTrue(alg.query(getFeatures(1, images), null, 3, matchesNull));
		assertEquals(matchesNull.size, matchesNull.size);
		matches.forIdx(( idx, e ) -> assertEquals(e.id, matchesNull.get(idx).id));

		// It should always find a word since it was part of the training set
		for (int imageIdx = 0; imageIdx < images.size(); imageIdx++) {
			// The word returned when we queried it should be the same as now
			int found = alg.lookupWord(images.get(imageIdx).get(0));
			assertEquals(wordZeroInImages.get(imageIdx), found);

			alg.lookupWords(images.get(imageIdx).get(0), words);
			assertTrue(words.size >= 1);
			assertTrue(words.contains(found));
		}


		// See if clearing the DB works
		alg.clearDatabase();
		assertFalse(alg.query(getFeatures(1, images), ( id ) -> true, 3, matches));
	}

	private FeatureSceneRecognition.Features<TD> getFeatures( int imageIdx, List<List<TD>> images ) {
		return new FeatureSceneRecognition.Features<>() {
			@Override public Point2D_F64 getPixel( int index ) {
				return new Point2D_F64(rand.nextInt(50), rand.nextInt(40));
			}

			@Override public TD getDescription( int index ) {return images.get(imageIdx).get(index);}

			@Override public int size() {return images.get(imageIdx).size();}
		};
	}

	@Test void getDescriptorType() {
		FeatureSceneRecognition<TD> alg = createAlg();
		assertSame(createDescriptor(0).getClass(), alg.getDescriptorType());
	}
}
