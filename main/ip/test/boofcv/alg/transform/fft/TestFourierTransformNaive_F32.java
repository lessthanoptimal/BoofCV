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

package boofcv.alg.transform.fft;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestFourierTransformNaive_F32 {

	Random rand = new Random(234);

	@Test
	public void forwardsRRI_inverseRIR() {
		float original[] = new float[]{0.5f,2f,-0.34f,5f,6f,2f,10f,10f,10f,0f,-0.4f,-6f};
		float tranImag[] = new float[original.length];
		float tranReal[] = new float[original.length];
		float found[] = new float[original.length];

		FourierTransformNaive_F32.forward(original, tranReal, tranImag, 0, original.length);
		FourierTransformNaive_F32.inverse(tranReal, tranImag, found, 0, original.length);

		for( int i = 0; i < original.length; i++ ) {
			assertEquals(original[i],found[i],1e-4);
		}
	}

	@Test
	public void transform_RIRI_RIRI() {
		float originalR[] = new float[]{0.5f,2f,-0.34f,5f,6f,2f,10f,10f,10f,0f,-0.4f,-6f};
		float originalI[] = new float[]{-0.5f,1.5f,-3f,1.5f,3.5f,-0.6f,-4f,4f,3f,-2f,-3f,2.5f};
		float tranImag[] = new float[originalR.length];
		float tranReal[] = new float[originalR.length];
		float foundR[] = new float[originalR.length];
		float foundI[] = new float[originalR.length];

		FourierTransformNaive_F32.transform(true, originalR, originalI, tranReal, tranImag, 0, originalR.length);
		FourierTransformNaive_F32.transform(false, tranReal, tranImag, foundR, foundI, 0, originalR.length);

		for( int i = 0; i < originalR.length; i++ ) {
			assertEquals(originalR[i],foundR[i],1e-4);
			assertEquals(originalI[i],foundI[i],1e-4);
		}
	}

	@Test
	public void forward_reverse_image() {
		GrayF32 input = new GrayF32(30,40);
		GrayF32 tranR = new GrayF32(30,40);
		GrayF32 tranI = new GrayF32(30,40);
		GrayF32 output = new GrayF32(30,40);

		ImageMiscOps.fillUniform(input,rand,0,100);
		FourierTransformNaive_F32.forward(input, tranR, tranI);
		FourierTransformNaive_F32.inverse(tranR, tranI, output);

		BoofTesting.assertEquals(input,output,1e-3);

	}

}
