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

package boofcv.abst.feature.dense;

import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDescribeImageDense_Convert {
	/**
	 * Checks to see if it converts the input image and that other functions are correctly
	 * implemented
	 */
	@Test
	public void basic() {
		Dummy dummy = new Dummy();

		DescribeImageDense<GrayU8,TupleDesc_F64> alg =
				new DescribeImageDense_Convert(dummy,ImageType.single(GrayU8.class));

		GrayU8 foo = new GrayU8(10,20);
		foo.set(5,2,10);

		alg.process(foo);

		assertTrue(dummy.process);

		assertEquals(5,alg.createDescription().size());
		assertTrue( TupleDesc_F64.class == alg.getDescriptionType() );
		assertTrue( alg.getImageType().getDataType() == ImageDataType.U8 );
		assertTrue( null != alg.getDescriptions() );
		assertTrue( null != alg.getLocations() );

	}

	public class Dummy implements DescribeImageDense<GrayF32,TupleDesc_F64> {

		public boolean process = false;

		@Override
		public TupleDesc_F64 createDescription() { return new TupleDesc_F64(5); }

		@Override
		public Class<TupleDesc_F64> getDescriptionType() { return TupleDesc_F64.class; }

		@Override
		public void process(GrayF32 input) {
			assertEquals(10,input.get(5,2),1e-4f);
			process = true;
		}

		@Override
		public List<TupleDesc_F64> getDescriptions() {
			return new ArrayList<>();
		}

		@Override
		public List<Point2D_I32> getLocations() {
			return new ArrayList<>();
		}

		@Override
		public ImageType<GrayF32> getImageType() {
			return ImageType.single(GrayF32.class);
		}
	}
}
