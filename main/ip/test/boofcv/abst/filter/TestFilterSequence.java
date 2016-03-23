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

package boofcv.abst.filter;

import boofcv.alg.filter.convolve.ConvolveImageNoBorder;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.convolve.FactoryConvolve;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class TestFilterSequence {

	Random rand = new Random(234);
	int kernelWidth = 5;
	int radius = kernelWidth/2;
	int width = 30;
	int height = 40;

	/**
	 * Perform a sequence of convolutions manually.  Should produce the same results
	 */
	@Test
	public void compareToManualSequence() {
		Kernel1D_F32 ker1 = FactoryKernel.random1D_F32(kernelWidth,radius,0,5,rand);
		Kernel1D_F32 ker2 = FactoryKernel.random1D_F32(kernelWidth+2,radius+1,0,5,rand);
		Kernel1D_F32 ker3 = FactoryKernel.random1D_F32(kernelWidth+4,radius+2,0,5,rand);

		GrayF32 input = new GrayF32(width,height);
		ImageMiscOps.fillUniform(input,rand,0,10);
		GrayF32 found = new GrayF32(width,height);
		GrayF32 expected = new GrayF32(width,height);

		FilterImageInterface f1 = FactoryConvolve.convolve(ker1,GrayF32.class,GrayF32.class, BorderType.SKIP, true);
		FilterImageInterface f2 = FactoryConvolve.convolve(ker2,GrayF32.class,GrayF32.class, BorderType.SKIP, true);
		FilterImageInterface f3 = FactoryConvolve.convolve(ker3,GrayF32.class,GrayF32.class, BorderType.SKIP, true);

		FilterSequence sequence = new FilterSequence(f1,f2,f3);
		sequence.process(input,found);
		assertEquals(radius+2,sequence.borderHorizontal);
		assertEquals(radius+2,sequence.borderVertical);

		GrayF32 tmp1 = new GrayF32(width,height);
		GrayF32 tmp2 = new GrayF32(width,height);
		ConvolveImageNoBorder.horizontal(ker1,input,tmp1);
		ConvolveImageNoBorder.horizontal(ker2,tmp1,tmp2);
		ConvolveImageNoBorder.horizontal(ker3,tmp2,expected);

		BoofTesting.assertEquals(expected,found,1e-4f);
	}
}
