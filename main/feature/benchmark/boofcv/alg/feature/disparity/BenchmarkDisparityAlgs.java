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

package boofcv.alg.feature.disparity;

import boofcv.alg.feature.disparity.impl.*;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GConvertImage;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkDisparityAlgs {
	static final long TEST_TIME = 1000;
	static final Random rand = new Random(234234);

	static final int width=640;
	static final int height=480;
	static final int min=0;
	static final int max=20;
	static final int radiusX=2;
	static final int radiusY=2;

	static final GrayU8 left = new GrayU8(width,height);
	static final GrayU8 right = new GrayU8(width,height);

	static final GrayF32 left_F32 = new GrayF32(width,height);
	static final GrayF32 right_F32 = new GrayF32(width,height);

	static final GrayU8 outU8 = new GrayU8(width,height);
	static final GrayF32 out_F32 = new GrayF32(width,height);

	public static class Naive extends PerformerBase {

		StereoDisparityWtoNaive<GrayU8> alg =
				new StereoDisparityWtoNaive<>(0, max, radiusX, radiusY);

		@Override
		public void process() {
			alg.process(left,right,out_F32);
		}
	}

	public static class EfficientSad_U8 extends PerformerBase {

//		DisparitySelect<int[],GrayU8> compDisp =
//				new ImplSelectRectBasicWta_S32_U8();
		DisparitySelect<int[],GrayU8> compDisp =
				new ImplSelectRectStandard_S32_U8(250,2,0.1);
		ImplDisparityScoreSadRect_U8<GrayU8> alg =
				new ImplDisparityScoreSadRect_U8<>(min,max,radiusX,radiusY,compDisp);

		@Override
		public void process() {
			alg.process(left,right, outU8);
		}
	}

	public static class EfficientSad_F32 extends PerformerBase {

		//		DisparitySelect<int[],GrayU8> compDisp =
//				new ImplSelectRectBasicWta_S32_U8();
		DisparitySelect<float[],GrayU8> compDisp =
				new ImplSelectRectStandard_F32_U8(250,2,0.1);
		ImplDisparityScoreSadRect_F32<GrayU8> alg =
				new ImplDisparityScoreSadRect_F32<>(min,max,radiusX,radiusY,compDisp);

		@Override
		public void process() {
			alg.process(left_F32,right_F32, outU8);
		}
	}

	public static class EfficientSubpixelSad extends PerformerBase {

		DisparitySelect<int[],GrayF32> compDisp =
				new SelectRectSubpixel.S32_F32(250,2,0.1);
		ImplDisparityScoreSadRect_U8<GrayF32> alg =
				new ImplDisparityScoreSadRect_U8<>(min,max,radiusX,radiusY,compDisp);

		@Override
		public void process() {
			alg.process(left,right, out_F32);
		}
	}

	public static class EfficientSadFive_U8 extends PerformerBase {

		//		DisparitySelect<int[],GrayU8> compDisp =
//				new ImplSelectRectBasicWta_S32_U8();
		DisparitySelect<int[],GrayU8> compDisp =
				new ImplSelectRectStandard_S32_U8(250,2,0.1);
		ImplDisparityScoreSadRectFive_U8<GrayU8> alg =
				new ImplDisparityScoreSadRectFive_U8<>(min,max,radiusX,radiusY,compDisp);

		@Override
		public void process() {
			alg.process(left,right, outU8);
		}
	}

	public static void main( String argsp[ ] ) {
		System.out.println("=========  Image Size "+ width +" "+height+"  disparity "+max);
		System.out.println();

		GImageMiscOps.fillUniform(left, rand, 0, 30);
		GImageMiscOps.fillUniform(right, rand, 0, 30);
		GConvertImage.convert(left, left_F32);
		GConvertImage.convert(right,right_F32);

		// the "fastest" seems to always be the first one tested
		ProfileOperation.printOpsPerSec(new EfficientSad_U8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new EfficientSadFive_U8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new EfficientSad_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new EfficientSubpixelSad(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new Naive(), TEST_TIME);

	}
}
