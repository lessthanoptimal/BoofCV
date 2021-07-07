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

import boofcv.concurrency.BoofConcurrency;
import boofcv.core.encoding.impl.ImplConvertYuyv;
import boofcv.core.encoding.impl.ImplConvertYuyv_MT;
import boofcv.struct.image.*;

/**
 * <p>Packed format with ½ horizontal chroma resolution, also known as YUV 4:2:2</p>
 *
 * In this format each four bytes is two pixels. Each four bytes is two Y's, a Cb and a Cr. Each Y goes to one of the
 * pixels, and the Cb and Cr belong to both pixels. As you can see, the Cr and Cb components have half the horizontal
 * resolution of the Y component. V4L2_PIX_FMT_YUYV is known in the Windows environment as YUY2.
 *
 * <p>Description taken from V4L documentation.</p>
 *
 * @author Peter Abeles
 */
public class ConvertYuyv {
	/**
	 * Converts a YU12 encoded byte array into a BoofCV formatted image.
	 *
	 * @param data (input) YU12 byte array
	 * @param width (input) image width
	 * @param height (input) image height
	 * @param output (output) BoofCV image
	 */
	public static void yuyvToBoof(byte[] data, int width, int height, ImageBase output) {

		if( output instanceof Planar) {
			Planar pl = (Planar) output;
			pl.reshape(width,height,3);

			if( BoofConcurrency.USE_CONCURRENT ) {
				if (pl.getBandType() == GrayU8.class) {
					ImplConvertYuyv_MT.yuyvToPlanarRgb_U8(data, pl);
				} else if (pl.getBandType() == GrayF32.class) {
					ImplConvertYuyv_MT.yuyvToPlanarRgb_F32(data, pl);
				} else {
					throw new IllegalArgumentException("Unsupported output band format");
				}
			} else {
				if (pl.getBandType() == GrayU8.class) {
					ImplConvertYuyv.yuyvToPlanarRgb_U8(data, pl);
				} else if (pl.getBandType() == GrayF32.class) {
					ImplConvertYuyv.yuyvToPlanarRgb_F32(data, pl);
				} else {
					throw new IllegalArgumentException("Unsupported output band format");
				}
			}
		} else if( output instanceof ImageGray) {
			if (output.getClass() == GrayU8.class) {
				yuyvToGray(data, width, height, (GrayU8) output);
			} else if (output.getClass() == GrayF32.class) {
				yuyvToGray(data, width, height, (GrayF32) output);
			} else {
				throw new IllegalArgumentException("Unsupported output type");
			}
		} else if( output instanceof ImageInterleaved ) {
			((ImageMultiBand)output).reshape(width,height,3);

			if( BoofConcurrency.USE_CONCURRENT ) {
				if (output.getClass() == InterleavedU8.class) {
					ImplConvertYuyv_MT.yuyvToInterleaved(data, (InterleavedU8) output);
				} else if (output.getClass() == InterleavedF32.class) {
					ImplConvertYuyv_MT.yuyvToInterleaved(data, (InterleavedF32) output);
				} else {
					throw new IllegalArgumentException("Unsupported output type");
				}
			} else {
				if (output.getClass() == InterleavedU8.class) {
					ImplConvertYuyv.yuyvToInterleaved(data, (InterleavedU8) output);
				} else if (output.getClass() == InterleavedF32.class) {
					ImplConvertYuyv.yuyvToInterleaved(data, (InterleavedF32) output);
				} else {
					throw new IllegalArgumentException("Unsupported output type");
				}
			}
		} else {
			throw new IllegalArgumentException("Boofcv image type not yet supported");
		}
	}

	/**
	 * Converts an Yuyv image into a gray scale U8 image.
	 *
	 * @param data Input: Yuyv image data
	 * @param width Input: image width
	 * @param height Input: image height
	 * @param output Output: Optional storage for output image. Can be null.
	 * @return Gray scale image
	 */
	public static GrayU8 yuyvToGray(byte[] data , int width , int height , GrayU8 output ) {
		if( output != null ) {
			output.reshape(width,height);
		} else {
			output = new GrayU8(width,height);
		}

		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertYuyv_MT.yuyvToGray(data, output);
		} else {
			ImplConvertYuyv.yuyvToGray(data, output);
		}

		return output;
	}

	/**
	 * Converts an Yuyv image into a gray scale F32 image.
	 *
	 * @param data Input: Yuyv image data
	 * @param width Input: image width
	 * @param height Input: image height
	 * @param output Output: Optional storage for output image. Can be null.
	 * @return Gray scale image
	 */
	public static GrayF32 yuyvToGray(byte[] data , int width , int height , GrayF32 output ) {
		if( output != null ) {
			output.reshape(width,height);
		} else {
			output = new GrayF32(width,height);
		}

		if(BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertYuyv_MT.yuyvToGray(data, output);
		} else {
			ImplConvertYuyv.yuyvToGray(data, output);
		}

		return output;
	}
}
