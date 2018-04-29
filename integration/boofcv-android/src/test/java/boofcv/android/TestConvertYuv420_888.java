///*
// * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
// *
// * This file is part of BoofCV (http://boofcv.org).
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package boofcv.android;
//
//import android.media.MockImage_420_888;
//import boofcv.struct.image.*;
//import org.junit.Test;
//
//import java.util.Random;
//
//
///**
// * Current units mostly check to see if an exception is thrown when processing these different images
// *
// * NOTE: To get this test to work you need ot move junit 4.x to the top and android.benchmark to the top in
// * your dependencies
// *
// * @author Peter Abeles
// */
//public class TestConvertYuv420_888 {
//	Random rand = new Random(234);
//
//	int width = 320,height=240;
//
//	MockImage_420_888 inputs[] = new MockImage_420_888[]{
//			new MockImage_420_888(rand,width,height,1,1,0),
//			new MockImage_420_888(rand,width,height,1,2,0),
//			new MockImage_420_888(rand,width,height,2,1,0),
//			new MockImage_420_888(rand,width,height,2,2,0),
//			new MockImage_420_888(rand,width,height,2,2,1),
//			new MockImage_420_888(rand,width+1,height,1,1,0),
//			new MockImage_420_888(rand,width,height+1,1,1,0),
//			new MockImage_420_888(rand,width+1,height,2,2,0),
//			new MockImage_420_888(rand,width,height+1,2,2,0),
//	};
//
//	@Test
//	public void yuvToGray_U8() {
//		GrayU8 output = new GrayU8(width,height);
//
//		for (int i = 0; i < inputs.length; i++) {
//			ConvertYuv420_888.yuvToGray(inputs[i].getPlanes()[0],width,height,output);
//		}
//	}
//
//	@Test
//	public void yuvToGray_F32() {
//		GrayF32 output = new GrayF32(width,height);
//
//		for (int i = 0; i < inputs.length; i++) {
//			byte[] work = ConvertYuv420_888.declareWork(inputs[i],null);
//			ConvertYuv420_888.yuvToGray(inputs[i].getPlanes()[0],width,height,output,work);
//		}
//	}
//
//	@Test
//	public void yuvToInterleavedRgbU8() {
//		InterleavedU8 output = new InterleavedU8(width,height,3);
//
//		for (int i = 0; i < inputs.length; i++) {
//			byte[] work = ConvertYuv420_888.declareWork(inputs[i],null);
//			ConvertYuv420_888.yuvToInterleavedRgbU8(inputs[i],output,work);
//		}
//	}
//
//
//	@Test
//	public void yuvToInterleavedRgbF32() {
//		InterleavedF32 output = new InterleavedF32(width,height,3);
//
//		for (int i = 0; i < inputs.length; i++) {
//			byte[] work = ConvertYuv420_888.declareWork(inputs[i],null);
//			ConvertYuv420_888.yuvToInterleavedRgbF32(inputs[i],output,work);
//		}
//	}
//
//	@Test
//	public void yuvToPlanarRgbU8() {
//		Planar<GrayU8> output = new Planar<>(GrayU8.class,width,height,3);
//
//		for (int i = 0; i < inputs.length; i++) {
//			byte[] work = ConvertYuv420_888.declareWork(inputs[i],null);
//			ConvertYuv420_888.yuvToPlanarRgbU8(inputs[i],output,work);
//		}
//	}
//
//	@Test
//	public void yuvToPlanarRgbF32() {
//		Planar<GrayF32> output = new Planar<>(GrayF32.class,width,height,3);
//
//		for (int i = 0; i < inputs.length; i++) {
//			byte[] work = ConvertYuv420_888.declareWork(inputs[i],null);
//			ConvertYuv420_888.yuvToPlanarRgbF32(inputs[i],output,work);
//		}
//	}
//}