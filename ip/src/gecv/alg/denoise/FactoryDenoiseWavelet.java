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

import gecv.alg.denoise.wavelet.DenoiseBayesShrink;
import gecv.alg.denoise.wavelet.DenoiseSureShrink;
import gecv.alg.denoise.wavelet.DenoiseTaws;
import gecv.alg.denoise.wavelet.DenoiseVisuShrink;
import gecv.struct.image.ImageBase;


/**
 * Factory for creating wavelet based image denoising classes.
 *
 *  @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryDenoiseWavelet {

	public <T extends ImageBase<T>> DenoiseWavelet<T> bayes( Class<?> imageType , ShrinkThresholdRule<T> rule ) {
		return (DenoiseWavelet)new DenoiseBayesShrink((ShrinkThresholdRule)rule);
	}

	public <T extends ImageBase<T>> DenoiseWavelet<T> bayes( Class<?> imageType ) {
		return (DenoiseWavelet)new DenoiseBayesShrink();
	}

	public <T extends ImageBase<T>> DenoiseWavelet<T> sure( Class<?> imageType ) {
		return (DenoiseWavelet)new DenoiseSureShrink();
	}

	public <T extends ImageBase<T>> DenoiseWavelet<T> visu( Class<?> imageType ) {
		return (DenoiseWavelet)new DenoiseVisuShrink();
	}

	public <T extends ImageBase<T>> DenoiseWavelet<T> taws( Class<?> imageType ) {
		return (DenoiseWavelet)new DenoiseTaws();
	}
}
