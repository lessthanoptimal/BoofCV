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

package gecv.alg.wavelet;

import gecv.alg.misc.ImageTestingOps;
import gecv.struct.image.ImageDimension;
import gecv.struct.image.ImageFloat32;
import gecv.struct.wavelet.WaveletDescription;
import gecv.struct.wavelet.WlCoef_F32;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class TestWaveletTransformOps {

	Random rand = new Random(234);
	int width = 250;
	int height = 300;

	/**
	 * Performs a forward and reverse transform and sees if it gets the original image back
	 */
	@Test
	public void singleLevel() {
		// try different sized images
		for( int adjust = 0; adjust < 5; adjust++ ) {
			ImageFloat32 input = new ImageFloat32(width+adjust,height+adjust);
			ImageFloat32 output = new ImageFloat32(input.width+(input.width%2),input.height+(input.height%2));
			ImageFloat32 found = new ImageFloat32(input.width,input.height);

			ImageTestingOps.randomize(input,rand,0,50);
			WaveletDescription<WlCoef_F32> desc = FactoryWaveletDaub.daubJ_F32(4);

			WaveletTransformOps.transform1(desc,input,output,null);
			WaveletTransformOps.inverse1(desc,output,found,null);

			GecvTesting.assertEquals(input,found,0,1e-4f);
		}
	}

	@Test
	public void multipleLevel() {
		// try different sized images
		for( int adjust = 0; adjust < 5; adjust++ ) {
			int w = width+adjust;
			int h = height+adjust;
			ImageFloat32 input = new ImageFloat32(w,h);
			ImageFloat32 found = new ImageFloat32(w,h);

			ImageTestingOps.randomize(input,rand,0,50);
			WaveletDescription<WlCoef_F32> desc = FactoryWaveletDaub.daubJ_F32(4);

			for( int level = 1; level <= 5; level++ ) {
				ImageDimension dim = UtilWavelet.transformDimension(w,h,level);
				ImageFloat32 output = new ImageFloat32(dim.width,dim.height);
//				System.out.println("adjust "+adjust+" level "+level+" scale "+ div);
				WaveletTransformOps.transformN(desc,input.clone(),output,null,level);
				WaveletTransformOps.inverseN(desc,output,found,null,level);

				GecvTesting.assertEquals(input,found,0,1e-4f);
			}
		}
	}
}
