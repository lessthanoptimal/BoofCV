/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.alg.misc.GPixelMath;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageInteger;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * Renders different primitive image types into a BufferedImage for visualization purposes.
 *
 * @author Peter Abeles
 */
public class VisualizeImageData {

	public static BufferedImage standard( ImageSingleBand<?> src , BufferedImage dst )
	{
		if( src.getTypeInfo().isInteger() ) {
			ImageInteger srcInt = (ImageInteger)src;

			if( src.getTypeInfo().isSigned() ) {
				double max = GPixelMath.maxAbs(srcInt);
				return colorizeSign(srcInt,dst,(int)max);
			} else {
				if( src.getTypeInfo().getNumBits() == 8 ) {
					dst = ConvertBufferedImage.convertTo((ImageUInt8)src,dst);
				} else {
					double max = GPixelMath.maxAbs(srcInt);
					dst = grayUnsigned(srcInt,dst,(int)max);
				}
			}
		} else if( ImageFloat32.class.isAssignableFrom(src.getClass()) ) {
			ImageFloat32 img = (ImageFloat32)src;
			float max = PixelMath.maxAbs(img);

			boolean hasNegative = false;
			for( int i = 0; i < img.getHeight(); i++ ) {
				for( int j = 0; j < img.getWidth(); j++ ) {
					if( img.get(j,i) < 0 ) {
						hasNegative = true;
						break;
					}
				}
			}

			if( hasNegative )
				return colorizeSign(img,dst,(int)max);
			else
				return grayMagnitude((ImageFloat32)src,dst,max);
		}

		return dst;
	}

	/**
	 * <p>
	 * Renders a colored image where the color indicates the sign and intensity its magnitude.   The input is divided
	 * by normalize to render it in the appropriate scale.
	 * </p>
	 *
	 * @param src Input single band image.
	 * @param dst Where the image is rendered into.  If null a new BufferedImage will be created and return.
	 * @param normalize Used to normalize the input image.
	 * @return Rendered image.
	 */
	public static BufferedImage colorizeSign( ImageSingleBand src, BufferedImage dst, double normalize ) {
		dst = ConvertBufferedImage.checkInputs(src, dst);

		if( normalize <= 0 ) {
			normalize = GPixelMath.maxAbs(src);
		}

		if( normalize == 0 )
			return dst;

		if( src.getClass().isAssignableFrom(ImageFloat32.class)) {
			return colorizeSign((ImageFloat32)src,dst,(float)normalize);
		} else {
			return colorizeSign((ImageInteger)src,dst,(int)normalize);
		}
	}

	private static BufferedImage colorizeSign( ImageInteger src, BufferedImage dst, int normalize ) {

		for( int y = 0; y < src.height; y++ ) {
			for( int x = 0; x < src.width; x++ ) {
				int v = src.get(x,y);

				int rgb;
				if( v > 0 ) {
					rgb = (255*v/normalize) << 16;
				} else {
					rgb = (-255*v/normalize) << 8;
				}
				dst.setRGB(x,y,rgb);
			}
		}

		return dst;
	}

	public static BufferedImage grayUnsigned( ImageInteger src, BufferedImage dst, int normalize )
	{
		dst = ConvertBufferedImage.checkInputs(src, dst);

		if( src.getTypeInfo().isSigned() )
			throw new IllegalArgumentException("Can only convert unsigned images.");

		for( int y = 0; y < src.height; y++ ) {
			for( int x = 0; x < src.width; x++ ) {
				int v = src.get(x,y);

				int rgb = 255 *v / normalize;

				dst.setRGB(x,y,rgb << 16 | rgb << 8 | rgb  );
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
	 * @param dst Where the image is rendered into.  If null a new BufferedImage will be created and return.
	 * @param normalize Used to normalize the input image.
	 * @return Rendered image.
	 */
	public static BufferedImage grayMagnitude( ImageSingleBand src, BufferedImage dst, double normalize )
	{
		if( normalize < 0 )
			normalize = GPixelMath.maxAbs(src);

		dst = ConvertBufferedImage.checkInputs(src, dst);

		if( src.getTypeInfo().isInteger() ) {
			return grayMagnitude((ImageInteger)src,dst,(int)normalize);
		} else {
			return grayMagnitude((ImageFloat32)src,dst,(float)normalize);
		}
	}

	private static BufferedImage grayMagnitude( ImageInteger src, BufferedImage dst, int maxValue )
	{
		for( int y = 0; y < src.height; y++ ) {
			for( int x = 0; x < src.width; x++ ) {
				int v = Math.abs(src.get(x,y));

				int rgb = 255 *v / maxValue;

				dst.setRGB(x,y,rgb << 16 | rgb << 8 | rgb  );
			}
		}

		return dst;
	}

	private static BufferedImage colorizeSign( ImageFloat32 src, BufferedImage dst, float maxAbsValue ) {
		for( int y = 0; y < src.height; y++ ) {
			for( int x = 0; x < src.width; x++ ) {
				float v = src.get(x,y);

				int rgb;
				if( v > 0 ) {
					rgb = (int)(255*v/maxAbsValue) << 16;
				} else {
					rgb = (int)(-255*v/maxAbsValue) << 8;
				}
				dst.setRGB(x,y,rgb);
			}
		}

		return dst;
	}

	public static BufferedImage graySign( ImageFloat32 src, BufferedImage dst, float maxAbsValue )
	{
		dst = ConvertBufferedImage.checkInputs(src, dst);

		if( maxAbsValue < 0 )
			maxAbsValue = PixelMath.maxAbs(src);

		for( int y = 0; y < src.height; y++ ) {
			for( int x = 0; x < src.width; x++ ) {
				float v = src.get(x,y);

				int rgb = 127+(int)(127 *v / maxAbsValue);

				dst.setRGB(x,y,rgb << 16 | rgb << 8 | rgb  );
			}
		}

		return dst;
	}

	private static BufferedImage grayMagnitude( ImageFloat32 src, BufferedImage dst, float maxAbsValue )
	{
		for( int y = 0; y < src.height; y++ ) {
			for( int x = 0; x < src.width; x++ ) {
				float v = Math.abs(src.get(x,y));

				int rgb = (int)(255 *v / maxAbsValue);

				dst.setRGB(x,y,rgb << 16 | rgb << 8 | rgb  );
			}
		}

		return dst;
	}
}
