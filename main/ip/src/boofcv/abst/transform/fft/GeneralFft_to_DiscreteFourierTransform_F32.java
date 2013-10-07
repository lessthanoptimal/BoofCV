/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.transform.fft;

import boofcv.alg.transform.fft.GeneralPurposeFFT_F32_2D;
import boofcv.struct.image.ImageFloat32;

/**
 * Wrapper around {@link GeneralPurposeFFT_F32_2D} which implements {@link DiscreteFourierTransform}
 *
 * @author Peter Abeles
 */
public class GeneralFft_to_DiscreteFourierTransform_F32 implements DiscreteFourierTransform<ImageFloat32>
{
	GeneralPurposeFFT_F32_2D alg;

	@Override
	public void forward(ImageFloat32 image, ImageFloat32 transform) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void inverse(ImageFloat32 transform, ImageFloat32 image) {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
