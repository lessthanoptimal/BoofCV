/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.PerformerBase;
import boofcv.ProfileOperation;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Benchmarks related to converting to and from BufferedImage.
 * 
 * @author Peter Abeles
 */
public class BenchmarkConvertBufferedImage {
	static Random rand = new Random(342543);

	static int imgWidth = 640;
	static int imgHeight = 480;

	static BufferedImage imgBuff;
	static ImageUInt8 imgInt8;

	public static class FromBuffToInt8 extends PerformerBase
	{
		@Override
		public void process() {
			ConvertBufferedImage.convertFrom(imgBuff,imgInt8);
		}
	}

	public static class FromInt8ToBuff extends PerformerBase
	{
		@Override
		public void process() {
			ConvertBufferedImage.convertTo(imgInt8,imgBuff);
		}
	}

	public static class FromInt8ToGenericBuff extends PerformerBase
	{
		@Override
		public void process() {
			ConvertRaster.grayToBuffered(imgInt8,imgBuff);
		}
	}

	public static class ExtractImageInt8 extends PerformerBase
	{
		@Override
		public void process() {
			ConvertBufferedImage.extractImageInt8(imgBuff);
		}
	}

	public static class ExtractBuffered extends PerformerBase
	{
		@Override
		public void process() {
			ConvertBufferedImage.extractBuffered(imgInt8);
		}
	}

	public static void createBufferedImage( int type ) {
		imgBuff = new BufferedImage(imgWidth,imgHeight,type);

		// randomize it to prevent some pathological condition
		for( int i = 0; i < imgHeight; i++ ) {
			for( int j = 0; j < imgWidth; j++ ) {
				imgBuff.setRGB(j,i,rand.nextInt());
			}
		}
	}

	public static void main( String args[] ) {
		imgInt8 = new ImageUInt8(imgWidth,imgHeight);

		System.out.println("=========  Profiling for ImageUInt8 ==========");
		System.out.println();

		createBufferedImage(BufferedImage.TYPE_3BYTE_BGR);
		System.out.printf("BufferedImage to ImageUInt8   %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new FromInt8ToGenericBuff(),1000, false));
		System.out.printf("TYPE_3BYTE_BGR to ImageUInt8  %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new FromBuffToInt8(),1000, false));
		System.out.printf("ImageUInt8 to TYPE_3BYTE_BGR  %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new FromInt8ToBuff(),1000, false));

		createBufferedImage(BufferedImage.TYPE_INT_RGB);
		System.out.printf("TYPE_INT_RGB to ImageUInt8    %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new FromBuffToInt8(),1000, false));
		System.out.printf("ImageUInt8 to TYPE_INT_RGB    %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new FromInt8ToBuff(),1000, false));

		createBufferedImage(BufferedImage.TYPE_BYTE_GRAY);
		System.out.printf("TYPE_BYTE_GRAY to ImageUInt8  %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new FromBuffToInt8(),1000, false));
		System.out.printf("ImageUInt8 to TYPE_BYTE_GRAY  %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new FromInt8ToBuff(),1000, false));

		System.out.printf("extractImageInt8             %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new ExtractImageInt8(),1000, false));
		System.out.printf("wxtractBuffered              %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new ExtractBuffered(),1000, false));

		System.out.println();
		System.out.println("=========  Profiling for ImageInterleavedInt8 ==========");
		System.out.println();
	}
}
