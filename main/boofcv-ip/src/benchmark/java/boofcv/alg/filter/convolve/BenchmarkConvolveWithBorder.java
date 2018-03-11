/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.*;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_S32;
import boofcv.struct.image.*;

import java.util.Random;

/**
 * Benchmark for different convolution which renormalize along the image edges.
 *
 * @author Peter Abeles
 */
public class BenchmarkConvolveWithBorder {
	static private int imgWidth = 640;
	static private int imgHeight = 480;
	static private int numBands = 2;

	static private Kernel2D_F32 kernel2D_F32;
	static private Kernel1D_F32 kernelF32;
	static private GrayF32 src_SB_F32;
	static private GrayF32 dst_SB_F32;
	static private Kernel1D_S32 kernelI32;
	static private Kernel2D_S32 kernel2D_I32;
	static private GrayU8 src_SB_U8;
	static private GrayS16 src_SB_S16;
	static private GrayU8 out_I8;
	static private GrayS16 out_SB_I16;
	static private GrayS32 out_I32;

	static private InterleavedF32 src_IL_F32 = new InterleavedF32(imgWidth,imgHeight,numBands);
	static private InterleavedF32 dst_IL_F32 = new InterleavedF32(imgWidth,imgHeight,numBands);

	static private ImageBorder_S32 border_I32 = new ImageBorder1D_S32(BorderIndex1D_Extend.class);
	static private ImageBorder_F32 border_F32 = new ImageBorder1D_F32(BorderIndex1D_Extend.class);

	static private ImageBorder_IL_S32 border_IL_S32 = new ImageBorder1D_IL_S32(BorderIndex1D_Extend.class);
	static private ImageBorder_IL_F32 border_IL_F32 = new ImageBorder1D_IL_F32(BorderIndex1D_Extend.class);

	static private ImageBorder<Planar<GrayU8>> border_PL_U8 = FactoryImageBorder.generic(BorderType.EXTENDED,
			ImageType.pl(numBands,GrayU8.class));
	static private ImageBorder<Planar<GrayF32>> border_PL_F32 = FactoryImageBorder.generic(BorderType.EXTENDED,
			ImageType.pl(numBands,GrayF32.class));


	static private Planar<GrayF32> src_PL_F32 = new Planar<GrayF32>(GrayF32.class, imgWidth, imgHeight, numBands);
	static private Planar<GrayF32> dst_PL_F32 = new Planar<GrayF32>(GrayF32.class, imgWidth, imgHeight, numBands);


	// iterate through different sized kernel radius
//	@Param({"1", "2", "3", "5","10"})

	public static int TEST_TIME = 2000;

	static {
		src_SB_U8 = new GrayU8(imgWidth,imgHeight);
		src_SB_S16 = new GrayS16(imgWidth,imgHeight);
		out_I32 = new GrayS32(imgWidth,imgHeight);
		out_SB_I16 = new GrayS16(imgWidth,imgHeight);
		out_I8 = new GrayU8(imgWidth,imgHeight);
		src_SB_F32 = new GrayF32(imgWidth,imgHeight);
		dst_SB_F32 = new GrayF32(imgWidth,imgHeight);

		Random rand = new Random(234234);
		ImageMiscOps.fillUniform(src_SB_U8,rand, 0, 10);
		ImageMiscOps.fillUniform(src_SB_S16,rand,0,20);
		ImageMiscOps.fillUniform(src_SB_F32,rand,0,20);
		ImageMiscOps.fillUniform(src_IL_F32,rand,0,20);
		GImageMiscOps.fillUniform(src_PL_F32,rand,0,20);
	}

	public static void setUp( int radius ) {
		kernelF32 = FactoryKernelGaussian.gaussian(Kernel1D_F32.class,-1,radius);
		kernelI32 = FactoryKernelGaussian.gaussian(Kernel1D_S32.class,-1,radius);
		kernel2D_F32 = FactoryKernelGaussian.gaussian(Kernel2D_F32.class,-1,radius);
		kernel2D_I32 = FactoryKernelGaussian.gaussian(Kernel2D_S32.class,-1,radius);
	}

	public static class Horizontal_SB_U8 extends PerformerBase {
		@Override
		public void process() {
			ConvolveImage.horizontal(kernelI32, src_SB_U8, out_SB_I16,border_I32);
		}
	}

	public static class Vertical_SB_U8 extends PerformerBase {
		@Override
		public void process() {
			ConvolveImage.vertical(kernelI32, src_SB_U8, out_SB_I16,border_I32);
		}
	}

	public static class Horizontal_SB_U16 extends PerformerBase {
		@Override
		public void process() {
			ConvolveImage.horizontal(kernelI32, src_SB_S16, out_SB_I16,border_I32);
		}
	}

	public static class Vertical_SB_U16 extends PerformerBase {
		@Override
		public void process() {
			ConvolveImage.vertical(kernelI32, src_SB_S16, out_SB_I16,border_I32);
		}
	}

	public static class Horizontal_SB_F32 extends PerformerBase {
		@Override
		public void process() {
			ConvolveImage.horizontal(kernelF32, src_SB_F32, dst_SB_F32,border_F32);
		}
	}

	public static class Convolve2D_SB_F32 extends PerformerBase {
		@Override
		public void process() {
			ConvolveImage.convolve(kernel2D_F32, src_SB_F32, dst_SB_F32,border_F32);
		}
	}

	public static class Horizontal_IL_F32 extends PerformerBase {
		@Override
		public void process() {
			ConvolveImage.horizontal(kernelF32,src_IL_F32,dst_IL_F32,border_IL_F32);
		}
	}

	public static class Vertical_IL_F32 extends PerformerBase {
		@Override
		public void process() {
			ConvolveImage.vertical(kernelF32,src_IL_F32,dst_IL_F32,border_IL_F32);
		}
	}

	public static class Convolve2D_IL_F32 extends PerformerBase {
		@Override
		public void process() {
			ConvolveImage.convolve(kernel2D_F32,src_IL_F32,dst_IL_F32,border_IL_F32);
		}
	}

	public static class Horizontal_PL_F32 extends PerformerBase {
		@Override
		public void process() {
			GConvolveImageOps.horizontal(kernelF32,src_PL_F32,dst_PL_F32,border_PL_F32);
		}
	}

	public static class Convolve2D_PL_F32 extends PerformerBase {
		@Override
		public void process() {
			GConvolveImageOps.convolve(kernel2D_F32,src_PL_F32,dst_PL_F32,border_PL_F32);
		}
	}

	public static void main( String args[] ) {
		System.out.println("=========  Profile Image Size "+ imgWidth +" x "+ imgHeight +" ==========");
		System.out.println("                    num bands "+ numBands);

		for( int radius : new int[]{1,2,5}) {
			System.out.println();
			System.out.println("Radius "+radius);
			setUp(radius);

			ProfileOperation.printOpsPerSec(new Horizontal_SB_U8(), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Vertical_SB_U8(), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Horizontal_SB_U16(), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Vertical_SB_U16(), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Horizontal_SB_F32(), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve2D_SB_F32(), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Horizontal_IL_F32(), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Horizontal_PL_F32(), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Vertical_IL_F32(), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve2D_IL_F32(), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve2D_PL_F32(), TEST_TIME);
		}
	}
}
