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

package boofcv.gui.image;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.misc.GImageStatistics;
import boofcv.alg.misc.ImageStatistics;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.*;
import sun.awt.image.IntegerInterleavedRaster;

import java.awt.image.BufferedImage;

/**
 * Renders different primitive image types into a BufferedImage for visualization purposes.
 *
 * @author Peter Abeles
 */
public class VisualizeImageData {

	public static BufferedImage standard(ImageGray<?> src, BufferedImage dst) {
		if (src.getDataType().isInteger()) {
			GrayI srcInt = (GrayI) src;

			if (src.getDataType().isSigned()) {
				double max = GImageStatistics.maxAbs(srcInt);
				return colorizeSign(srcInt, dst, (int) max);
			} else {
				if (src.getDataType().getNumBits() == 8) {
					dst = ConvertBufferedImage.convertTo((GrayU8) src, dst);
				} else {
					double max = GImageStatistics.maxAbs(srcInt);
					dst = grayUnsigned(srcInt, dst, (int) max);
				}
			}
		} else if (GrayF32.class.isAssignableFrom(src.getClass())) {
			GrayF32 img = (GrayF32) src;
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
				return colorizeSign(img, dst, (int) max);
			else
				return grayMagnitude((GrayF32) src, dst, max);
		}

		return dst;
	}

	/**
	 * <p>
	 * Renders a colored image where the color indicates the sign and intensity its magnitude.   The input is divided
	 * by normalize to render it in the appropriate scale.
	 * </p>
	 *
	 * @param src       Input single band image.
	 * @param dst       Where the image is rendered into.  If null a new BufferedImage will be created and return.
	 * @param normalize Used to normalize the input image. If &le; 0 then the max value will be used
	 * @return Rendered image.
	 */
	public static BufferedImage colorizeSign(ImageGray src, BufferedImage dst, double normalize) {
		dst = checkInputs(src, dst);

		if (normalize <= 0) {
			normalize = GImageStatistics.maxAbs(src);
		}

		if (normalize == 0) {
			// sets the output to black
			ConvertBufferedImage.convertTo(src,dst,true);
			return dst;
		}

		if (src.getClass().isAssignableFrom(GrayF32.class)) {
			return colorizeSign((GrayF32) src, dst, (float) normalize);
		} else {
			return colorizeSign((GrayI) src, dst, (int) normalize);
		}
	}

	private static BufferedImage colorizeSign(GrayI src, BufferedImage dst, int normalize) {
		dst = checkInputs(src, dst);

		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				int v = src.get(x, y);

				int rgb;
				if (v > 0) {
					rgb = ((255 * v / normalize) << 16);
				} else {
					rgb = -((255 * v / normalize) << 8);
				}
				dst.setRGB(x, y, rgb);
			}
		}

		return dst;
	}

	public static BufferedImage grayUnsigned(GrayI src, BufferedImage dst, int normalize) {
		dst = checkInputs(src, dst);

		if (src.getDataType().isSigned())
			throw new IllegalArgumentException("Can only convert unsigned images.");

		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				int v = src.get(x, y);

				int rgb = 255 * v / normalize;

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
	 * @param src       Input single band image.
	 * @param dst       Where the image is rendered into.  If null a new BufferedImage will be created and return.
	 * @param normalize Used to normalize the input image. If < 0 then this value is automatically computed.
	 * @return Rendered image.
	 */
	public static BufferedImage grayMagnitude(ImageGray src, BufferedImage dst, double normalize) {
		if (normalize < 0)
			normalize = GImageStatistics.maxAbs(src);

		dst = checkInputs(src, dst);

		if (src.getDataType().isInteger()) {
			return grayMagnitude((GrayI) src, dst, (int) normalize);
		} else if( src instanceof GrayF32){
			return grayMagnitude((GrayF32) src, dst, (float) normalize);
		} else if( src instanceof GrayF64){
			return grayMagnitude((GrayF64) src, dst, (float) normalize);
		} else {
			throw new RuntimeException("Unsupported type");
		}
	}

	/**
	 * <p>
	 * Renders a gray scale image using color values from cold to hot.
	 * </p>
	 *
	 * @param src       Input single band image.
	 * @param dst       Where the image is rendered into.  If null a new BufferedImage will be created and return.
	 * @param normalize Used to normalize the input image.
	 * @return Rendered image.
	 */
	public static BufferedImage grayMagnitudeTemp(ImageGray src, BufferedImage dst, double normalize) {
		if (normalize < 0)
			normalize = GImageStatistics.maxAbs(src);

		dst = checkInputs(src, dst);

		if (src.getDataType().isInteger()) {
			return grayMagnitudeTemp((GrayI) src, dst, (int) normalize);
		} else {
			throw new RuntimeException("Add support");
		}
	}

	private static BufferedImage grayMagnitude(GrayI src, BufferedImage dst, int maxValue) {
		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				int v = Math.abs(src.get(x, y));

				int rgb = 255 * v / maxValue;

				dst.setRGB(x, y, rgb << 16 | rgb << 8 | rgb);
			}
		}

		return dst;
	}

	private static BufferedImage grayMagnitudeTemp(GrayI src, BufferedImage dst, int maxValue) {
		int halfValue = maxValue / 2 + maxValue % 2;

		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				int v = Math.abs(src.get(x, y));

				int r, b;

				if (v >= halfValue) {
					r = 255 * (v - halfValue) / halfValue;
					b = 0;
				} else {
					r = 0;
					b = 255 * v / halfValue;
				}

				if (v == 0) {
					r = b = 0;
				} else {
					r = 255 * v / maxValue;
					b = 255 * (maxValue - v) / maxValue;
				}

				dst.setRGB(x, y, r << 16 | b);
			}
		}

		return dst;
	}

	/**
	 * <p>
	 * Renders a gray scale image using color values from cold to hot.
	 * </p>
	 *
	 * @param disparity    Input disparity image
	 * @param dst          Where the image is rendered into.  If null a new BufferedImage will be created and return.
	 * @param minDisparity Minimum disparity that can be computed
	 * @param maxDisparity Maximum disparity that can be computed
	 * @param invalidColor RGB value for invalid pixels.  Try 0xFF << 8 for green
	 * @return Rendered image.
	 */
	public static BufferedImage disparity(ImageGray disparity, BufferedImage dst,
										  int minDisparity, int maxDisparity, int invalidColor) {
		if( dst == null )
			dst = new BufferedImage(disparity.getWidth(),disparity.getHeight(),BufferedImage.TYPE_INT_RGB);

		if (disparity.getDataType().isInteger()) {
			return disparity((GrayI) disparity, dst, minDisparity, maxDisparity, invalidColor);
		} else if (disparity instanceof GrayF32) {
			return disparity((GrayF32) disparity, dst, minDisparity, maxDisparity, invalidColor);
		} else {
			throw new RuntimeException("Add support");
		}
	}

	private static BufferedImage disparity(GrayI src, BufferedImage dst,
										   int minValue, int maxValue, int invalidColor) {
		int range = maxValue - minValue;

		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				int v = src.unsafe_get(x, y);

				if (v > range) {
					dst.setRGB(x, y, invalidColor);
				} else {
					int r, b;

					if (v == 0) {
						r = b = 0;
					} else {
						r = 255 * v / maxValue;
						b = 255 * (maxValue - v) / maxValue;
					}

					dst.setRGB(x, y, r << 16 | b);
				}
			}
		}

		return dst;
	}

	private static BufferedImage disparity(GrayF32 src, BufferedImage dst,
										   int minValue, int maxValue, int invalidColor) {
		float range = maxValue - minValue;

		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				float v = src.unsafe_get(x, y);

				if (v > range) {
					dst.setRGB(x, y, invalidColor);
				} else {
					int r, b;

					if (v == 0) {
						r = b = 0;
					} else {
						r = (int) (255 * v / maxValue);
						b = (int) (255 * (maxValue - v) / maxValue);
					}

					dst.setRGB(x, y, r << 16 | b);
				}
			}
		}

		return dst;
	}

	private static BufferedImage colorizeSign(GrayF32 src, BufferedImage dst, float maxAbsValue) {
		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				float v = src.get(x, y);

				int rgb;
				if (v > 0) {
					rgb = (int) (255 * v / maxAbsValue) << 16;
				} else {
					rgb = (int) (-255 * v / maxAbsValue) << 8;
				}
				dst.setRGB(x, y, rgb);
			}
		}

		return dst;
	}

	public static BufferedImage graySign(GrayF32 src, BufferedImage dst, float maxAbsValue) {
		dst = checkInputs(src, dst);

		if (maxAbsValue < 0)
			maxAbsValue = ImageStatistics.maxAbs(src);

		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				float v = src.get(x, y);

				int rgb = 127 + (int) (127 * v / maxAbsValue);

				dst.setRGB(x, y, rgb << 16 | rgb << 8 | rgb);
			}
		}

		return dst;
	}

	private static BufferedImage grayMagnitude(GrayF32 src, BufferedImage dst, float maxAbsValue) {
		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				float v = Math.abs(src.get(x, y));

				int rgb = (int) (255 * v / maxAbsValue);

				dst.setRGB(x, y, rgb << 16 | rgb << 8 | rgb);
			}
		}

		return dst;
	}

	private static BufferedImage grayMagnitude(GrayF64 src, BufferedImage dst, double maxAbsValue) {
		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				double v = Math.abs(src.get(x, y));

				int rgb = (int) (255 * v / maxAbsValue);

				dst.setRGB(x, y, rgb << 16 | rgb << 8 | rgb);
			}
		}

		return dst;
	}

	/**
	 * If null the dst is declared, otherwise it checks to see if the 'dst' as the same shape as 'src'.
	 *
	 * The returned image will be 8-bit RGB
	 */
	private static BufferedImage checkInputs(ImageBase src, BufferedImage dst) {
		if (dst != null) {
			if (dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight()) {
				throw new IllegalArgumentException("image dimension are different");
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
	 * @param maxAbsValue  The largest absolute value of any pixel in the image.  Set to < 0 if not known.
	 * @return visualized gradient
	 */
	public static BufferedImage colorizeGradient(ImageGray derivX , ImageGray derivY , double maxAbsValue ){
		if( derivX instanceof GrayS16) {
			return colorizeGradient((GrayS16)derivX,(GrayS16)derivY,(int)maxAbsValue);
		} else if( derivX instanceof GrayF32) {
			return colorizeGradient((GrayF32)derivX,(GrayF32)derivY,(int)maxAbsValue);
		} else {
			throw new IllegalArgumentException("Image type not supported");
		}
	}

	/**
	 * Renders two gradients on the same image using two sets of colors, on for each input image.
	 *
	 * @param derivX (Input) Image with positive and negative values.
	 * @param derivY (Input) Image with positive and negative values.
	 * @param maxAbsValue  The largest absolute value of any pixel in the image.  Set to < 0 if not known.
	 * @return visualized gradient
	 */
	public static BufferedImage colorizeGradient(GrayS16 derivX , GrayS16 derivY , int maxAbsValue ) {
		InputSanityCheck.checkSameShape(derivX,derivY);

		BufferedImage output = new BufferedImage(derivX.width,derivX.height,BufferedImage.TYPE_INT_RGB);

		IntegerInterleavedRaster outputRaster = (IntegerInterleavedRaster)output.getRaster();
		int[] outData = outputRaster.getDataStorage();
		int outStride = outputRaster.getScanlineStride();
		int outOffset = outputRaster.getDataOffset(0)-outputRaster.getPixelStride()+1;

		if( maxAbsValue < 0 ) {
			maxAbsValue = ImageStatistics.maxAbs(derivX);
			maxAbsValue = Math.max(maxAbsValue, ImageStatistics.maxAbs(derivY));
		}
		if( maxAbsValue == 0 )
			return output;

		int indexOut = outOffset;

		for( int y = 0; y < derivX.height; y++ ) {
			int indexX = derivX.startIndex + y*derivX.stride;
			int indexY = derivY.startIndex + y*derivY.stride;

			for( int x = 0; x < derivX.width; x++ ) {
				int valueX = derivX.data[ indexX++ ];
				int valueY = derivY.data[ indexY++ ];

				int r=0,g=0,b=0;

				if( valueX > 0 ) {
					r = 255*valueX/maxAbsValue;
				} else {
					g = -255*valueX/maxAbsValue;
				}
				if( valueY > 0 ) {
					b = 255*valueY/maxAbsValue;
				} else {
					int v = -255*valueY/maxAbsValue;
					r += v;
					g += v;
					if( r > 255 ) r = 255;
					if( g > 255 ) g = 255;
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
	 * @param maxAbsValue  The largest absolute value of any pixel in the image.  Set to < 0 if not known.
	 * @return visualized gradient
	 */
	public static BufferedImage colorizeGradient(GrayF32 derivX , GrayF32 derivY , float maxAbsValue ) {
		InputSanityCheck.checkSameShape(derivX,derivY);

		BufferedImage output = new BufferedImage(derivX.width,derivX.height,BufferedImage.TYPE_INT_RGB);

		IntegerInterleavedRaster outputRaster = (IntegerInterleavedRaster)output.getRaster();
		int[] outData = outputRaster.getDataStorage();
		int outStride = outputRaster.getScanlineStride();
		int outOffset = outputRaster.getDataOffset(0)-outputRaster.getPixelStride()+1;


		if( maxAbsValue < 0 ) {
			maxAbsValue = ImageStatistics.maxAbs(derivX);
			maxAbsValue = Math.max(maxAbsValue, ImageStatistics.maxAbs(derivY));
		}
		if( maxAbsValue == 0 )
			return output;

		int indexOut = outOffset;

		for( int y = 0; y < derivX.height; y++ ) {
			int indexX = derivX.startIndex + y*derivX.stride;
			int indexY = derivY.startIndex + y*derivY.stride;

			for( int x = 0; x < derivX.width; x++ ) {
				float valueX = derivX.data[ indexX++ ];
				float valueY = derivY.data[ indexY++ ];

				int r=0,g=0,b=0;

				if( valueX > 0 ) {
					r = (int)(255*valueX/maxAbsValue);
				} else {
					g = -(int)(255*valueX/maxAbsValue);
				}
				if( valueY > 0 ) {
					b = (int)(255*valueY/maxAbsValue);
				} else {
					int v = -(int)(255*valueY/maxAbsValue);
					r += v;
					g += v;
					if( r > 255 ) r = 255;
					if( g > 255 ) g = 255;
				}

				outData[indexOut++] = r << 16 | g << 8 | b;
			}
		}

		return output;
	}
}
