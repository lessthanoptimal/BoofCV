/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.denoise;

import gecv.alg.denoise.wavelet.DenoiseBayesShrink_F32;
import gecv.alg.denoise.wavelet.DenoiseSureShrink_F32;
import gecv.alg.denoise.wavelet.DenoiseVisuShrink_F32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt32;


/**
 * Factory for creating wavelet based image denoising classes.
 *
 *  @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryDenoiseWavelet {

	public static DenoiseWavelet<ImageFloat32> bayes_F32( ShrinkThresholdRule<ImageFloat32> rule ) {
		return new DenoiseBayesShrink_F32(rule);
	}

	public static DenoiseWavelet<ImageSInt32> bayes_I32( ShrinkThresholdRule<ImageSInt32> rule ) {
		return null;
	}

	public static DenoiseWavelet<ImageFloat32> bayes_F32() {
		return new DenoiseBayesShrink_F32();
	}

	public static DenoiseWavelet<ImageSInt32> bayes_I32() {
		return null;
	}

	public static DenoiseWavelet<ImageFloat32> sure_F32() {
		return new DenoiseSureShrink_F32();
	}

	public static DenoiseWavelet<ImageSInt32> sure_I32() {
		return null;
	}

	public static DenoiseWavelet<ImageFloat32> visu_F32() {
		return new DenoiseVisuShrink_F32();
	}

	public static DenoiseWavelet<ImageSInt32> visu_I32() {
		return null;
	}
}
