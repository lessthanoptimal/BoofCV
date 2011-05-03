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

package gecv.alg.pyramid;

import gecv.alg.filter.blur.BlurImageOps;
import gecv.alg.filter.convolve.KernelFactory;
import gecv.core.image.UtilImageInt8;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestConvolutionPyramid_I8 {

	Random rand = new Random(234);
	int width = 80;
	int height = 160;

	/**
	 * Make sure this flag is handled correctly on update
	 */
	@Test
	public void saveOriginalReference() {
		ImageInt8 img = new ImageInt8(width,height);
		UtilImageInt8.randomize(img,rand);

		Kernel1D_I32 kernel = KernelFactory.gaussian1D_I32(3);

		ConvolutionPyramid_I8 alg = new ConvolutionPyramid_I8(width,height,true,kernel);
		alg.setScaling(1,2,2);
		alg.update(img);

		assertTrue(img==alg.getLayer(0));

		alg = new ConvolutionPyramid_I8(width,height,false,kernel);
		alg.setScaling(1,2,2);
		alg.update(img);

		assertTrue(img!=alg.getLayer(0));

		alg = new ConvolutionPyramid_I8(width,height,true,kernel);
		alg.setScaling(2,2);
		alg.update(img);

		assertTrue(img!=alg.getLayer(0));
	}

	/**
	 * Compares update to a convolution and sub-sampling of upper layers.
	 */
	@Test
	public void _update() {
		ImageInt8 img = new ImageInt8(width,height);
		UtilImageInt8.randomize(img,rand);

		GecvTesting.checkSubImage(this,"_update",true, img );
	}

	public void _update(ImageInt8 img) {
		Kernel1D_I32 kernel = KernelFactory.gaussian1D_I32(3);
		ImageInt8 convImg = new ImageInt8(width,height);

		BlurImageOps.kernel(img,convImg,kernel,new ImageInt8(width,height));

		ConvolutionPyramid_I8 alg = new ConvolutionPyramid_I8(width,height,false,kernel);
		alg.setScaling(1,2,2);
		alg.update(img);

		// top layer should be the same as the input layer
		GecvTesting.assertEquals(img,alg.getLayer(0),1);

		for( int i = 0; i < height; i += 2 ) {
			for( int j = 0; j < width; j += 2 ) {
				int a = convImg.get(j,i);
				int b = alg.getLayer(1).get(j/2,i/2);

				assertEquals(a,b);
			}
		}
	}
}
