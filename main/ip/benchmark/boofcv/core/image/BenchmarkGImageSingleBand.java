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

package boofcv.core.image;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkGImageSingleBand {

	static int period = 1000;

	static int width = 640;
	static int height = 480;

	static ImageFloat32 input = new ImageFloat32(width,height);

	public static class IndexDirect_U8 extends PerformerBase
	{
		ImageUInt8 output;

		public IndexDirect_U8(ImageUInt8 output) {
			this.output = output;
		}

		@Override
		public void process() {
			int index = 0;
			for( int y = 0; y < input.height; y++ ) {
				for( int x = 0; x < input.width; x++ , index++) {
					output.data[index] = (byte)input.data[index];
				}
			}
		}
	}

	public static class IndexDirect_F32 extends PerformerBase
	{
		ImageFloat32 output;

		public IndexDirect_F32( ImageFloat32 output) {
			this.output = output;
		}

		@Override
		public void process() {
			int index = 0;
			for( int y = 0; y < input.height; y++ ) {
				for( int x = 0; x < input.width; x++ , index++) {
					output.data[index] = input.data[index];
				}
			}
		}
	}

	public static class IndexAccess extends PerformerBase
	{
		GImageSingleBand output;

		public IndexAccess( ImageSingleBand output) {
			this.output = FactoryGImageSingleBand.wrap(output);
		}

		@Override
		public void process() {
			int index = 0;
			for( int y = 0; y < input.height; y++ ) {
				for( int x = 0; x < input.width; x++ , index++) {
					output.set(index,input.data[index]);
				}
			}
		}
	}

	public static class PixelAccess extends PerformerBase
	{
		GImageSingleBand output;

		public PixelAccess( ImageSingleBand output) {
			this.output = FactoryGImageSingleBand.wrap(output);
		}

		@Override
		public void process() {

			int index = 0;
			for( int y = 0; y < input.height; y++ ) {
				for( int x = 0; x < input.width; x++ , index++) {
					output.set(x,y,input.data[index]);
				}
			}
		}
	}

	public static void main( String args[] ) {
		ImageFloat32 output_F32 = new ImageFloat32(width,height);
		ImageUInt8 output_U8 = new ImageUInt8(width,height);

		Random rand = new Random(234);

		ImageMiscOps.fillUniform(input,rand,0,50);

		System.out.println("=========  Profile Image Size "+width+" x "+height+" ==========");
		System.out.println();

		System.out.printf("Direct  U8          %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new IndexDirect_U8(output_U8), period, false));
		System.out.printf("Wrapped U8 Index    %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new IndexAccess(output_U8),period, false));
		System.out.printf("Wrapped U8 Pixel    %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new PixelAccess(output_U8),period, false));
		System.out.printf("Direct  F32         %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new IndexDirect_F32(output_F32), period, false));
		System.out.printf("Wrapped F32 Index   %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new IndexAccess(output_F32),period, false));
		System.out.printf("Wrapped F32 Pixel   %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new PixelAccess(output_F32),period, false));
	}
}
