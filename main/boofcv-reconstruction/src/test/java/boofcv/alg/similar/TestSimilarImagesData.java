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

package boofcv.alg.similar;

import boofcv.struct.feature.AssociatedIndex;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSimilarImagesData extends BoofStandardJUnit {
	/**
	 * Checks all functions to some extent
	 */
	@Test void buildAndLookUp() {
		var alg = new SimilarImagesData();

		List<List<Point2D_F64>> features = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			features.add(UtilPoint2D_F64.random(0, 200, 10 + i*2, rand));
		}

		alg.add("0", features.get(0));
		alg.add("1", features.get(1));
		alg.add("2", features.get(2));
		alg.add("3", features.get(3));

		for (int i = 0; i < 3; i++) {
			for (int j = i + 1; j < 3; j++) {
				if ((i+j)%2==0)
					alg.setRelationship(i + "", j + "",
							randomAssociated(features.get(i).size(), features.get(j).size(), 8).toList());
				else
					alg.setRelationship(j + "", i + "",
							randomAssociated(features.get(j).size(), features.get(i).size(), 8).toList());
			}
		}

		// See if the features were stored correctly
		DogArray<Point2D_F64> foundFeatures = new DogArray<>(Point2D_F64::new);
		for (int view = 0; view < features.size(); view++) {
			List<Point2D_F64> expected = features.get(view);
			alg.lookupPixelFeats(view + "", foundFeatures);
			assertEquals(expected.size(), foundFeatures.size);
			foundFeatures.forIdx(( idx, v ) -> assertTrue(v.isIdentical(expected.get(idx), 0.0)));
		}

		// Basic sanity check for similar views
		List<String> foundSimilar = new ArrayList<>();
		alg.findSimilar("2", ( v ) -> true, foundSimilar);
		assertEquals(2, foundSimilar.size());

		alg.findSimilar("3", ( v ) -> true, foundSimilar);
		assertEquals(0, foundSimilar.size());

		// Inspect the relationship between one set of views
		var foundPairsA = new DogArray<>(AssociatedIndex::new);
		var foundPairsB = new DogArray<>(AssociatedIndex::new);

		// do it in both directions
		alg.findSimilar("2", ( v ) -> true, foundSimilar);
		alg.lookupAssociated("1", foundPairsA);
		alg.findSimilar("1", ( v ) -> true, foundSimilar);
		alg.lookupAssociated("2", foundPairsB);

		// Other than the swap they should be identical
		assertEquals(foundPairsA.size, foundPairsB.size);
		for (int i = 0; i < foundPairsB.size; i++) {
			AssociatedIndex a = foundPairsA.get(i);
			AssociatedIndex b = foundPairsB.get(i);

			assertEquals(a.src, b.dst);
			assertEquals(a.dst, b.src);
		}
	}

	private DogArray<AssociatedIndex> randomAssociated( int sizeA, int sizeB, int total ) {
		var list = new DogArray<>(AssociatedIndex::new);
		for (int i = 0; i < total; i++) {
			list.grow().setTo(rand.nextInt(sizeA), rand.nextInt(sizeB));
		}
		return list;
	}
}
