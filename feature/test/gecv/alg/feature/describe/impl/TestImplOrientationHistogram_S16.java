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

package gecv.alg.feature.describe.impl;

import gecv.alg.filter.kernel.FactoryKernelGaussian;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestImplOrientationHistogram_S16 {

	int N = 10;
	int r = 3;

	@Test
	public void standardUnweighted() {
		GenericOrientationTests<ImageSInt16> tests = new GenericOrientationTests<ImageSInt16>();

		ImplOrientationHistogram_S16 alg = new ImplOrientationHistogram_S16(N);
		alg.setRadius(r);

		tests.setup(2.0*Math.PI/N, r*2+1 , alg);
		tests.performAll();
	}

	@Test
	public void standardWeighted() {
		GenericOrientationTests<ImageSInt16> tests = new GenericOrientationTests<ImageSInt16>();
		Kernel2D_F32 w = FactoryKernelGaussian.gaussian2D(ImageFloat32.class,-1,r);

		ImplOrientationHistogram_S16 alg = new ImplOrientationHistogram_S16(N);
		alg.setRadius(r);
		alg.setWeights(w);

		tests.setup(2.0*Math.PI/N, r*2+1 ,alg);
		tests.performAll();

		w = new Kernel2D_F32(r*2+1);
		w.set(r,r,1);

		alg.setWeights(w);
		tests.performWeightTests();

	}

	/**
	 * See if the weight is being used by provided a case where different answers will be
	 * created for weighted and unweighted
	 */
	@Test
	public void checkWeightsUsed() {
		GenericOrientationTests<ImageSInt16> tests = new GenericOrientationTests<ImageSInt16>();
		Kernel2D_F32 w = new Kernel2D_F32(r*2+1);
		w.set(r,r,1);

		ImplOrientationHistogram_S16 alg = new ImplOrientationHistogram_S16(N);
		alg.setRadius(r);
		alg.setWeights(w);

		tests.setup(2.0*Math.PI/N, r*2+1 ,alg);
		tests.performWeightTests();
	}

}
