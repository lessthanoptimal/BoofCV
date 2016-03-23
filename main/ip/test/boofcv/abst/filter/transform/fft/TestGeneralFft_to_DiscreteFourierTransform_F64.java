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

package boofcv.abst.filter.transform.fft;

import boofcv.abst.transform.fft.DiscreteFourierTransform;
import boofcv.abst.transform.fft.GeneralFft_to_DiscreteFourierTransform_F64;
import boofcv.struct.image.GrayF64;
import boofcv.struct.image.InterleavedF64;

/**
 * @author Peter Abeles
 */
public class TestGeneralFft_to_DiscreteFourierTransform_F64
		extends GenericTestDiscreteFourierTransform<GrayF64,InterleavedF64> {

	public TestGeneralFft_to_DiscreteFourierTransform_F64() {
		super(false,1e-3);
	}

	@Override
	public DiscreteFourierTransform<GrayF64,InterleavedF64> createAlgorithm() {
		return new GeneralFft_to_DiscreteFourierTransform_F64();
	}

	@Override
	public GrayF64 createImage(int width, int height) {
		return new GrayF64(width,height);
	}

	@Override
	public InterleavedF64 createTransform(int width, int height) {
		return new InterleavedF64(width,height,2);
	}
}
