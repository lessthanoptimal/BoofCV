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

package boofcv.alg.filter.derivative.impl;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;


/**
 * Prewitt implementation that shares values for horizontal and vertical gradients
 *
 * @author Peter Abeles
 */
public class HessianSobel_Shared {
	public static void process(GrayU8 orig,
							   GrayS16 derivXX,
							   GrayS16 derivYY,
							   GrayS16 derivXY) {
		final byte[] data = orig.data;
		final short[] imgX = derivXX.data;
		final short[] imgY = derivYY.data;
		final short[] imgXY = derivXY.data;

		final int width = orig.getWidth() - 2;
		final int height = orig.getHeight() - 2;
		final int strideSrc = orig.getStride();

		for (int y = 2; y < height; y++) {
			int indexSrc = orig.startIndex + orig.stride * y + 2;
			final int endX = indexSrc + width-2;

			int indexX = derivXX.startIndex + derivXX.stride * y + 2;
			int indexY = derivYY.startIndex + derivYY.stride * y + 2;
			int indexXY = derivXY.startIndex + derivXY.stride * y + 2;

			for (; indexSrc < endX; indexSrc++) {
				int a11 = (data[indexSrc - 2*strideSrc - 2] & 0xFF);
				int a12 = (data[indexSrc - 2*strideSrc - 1] & 0xFF);
				int a13 = (data[indexSrc - 2*strideSrc    ] & 0xFF);
				int a14 = (data[indexSrc - 2*strideSrc + 1] & 0xFF);
				int a15 = (data[indexSrc - 2*strideSrc + 2] & 0xFF);
				int a21 = (data[indexSrc -   strideSrc - 2] & 0xFF);
				int a22 = (data[indexSrc -   strideSrc - 1] & 0xFF);
				int a23 = (data[indexSrc -   strideSrc    ] & 0xFF);
				int a24 = (data[indexSrc -   strideSrc + 1] & 0xFF);
				int a25 = (data[indexSrc -   strideSrc + 2] & 0xFF);
				int a31 = (data[indexSrc               - 2] & 0xFF);
				int a32 = (data[indexSrc               - 1] & 0xFF);
				int a33 = (data[indexSrc                  ] & 0xFF);
				int a34 = (data[indexSrc               + 1] & 0xFF);
				int a35 = (data[indexSrc               + 2] & 0xFF);
				int a41 = (data[indexSrc +   strideSrc - 2] & 0xFF);
				int a42 = (data[indexSrc +   strideSrc - 1] & 0xFF);
				int a43 = (data[indexSrc +   strideSrc    ] & 0xFF);
				int a44 = (data[indexSrc +   strideSrc + 1] & 0xFF);
				int a45 = (data[indexSrc +   strideSrc + 2] & 0xFF);
				int a51 = (data[indexSrc + 2*strideSrc - 2] & 0xFF);
				int a52 = (data[indexSrc + 2*strideSrc - 1] & 0xFF);
				int a53 = (data[indexSrc + 2*strideSrc    ] & 0xFF);
				int a54 = (data[indexSrc + 2*strideSrc + 1] & 0xFF);
				int a55 = (data[indexSrc + 2*strideSrc + 2] & 0xFF);


				imgY[indexY++]   = (short) (a11+a15+a51+a55+4*(a12+a52+a14+a54)+6*(a13+a53)-2*(a31+a35)-8*(a32+a34)-12*a33);
				imgX[indexX++]   = (short) (a11+a51+a15+a55+4*(a21+a25+a41+a45)+6*(a31+a35)-2*(a13+a53)-8*(a23+a43)-12*a33);
				imgXY[indexXY++] = (short) (a11+a55-a15-a51+2*(a12+a21+a45+a54-a41-a52-a14-a25) + 4*(a22+a44-a42-a24));
			}
		}
	}

	public static void process(GrayF32 orig,
							   GrayF32 derivXX,
							   GrayF32 derivYY,
							   GrayF32 derivXY) {
		final float[] data = orig.data;
		final float[] imgX = derivXX.data;
		final float[] imgY = derivYY.data;
		final float[] imgXY = derivXY.data;

		final int width = orig.getWidth() - 2;
		final int height = orig.getHeight() - 2;
		final int strideSrc = orig.getStride();

		for (int y = 2; y < height; y++) {
			int indexSrc = orig.startIndex + orig.stride * y + 2;
			final int endX = indexSrc + width-2;

			int indexX = derivXX.startIndex + derivXX.stride * y + 2;
			int indexY = derivYY.startIndex + derivYY.stride * y + 2;
			int indexXY = derivXY.startIndex + derivXY.stride * y + 2;

			for (; indexSrc < endX; indexSrc++) {
				float a11 = data[indexSrc - 2*strideSrc - 2];
				float a12 = data[indexSrc - 2*strideSrc - 1];
				float a13 = data[indexSrc - 2*strideSrc    ];
				float a14 = data[indexSrc - 2*strideSrc + 1];
				float a15 = data[indexSrc - 2*strideSrc + 2];
				float a21 = data[indexSrc -   strideSrc - 2];
				float a22 = data[indexSrc -   strideSrc - 1];
				float a23 = data[indexSrc -   strideSrc    ];
				float a24 = data[indexSrc -   strideSrc + 1];
				float a25 = data[indexSrc -   strideSrc + 2];
				float a31 = data[indexSrc               - 2];
				float a32 = data[indexSrc               - 1];
				float a33 = data[indexSrc                  ];
				float a34 = data[indexSrc               + 1];
				float a35 = data[indexSrc               + 2];
				float a41 = data[indexSrc +   strideSrc - 2];
				float a42 = data[indexSrc +   strideSrc - 1];
				float a43 = data[indexSrc +   strideSrc    ];
				float a44 = data[indexSrc +   strideSrc + 1];
				float a45 = data[indexSrc +   strideSrc + 2];
				float a51 = data[indexSrc + 2*strideSrc - 2];
				float a52 = data[indexSrc + 2*strideSrc - 1];
				float a53 = data[indexSrc + 2*strideSrc    ];
				float a54 = data[indexSrc + 2*strideSrc + 1];
				float a55 = data[indexSrc + 2*strideSrc + 2];


				imgY[indexY++]   = (a11+a15+a51+a55+4*(a12+a52+a14+a54)+6*(a13+a53)-2*(a31+a35)-8*(a32+a34)-12*a33);
				imgX[indexX++]   = (a11+a51+a15+a55+4*(a21+a25+a41+a45)+6*(a31+a35)-2*(a13+a53)-8*(a23+a43)-12*a33);
				imgXY[indexXY++] = (a11+a55-a15-a51+2*(a12+a21+a45+a54-a41-a52-a14-a25) + 4*(a22+a44-a42-a24));
			}
		}
	}

}
