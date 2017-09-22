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

package boofcv.core.image;

import boofcv.struct.image.*;
import org.ddogleg.struct.GrowQueue_I8;

import java.nio.ByteBuffer;

/**
 * Converts images that are stored in {@link java.nio.ByteBuffer} into BoofCV image types and performs
 * a local copy when the raw array can't be accessed
 *
 * @author Peter Abeles
 */
public class ConvertByteBufferImage {

	public static void from_3BU8_to_U8(ByteBuffer src , int srcOffset , int srcStride ,
									   GrayU8 dst , GrowQueue_I8 work )
	{
		work.resize(dst.width*3);

		int indexSrc = srcOffset;
		for (int y = 0; y < dst.height; y++) {
			src.position(indexSrc);
			src.get(work.data,0,work.size);

			int indexDst = dst.startIndex + dst.stride * y;
			for (int i = 0; i < work.size;) {
				int r = work.data[i++] & 0xFF;
				int g = work.data[i++] & 0xFF;
				int b = work.data[i++] & 0xFF;

				int ave = (r + g + b) / 3;

				dst.data[indexDst++] = (byte) ave;
			}
			indexSrc += srcStride;
		}
	}

	public static void from_3BU8_to_3PU8(ByteBuffer src , int srcOffset , int srcStride ,
										 Planar<GrayU8> dst , GrowQueue_I8 work )
	{
		work.resize(dst.width*3);

		GrayU8 r = dst.getBand(0);
		GrayU8 g = dst.getBand(1);
		GrayU8 b = dst.getBand(2);

		int indexSrc = srcOffset;
		for (int y = 0; y < dst.height; y++) {
			src.position(indexSrc);
			src.get(work.data,0,work.size);

			int indexDst = dst.startIndex + dst.stride * y;
			for (int i = 0; i < work.size; indexDst++) {
				r.data[indexDst] = work.data[i++];
				g.data[indexDst] = work.data[i++];
				b.data[indexDst] = work.data[i++];
			}
			indexSrc += srcStride;
		}
	}

	public static void from_3BU8_to_3IU8(ByteBuffer src , int srcOffset , int srcStride ,
										 InterleavedU8 dst )
	{
		int indexSrc = srcOffset;
		for (int y = 0; y < dst.height; y++) {
			src.position(indexSrc);
			src.get(dst.data,0,dst.width*3);
			indexSrc += srcStride;
		}
	}

	public static void from_3BU8_to_F32(ByteBuffer src , int srcOffset , int srcStride ,
										GrayF32 dst , GrowQueue_I8 work )
	{
		work.resize(dst.width*3);

		int indexSrc = srcOffset;
		for (int y = 0; y < dst.height; y++) {
			src.position(indexSrc);
			src.get(work.data,0,work.size);

			int indexDst = dst.startIndex + dst.stride * y;
			for (int i = 0; i < work.size;) {
				int r = work.data[i++] & 0xFF;
				int g = work.data[i++] & 0xFF;
				int b = work.data[i++] & 0xFF;

				int ave = (r + g + b) / 3;

				dst.data[indexDst++] = ave;
			}
			indexSrc += srcStride;
		}
	}

	public static void from_3BU8_to_3PF32(ByteBuffer src , int srcOffset , int srcStride ,
										  Planar<GrayF32> dst , GrowQueue_I8 work )
	{
		work.resize(dst.width*3);

		GrayF32 r = dst.getBand(0);
		GrayF32 g = dst.getBand(1);
		GrayF32 b = dst.getBand(2);

		int indexSrc = srcOffset;
		for (int y = 0; y < dst.height; y++) {
			src.position(indexSrc);
			src.get(work.data,0,work.size);

			int indexDst = dst.startIndex + dst.stride * y;
			for (int i = 0; i < work.size; indexDst++) {
				r.data[indexDst] = work.data[i++] & 0xFF;
				g.data[indexDst] = work.data[i++] & 0xFF;
				b.data[indexDst] = work.data[i++] & 0xFF;
			}
			indexSrc += srcStride;
		}
	}

	public static void from_3BU8_to_3IF32(ByteBuffer src , int srcOffset , int srcStride ,
										  InterleavedF32 dst , GrowQueue_I8 work )
	{
		work.resize(dst.width*3);

		int indexSrc = srcOffset;
		for (int y = 0; y < dst.height; y++) {
			src.position(indexSrc);
			src.get(work.data,0,work.size);

			int indexDst = dst.startIndex + dst.stride * y;
			for (int i = 0; i < work.size; ) {
				dst.data[indexDst++] = work.data[i++] & 0xFF;
				dst.data[indexDst++] = work.data[i++] & 0xFF;
				dst.data[indexDst++] = work.data[i++] & 0xFF;
			}
			indexSrc += srcStride;
		}
	}
}
