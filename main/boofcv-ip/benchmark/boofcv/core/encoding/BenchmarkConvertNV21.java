/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.core.encoding;

import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.*;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkConvertNV21 {

	static int width=640,height=480;

	static byte nv21[] = new byte[width*height*2];

	static GrayU8 grayU8 = new GrayU8(width,height);
	static GrayF32 grayF32 = new GrayF32(width,height);
	static Planar<GrayU8> planarU8 = new Planar<GrayU8>(GrayU8.class,width,height,3);
	static Planar<GrayF32> planarF32 = new Planar<GrayF32>(GrayF32.class,width,height,3);
	static InterleavedU8 interleavedU8 = new InterleavedU8(width,height,3);
	static InterleavedF32 interleavedF32 = new InterleavedF32(width,height,3);

	static {
		Random rand = new Random(234);
		for (int i = 0; i < nv21.length; i++) {
			nv21[i] = (byte)rand.nextInt(256);
		}
	}

	public static class TGrayU8 extends PerformerBase
	{
		@Override
		public void process() {
			ConvertNV21.nv21ToGray(nv21,width,height,grayU8);
		}
	}

	public static class TGrayF32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvertNV21.nv21ToGray(nv21,width,height,grayF32);
		}
	}

	public static class PlanarU8 extends PerformerBase
	{
		@Override
		public void process() {
			ConvertNV21.nv21TPlanarRgb_U8(nv21,width,height,planarU8);
		}
	}

	public static class PlanarF32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvertNV21.nv21ToPlanarRgb_F32(nv21,width,height,planarF32);
		}
	}

	public static class InterU8 extends PerformerBase
	{
		@Override
		public void process() {
			ConvertNV21.nv21ToInterleaved(nv21,width,height,interleavedU8);
		}
	}

	public static class InterF32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvertNV21.nv21ToInterleaved(nv21,width,height,interleavedF32);
		}
	}

	public static void main( String args[] ) {

		System.out.println("=========  Profile Image Size " + width + " x " + height + " ==========");
		System.out.println();

		System.out.printf("nv21 to gray   U8              %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new TGrayU8(), 1000, false));
		System.out.printf("nv21 to gray   F32             %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new TGrayF32(), 1000, false));
		System.out.printf("nv21 to planar U8              %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new PlanarU8(), 1000, false));
		System.out.printf("nv21 to planar F32             %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new PlanarF32(), 1000, false));
		System.out.printf("nv21 to interleaved U8         %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new InterU8(), 1000, false));
		System.out.printf("nv21 to interleaved F32        %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new InterF32(), 1000, false));

	}
}
