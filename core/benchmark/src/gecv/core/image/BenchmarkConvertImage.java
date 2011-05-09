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

package gecv.core.image;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;

/**
 * Benchmarks related to functions inside of ConvertImage
 * 
 * @author Peter Abeles
 */
public class BenchmarkConvertImage {
	static int imgWidth = 640;
	static int imgHeight = 480;

	static ImageFloat32 imgFloat32;
	static ImageInt8 imgUInt8;
	static ImageInt16 imgUInt16;
	static ImageInt8 imgSInt8;
	static ImageInt16 imgSInt16;

	public static class Float32toInt8 extends PerformerBase
	{
		@Override
		public void process() {
			ConvertImage.convert(imgFloat32,imgUInt8);
		}
	}

	public static class Int8ToFloat32 extends PerformerBase
	{
		ImageInt8 img;

		public Int8ToFloat32(ImageInt8 img) {
			this.img = img;
		}

		@Override
		public void process() {
			ConvertImage.convert(img,imgFloat32);
		}
	}

	public static class Int16ToFloat32 extends PerformerBase
	{
		ImageInt16 img;

		public Int16ToFloat32(ImageInt16 img) {
			this.img = img;
		}

		@Override
		public void process() {
			ConvertImage.convert(img,imgFloat32);
		}
	}

	public static class Int16ToInt8 extends PerformerBase
	{
		ImageInt16 img;

		public Int16ToInt8(ImageInt16 img) {
			this.img = img;
		}

		@Override
		public void process() {
			ConvertImage.convert(img,imgSInt8);
		}
	}

	public static void main( String args[] ) {
		imgSInt8 = new ImageInt8(imgWidth,imgHeight,true);
		imgSInt16 = new ImageInt16(imgWidth,imgHeight, true);
		imgUInt8 = new ImageInt8(imgWidth,imgHeight,false);
		imgUInt16 = new ImageInt16(imgWidth,imgHeight, false);
		imgFloat32 = new ImageFloat32(imgWidth,imgHeight);

		System.out.println("=========  Profile Image Size "+imgWidth+" x "+imgHeight+" ==========");
		System.out.println();

		System.out.printf("Float32 to Int8              %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new Float32toInt8(),1000));
		System.out.printf("Int8 to Float32 signed       %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new Int8ToFloat32(imgSInt8),1000));
		System.out.printf("Int8 to Float32 unsigned     %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new Int8ToFloat32(imgUInt8),1000));
		System.out.printf("Int16 to Float32 signed       %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new Int16ToFloat32(imgSInt16),1000));
		System.out.printf("Int16 to Float32 unsigned     %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new Int16ToFloat32(imgUInt16),1000));
		System.out.printf("Int16 to Int8 signed          %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new Int16ToInt8(imgSInt16),1000));
		System.out.printf("Int16 to Int8 unsigned        %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new Int16ToInt8(imgUInt16),1000));

	}
}
