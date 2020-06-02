/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.descriptor;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.associate.AssociateDescriptionSets;
import boofcv.abst.feature.associate.AssociateDescriptionSets2D;
import boofcv.struct.feature.TupleDesc_F64;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestUtilFeature {

	@Test
	void combine() {
		TupleDesc_F64 feature0 = new TupleDesc_F64(64);
		TupleDesc_F64 feature1 = new TupleDesc_F64(32);

		feature0.value[5] = 10;
		feature1.value[3] = 13;

		List<TupleDesc_F64> list = new ArrayList<>();
		list.add(feature0);
		list.add(feature1);

		TupleDesc_F64 combined = UtilFeature.combine(list, null);

		assertEquals(10, combined.getDouble(5), 1e-8);
		assertEquals(13, combined.getDouble(67), 1e-8);
	}

	@Test
	void normalizeL2_F64() {
		TupleDesc_F64 feature = new TupleDesc_F64(64);
		feature.value[5] = 2;
		feature.value[10] = 4;
		UtilFeature.normalizeL2(feature);
		assertEquals(0.44721, feature.value[5], 1e-3);
		assertEquals(0.89443, feature.value[10], 1e-3);
	}

	/**
	 * The descriptor is all zeros.  See if it handles this special case.
	 */
	@Test
	void normalizeL2_zeros_F64() {
		TupleDesc_F64 feature = new TupleDesc_F64(64);
		UtilFeature.normalizeL2(feature);
		for (int i = 0; i < feature.value.length; i++)
			assertEquals(0, feature.value[i], 1e-4);
	}

	@Test
	void normalizeSumOne_F64() {
		TupleDesc_F64 feature = new TupleDesc_F64(64);
		feature.value[5] = 2;
		feature.value[10] = 4;
		UtilFeature.normalizeSumOne(feature);

		double total = 0;
		for (int i = 0; i < feature.size(); i++) {
			total += feature.getDouble(i);
		}
		assertEquals(1, total, 1e-8);
	}

	/**
	 * The descriptor is all zeros.  See if it handles this special case.
	 */
	@Test
	void normalizeSumOne_zeros_F64() {
		TupleDesc_F64 feature = new TupleDesc_F64(64);
		UtilFeature.normalizeSumOne(feature);
		for (int i = 0; i < feature.value.length; i++)
			assertEquals(0, feature.value[i], 1e-4);
	}

	/**
	 * Simple test to see if it broke the features up when adding them to the associator
	 */
	@Test
	void setSource() {
		var associate = new PAssociateDescriptionSets<>(null, TupleDesc_F64.class);

		var descriptors = new FastQueue<>(() -> new TupleDesc_F64(1));
		var sets = new GrowQueue_I32();

		for (int i = 0; i < 5; i++) {
			descriptors.grow();
		}
		sets.add(1);
		sets.add(0);
		sets.add(0);
		sets.add(2);
		sets.add(0);

		associate.initialize(3);
		UtilFeature.setSource(descriptors, sets, associate);

		assertEquals(3, associate.getCountSrc(0));
		assertEquals(1, associate.getCountSrc(1));
		assertEquals(1, associate.getCountSrc(2));
		for (int i = 0; i < 3; i++) {
			assertEquals(0, associate.getCountDst(i));
		}
	}

	@Test
	void setDestination() {
		var associate = new PAssociateDescriptionSets<>(null, TupleDesc_F64.class);

		var descriptors = new FastQueue<>(() -> new TupleDesc_F64(1));
		var sets = new GrowQueue_I32();

		for (int i = 0; i < 5; i++) {
			descriptors.grow();
		}
		sets.add(1);
		sets.add(0);
		sets.add(0);
		sets.add(2);
		sets.add(0);

		associate.initialize(3);
		UtilFeature.setDestination(descriptors, sets, associate);

		assertEquals(3, associate.getCountDst(0));
		assertEquals(1, associate.getCountDst(1));
		assertEquals(1, associate.getCountDst(2));
		for (int i = 0; i < 3; i++) {
			assertEquals(0, associate.getCountSrc(i));
		}
	}

	@Test
	void setSource_2D() {
		var associate = new PAssociateDescriptionSets2D<>(null, TupleDesc_F64.class);

		var descriptors = new FastQueue<>(() -> new TupleDesc_F64(1));
		var locs = new FastQueue<>(Point2D_F64::new);
		var sets = new GrowQueue_I32();

		for (int i = 0; i < 5; i++) {
			descriptors.grow();
			locs.grow();
		}
		sets.add(1);
		sets.add(0);
		sets.add(0);
		sets.add(2);
		sets.add(0);

		associate.initialize(3);
		UtilFeature.setSource(descriptors, sets, locs, associate);

		assertEquals(3, associate.getCountSrc(0));
		assertEquals(1, associate.getCountSrc(1));
		assertEquals(1, associate.getCountSrc(2));
		for (int i = 0; i < 3; i++) {
			assertEquals(0, associate.getCountDst(i));
		}
	}

	@Test
	void setDestination_2D() {
		var associate = new PAssociateDescriptionSets2D<>(null, TupleDesc_F64.class);

		var descriptors = new FastQueue<>(() -> new TupleDesc_F64(1));
		var locs = new FastQueue<>(Point2D_F64::new);
		var sets = new GrowQueue_I32();

		for (int i = 0; i < 5; i++) {
			descriptors.grow();
			locs.grow();
		}
		sets.add(1);
		sets.add(0);
		sets.add(0);
		sets.add(2);
		sets.add(0);

		associate.initialize(3);
		UtilFeature.setDestination(descriptors, sets, locs, associate);

		assertEquals(3, associate.getCountDst(0));
		assertEquals(1, associate.getCountDst(1));
		assertEquals(1, associate.getCountDst(2));
		for (int i = 0; i < 3; i++) {
			assertEquals(0, associate.getCountSrc(i));
		}
	}

	static class PAssociateDescriptionSets<Desc> extends AssociateDescriptionSets<Desc> {
		public PAssociateDescriptionSets(AssociateDescription<Desc> associator, Class<Desc> type) {
			super(associator, type);
		}

		public int getCountSrc(int set) {
			return sets.get(set).src.size;
		}

		public int getCountDst(int set) {
			return sets.get(set).dst.size;
		}
	}

	static class PAssociateDescriptionSets2D<Desc> extends AssociateDescriptionSets2D<Desc> {
		public PAssociateDescriptionSets2D(AssociateDescription2D<Desc> associator, Class<Desc> type) {
			super(associator, type);
		}

		public int getCountSrc(int set) {
			return sets.get(set).src.size;
		}

		public int getCountDst(int set) {
			return sets.get(set).dst.size;
		}
	}
}