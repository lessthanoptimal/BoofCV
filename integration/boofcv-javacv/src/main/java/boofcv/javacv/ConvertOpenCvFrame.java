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

package boofcv.javacv;

import boofcv.core.image.ConvertByteBufferImage;
import boofcv.struct.image.*;
import org.bytedeco.javacv.Frame;
import org.ddogleg.struct.GrowQueue_I8;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * @author Peter Abeles
 */
public class ConvertOpenCvFrame {

	public static void BGR_to_RGB(Planar planar ) {
		ImageGray tmp0 = planar.getBand(0);
		ImageGray tmp2 = planar.getBand(2);

		planar.bands[0] = tmp2;
		planar.bands[2] = tmp0;
	}

	public static void convert(Frame input , ImageBase output , boolean swapRgb, GrowQueue_I8 work) {

		if( work == null )
			work = new GrowQueue_I8();

		Buffer data = input.image[0];
		if( !(data instanceof ByteBuffer) ) {
			return;
		}
		ByteBuffer bb = (ByteBuffer)data;
		output.reshape(input.imageWidth,input.imageHeight);

		if( output instanceof Planar ) {
			((Planar)output).setNumberOfBands(input.imageChannels);
			ConvertByteBufferImage.from_3BU8_to_3PU8(bb,0,input.imageStride,(Planar)output,work);

			if( swapRgb ) {
				BGR_to_RGB((Planar)output);
			}
		} else if( output instanceof ImageGray ) {
			ConvertByteBufferImage.from_3BU8_to_U8(bb,0,input.imageStride,(GrayU8)output,work);
		} else if( output instanceof ImageInterleaved) {
			ConvertByteBufferImage.from_3BU8_to_3IU8(bb,0,input.imageStride,(InterleavedU8)output);
		} else {
			throw new IllegalArgumentException("Unsupported output type");
		}
	}

}
