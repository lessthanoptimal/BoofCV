/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.derivative.impl;

import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;

/**
 * Hessian-Three derivative which processes the outer image border only
 *
 * @see boofcv.alg.filter.derivative.HessianThreeDeterminant
 *
 * @author Peter Abeles
 */
public class HessianThreeDeterminant_Border {
	public static void process(GrayU8 input, GrayS16 output, ImageBorder_S32<GrayU8> border ) {
		border.setImage(input);

		final byte[] src = input.data;
		final short[] dst = output.data;

		final int width = input.getWidth();
		final int height = input.getHeight();
		final int stride = input.stride;

		for (int y = 0; y < 2; y++) {
			int idxSrc = input.startIndex + stride * y;
			int idxDst = output.startIndex + stride * y;

			for (int x = 0; x < width; x++, idxSrc++) {
				int center = src[idxSrc]&0xFF;
				int Lxx = border.get(x-2,y) - 2*center + border.get(x+2,y);
				int Lyy = border.get(x,y-2) - 2*center + border.get(x,y+2);
				int Lxy = border.get(x-1,y-1) + border.get(x+1,y+1);
				Lxy -= border.get(x+1,y-1) + border.get(x-1,y+1);

				dst[idxDst++] = (short)(Lxx*Lyy-Lxy*Lxy);
			}
		}

		for (int y = height-2; y < height; y++) {
			int idxSrc = input.startIndex + stride * y;
			int idxDst = output.startIndex + stride * y;

			for (int x = 0; x < width; x++, idxSrc++) {
				int center = src[idxSrc]&0xFF;
				int Lxx = border.get(x-2,y) - 2*center + border.get(x+2,y);
				int Lyy = border.get(x,y-2) - 2*center + border.get(x,y+2);
				int Lxy = border.get(x-1,y-1) + border.get(x+1,y+1);
				Lxy -= border.get(x+1,y-1) + border.get(x-1,y+1);

				dst[idxDst++] = (short)(Lxx*Lyy-Lxy*Lxy);
			}
		}

		for (int y = 2; y < height-2; y++) {
			for (int x = 0; x < 2; x++) {
				int idxSrc = input.startIndex + stride * y+x;
				int idxDst = output.startIndex + stride * y+x;

				int center = src[idxSrc]&0xFF;
				int Lxx = border.get(x-2,y) - 2*center + border.get(x+2,y);
				int Lyy = border.get(x,y-2) - 2*center + border.get(x,y+2);
				int Lxy = border.get(x-1,y-1) + border.get(x+1,y+1);
				Lxy -= border.get(x+1,y-1) + border.get(x-1,y+1);

				dst[idxDst] = (short)(Lxx*Lyy-Lxy*Lxy);
			}

			for (int x = width-2; x < width; x++) {
				int idxSrc = input.startIndex + stride * y+x;
				int idxDst = output.startIndex + stride * y+x;

				int center = src[idxSrc]&0xFF;
				int Lxx = border.get(x-2,y) - 2*center + border.get(x+2,y);
				int Lyy = border.get(x,y-2) - 2*center + border.get(x,y+2);
				int Lxy = border.get(x-1,y-1) + border.get(x+1,y+1);
				Lxy -= border.get(x+1,y-1) + border.get(x-1,y+1);

				dst[idxDst] = (short)(Lxx*Lyy-Lxy*Lxy);
			}
		}
	}

	public static void process(GrayU8 input, GrayF32 output, ImageBorder_S32<GrayU8> border) {
		border.setImage(input);

		final byte[] src = input.data;
		final float[] dst = output.data;

		final int width = input.getWidth();
		final int height = input.getHeight();
		final int stride = input.stride;

		for (int y = 0; y < 2; y++) {
			int idxSrc = input.startIndex + stride * y;
			int idxDst = output.startIndex + stride * y;

			for (int x = 0; x < width; x++, idxSrc++) {
				int center = src[idxSrc]&0xFF;
				int Lxx = border.get(x-2,y) - 2*center + border.get(x+2,y);
				int Lyy = border.get(x,y-2) - 2*center + border.get(x,y+2);
				int Lxy = border.get(x-1,y-1) + border.get(x+1,y+1);
				Lxy -= border.get(x+1,y-1) + border.get(x-1,y+1);

				dst[idxDst++] = Lxx*Lyy-Lxy*Lxy;
			}
		}

		for (int y = height-2; y < height; y++) {
			int idxSrc = input.startIndex + stride * y;
			int idxDst = output.startIndex + stride * y;

			for (int x = 0; x < width; x++, idxSrc++) {
				int center = src[idxSrc]&0xFF;
				int Lxx = border.get(x-2,y) - 2*center + border.get(x+2,y);
				int Lyy = border.get(x,y-2) - 2*center + border.get(x,y+2);
				int Lxy = border.get(x-1,y-1) + border.get(x+1,y+1);
				Lxy -= border.get(x+1,y-1) + border.get(x-1,y+1);

				dst[idxDst++] = Lxx*Lyy-Lxy*Lxy;
			}
		}

		for (int y = 2; y < height-2; y++) {
			for (int x = 0; x < 2; x++) {
				int idxSrc = input.startIndex + stride * y+x;
				int idxDst = output.startIndex + stride * y+x;

				int center = src[idxSrc]&0xFF;
				int Lxx = border.get(x-2,y) - 2*center + border.get(x+2,y);
				int Lyy = border.get(x,y-2) - 2*center + border.get(x,y+2);
				int Lxy = border.get(x-1,y-1) + border.get(x+1,y+1);
				Lxy -= border.get(x+1,y-1) + border.get(x-1,y+1);

				dst[idxDst] = Lxx*Lyy-Lxy*Lxy;
			}

			for (int x = width-2; x < width; x++) {
				int idxSrc = input.startIndex + stride * y+x;
				int idxDst = output.startIndex + stride * y+x;

				int center = src[idxSrc]&0xFF;
				int Lxx = border.get(x-2,y) - 2*center + border.get(x+2,y);
				int Lyy = border.get(x,y-2) - 2*center + border.get(x,y+2);
				int Lxy = border.get(x-1,y-1) + border.get(x+1,y+1);
				Lxy -= border.get(x+1,y-1) + border.get(x-1,y+1);

				dst[idxDst] = Lxx*Lyy-Lxy*Lxy;
			}
		}
	}

	public static void process(GrayF32 input, GrayF32 output, ImageBorder_F32 border) {
		border.setImage(input);

		final float[] src = input.data;
		final float[] dst = output.data;

		final int width = input.getWidth();
		final int height = input.getHeight();
		final int stride = input.stride;

		for (int y = 0; y < 2; y++) {
			int idxSrc = input.startIndex + stride * y;
			int idxDst = output.startIndex + stride * y;

			for (int x = 0; x < width; x++, idxSrc++) {
				float center = src[idxSrc];
				float Lxx = border.get(x-2,y) - 2*center + border.get(x+2,y);
				float Lyy = border.get(x,y-2) - 2*center + border.get(x,y+2);
				float Lxy = border.get(x-1,y-1) + border.get(x+1,y+1);
				Lxy -= border.get(x+1,y-1) + border.get(x-1,y+1);

				dst[idxDst++] = Lxx*Lyy-Lxy*Lxy;
			}
		}

		for (int y = height-2; y < height; y++) {
			int idxSrc = input.startIndex + stride * y;
			int idxDst = output.startIndex + stride * y;

			for (int x = 0; x < width; x++, idxSrc++) {
				float center = src[idxSrc];
				float Lxx = border.get(x-2,y) - 2*center + border.get(x+2,y);
				float Lyy = border.get(x,y-2) - 2*center + border.get(x,y+2);
				float Lxy = border.get(x-1,y-1) + border.get(x+1,y+1);
				Lxy -= border.get(x+1,y-1) + border.get(x-1,y+1);

				dst[idxDst++] = Lxx*Lyy-Lxy*Lxy;
			}
		}

		for (int y = 2; y < height-2; y++) {
			for (int x = 0; x < 2; x++) {
				int idxSrc = input.startIndex + stride * y+x;
				int idxDst = output.startIndex + stride * y+x;

				float center = src[idxSrc];
				float Lxx = border.get(x-2,y) - 2*center + border.get(x+2,y);
				float Lyy = border.get(x,y-2) - 2*center + border.get(x,y+2);
				float Lxy = border.get(x-1,y-1) + border.get(x+1,y+1);
				Lxy -= border.get(x+1,y-1) + border.get(x-1,y+1);

				dst[idxDst] = Lxx*Lyy-Lxy*Lxy;
			}

			for (int x = width-2; x < width; x++) {
				int idxSrc = input.startIndex + stride * y+x;
				int idxDst = output.startIndex + stride * y+x;

				float center = src[idxSrc];
				float Lxx = border.get(x-2,y) - 2*center + border.get(x+2,y);
				float Lyy = border.get(x,y-2) - 2*center + border.get(x,y+2);
				float Lxy = border.get(x-1,y-1) + border.get(x+1,y+1);
				Lxy -= border.get(x+1,y-1) + border.get(x-1,y+1);

				dst[idxDst] = Lxx*Lyy-Lxy*Lxy;
			}
		}
	}
}
