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

package boofcv.abst.feature.detdesc;

import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestDetectDescribeSingleToMulti extends BoofStandardJUnit {

	@Test void basic() {
		Helper helper = new Helper();

		DetectDescribeSingleToMulti<GrayF32,TupleDesc_F64> alg =
				new DetectDescribeSingleToMulti<>(helper);

		assertFalse(helper.calledDetect);
		alg.process(null);
		assertTrue(helper.calledDetect);

		assertSame(TupleDesc_F64.class, alg.getDescriptionType());
		assertNotNull(alg.createDescription());

		assertEquals(1,alg.getNumberOfSets());
		assertEquals(16,alg.getFeatureSet(0).getNumberOfFeatures());
		assertNotNull(alg.getFeatureSet(0).getLocation(0));
		assertNotNull(alg.getFeatureSet(0).getDescription(0));
	}

	protected static class Helper extends DetectDescribePointAbstract<GrayF32,TupleDesc_F64> {

		boolean calledDetect = false;

		@Override
		public TupleDesc_F64 createDescription() {
			return new TupleDesc_F64(10);
		}

		@Override
		public TupleDesc_F64 getDescription(int index) {
			return new TupleDesc_F64(10);
		}

		@Override
		public ImageType<GrayF32> getInputType() {
			return ImageType.SB_F32;
		}

		@Override
		public Class<TupleDesc_F64> getDescriptionType() {
			return TupleDesc_F64.class;
		}

		@Override
		public void detect(GrayF32 input) {
			calledDetect = true;
		}

		@Override
		public boolean hasScale() {
			return true;
		}

		@Override
		public boolean hasOrientation() {
			return true;
		}

		@Override
		public int getNumberOfFeatures() {
			return 16;
		}

		@Override
		public Point2D_F64 getLocation(int featureIndex) {
			return new Point2D_F64(1,1);
		}

		@Override
		public double getRadius(int featureIndex) {
			return 1.5;
		}

		@Override
		public double getOrientation(int featureIndex) {
			return -0.5;
		}
	}
}
