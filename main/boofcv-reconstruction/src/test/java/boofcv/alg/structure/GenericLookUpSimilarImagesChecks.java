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

package boofcv.alg.structure;

import boofcv.struct.feature.AssociatedIndex;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic checks that the contract for LookUpSimilarImages is being obeyed.
 *
 * @author Peter Abeles
 */
public abstract class GenericLookUpSimilarImagesChecks extends BoofStandardJUnit {
	/**
	 * Creates a new instance which already has several images loaded.
	 */
	public abstract <T extends LookUpSimilarImages> T createFullyLoaded();

	/**
	 * Makes the passed in array is cleared
	 */
	@Test void findSimilar_cleared() {
		LookUpSimilarImages alg = createFullyLoaded();

		List<String> similar = new ArrayList<>();
		similar.add("ASDASDASD");

		alg.findSimilar(alg.getImageIDs().get(0), ( id ) -> true, similar);

		assertTrue(similar.size() >= 1);
		assertNotEquals("ASDASDASD", similar.get(0));
	}

	/**
	 * Makes the passed in array is cleared
	 */
	@Test void lookupPixelFeats_cleared() {
		LookUpSimilarImages alg = createFullyLoaded();

		List<String> images = alg.getImageIDs();
		String viewA = images.get(0);

		var features = new DogArray<>(Point2D_F64::new);
		features.grow().setTo(-1, -1);
		alg.lookupPixelFeats(viewA, features);

		assertNotEquals(-1.0, features.get(0).x);
		assertNotEquals(-1.0, features.get(0).y);
	}

	/**
	 * Requested view does not exist.
	 */
	@Test void lookupPixelFeats_nonExistentView() {
		LookUpSimilarImages alg = createFullyLoaded();

		var features = new DogArray<>(Point2D_F64::new);
		assertThrows(IllegalArgumentException.class, () -> alg.lookupPixelFeats("asdasd", features));
	}

	@Test void findSimilar_filter() {
		LookUpSimilarImages alg = createFullyLoaded();

		List<String> images = alg.getImageIDs();
		String viewA = images.get(0);

		List<String> similar = new ArrayList<>();
		alg.findSimilar(viewA, ( id ) -> true, similar);
		String viewB = similar.get(0);

		// search a second time, but now exclude viewB and see if it's removed from the similar list
		alg.findSimilar(viewA, ( id ) -> !id.equals(viewB), similar);
		similar.forEach(id -> assertNotEquals(id, viewB));
	}

	/**
	 * Compare results when passing in null against results when the filter is always true
	 */
	@Test void findSimilar_filter_null() {
		LookUpSimilarImages alg = createFullyLoaded();

		List<String> images = alg.getImageIDs();
		String viewA = images.get(0);

		List<String> expected = new ArrayList<>();
		alg.findSimilar(viewA, ( id ) -> true, expected);

		List<String> found = new ArrayList<>();
		alg.findSimilar(viewA, null, found);

		assertEquals(expected.size(), found.size());
		for (int i = 0; i < expected.size(); i++) {
			assertEquals(expected.get(i), found.get(i));
		}
	}

	/**
	 * Simple sanity check that just sees if it blows up or not
	 */
	@Test void lookupAssociated() {
		LookUpSimilarImages alg = createFullyLoaded();

		List<String> images = alg.getImageIDs();

		assertFalse(images.isEmpty());

		int totalNonZero = 0;
		DogArray<AssociatedIndex> pairs = new DogArray<>(AssociatedIndex::new);
		List<String> similar = new ArrayList<>();

		int N = Math.min(5, images.size());
		for (int i = 0; i < N; i++) {
			String id = images.get(i);

			alg.findSimilar(id, (a)->true, similar);

			for (String s : similar) {
				if (alg.lookupAssociated(s, pairs)) {
					assertTrue(pairs.size>0);
					totalNonZero++;
				} else {
					assertTrue(pairs.isEmpty());
				}
			}
		}

		assertTrue(totalNonZero>1);
	}

	/**
	 * Attempt to look up associated features for a view which does not exist/was not similar to the query image
	 */
	@Test void lookupAssociated_nonExistentViews() {
		LookUpSimilarImages alg = createFullyLoaded();

		List<String> images = alg.getImageIDs();
		String viewA = images.get(0);

		List<String> similar = new ArrayList<>();
		alg.findSimilar(viewA, ( id ) -> true, similar);

		// Request a match to a non-existent similar image
		var pairs = new DogArray<>(AssociatedIndex::new);
		assertThrows(IllegalArgumentException.class, () -> alg.lookupAssociated("adsasdasd",pairs));
	}

	/**
	 * Makes sure the passed in array is cleared
	 */
	@Test void lookupAssociated_cleared() {
		LookUpSimilarImages alg = createFullyLoaded();

		List<String> images = alg.getImageIDs();
		String viewA = images.get(0);

		List<String> similar = new ArrayList<>();
		alg.findSimilar(viewA, ( id ) -> true, similar);
		String viewB = similar.get(0);

		DogArray<AssociatedIndex> pairsAB = new DogArray<>(AssociatedIndex::new);
		pairsAB.resize(10, ( e ) -> e.setTo(-2, -2));
		assertTrue(alg.lookupAssociated(viewB, pairsAB));
		assertTrue(pairsAB.size() >= 1);
		// if wasn't cleared then it will have the same value for first element
		assertNotEquals(-2, pairsAB.get(0).src);
		assertNotEquals(-2, pairsAB.get(0).dst);
	}
}
