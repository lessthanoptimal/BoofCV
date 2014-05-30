/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation.ms;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSegmentMeanShiftSearch {

	@Test
	public void constructor() {
		SegmentMeanShiftSearch alg = new Dummy(1,2,2,3,100);

		assertEquals(5,alg.widthX);
		assertEquals(7,alg.widthY);
	}

	@Test
	public void distanceSq() {
		float a[] = new float[]{2,3,4};
		float b[] = new float[]{4,3,2};

		float found = SegmentMeanShiftSearch.distanceSq(a,b);

		assertEquals(8,found,1e-4);
	}

	@Test
	public void weight() {
		SegmentMeanShiftSearch alg = new Dummy(1,2,2,3,100);

		for( int i = 0; i < 1000; i++ ) {
			float val = i/999f;
			float expected = (float)Math.exp(-val);
			float found = alg.weight(val);

			assertEquals(expected,found,1e-2);
		}
	}

	public static class Dummy extends SegmentMeanShiftSearch {

		public Dummy(int maxIterations, float convergenceTol, int radiusX , int radiusY , int radiusColor ) {
			super(maxIterations, convergenceTol,radiusX,radiusY,radiusColor,false);
		}

		@Override
		public void process(ImageBase image) {}

		@Override
		public ImageType getImageType() {
			return null;
		}
	}

}
