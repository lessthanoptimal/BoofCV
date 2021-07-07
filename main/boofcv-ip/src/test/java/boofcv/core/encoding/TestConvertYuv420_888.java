/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.color.ColorFormat;
import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;


/**
 * Current units mostly check to see if an exception is thrown when processing these different images
 *
 * NOTE: To get this test to work you need ot move junit 4.x to the top and android.benchmark to the top in
 * your dependencies
 *
 * @author Peter Abeles
 */
public class TestConvertYuv420_888 extends BoofStandardJUnit {

	int width = 320,height=240;

	ByteBuffer bufferY,bufferU,bufferV;
	int strideY,strideUV,stridePixelUV;


	ImageShape[] inputs = new ImageShape[]{
			new ImageShape(width,height,1,1,0),
			new ImageShape(width,height,1,2,0),
			new ImageShape(width,height,2,1,0),
			new ImageShape(width,height,2,2,0),
			new ImageShape(width,height,2,2,1),
			new ImageShape(width+1,height,1,1,0),
			new ImageShape(width,height+1,1,1,0),
			new ImageShape(width+1,height,2,2,0),
			new ImageShape(width,height+1,2,2,0),
	};

	private static class ImageShape
	{
		int width,height;
		int pixelStrideUV, periodUV, extra;

		public ImageShape(int width, int height, int pixelStrideUV, int periodUV, int extra) {
			this.width = width;
			this.height = height;
			this.pixelStrideUV = pixelStrideUV;
			this.periodUV = periodUV;
			this.extra = extra;
		}
	}

	private void init( ImageShape shape ) {
		this.stridePixelUV = shape.pixelStrideUV;
		this.width = shape.width;
		this.height = shape.height;

		strideY = width+shape.extra;
		strideUV = stridePixelUV*(width/shape.periodUV + (width%shape.periodUV))+shape.extra;

		byte[] gray = new byte[strideY*height];
		byte[] bandUV = new byte[2*strideUV*(height/shape.periodUV)];

		rand.nextBytes(gray);
		rand.nextBytes(bandUV);

		bufferY = ByteBuffer.wrap(gray);
		bufferU = ByteBuffer.wrap(bandUV,0,bandUV.length);
		bufferV = ByteBuffer.wrap(bandUV,1,bandUV.length-1);
	}

	@Test void yuvToGray_U8() {
		GrayU8 output = new GrayU8(width,height);

		for (int i = 0; i < inputs.length; i++) {
			init(inputs[i]);
			ConvertYuv420_888.yuvToBoof(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,
					ColorFormat.RGB,output,null);
		}
	}

	@Test void yuvToGray_F32() {
		GrayF32 output = new GrayF32(width,height);

		for (int i = 0; i < inputs.length; i++) {
			init(inputs[i]);
			ConvertYuv420_888.yuvToBoof(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,
					ColorFormat.RGB,output,null);
		}
	}

	@Test void yuvToInterleavedRgbU8() {
		InterleavedU8 output = new InterleavedU8(width,height,3);

		for (int i = 0; i < inputs.length; i++) {
			init(inputs[i]);
			ConvertYuv420_888.yuvToBoof(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,
					ColorFormat.RGB,output,null);
		}
	}


	@Test void yuvToInterleavedRgbF32() {
		InterleavedF32 output = new InterleavedF32(width,height,3);

		for (int i = 0; i < inputs.length; i++) {
			init(inputs[i]);
			ConvertYuv420_888.yuvToBoof(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,
					ColorFormat.RGB,output,null);
		}
	}

	@Test void yuvToPlanarRgbU8() {
		Planar<GrayU8> output = new Planar<>(GrayU8.class,width,height,3);

		for (int i = 0; i < inputs.length; i++) {
			init(inputs[i]);
			ConvertYuv420_888.yuvToBoof(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,
					ColorFormat.RGB,output,null);
		}
	}

	@Test void yuvToPlanarRgbF32() {
		Planar<GrayF32> output = new Planar<>(GrayF32.class,width,height,3);

		for (int i = 0; i < inputs.length; i++) {
			init(inputs[i]);
			ConvertYuv420_888.yuvToBoof(bufferY,bufferU,bufferV,width,height,strideY,strideUV,stridePixelUV,
					ColorFormat.RGB,output,null);
		}
	}
}
