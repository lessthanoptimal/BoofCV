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

package boofcv.alg.sfm.structure;

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
class TestLookUpSimilarGivenTracks extends BoofStandardJUnit {
	@Test void computeSimilarRelationships() {
		var alg = new LookUpSimilarGivenTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		alg.addFrame(10, 5, createFeatures(10, 20));
		alg.addFrame(10, 5, createFeatures(15, 25));
		alg.addFrame(10, 5, createFeatures(20, 30));
		alg.addFrame(10, 5, createFeatures(26, 50));
		alg.addFrame(10, 5, createFeatures(5, 20));

		alg.computeSimilarRelationships(true, 5);
		assertEquals(1, alg.frames.get(0).similar.size());
		assertEquals(2, alg.frames.get(1).similar.size());
		assertEquals(1, alg.frames.get(2).similar.size());
		assertEquals(0, alg.frames.get(3).similar.size());
		assertEquals(0, alg.frames.get(4).similar.size());

		alg.computeSimilarRelationships(false, 5);
		assertEquals(2, alg.frames.get(0).similar.size());
		assertEquals(3, alg.frames.get(1).similar.size());
		assertEquals(1, alg.frames.get(2).similar.size());
		assertEquals(0, alg.frames.get(3).similar.size());
		assertEquals(2, alg.frames.get(4).similar.size());

		alg.computeSimilarRelationships(true, 4);
		assertEquals(1, alg.frames.get(0).similar.size());
		assertEquals(2, alg.frames.get(1).similar.size());
		assertEquals(2, alg.frames.get(2).similar.size());
		assertEquals(1, alg.frames.get(3).similar.size());
		assertEquals(0, alg.frames.get(4).similar.size());
	}

	static List<PointIndex2D_F64> createFeatures( int id0, int id1 ) {
		List<PointIndex2D_F64> feat = new ArrayList<>();

		for (int i = 0; i < id1 - id0; i++) {
			feat.add(new PointIndex2D_F64(0, 0, i + id0));
		}

		return feat;
	}

	@Test void countCommon() {
		var alg = new LookUpSimilarGivenTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

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
		var alg = new LookUpSimilarGivenTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		try {
			alg.getFrame("0");
			fail("Failed");
		} catch (IllegalArgumentException ignore) {}

		alg.addFrame(10, 5, new ArrayList<>());
		alg.addFrame(10, 5, new ArrayList<>());

		assertEquals(alg.getFrame("0").id, "0");
		assertEquals(alg.getFrame("1").id, "1");

		try {
			alg.getFrame("2");
			fail("Failed");
		} catch (IllegalArgumentException ignore) {}
	}

	@Test void getImageIDs() {
		var alg = new LookUpSimilarGivenTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		assertEquals(0, alg.getImageIDs().size());

		alg.addFrame(10, 5, new ArrayList<>());
		assertEquals(1, alg.getImageIDs().size());
		assertEquals("0", alg.getImageIDs().get(0));
	}

	@Test void findSimilar() {
		var alg = new LookUpSimilarGivenTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		// Add excessive frames which should be ignored
		alg.addFrame(10, 5, new ArrayList<>());
		alg.addFrame(10, 5, new ArrayList<>());
		alg.addFrame(10, 5, new ArrayList<>());
		alg.addFrame(10, 5, new ArrayList<>());

		// Only one frame will have non-empty similar list
		alg.frames.get(1).similar.add(alg.frames.get(0));
		alg.frames.get(1).similar.add(alg.frames.get(3));

		// Create an array and initialize with an element to make sure it's cleared
		var found = new ArrayList<String>();
		found.add("234");
		alg.findSimilar("1", found);
		assertEquals(2, found.size());
		assertTrue(found.contains("0"));
		assertTrue(found.contains("3"));
	}

	@Test void lookupPixelFeats() {
		var alg = new LookUpSimilarGivenTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

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
		var alg = new LookUpSimilarGivenTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		List<PointIndex2D_F64> featsA = new ArrayList<>();
		List<PointIndex2D_F64> featsB = new ArrayList<>();

		featsA.add(new PointIndex2D_F64(1, 2, 2));
		featsA.add(new PointIndex2D_F64(1, 2, 4));
		featsA.add(new PointIndex2D_F64(1, 2, 3));

		featsB.add(new PointIndex2D_F64(1, 2, 3));
		featsB.add(new PointIndex2D_F64(1, 2, 4));

		alg.addFrame(10, 5, featsA);
		alg.addFrame(10, 5, featsB);

		alg.frames.get(0).similar.add(alg.frames.get(1));
		alg.frames.get(1).similar.add(alg.frames.get(0));

		DogArray<AssociatedIndex> found = new DogArray<>(AssociatedIndex::new);
		alg.lookupMatches("1", "0", found);
		assertEquals(2, found.size);
		assertNotNull(found.find(( a ) -> a.src == 0 && a.dst == 2));
		assertNotNull(found.find(( a ) -> a.src == 1 && a.dst == 1));
	}

	@Test void lookupShape() {
		var alg = new LookUpSimilarGivenTracks<PointIndex2D_F64>(a -> a.index, ( a, dst ) -> dst.setTo(a.p));

		alg.addFrame(10, 5, new ArrayList<>());

		ImageDimension found = new ImageDimension();
		alg.lookupShape("0", found);

		assertEquals(10, found.width);
		assertEquals(5, found.height);
	}
}
