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

package boofcv.alg.color;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author Peter Abeles
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value=1)
public class BenchmarkColorConvert {
	public static final int imgWidth = 640;
	public static final int imgHeight = 480;
	public static final Random rand = new Random(234);

	public static final int TEST_TIME = 1000;

	public static Planar<GrayF32> src_F32;
	public static Planar<GrayF32> dst_F32;

	{
//		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
//		System.out.println();

		src_F32 = new Planar<>(GrayF32.class,imgWidth,imgHeight,3);
		dst_F32 = new Planar<>(GrayF32.class,imgWidth,imgHeight,3);

		GImageMiscOps.addUniform(src_F32,rand,0,255);
	}

	@Benchmark
	public void RGB_to_HSV_F32() {
		ColorHsv.rgbToHsv_F32(src_F32,dst_F32);
	}

	@Benchmark
	public void HSV_to_RGB_F32() {
		ColorHsv.hsvToRgb_F32(src_F32,dst_F32);
	}

	@Benchmark
	public void RGB_to_YUV_F32() {
		ColorYuv.rgbToYuv_F32(src_F32,dst_F32);
	}

	@Benchmark
	public void YUV_to_RGB_F32() {
		ColorYuv.yuvToRgb_F32(src_F32,dst_F32);
	}

}
