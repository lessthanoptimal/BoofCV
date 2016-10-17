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

package boofcv.abst.sfm.d2;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.Planar;
import georegression.struct.InvertibleTransform;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestMsToGrayMotion2D {

	@Test
	public void basicTests() {
		Dummy child = new Dummy();
		PlToGrayMotion2D alg = new PlToGrayMotion2D(child, GrayF32.class);

		Planar<GrayF32> ms = new Planar<>(GrayF32.class,20,30,3);

		assertTrue(alg.process(ms));
		assertTrue(child.input != null );
		assertEquals(0, child.numReset);
		assertEquals(0, child.numSetToFirst);
		alg.reset();
		assertEquals(1, child.numReset);
		assertEquals(0, child.numSetToFirst);
		alg.setToFirst();
		assertEquals(1, child.numReset);
		assertEquals(1, child.numSetToFirst);
	}


	protected class Dummy implements ImageMotion2D {

		ImageBase input;
		int numReset = 0;
		int numSetToFirst = 0;

		@Override
		public boolean process(ImageBase input) {
			this.input = input;
			return true;
		}

		@Override
		public void reset() {
			numReset++;
		}

		@Override
		public void setToFirst() {
			numSetToFirst++;
		}

		@Override
		public InvertibleTransform getFirstToCurrent() {
			return new Se3_F64();
		}

		@Override
		public Class getTransformType() {
			return Se3_F64.class;
		}
	}
}
