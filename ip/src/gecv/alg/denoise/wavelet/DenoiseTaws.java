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

package gecv.alg.denoise.wavelet;

import gecv.alg.denoise.DenoiseWavelet;
import gecv.struct.image.ImageFloat32;


/**
 * <p>
 * Wavelet based image-denoising algorithm
 * </p>
 *
 * <p>
 * J. S. Walker, "Tree-Adapted Wavelet Shrinkage," Advances in Imaging and Electron Physics, 2003
 * </p>
 *
 * @author Peter Abeles
 */
public class DenoiseTaws implements DenoiseWavelet<ImageFloat32> {
	@Override
	public void denoise(ImageFloat32 transform, int numLevels) {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
