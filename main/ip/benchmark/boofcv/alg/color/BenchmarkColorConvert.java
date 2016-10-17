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

package boofcv.alg.color;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkColorConvert {
	public static final int imgWidth = 640;
	public static final int imgHeight = 480;
	public static final Random rand = new Random(234);

	public static final int TEST_TIME = 1000;

	public static Planar<GrayF32> src_F32;
	public static Planar<GrayF32> dst_F32;

	public static class RGB_to_HSV_F32 extends PerformerBase {

		@Override
		public void process() {
			ColorHsv.rgbToHsv_F32(src_F32,dst_F32);
		}
	}

	public static class HSV_to_RGB_F32 extends PerformerBase {

		@Override
		public void process() {
			ColorHsv.hsvToRgb_F32(src_F32,dst_F32);
		}
	}

	public static class RGB_to_YUV_F32 extends PerformerBase {

		@Override
		public void process() {
			ColorYuv.rgbToYuv_F32(src_F32,dst_F32);
		}
	}

	public static class YUV_to_RGB_F32 extends PerformerBase {

		@Override
		public void process() {
			ColorYuv.yuvToRgb_F32(src_F32,dst_F32);
		}
	}

	public static void main( String args[] ) {
		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		src_F32 = new Planar<>(GrayF32.class,imgWidth,imgHeight,3);
		dst_F32 = new Planar<>(GrayF32.class,imgWidth,imgHeight,3);

		GImageMiscOps.addUniform(src_F32,rand,0,255);

		ProfileOperation.printOpsPerSec(new RGB_to_HSV_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new HSV_to_RGB_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new RGB_to_YUV_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new YUV_to_RGB_F32(),TEST_TIME);
	}
}
