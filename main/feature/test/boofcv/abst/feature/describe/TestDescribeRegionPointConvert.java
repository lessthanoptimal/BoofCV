/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.describe;

import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.feature.TupleDesc_S8;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestDescribeRegionPointConvert {

	@Test
	public void basic() {
		DummyConvert convert = new DummyConvert();
		DummyDescribe original = new DummyDescribe();

		DescribeRegionPointConvert<GrayF32,TupleDesc_F64,TupleDesc_S8> alg =
				new DescribeRegionPointConvert<>(original, convert);

		TupleDesc_S8 found = alg.createDescription();
		assertTrue(found.value.length==5);

		assertFalse(original.calledImageSet);
		alg.setImage(null);
		assertTrue(original.calledImageSet);


		alg.process(1,2,2,2,found);
		assertEquals(5,found.value[0]);

		assertTrue(alg.requiresOrientation()==original.requiresOrientation());
		assertTrue(alg.requiresRadius()==original.requiresRadius());
		assertTrue(alg.getDescriptionType()==TupleDesc_S8.class);

	}

	private static class DummyConvert implements ConvertTupleDesc<TupleDesc_F64,TupleDesc_S8> {

		@Override
		public TupleDesc_S8 createOutput() {
			return new TupleDesc_S8(5);
		}

		@Override
		public void convert(TupleDesc_F64 input, TupleDesc_S8 output) {
			assertTrue(input.value[0] == 1);
			output.value[0] = 5;
		}

		@Override
		public Class<TupleDesc_S8> getOutputType() {
			return TupleDesc_S8.class;
		}
	}

	private static class DummyDescribe implements DescribeRegionPoint<GrayF32,TupleDesc_F64> {

		public boolean calledImageSet = false;

		@Override
		public void setImage(GrayF32 image) {
			calledImageSet = true;
		}

		@Override
		public TupleDesc_F64 createDescription() {
			return new TupleDesc_F64(5);
		}

		@Override
		public boolean process(double x, double y, double orientation, double radius, TupleDesc_F64 ret) {
			ret.value[0] = 1;
			return true;
		}

		@Override
		public boolean requiresRadius() {
			return false;
		}

		@Override
		public boolean requiresOrientation() {
			return true;
		}

		@Override
		public Class<TupleDesc_F64> getDescriptionType() {
			return TupleDesc_F64.class;
		}

		@Override
		public ImageType getImageType() {return null;}

		@Override
		public double getCanonicalWidth() {
			throw new RuntimeException("Foo");
		}
	}
}
