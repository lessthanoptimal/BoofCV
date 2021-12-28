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

package boofcv.android;

import android.graphics.ImageFormat;
import android.media.Image;
import boofcv.alg.color.ColorFormat;
import boofcv.core.encoding.ConvertYuv420_888;
import boofcv.struct.image.ImageBase;
import org.ddogleg.struct.DogArray_I8;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

import java.nio.ByteBuffer;

/**
 * Converts the android {@link Image} into a boofcv format.
 */
public class ConvertCameraImage {
	public static void imageToBoof( Image yuv, ColorFormat colorOutput, ImageBase output,
									@Nullable GrowArray<DogArray_I8> workArrays ) {
		if (BOverrideConvertAndroid.invokeYuv420ToBoof(yuv, colorOutput, output))
			return;

		if (ImageFormat.YUV_420_888 != yuv.getFormat())
			throw new RuntimeException("Unexpected format");

		Image.Plane[] planes = yuv.getPlanes();

		ByteBuffer bufferY = planes[0].getBuffer();
		ByteBuffer bufferU = planes[2].getBuffer();
		ByteBuffer bufferV = planes[1].getBuffer();

		int width = yuv.getWidth();
		int height = yuv.getHeight();

		int strideY = planes[0].getRowStride();
		int strideUV = planes[1].getRowStride();
		int stridePixelUV = planes[1].getPixelStride();

		ConvertYuv420_888.yuvToBoof(
				bufferY, bufferU, bufferV,
				width, height, strideY, strideUV, stridePixelUV,
				colorOutput, output, workArrays);
	}
}
