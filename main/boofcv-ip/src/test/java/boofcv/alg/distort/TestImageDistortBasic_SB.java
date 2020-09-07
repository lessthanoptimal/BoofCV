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

package boofcv.alg.distort;

import boofcv.alg.interpolate.BilinearPixelS;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestImageDistortBasic_SB extends CommonImageDistort_SB {

	@Override
	protected ImageDistortHelper createAlg( BilinearPixelS<GrayF32> interp ) {
		return new Helper(interp);
	}

	private static class Helper extends ImageDistortBasic_SB implements ImageDistortHelper {

		int total = 0;

		public Helper( InterpolatePixelS interp ) {
			super(null, interp);
			this.assigner = new AssignPixelValue_SB.F32() {
				@Override
				public void assign( int indexDst, float value ) {
					total++;
					int x = (indexDst - dstImg.startIndex)%dstImg.stride;
					int y = (indexDst - dstImg.startIndex)/dstImg.stride;
					assertTrue(dstImg.isInBounds(x, y));
					GeneralizedImageOps.set((ImageGray)dstImg, x, y, value);
				}
			};
		}

		@Override
		public void reset() {
			total = 0;
		}

		@Override
		public int getTotal() {
			return total;
		}
	}
}
