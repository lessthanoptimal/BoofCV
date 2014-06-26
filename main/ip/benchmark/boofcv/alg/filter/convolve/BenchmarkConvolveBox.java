/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.convolve;

import boofcv.alg.filter.convolve.noborder.ImplConvolveBox;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 * @author Peter Abeles
 */
@SuppressWarnings("UnusedDeclaration")
public class BenchmarkConvolveBox {

	static int width = 640;
	static int height = 480;
	static long TEST_TIME = 1000;
	static Random rand = new Random(234);

	static Kernel1D_I32 kernelI32;
	static Kernel1D_F32 kernelF32;
	static ImageFloat32 input_F32 = new ImageFloat32(width,height);
	static ImageFloat32 out_F32 = new ImageFloat32(width,height);
	static ImageFloat32 storageF32 = new ImageFloat32(width,height);
	static ImageUInt8 input_I8 = new ImageUInt8(width,height);
	static ImageSInt16 input_I16 = new ImageSInt16(width,height);
	static ImageUInt8 out_I8 = new ImageUInt8(width,height);
	static ImageSInt16 out_I16 = new ImageSInt16(width,height);
	static ImageSInt32 out_I32 = new ImageSInt32(width,height);

	// iterate through different sized kernel radius
//	@Param({"1", "2", "3", "5","10"})
	private int radius;

	public BenchmarkConvolveBox() {
		ImageMiscOps.fillUniform(input_I8,rand,0,20);
		ImageMiscOps.fillUniform(input_I16,rand,0,20);
		ImageMiscOps.fillUniform(input_F32,rand,0,20);
	}

	protected void setUp() throws Exception {
		kernelF32 = FactoryKernel.table1D_F32(radius,false);
		kernelI32 = FactoryKernel.table1D_I32(radius);
	}

	public int timeConvolve_Vertical_I8_I16(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageNoBorder.vertical(kernelI32, input_I8,out_I16);
		return 0;
	}

	public int timeConvolve_Vertical_I8_I32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageNoBorder.vertical(kernelI32, input_I8,out_I32);
		return 0;
	}

	public int timeBox_U8_I16_Vertical(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplConvolveBox.vertical(input_I8,out_I16,radius);
		return 0;
	}

	public int timeBox_U8_I32_Vertical(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplConvolveBox.vertical(input_I8,out_I32,radius);
		return 0;
	}

	public int timeBox_S16_I16_Vertical(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplConvolveBox.vertical(input_I16,out_I16,radius);
		return 0;
	}

	public int timeBox_F32_F32_Vertical(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplConvolveBox.vertical(input_F32,out_F32,radius);
		return 0;
	}

	public int timeBoxAlt_F32_F32_Vertical(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveBoxAlt.vertical(input_F32,out_F32,radius,false);
		return 0;
	}

	public int timeBox_U8_I16_Horizontal(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplConvolveBox.horizontal(input_I8, out_I16, radius);
		return 0;
	}

	public int timeBox_U8_I32_Horizontal(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplConvolveBox.horizontal(input_I8,out_I32,radius);
		return 0;
	}

	public int timeBox_S16_I16_Horizontal(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplConvolveBox.horizontal(input_I16,out_I16,radius);
		return 0;
	}

	public int timeBox_F32_F32_Horizontal(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplConvolveBox.horizontal(input_F32,out_F32,radius);
		return 0;
	}

//	public static void main( String args[] ) {
//		Runner.main(BenchmarkConvolveBox.class, args);
//	}
}
