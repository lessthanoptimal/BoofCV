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
import boofcv.struct.image.GrayF64;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestFourierTransformNaive_F64 {

	Random rand = new Random(234);

	@Test
	public void forwardsRRI_inverseRIR() {
		double original[] = new double[]{0.5,2,-0.34,5,6,2,10,10,10,0f,-0.4,-6};
		double tranImag[] = new double[original.length];
		double tranReal[] = new double[original.length];
		double found[] = new double[original.length];

		FourierTransformNaive_F64.forward(original, tranReal, tranImag, 0, original.length);
		FourierTransformNaive_F64.inverse(tranReal, tranImag, found, 0, original.length);

		for( int i = 0; i < original.length; i++ ) {
			assertEquals(original[i],found[i],1e-8);
		}
	}

	@Test
	public void transform_RIRI_RIRI() {
		double originalR[] = new double[]{0.5,2,-0.34,5,6,2,10,10,10,0,-0.4,-6};
		double originalI[] = new double[]{-0.5,1.5,-3,1.5,3.5,-0.6,-4,4,3,-2,-3,2.5};
		double tranImag[] = new double[originalR.length];
		double tranReal[] = new double[originalR.length];
		double foundR[] = new double[originalR.length];
		double foundI[] = new double[originalR.length];

		FourierTransformNaive_F64.transform(true, originalR, originalI, tranReal, tranImag, 0, originalR.length);
		FourierTransformNaive_F64.transform(false, tranReal, tranImag, foundR, foundI, 0, originalR.length);

		for( int i = 0; i < originalR.length; i++ ) {
			assertEquals(originalR[i],foundR[i],1e-8);
			assertEquals(originalI[i],foundI[i],1e-8);
		}
	}

	@Test
	public void forward_reverse_image() {
		GrayF64 input = new GrayF64(30,40);
		GrayF64 tranR = new GrayF64(30,40);
		GrayF64 tranI = new GrayF64(30,40);
		GrayF64 output = new GrayF64(30,40);

		ImageMiscOps.fillUniform(input,rand,0,100);
		FourierTransformNaive_F64.forward(input, tranR, tranI);
		FourierTransformNaive_F64.inverse(tranR, tranI, output);

		BoofTesting.assertEquals(input,output,1e-7);

	}

}
