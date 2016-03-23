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

package boofcv.alg.filter.kernel;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.BoofDefaults;
import boofcv.struct.convolve.*;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestKernelMath {

	Random rand = new Random(234);

	@Test
	public void scale_F32() {
		Kernel1D_F32 a = FactoryKernel.random1D_F32(11,5,-1,1,rand);
		Kernel1D_F32 expected = new Kernel1D_F32(11);

		float scale = 1.5f;

		for( int i = 0; i < a.width; i++ ) {
			expected.data[i] = a.data[i]*scale;
		}

		KernelMath.scale(a,scale);

		for( int i = 0; i < a.width; i++ ) {
			assertEquals(expected.data[i],a.data[i],1e-4);
		}
	}

	@Test
	public void scale_F64() {
		Kernel1D_F64 a = FactoryKernel.random1D_F64(11,5,-1,1,rand);
		Kernel1D_F64 expected = new Kernel1D_F64(11);

		double scale = 1.5;

		for( int i = 0; i < a.width; i++ ) {
			expected.data[i] = a.data[i]*scale;
		}

		KernelMath.scale(a,scale);

		for( int i = 0; i < a.width; i++ ) {
			assertEquals(expected.data[i],a.data[i],1e-4);
		}
	}

	@Test
	public void divide_1D_F32() {
		Kernel1D_F32 a = FactoryKernel.random1D_F32(11,5,-10f,10f,rand);
		Kernel1D_F32 b = a.copy();

		float value = 2.1f;
		KernelMath.divide(a,value);

		for (int i = 0; i < a.data.length; i++) {
			assertEquals(b.data[i]/value,a.data[i], BoofDefaults.TEST_FLOAT_TOL);
		}
	}

	@Test
	public void divide_1D_F64() {
		Kernel1D_F64 a = FactoryKernel.random1D_F64(11,5,-10f,10f,rand);
		Kernel1D_F64 b = a.copy();

		double value = 2.1;
		KernelMath.divide(a,value);

		for (int i = 0; i < a.data.length; i++) {
			assertEquals(b.data[i]/value,a.data[i],BoofDefaults.TEST_DOUBLE_TOL);
		}
	}

	@Test
	public void divide_2D_F32() {
		Kernel2D_F32 a = FactoryKernel.random2D_F32(11,5,-10f,10f,rand);
		Kernel2D_F32 b = a.copy();

		float value = 2.1f;
		KernelMath.divide(a,value);

		for (int i = 0; i < a.data.length; i++) {
			assertEquals(b.data[i]/value,a.data[i], BoofDefaults.TEST_FLOAT_TOL);
		}
	}

	@Test
	public void divide_2D_F64() {
		Kernel2D_F64 a = FactoryKernel.random2D_F64(11,5,-10f,10f,rand);
		Kernel2D_F64 b = a.copy();

		double value = 2.1;
		KernelMath.divide(a,value);

		for (int i = 0; i < a.data.length; i++) {
			assertEquals(b.data[i]/value,a.data[i],BoofDefaults.TEST_DOUBLE_TOL);
		}
	}

	@Test
	public void fill_F32() {
		Kernel2D_F32 a = FactoryKernel.random2D_F32(5,2, -2, 2, rand);
		KernelMath.fill(a,1);

		for( int i = 0; i < a.width; i++ ) {
			for( int j = 0; j < a.width; j++ ) {
				assertEquals(1,a.get(j,i),1e-4);
			}
		}
	}

	@Test
	public void fill_I32() {
		Kernel2D_I32 a = FactoryKernel.random2D_I32(5,2, -2, 2, rand);
		KernelMath.fill(a,1);

		for( int i = 0; i < a.width; i++ ) {
			for( int j = 0; j < a.width; j++ ) {
				assertEquals(1,a.get(j,i),1e-4);
			}
		}
	}

	@Test
	public void transpose_F32() {
		Kernel2D_F32 a = FactoryKernel.random2D_F32(5,2, -2, 2, rand);
		Kernel2D_F32 b = KernelMath.transpose(a);

		for( int i = 0; i < a.width; i++ ) {
			for( int j = 0; j < a.width; j++ ) {
				assertEquals(a.get(i,j),b.get(j,i),1e-4);
			}
		}
	}

	@Test
	public void transpose_I32() {
		Kernel2D_I32 a = FactoryKernel.random2D_I32(5,2, -2, 2, rand);
		Kernel2D_I32 b = KernelMath.transpose(a);

		for( int i = 0; i < a.width; i++ ) {
			for( int j = 0; j < a.width; j++ ) {
				assertEquals(a.get(i,j),b.get(j,i),1e-4);
			}
		}
	}

	@Test
	public void convolve1D_F32() {
		Kernel1D_F32 k1 = new Kernel1D_F32(new float[]{1,2,3,4,5},5);
		Kernel1D_F32 k2 = new Kernel1D_F32(new float[]{6,7,8},3);

		// computed using conv() in octave
		Kernel1D_F32 expected = new Kernel1D_F32(new float[]{6,19,40,61,82,67,40},7);
		Kernel1D_F32 c = KernelMath.convolve1D_F32(k1,k2);

		for( int i = 0; i < 7; i++ ) {
			assertEquals(expected.data[i],c.data[i],1e-4);
		}

		c = KernelMath.convolve1D_F32(k2,k1);
		for( int i = 0; i < 7; i++ ) {
			assertEquals(expected.data[i],c.data[i],1e-4);
		}
	}

	@Test
	public void convolve2D_1D_F32() {
		Kernel1D_F32 k1 = new Kernel1D_F32(new float[]{4,3,2},3,1);
		Kernel1D_F32 k2 = new Kernel1D_F32(new float[]{9,5,1},3,1);

		Kernel2D_F32 c = KernelMath.convolve2D(k1, k2);
		assertEquals(3,c.width);
		assertEquals(1, c.offset);

		for( int i = 0; i < 3; i++ ) {
			for( int j = 0; j < 3; j++ ) {
				assertEquals(k1.data[i]*k2.data[j],c.get(j,i),1e-4);
			}
		}
	}

	@Test
	public void convolve_1D_F64() {
		Kernel1D_F64 k1 = new Kernel1D_F64(new double[]{4,3,2},3,1);
		Kernel1D_F64 k2 = new Kernel1D_F64(new double[]{9,5,1},3,1);

		Kernel2D_F64 c = KernelMath.convolve2D(k1, k2);
		assertEquals(3,c.width);
		assertEquals(1,c.offset);

		for( int i = 0; i < 3; i++ ) {
			for( int j = 0; j < 3; j++ ) {
				assertEquals(k1.data[i]*k2.data[j],c.get(j,i),1e-8);
			}
		}
	}

	@Test
	public void convolve_1D_I32() {
		Kernel1D_I32 k1 = new Kernel1D_I32(new int[]{4,3,2},3,1);
		Kernel1D_I32 k2 = new Kernel1D_I32(new int[]{9,5,1},3,1);

		Kernel2D_I32 c = KernelMath.convolve2D(k1, k2);
		assertEquals(3,c.width);
		assertEquals(1,c.offset);

		for( int i = 0; i < 3; i++ ) {
			for( int j = 0; j < 3; j++ ) {
				assertEquals(k1.data[i]*k2.data[j],c.get(j,i));
			}
		}
	}

	@Test
	public void convolve2D_F32() {
		Kernel2D_F32 k1 = new Kernel2D_F32(3,new float[]{1,2,3,4,5,6,7,8,9});
		Kernel2D_F32 k2 = new Kernel2D_F32(3,new float[]{2,3,4,5,6,7,8,9,10});

		Kernel2D_F32 c = KernelMath.convolve2D(k1,k2);

		assertEquals(5,c.width);

		// solution computed using octave
		float[] expected = new float[]{
				2,7,16,17,12,
				13,38,77,70,45,
				42,111,210,177,108,
				67,158,275,214,123,
				56,127,214,161, 90};

		for( int i = 0; i < 25; i++ ) {
			assertEquals(expected[i],c.data[i],1e-4);
		}
	}

	@Test
	public void normalizeSumToOne_F32_1D() {
		Kernel1D_F32 kernel = new Kernel1D_F32(3);
		for (int i = 0; i < 3; i++)
			kernel.data[i] = 2.0F;

		KernelMath.normalizeSumToOne(kernel);

		for (int i = 0; i < 3; i++)
			assertEquals(kernel.data[i], 2.0F / 6.0f, 1e-4);
	}

	@Test
	public void normalizeSumToOne_F64_1D() {
		Kernel1D_F64 kernel = new Kernel1D_F64(3);
		for (int i = 0; i < 3; i++)
			kernel.data[i] = 2.0;

		KernelMath.normalizeSumToOne(kernel);

		for (int i = 0; i < 3; i++)
			assertEquals(kernel.data[i], 2.0 / 6.0, 1e-8);
	}

	@Test
	public void normalizeSumToOne_F32_2D() {
		Kernel2D_F32 kernel = new Kernel2D_F32(3);
		for (int i = 0; i < 9; i++)
			kernel.data[i] = 2.0F;

		KernelMath.normalizeSumToOne(kernel);

		float total = 2f*9f;

		for (int i = 0; i < 3; i++)
			assertEquals(2.0F / total,kernel.data[i], 1e-4);
	}

	@Test
	public void normalizeSumToOne_F64_2D() {
		Kernel2D_F64 kernel = new Kernel2D_F64(3);
		for (int i = 0; i < 9; i++)
			kernel.data[i] = 2.0F;

		KernelMath.normalizeSumToOne(kernel);

		double total = 2*9;

		for (int i = 0; i < 9; i++)
			assertEquals(2.0 / total,kernel.data[i], 1e-8);
	}

	@Test
	public void normalizeMaxOne_F64_2D() {
		Kernel2D_F64 kernel = new Kernel2D_F64(3);
		for (int i = 0; i < 9; i++)
			kernel.data[i] = 2.0F*i;

		KernelMath.normalizeMaxOne(kernel);

		int N = kernel.width*kernel.width;

		double max = 0;
		int numZero = 0;
		for (int i = 0; i < N; i++) {
			max = Math.max(max,kernel.data[i]);
			if( kernel.data[i] == 0 )
				numZero++;
		}

		assertEquals(1,numZero);
		assertEquals(1,max,1e-8);
	}

	@Test
	public void convertToImage_F32() {
		Kernel2D_F32 kernel = FactoryKernel.random2D_F32(7,3,-10,10,rand);
		GrayF32 image = KernelMath.convertToImage(kernel);

		assertEquals(kernel.width,image.width);
		assertEquals(kernel.width,image.height);

		for( int i = 0; i < kernel.width; i++ ) {
			for( int j = 0; j < kernel.width; j++ ) {
				assertEquals(kernel.get(j,i),image.get(j,i),1e-4);
			}
		}
	}

	@Test
	public void convertToImage_I32() {
		Kernel2D_I32 kernel = FactoryKernel.random2D_I32(7,3,-10,10,rand);
		GrayS32 image = KernelMath.convertToImage(kernel);

		assertEquals(kernel.width,image.width);
		assertEquals(kernel.width,image.height);

		for( int i = 0; i < kernel.width; i++ ) {
			for( int j = 0; j < kernel.width; j++ ) {
				assertEquals(kernel.get(j,i),image.get(j,i));
			}
		}
	}

	@Test
	public void convertToKernel_F32() {
		GrayF32 image = new GrayF32(7,7);
		ImageMiscOps.fillUniform(image,rand,-10,10);
		Kernel2D_F32 kernel = KernelMath.convertToKernel(image);

		assertEquals(kernel.width,image.width);
		assertEquals(kernel.width,image.height);

		for( int i = 0; i < kernel.width; i++ ) {
			for( int j = 0; j < kernel.width; j++ ) {
				assertEquals(image.get(j,i),kernel.get(j,i),1e-4);
			}
		}
	}

	@Test
	public void convertToKernel_I32() {
		GrayS32 image = new GrayS32(7,7);
		ImageMiscOps.fillUniform(image,rand,-10,10);
		Kernel2D_I32 kernel = KernelMath.convertToKernel(image);

		assertEquals(kernel.width,image.width);
		assertEquals(kernel.width,image.height);

		for( int i = 0; i < kernel.width; i++ ) {
			for( int j = 0; j < kernel.width; j++ ) {
				assertEquals(image.get(j,i),kernel.get(j,i),1e-4);
			}
		}
	}

	@Test
	public void convert_1D_F32_to_I32() {
		Kernel1D_F32 orig = new Kernel1D_F32(new float[]{0.1f,1,1e-8f,-1,-0.1f}, 5, 1);
		Kernel1D_I32 found = KernelMath.convert(orig,1f/60f);

		assertEquals(orig.offset,found.offset);
		assertEquals(orig.width,found.width);
		assertEquals(found.data[0],1);
		assertEquals(found.data[1],10);
		assertEquals(found.data[2],0);
		assertEquals(found.data[3],-10);
		assertEquals(found.data[4],-1);
	}

	@Test
	public void convert_1D_F64_to_I32() {
		Kernel1D_F64 orig = new Kernel1D_F64(new double[]{0.1,1,1e-8,-1,-0.1}, 5, 1);
		Kernel1D_I32 found = KernelMath.convert(orig,1f/60f);

		assertEquals(orig.offset,found.offset);
		assertEquals(orig.width,found.width);
		assertEquals(found.data[0],1);
		assertEquals(found.data[1],10);
		assertEquals(found.data[2],0);
		assertEquals(found.data[3],-10);
		assertEquals(found.data[4],-1);
	}

	@Test
	public void convert_2D_F32_to_I32() {
		Kernel2D_F32 orig = new Kernel2D_F32(3,new float[]{0.1f,1,0.1f,1,1e-6f,-1,-0.1f,-1,-0.1f});
		Kernel2D_I32 found = KernelMath.convert(orig,1f/60f);

		assertEquals(orig.width,found.width);
		assertEquals(found.data[0],1);
		assertEquals(found.data[1],10);
		assertEquals(found.data[2],1);
		assertEquals(found.data[3],10);
		assertEquals(found.data[4],0);
		assertEquals(found.data[5],-10);
		assertEquals(found.data[6],-1);
		assertEquals(found.data[7],-10);
		assertEquals(found.data[8],-1);
	}
}
