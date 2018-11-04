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

package boofcv.core.image;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.InterleavedU8;

import java.util.Random;

/**
 * Benchmarks related to functions inside of ConvertImage
 * 
 * @author Peter Abeles
 */
public class BenchmarkConvertImage {
	static int imgWidth = 4000;
	static int imgHeight = 2000;

	static GrayF32 imgFloat32;
	static GrayU8 imgUInt8;
	static GrayS16 imgUInt16;
	static GrayU8 imgSInt8;
	static GrayS16 imgSInt16;
	static InterleavedU8 interU8;

	public static class IU8_to_U8 extends PerformerBase
	{
		@Override
		public void process() {
			ConvertImage.average(interU8,imgUInt8);
		}
	}

	public static class F32_to_U8 extends PerformerBase
	{
		@Override
		public void process() {
			ConvertImage.convert(imgFloat32,imgUInt8);
		}
	}

	public static class U8_to_F32 extends PerformerBase
	{
		GrayU8 img;

		public U8_to_F32(GrayU8 img) {
			this.img = img;
		}

		@Override
		public void process() {
			ConvertImage.convert(img,imgFloat32);
		}
	}

	public static class S16_to_F32 extends PerformerBase
	{
		GrayS16 img;

		public S16_to_F32(GrayS16 img) {
			this.img = img;
		}

		@Override
		public void process() {
			ConvertImage.convert(img,imgFloat32);
		}
	}

	public static class S16_to_S8 extends PerformerBase
	{
		GrayS16 img;

		public S16_to_S8(GrayS16 img) {
			this.img = img;
		}

		@Override
		public void process() {
			ConvertImage.convert(img,imgSInt8);
		}
	}

	public static void main( String args[] ) {
		Random rand = new Random(234);

		imgSInt8 = new GrayU8(imgWidth,imgHeight);
		imgSInt16 = new GrayS16(imgWidth,imgHeight);
		imgUInt8 = new GrayU8(imgWidth,imgHeight);
		imgUInt16 = new GrayS16(imgWidth,imgHeight);
		imgFloat32 = new GrayF32(imgWidth,imgHeight);
		interU8 = new InterleavedU8(imgWidth,imgHeight,3);

		// randomize images so that they aren't all zeros, which can skew results
		GImageMiscOps.fillUniform(imgSInt8,rand,0,10);
		GImageMiscOps.fillUniform(imgSInt16,rand,0,10);
		GImageMiscOps.fillUniform(imgUInt8,rand,0,10);
		GImageMiscOps.fillUniform(imgUInt16,rand,0,10);
		GImageMiscOps.fillUniform(imgFloat32,rand,0,10);
		GImageMiscOps.fillUniform(interU8,rand,0,10);

		System.out.println("=========  Profile Image Size "+imgWidth+" x "+imgHeight+" ==========");
		System.out.println();

		System.out.printf("InterleavedU8 to GrayU8      %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new IU8_to_U8(),1000, false));
		System.out.printf("Float32 to Int8              %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new F32_to_U8(),1000, false));
		System.out.printf("Int8 to Float32 signed       %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new U8_to_F32(imgSInt8),1000, false));
		System.out.printf("Int8 to Float32 unsigned     %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new U8_to_F32(imgUInt8),1000, false));
		System.out.printf("Int16 to Float32 signed       %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new S16_to_F32(imgSInt16),1000, false));
		System.out.printf("Int16 to Float32 unsigned     %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new S16_to_F32(imgUInt16),1000, false));
		System.out.printf("Int16 to Int8 signed          %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new S16_to_S8(imgSInt16),1000, false));
		System.out.printf("Int16 to Int8 unsigned        %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new S16_to_S8(imgUInt16),1000, false));

	}
}
