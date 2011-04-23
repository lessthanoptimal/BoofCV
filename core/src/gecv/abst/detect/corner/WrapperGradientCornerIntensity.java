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

package gecv.abst.detect.corner;

import gecv.alg.detect.corner.GradientCornerIntensity;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;

/**
 * Wrapper around children of {@link gecv.alg.detect.corner.GradientCornerIntensity}.
 * 
 * @author Peter Abeles
 */
public class WrapperGradientCornerIntensity <I extends ImageBase> implements CornerIntensityGradient<I> {

	GradientCornerIntensity<I> alg;

	public WrapperGradientCornerIntensity(GradientCornerIntensity<I> alg) {
		this.alg = alg;
	}

	@Override
	public void process(I derivX, I derivY) {
		alg.process(derivX,derivY);
	}

	@Override
	public ImageFloat32 getIntensity() {
		return alg.getIntensity();
	}
}
