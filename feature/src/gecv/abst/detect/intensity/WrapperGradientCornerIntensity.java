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

package gecv.abst.detect.intensity;

import gecv.alg.feature.detect.intensity.GradientCornerIntensity;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;

/**
 * Wrapper around children of {@link gecv.alg.feature.detect.intensity.GradientCornerIntensity}.
 * 
 * @author Peter Abeles
 */
public class WrapperGradientCornerIntensity<I extends ImageBase,D extends ImageBase> implements GeneralFeatureIntensity<I,D> {

	GradientCornerIntensity<D> alg;

	public WrapperGradientCornerIntensity(GradientCornerIntensity<D> alg) {
		this.alg = alg;
	}

	@Override
	public void process(I image , D derivX, D derivY, D derivXX, D derivYY, D derivXY ) {
		alg.process(derivX,derivY);
	}

	@Override
	public ImageFloat32 getIntensity() {
		return alg.getIntensity();
	}

	@Override
	public QueueCorner getCandidates() {
		return null;
	}

	@Override
	public boolean getRequiresGradient() {
		return true;
	}

	@Override
	public boolean getRequiresHessian() {
		return false;
	}

	@Override
	public boolean hasCandidates() {
		return false;
	}
}
