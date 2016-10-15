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

package boofcv.deepboof;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.PixelMath;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofTesting;
import deepboof.tensors.Tensor_F32;
import georegression.misc.GrlConstants;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestDataManipulationOps {
	Random rand = new Random(234);

	@Test
	public void normalize() {
		GrayF32 original = new GrayF32(30,20);
		GImageMiscOps.fillUniform(original,rand,-1,1);
		GrayF32 expected = original.createSameShape();
		GrayF32 found = original.clone();

		float mean = 4.5f;
		float stdev = 1.2f;

		PixelMath.minus(original,mean,expected);
		PixelMath.divide(expected,stdev,expected);

		DataManipulationOps.normalize(found,mean,stdev);

		BoofTesting.assertEquals(expected,found, GrlConstants.DOUBLE_TEST_TOL);
	}

	@Test
	public void create1D_F32() {
		double array[] = new double[]{1,2,3,4,5,6};

		Kernel1D_F32 kernel = DataManipulationOps.create1D_F32(array);

		assertEquals(array.length,kernel.getWidth());
		assertEquals(kernel.getOffset(),array.length/2);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], kernel.get(i), 1e-4);
		}
	}

	@Test
	public void imageToTensor() {
		Planar<GrayF32> image = new Planar<>(GrayF32.class,30,25,2);
		GImageMiscOps.fillUniform(image,rand,-2,2);

		Tensor_F32 tensor = new Tensor_F32(1,2,25,30);
		DataManipulationOps.imageToTensor(image,tensor,0);

		for (int band = 0; band < image.bands.length; band++) {
			for (int i = 0; i < image.height; i++) {
				for (int j = 0; j < image.width; j++) {
					assertEquals(image.getBand(band).get(j,i),tensor.get(0,band,i,j),1e-4f);
				}
			}
		}
	}

	@Test
	public void imageToTensor_fail() {
		Planar<GrayF32> image = new Planar<>(GrayF32.class,30,25,2);
		try {
			DataManipulationOps.imageToTensor(image,new Tensor_F32(2,25,30),0);
			fail("expected exception");
		} catch( RuntimeException ignore){}

		try {
			DataManipulationOps.imageToTensor(image,new Tensor_F32(0,3,25,30),0);
			fail("expected exception");
		} catch( RuntimeException ignore){}

		try {
			DataManipulationOps.imageToTensor(image,new Tensor_F32(1,2,26,30),0);
			fail("expected exception");
		} catch( RuntimeException ignore){}

		try {
			DataManipulationOps.imageToTensor(image,new Tensor_F32(1,2,25,31),0);
			fail("expected exception");
		} catch( RuntimeException ignore){}
	}
}
