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

package boofcv.alg.sfm.structure;

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.ImageDimension;
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

		alg.findSimilar(alg.getImageIDs().get(0), similar);

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

	/**
	 * Tests to see if when requesting matching features between the two views that the src and dst order
	 * is respected
	 */
	@Test void lookupMatches_src_dst() {
		LookUpSimilarImages alg = createFullyLoaded();

		List<String> images = alg.getImageIDs();
		String viewA = images.get(0);

		List<String> similar = new ArrayList<>();
		alg.findSimilar(viewA, similar);
		String viewB = similar.get(0);

		var pairsAB = new DogArray<>(AssociatedIndex::new);
		assertTrue(alg.lookupMatches(viewA, viewB, pairsAB));

		var pairsBA = new DogArray<>(AssociatedIndex::new);
		assertTrue(alg.lookupMatches(viewB, viewA, pairsBA));

		// Make sure there are a few elements in it
		assertTrue(pairsAB.size >= 5);
		// Make sure they are "equivalent"
		assertEquals(pairsAB.size, pairsBA.size);
		int countIdentical = 0;
		for (int i = 0; i < pairsAB.size; i++) {
			if (pairsAB.get(i).src == pairsAB.get(i).dst)
				countIdentical++;
			assertEquals(pairsAB.get(i).src, pairsBA.get(i).dst);
			assertEquals(pairsAB.get(i).dst, pairsBA.get(i).src);
		}

		// Sanity check to see if a good set of test data was constructed. If src and dst are the same then
		// this tests nothing
		assertNotEquals(countIdentical, pairsAB.size);
	}

	/**
	 * Requested matches when the src and dst are the same view. All features will be the match
	 */
	@Test void lookupMatches_sameView() {
		LookUpSimilarImages alg = createFullyLoaded();
		List<String> images = alg.getImageIDs();
		String viewA = images.get(0);

		var pairs = new DogArray<>(AssociatedIndex::new);
		assertTrue(alg.lookupMatches(viewA, viewA, pairs));

		var features = new DogArray<>(Point2D_F64::new);
		alg.lookupPixelFeats(viewA, features);

		// Number of features and pairs must match
		assertEquals(features.size, pairs.size);
		assertTrue(features.size >= 5);
		pairs.forEach(p -> assertEquals(p.src, p.dst));
	}

	/**
	 * See if it handles requests for views which don't exist well
	 */
	@Test void lookupMatches_nonExistentViews() {
		LookUpSimilarImages alg = createFullyLoaded();
		DogArray<AssociatedIndex> associated = new DogArray<>(AssociatedIndex::new);

		// Two identical strings which don't exist
		assertThrows(IllegalArgumentException.class, () -> alg.lookupMatches("asdf", "asdf", associated));
		// Two different strings which don't exist
		assertThrows(IllegalArgumentException.class, () -> alg.lookupMatches("asdf", "asdfA", associated));

		// One frame does exists and the other doesn't
		List<String> images = alg.getImageIDs();
		assertThrows(IllegalArgumentException.class, () -> alg.lookupMatches(images.get(0), "asdf", associated));
		assertThrows(IllegalArgumentException.class, () -> alg.lookupMatches("asdf", images.get(0), associated));
	}

	/**
	 * Makes the passed in array is cleared
	 */
	@Test void lookupMatches_cleared() {
		LookUpSimilarImages alg = createFullyLoaded();

		List<String> images = alg.getImageIDs();
		String viewA = images.get(0);

		List<String> similar = new ArrayList<>();
		alg.findSimilar(viewA, similar);
		String viewB = similar.get(0);

		DogArray<AssociatedIndex> pairsAB = new DogArray<>(AssociatedIndex::new);
		pairsAB.resize(10, ( e ) -> e.setTo(-2, -2));
		assertTrue(alg.lookupMatches(viewA, viewB, pairsAB));
		assertTrue(pairsAB.size() >= 1);
		// if wasn't cleared then it will have the same value for first element
		assertNotEquals(-2, pairsAB.get(0).src);
		assertNotEquals(-2, pairsAB.get(0).dst);
	}

	/**
	 * Very basic sanity check to make sure it returns a shape for every view and that it's a valid value
	 */
	@Test void lookupShape_valid() {
		LookUpSimilarImages alg = createFullyLoaded();

		List<String> images = alg.getImageIDs();

		ImageDimension found = new ImageDimension(-1, -1);
		for (String view : images) {
			found.setTo(-1, -1);
			alg.lookupShape(view, found);
			assertTrue(found.width > 0);
			assertTrue(found.height > 0);
		}
	}
}
