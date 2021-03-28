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
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.ImageDimension;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestSimilarImagesFromTracks extends BoofStandardJUnit {
	@Test void computeSimilarRelationships() {
		var alg = new SimilarImagesFromTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		alg.addFrame(10, 5, createFeatures(10, 20));
		alg.addFrame(10, 5, createFeatures(15, 25));
		alg.addFrame(10, 5, createFeatures(20, 30));
		alg.addFrame(10, 5, createFeatures(26, 50));
		alg.addFrame(10, 5, createFeatures(5, 20));

		alg.computeSimilarRelationships(false, 5, -1);
		assertEquals(1, alg.frames.get(0).similar.size());
		assertEquals(2, alg.frames.get(1).similar.size());
		assertEquals(1, alg.frames.get(2).similar.size());
		assertEquals(0, alg.frames.get(3).similar.size());
		assertEquals(0, alg.frames.get(4).similar.size());

		alg.computeSimilarRelationships(true, 5, -1);
		assertEquals(2, alg.frames.get(0).similar.size());
		assertEquals(3, alg.frames.get(1).similar.size());
		assertEquals(1, alg.frames.get(2).similar.size());
		assertEquals(0, alg.frames.get(3).similar.size());
		assertEquals(2, alg.frames.get(4).similar.size());

		alg.computeSimilarRelationships(false, 4, -1);
		assertEquals(1, alg.frames.get(0).similar.size());
		assertEquals(2, alg.frames.get(1).similar.size());
		assertEquals(2, alg.frames.get(2).similar.size());
		assertEquals(1, alg.frames.get(3).similar.size());
		assertEquals(0, alg.frames.get(4).similar.size());
	}

	/**
	 * Checks to see if the maxSimilar parameter is correctly enforced
	 */
	@Test void computeSimilarRelationships_MaxSimilar() {
		var alg = new SimilarImagesFromTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		alg.addFrame(10, 5, createFeatures(10, 40));
		alg.addFrame(10, 5, createFeatures(15, 40));
		alg.addFrame(10, 5, createFeatures(30, 40));
		alg.addFrame(10, 5, createFeatures(30, 45));
		alg.addFrame(10, 5, createFeatures(30, 45));

		// Only one connection is allowed
		alg.computeSimilarRelationships(true, 0, 1);
		assertEquals(1, alg.frames.get(0).similar.size());
		assertEquals(1, alg.frames.get(1).similar.size());
		assertEquals(0, alg.frames.get(2).similar.size());
		assertEquals(1, alg.frames.get(3).similar.size());
		assertEquals(1, alg.frames.get(4).similar.size());
	}

	/**
	 * See if the situation where two equally good candidates have been found
	 */
	@Test void computeSimilarRelationships_MaxSimilar_SameCount() {
		var alg = new SimilarImagesFromTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		alg.addFrame(10, 5, createFeatures(10, 40));
		alg.addFrame(10, 5, createFeatures(10, 40));
		alg.addFrame(10, 5, createFeatures(10, 40));
		alg.addFrame(10, 5, createFeatures(10, 45));
		alg.addFrame(10, 5, createFeatures(10, 45));

		// Only one connection is allowed
		alg.computeSimilarRelationships(true, 0, 1);
		assertEquals(1, alg.frames.get(0).similar.size());
		assertEquals(1, alg.frames.get(1).similar.size());
		assertEquals(0, alg.frames.get(2).similar.size());
		assertEquals(1, alg.frames.get(3).similar.size());
		assertEquals(1, alg.frames.get(4).similar.size());

		assertSame(alg.frames.get(0).similar.get(0).dst, alg.frames.get(1));
		assertSame(alg.frames.get(1).similar.get(0).dst, alg.frames.get(0));
		assertSame(alg.frames.get(3).similar.get(0).dst, alg.frames.get(4));
		assertSame(alg.frames.get(4).similar.get(0).dst, alg.frames.get(3));
	}

	static List<PointIndex2D_F64> createFeatures( int id0, int id1 ) {
		List<PointIndex2D_F64> feat = new ArrayList<>();

		for (int i = 0; i < id1 - id0; i++) {
			feat.add(new PointIndex2D_F64(0, 0, i + id0));
		}

		return feat;
	}

	@Test void countCommon() {
		var alg = new SimilarImagesFromTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		alg.addFrame(10, 5, new ArrayList<>());
		alg.addFrame(10, 5, new ArrayList<>());
		alg.addFrame(10, 5, new ArrayList<>());

		// nothing has been added yet
		assertEquals(0, alg.countCommon(alg.frames.get(2), alg.frames.get(1)));

		List<PointIndex2D_F64> featsA = new ArrayList<>();
		List<PointIndex2D_F64> featsB = new ArrayList<>();

		featsA.add(new PointIndex2D_F64(1, 2, 2));
		featsA.add(new PointIndex2D_F64(1, 2, 4));
		featsA.add(new PointIndex2D_F64(1, 2, 3));

		featsB.add(new PointIndex2D_F64(1, 2, 3));
		featsB.add(new PointIndex2D_F64(1, 2, 4));

		alg.addFrame(10, 5, featsA);
		alg.addFrame(10, 5, featsB);

		assertEquals(2, alg.countCommon(alg.frames.get(3), alg.frames.get(4)));
	}

	@Test void getFrame() {
		var alg = new SimilarImagesFromTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		try {
			alg.getFrame("0");
			fail("Failed");
		} catch (IllegalArgumentException ignore) {
		}

		alg.addFrame(10, 5, new ArrayList<>());
		alg.addFrame(10, 5, new ArrayList<>());

		assertEquals(alg.getFrame("0").id, "0");
		assertEquals(alg.getFrame("1").id, "1");

		try {
			alg.getFrame("2");
			fail("Failed");
		} catch (IllegalArgumentException ignore) {
		}
	}

	@Test void getImageIDs() {
		var alg = new SimilarImagesFromTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		assertEquals(0, alg.getImageIDs().size());

		alg.addFrame(10, 5, new ArrayList<>());
		assertEquals(1, alg.getImageIDs().size());
		assertEquals("0", alg.getImageIDs().get(0));
	}

	@Test void findSimilar() {
		var alg = new SimilarImagesFromTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		// Add excessive frames which should be ignored
		alg.addFrame(10, 5, new ArrayList<>());
		alg.addFrame(10, 5, new ArrayList<>());
		alg.addFrame(10, 5, new ArrayList<>());
		alg.addFrame(10, 5, new ArrayList<>());

		// Only one frame will have non-empty similar list
		alg.frames.get(1).similar.grow().setTo(alg.frames.get(0), 0);
		alg.frames.get(1).similar.grow().setTo(alg.frames.get(3), 0);

		// Create an array and initialize with an element to make sure it's cleared
		var found = new ArrayList<String>();
		found.add("234");
		alg.findSimilar("1", found);
		assertEquals(2, found.size());
		assertTrue(found.contains("0"));
		assertTrue(found.contains("3"));
	}

	@Test void lookupPixelFeats() {
		var alg = new SimilarImagesFromTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		List<PointIndex2D_F64> featsA = new ArrayList<>();

		featsA.add(new PointIndex2D_F64(1, 2, 2));
		featsA.add(new PointIndex2D_F64(2, 3, 4));
		featsA.add(new PointIndex2D_F64(3, 4, 3));

		alg.addFrame(10, 5, featsA);

		var found = new DogArray<>(Point2D_F64::new);
		alg.lookupPixelFeats("0", found);
		assertEquals(3, found.size);
		assertNotNull(found.find(a -> a.distance2(1, 2) == 0));
		assertNotNull(found.find(a -> a.distance2(2, 3) == 0));
		assertNotNull(found.find(a -> a.distance2(3, 4) == 0));
	}

	@Test void lookupMatches() {
		var alg = new SimilarImagesFromTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		List<PointIndex2D_F64> featsA = new ArrayList<>();
		List<PointIndex2D_F64> featsB = new ArrayList<>();

		featsA.add(new PointIndex2D_F64(1, 2, 2));
		featsA.add(new PointIndex2D_F64(1, 2, 4));
		featsA.add(new PointIndex2D_F64(1, 2, 3));

		featsB.add(new PointIndex2D_F64(1, 2, 3));
		featsB.add(new PointIndex2D_F64(1, 2, 4));

		alg.addFrame(10, 5, featsA);
		alg.addFrame(10, 5, featsB);

		alg.frames.get(0).similar.grow().setTo(alg.frames.get(1), 0);
		alg.frames.get(1).similar.grow().setTo(alg.frames.get(0), 0);

		DogArray<AssociatedIndex> found = new DogArray<>(AssociatedIndex::new);
		alg.lookupMatches("1", "0", found);
		assertEquals(2, found.size);
		assertNotNull(found.find(( a ) -> a.src == 0 && a.dst == 2));
		assertNotNull(found.find(( a ) -> a.src == 1 && a.dst == 1));
	}

	@Test void lookupShape() {
		var alg = new SimilarImagesFromTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		alg.addFrame(10, 5, new ArrayList<>());

		ImageDimension found = new ImageDimension();
		alg.lookupShape("0", found);

		assertEquals(10, found.width);
		assertEquals(5, found.height);
	}
}
