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

package boofcv.gui.image;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.misc.GImageStatistics;
import boofcv.alg.misc.ImageStatistics;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.ConvertRaster;
import boofcv.struct.image.*;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;

/**
 * Renders different primitive image types into a BufferedImage for visualization purposes.
 *
 * @author Peter Abeles
 */
public class VisualizeImageData {

	public static BufferedImage standard( ImageGray<?> src, BufferedImage dst ) {
		if (src.getDataType().isInteger()) {
			GrayI srcInt = (GrayI)src;

			if (src.getDataType().isSigned()) {
				dst = checkInputs(src, dst);
				double max = GImageStatistics.maxAbs(srcInt);
				colorizeSign(srcInt, dst, (int)max);
			} else {
				if (src.getDataType().getNumBits() == 8) {
					dst = ConvertBufferedImage.convertTo((GrayU8)src, dst);
				} else {
					double max = GImageStatistics.maxAbs(srcInt);
					dst = grayUnsigned(srcInt, dst, (int)max);
				}
			}
		} else if (GrayF32.class.isAssignableFrom(src.getClass())) {
			GrayF32 img = (GrayF32)src;
			float max = ImageStatistics.maxAbs(img);

			boolean hasNegative = false;
			for (int i = 0; i < img.getHeight(); i++) {
				for (int j = 0; j < img.getWidth(); j++) {
					if (img.get(j, i) < 0) {
						hasNegative = true;
						break;
					}
				}
			}

			if (hasNegative)
				colorizeSign(img, dst, (int)max);
			else
				grayMagnitude((GrayF32)src, dst, max);
		}

		return dst;
	}

	/**
	 * <p>
	 * Renders a colored image where the color indicates the sign and intensity its magnitude. The input is divided
	 * by normalize to render it in the appropriate scale.
	 * </p>
	 *
	 * @param src Input single band image.
	 * @param dst Where the image is rendered into. If null a new BufferedImage will be created and return.
	 * @param normalize Used to normalize the input image. If &le; 0 then the max value will be used
	 * @return Rendered image.
	 */
	public static BufferedImage colorizeSign( ImageGray src, @Nullable BufferedImage dst, double normalize ) {
		dst = checkInputs(src, dst);

		if (normalize <= 0) {
			normalize = GImageStatistics.maxAbs(src);
		}

		if (normalize == 0) {
			// sets the output to black
			ConvertBufferedImage.convertTo(src, dst, true);
			return dst;
		}

		if (src.getClass().isAssignableFrom(GrayF32.class)) {
			colorizeSign((GrayF32)src, dst, (float)normalize);
		} else {
			colorizeSign((GrayI)src, dst, (int)normalize);
		}

		return dst;
	}

	private static void colorizeSign( GrayI src, BufferedImage dst, int normalize ) {
		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				int v = src.get(x, y);

				int rgb;
				if (v > 0) {
					rgb = ((255*v/normalize) << 16);
				} else {
					rgb = -((255*v/normalize) << 8);
				}
				dst.setRGB(x, y, rgb);
			}
		}
	}

	public static BufferedImage grayUnsigned( GrayI src, BufferedImage dst, int normalize ) {
		dst = checkInputs(src, dst);

		if (src.getDataType().isSigned())
			throw new IllegalArgumentException("Can only convert unsigned images.");

		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				int v = src.get(x, y);

				int rgb = 255*v/normalize;

				dst.setRGB(x, y, rgb << 16 | rgb << 8 | rgb);
			}
		}

		return dst;
	}

	/**
	 * <p>
	 * Renders a gray scale image of the input image's intensity.<br>
	 * <br>
	 * dst(i,j) = 255*abs(src(i,j))/normalize
	 * </p>
	 *
	 * @param src Input single band image.
	 * @param dst Where the image is rendered into. If null a new BufferedImage will be created and return.
	 * @param normalize Used to normalize the input image. If < 0 then this value is automatically computed.
	 * @return Rendered image.
	 */
	public static BufferedImage grayMagnitude( ImageGray src, @Nullable BufferedImage dst, double normalize ) {
		if (normalize < 0)
			normalize = GImageStatistics.maxAbs(src);

		dst = checkInputs(src, dst);

		if (src.getDataType().isInteger()) {
			grayMagnitude((GrayI)src, dst, (int)normalize);
		} else if (src instanceof GrayF32) {
			grayMagnitude((GrayF32)src, dst, (float)normalize);
		} else if (src instanceof GrayF64) {
			grayMagnitude((GrayF64)src, dst, (float)normalize);
		} else {
			throw new RuntimeException("Unsupported type");
		}

		return dst;
	}

	/**
	 * <p>
	 * Renders a gray scale image using color values from cold to hot.
	 * </p>
	 *
	 * @param src Input single band image.
	 * @param dst Where the image is rendered into. If null a new BufferedImage will be created and return.
	 * @param normalize Used to normalize the input image.
	 */
	public static void grayMagnitudeTemp( ImageGray src, BufferedImage dst, double normalize ) {
		if (normalize < 0)
			normalize = GImageStatistics.maxAbs(src);

		dst = checkInputs(src, dst);

		if (src.getDataType().isInteger()) {
			grayMagnitudeTemp((GrayI)src, dst, (int)normalize);
		} else {
			throw new RuntimeException("Add support");
		}
	}

	private static void grayMagnitude( GrayI src, BufferedImage dst, int maxValue ) {
		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				int v = Math.abs(src.get(x, y));

				int rgb = 255*v/maxValue;

				dst.setRGB(x, y, rgb << 16 | rgb << 8 | rgb);
			}
		}
	}

	private static void grayMagnitudeTemp( GrayI src, BufferedImage dst, int maxValue ) {
//		int halfValue = maxValue / 2 + maxValue % 2;

		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				int v = Math.abs(src.get(x, y));

				int r, b;

				if (v == 0) {
					r = b = 0;
				} else {
					r = 255*v/maxValue;
					b = 255*(maxValue - v)/maxValue;
				}

				dst.setRGB(x, y, r << 16 | b);
			}
		}
	}

	/**
	 * <p>
	 * Renders a gray scale image using color values from cold to hot.
	 * </p>
	 *
	 * @param disparity Input disparity image
	 * @param dst Where the image is rendered into. If null a new BufferedImage will be created and return.
	 * @param disparityRange Number of possible disparity values
	 * @param invalidColor RGB value for invalid pixels. Try 0xFF << 8 for green
	 * @return Rendered image.
	 */
	public static BufferedImage disparity( ImageGray disparity, @Nullable BufferedImage dst,
										   int disparityRange, int invalidColor ) {
		dst = ConvertBufferedImage.checkDeclare(disparity.width, disparity.height, dst, BufferedImage.TYPE_INT_RGB);

		if (disparity.getDataType().isInteger()) {
			return disparity((GrayI)disparity, dst, disparityRange, invalidColor);
		} else if (disparity instanceof GrayF32) {
			return disparity((GrayF32)disparity, dst, disparityRange, invalidColor);
		} else {
			throw new RuntimeException("Add support");
		}
	}

	private static BufferedImage disparity( GrayI src, BufferedImage dst, int range, int invalidColor ) {
		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				int v = src.unsafe_get(x, y);

				if (v >= range) {
					dst.setRGB(x, y, invalidColor);
				} else {
					int r, b;

					if (v == 0) {
						r = b = 0;
					} else {
						r = 255*v/range;
						b = 255*(range - v)/range;
					}

					dst.setRGB(x, y, r << 16 | b);
				}
			}
		}

		return dst;
	}

	private static BufferedImage disparity( GrayF32 src, @Nullable BufferedImage dst,
											int range, int invalidColor ) {
		dst = ConvertBufferedImage.checkDeclare(src.width, src.height, dst, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				float v = src.unsafe_get(x, y);

				if (v >= range) {
					dst.setRGB(x, y, invalidColor);
				} else {
					int r, b;

					if (v == 0) {
						r = b = 0;
					} else {
						r = (int)(255*v/range);
						b = (int)(255*(range - v)/range);
					}

					dst.setRGB(x, y, r << 16 | b);
				}
			}
		}

		return dst;
	}

	private static void colorizeSign( GrayF32 src, BufferedImage dst, float maxAbsValue ) {
		DataBuffer buffer = dst.getRaster().getDataBuffer();
		if (buffer.getDataType() == DataBuffer.TYPE_INT) {
			colorizeSign(src, (DataBufferInt)buffer, maxAbsValue);
		} else {
			for (int y = 0; y < src.height; y++) {
				for (int x = 0; x < src.width; x++) {
					float v = src.get(x, y);

					int rgb;
					if (v > 0) {
						rgb = (int)(255*v/maxAbsValue) << 16;
					} else {
						rgb = (int)(-255*v/maxAbsValue) << 8;
					}
					dst.setRGB(x, y, rgb);
				}
			}
		}
	}

	private static void colorizeSign( GrayF32 src, DataBufferInt buffer, float maxAbsValue ) {
		final float[] srcData = src.data;
		final int[] dstData = buffer.getData();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
		for (int y = 0; y < src.height; y++) {
			int indexSrc = src.startIndex + y*src.stride;
			int indexDst = y*src.width;
			for (int x = 0; x < src.width; x++) {
				float v = srcData[indexSrc++];

				int rgb;
				if (v > 0) {
					rgb = (int)(255*v/maxAbsValue) << 16;
				} else {
					rgb = (int)(-255*v/maxAbsValue) << 8;
				}
				dstData[indexDst++] = 0xFF << 24 | rgb;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static BufferedImage graySign( GrayF32 src, @Nullable BufferedImage dst, float maxAbsValue ) {
		dst = checkInputs(src, dst);

		if (maxAbsValue < 0)
			maxAbsValue = ImageStatistics.maxAbs(src);

		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				float v = src.get(x, y);

				int rgb = 127 + (int)(127*v/maxAbsValue);

				dst.setRGB(x, y, rgb << 16 | rgb << 8 | rgb);
			}
		}

		return dst;
	}

	private static void grayMagnitude( GrayF32 src, BufferedImage dst, float maxAbsValue ) {
		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				float v = Math.abs(src.get(x, y));

				int rgb = (int)(255*v/maxAbsValue);

				dst.setRGB(x, y, rgb << 16 | rgb << 8 | rgb);
			}
		}
	}

	private static void grayMagnitude( GrayF64 src, BufferedImage dst, double maxAbsValue ) {
		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				double v = Math.abs(src.get(x, y));

				int rgb = (int)(255*v/maxAbsValue);

				dst.setRGB(x, y, rgb << 16 | rgb << 8 | rgb);
			}
		}
	}

	/**
	 * If null the dst is declared, otherwise it checks to see if the 'dst' as the same shape as 'src'.
	 *
	 * The returned image will be 8-bit RGB
	 */
	private static BufferedImage checkInputs( ImageBase src, @Nullable BufferedImage dst ) {
		if (dst != null) {
			if (dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight()) {
				dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
//				throw new IllegalArgumentException("image dimension are different. src="
//						+src.width+"x"+src.height+"  dst="+dst.getWidth()+"x"+dst.getHeight());
			}
		} else {
			dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
		}
		return dst;
	}

	/**
	 * Renders two gradients on the same image using two sets of colors, on for each input image.
	 *
	 * @param derivX (Input) Image with positive and negative values.
	 * @param derivY (Input) Image with positive and negative values.
	 * @param maxAbsValue The largest absolute value of any pixel in the image. Set to < 0 if not known.
	 * @return visualized gradient
	 */
	public static BufferedImage colorizeGradient( ImageGray derivX, ImageGray derivY, double maxAbsValue,
												  BufferedImage output ) {
		if (derivX instanceof GrayS16) {
			return colorizeGradient((GrayS16)derivX, (GrayS16)derivY, (int)maxAbsValue, output);
		} else if (derivX instanceof GrayF32) {
			return colorizeGradient((GrayF32)derivX, (GrayF32)derivY, (float)maxAbsValue, output);
		} else {
			throw new IllegalArgumentException("Image type not supported");
		}
	}

	/**
	 * Renders two gradients on the same image using two sets of colors, on for each input image.
	 *
	 * @param derivX (Input) Image with positive and negative values.
	 * @param derivY (Input) Image with positive and negative values.
	 * @param maxAbsValue The largest absolute value of any pixel in the image. Set to < 0 if not known.
	 * @return visualized gradient
	 */
	public static BufferedImage colorizeGradient( GrayS16 derivX, GrayS16 derivY, int maxAbsValue,
												  @Nullable BufferedImage output ) {
		InputSanityCheck.checkSameShape(derivX, derivY);

		if (output == null)
			output = new BufferedImage(derivX.width, derivX.height, BufferedImage.TYPE_INT_RGB);

		WritableRaster raster = output.getRaster();
		DataBufferInt buffer = (DataBufferInt)raster.getDataBuffer();
		int[] outData = buffer.getData();
		int outOffset = ConvertRaster.getOffset(raster);

		if (maxAbsValue < 0) {
			maxAbsValue = ImageStatistics.maxAbs(derivX);
			maxAbsValue = Math.max(maxAbsValue, ImageStatistics.maxAbs(derivY));
		}
		if (maxAbsValue == 0)
			return output;

		int indexOut = outOffset;

		for (int y = 0; y < derivX.height; y++) {
			int indexX = derivX.startIndex + y*derivX.stride;
			int indexY = derivY.startIndex + y*derivY.stride;

			for (int x = 0; x < derivX.width; x++) {
				int valueX = derivX.data[indexX++];
				int valueY = derivY.data[indexY++];

				int r = 0, g = 0, b = 0;

				if (valueX > 0) {
					r = 255*valueX/maxAbsValue;
				} else {
					g = -255*valueX/maxAbsValue;
				}
				if (valueY > 0) {
					b = 255*valueY/maxAbsValue;
				} else {
					int v = -255*valueY/maxAbsValue;
					r += v;
					g += v;
					if (r > 255) r = 255;
					if (g > 255) g = 255;
				}

				outData[indexOut++] = r << 16 | g << 8 | b;
			}
		}

		return output;
	}

	/**
	 * Renders two gradients on the same image using two sets of colors, on for each input image.
	 *
	 * @param derivX (Input) Image with positive and negative values.
	 * @param derivY (Input) Image with positive and negative values.
	 * @param maxAbsValue The largest absolute value of any pixel in the image. Set to < 0 if not known.
	 * @return visualized gradient
	 */
	public static BufferedImage colorizeGradient( GrayF32 derivX, GrayF32 derivY, float maxAbsValue,
												  @Nullable BufferedImage output ) {
		InputSanityCheck.checkSameShape(derivX, derivY);

		output = ConvertBufferedImage.checkDeclare(derivX.width, derivX.height, output, BufferedImage.TYPE_INT_RGB);

		WritableRaster raster = output.getRaster();
		DataBufferInt buffer = (DataBufferInt)raster.getDataBuffer();
		int[] outData = buffer.getData();
		int outOffset = ConvertRaster.getOffset(raster);

		if (maxAbsValue < 0) {
			maxAbsValue = ImageStatistics.maxAbs(derivX);
			maxAbsValue = Math.max(maxAbsValue, ImageStatistics.maxAbs(derivY));
		}
		if (maxAbsValue == 0)
			return output;

		int indexOut = outOffset;

		for (int y = 0; y < derivX.height; y++) {
			int indexX = derivX.startIndex + y*derivX.stride;
			int indexY = derivY.startIndex + y*derivY.stride;

			for (int x = 0; x < derivX.width; x++) {
				float valueX = derivX.data[indexX++];
				float valueY = derivY.data[indexY++];

				int r = 0, g = 0, b = 0;

				if (valueX > 0) {
					r = (int)(255*valueX/maxAbsValue);
				} else {
					g = -(int)(255*valueX/maxAbsValue);
				}
				if (valueY > 0) {
					b = (int)(255*valueY/maxAbsValue);
				} else {
					int v = -(int)(255*valueY/maxAbsValue);
					r += v;
					g += v;
					if (r > 255) r = 255;
					if (g > 255) g = 255;
				}

				outData[indexOut++] = r << 16 | g << 8 | b;
			}
		}

		return output;
	}
}
