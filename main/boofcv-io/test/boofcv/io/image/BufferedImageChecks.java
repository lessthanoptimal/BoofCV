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

package boofcv.io.image;

import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageMultiBand;
import boofcv.struct.image.*;

import java.awt.image.*;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class BufferedImageChecks {
	public static void checkIdentical(BufferedImage imgA, BufferedImage imgB ) {
		if(imgA.getWidth() != imgB.getWidth()) throw new IllegalArgumentException("Widths not equal");
		if(imgA.getHeight() != imgB.getHeight()) throw new IllegalArgumentException("Heights not equal");
		if(imgA.getType() != imgB.getType()) throw new IllegalArgumentException("Types not equal");

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgB.getWidth(); x++) {
				if( imgA.getRGB(x,y) != imgB.getRGB(x,y) )
					throw new IllegalArgumentException("RGB values not identical at "+x+" "+y);
			}
		}
	}

	public static void checkEquals(BufferedImage imgA, ImageBase imgB, boolean boofcvBandOrder, double tol ) {
		if (GrayU8.class == imgB.getClass()) {
			checkEquals(imgA, (GrayU8) imgB);
		} else if (GrayI16.class.isAssignableFrom(imgB.getClass())) {
			checkEquals( imgA, (GrayI16) imgB );
		} else if (GrayF32.class == imgB.getClass()) {
			checkEquals(imgA, (GrayF32) imgB, (float) tol);
		} else if (ImageInterleaved.class.isInstance(imgB) ) {
			checkEquals(imgA, (ImageMultiBand) imgB, boofcvBandOrder, (float)tol );
		} else if (Planar.class == imgB.getClass()) {
			checkEquals(imgA, (ImageMultiBand) imgB, boofcvBandOrder,(float) tol);
		} else {
			throw new IllegalArgumentException("Unknown");
		}
	}

	/**
	 * Checks to see if the BufferedImage has the same intensity values as the GrayU8
	 *
	 * @param imgA BufferedImage
	 * @param imgB GrayU8
	 */
	public static void checkEquals(BufferedImage imgA, GrayU8 imgB) {

		WritableRaster raster = imgA.getRaster();
		if (raster.getDataBuffer().getDataType() == DataBuffer.TYPE_BYTE &&
				ConvertBufferedImage.isKnownByteFormat(imgA) ) {

			if (raster.getNumBands() == 1) {

				byte []dataA = ((DataBufferByte)raster.getDataBuffer()).getData();
				int strideA = ConvertRaster.stride(raster);
				int offsetA = ConvertRaster.getOffset(raster);

				check(dataA,strideA,offsetA,imgB);
				return;
			}
		}

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				int rgb = imgA.getRGB(x, y);

				int grayA = (byte) ((((rgb >>> 16) & 0xFF) + ((rgb >>> 8) & 0xFF) + (rgb & 0xFF)) / 3);
				int grayB = imgB.get(x, y);
				if (!imgB.getDataType().isSigned())
					grayA &= 0xFF;

				if (Math.abs(grayA - grayB) != 0) {
					throw new RuntimeException("images are not equal: (" + x + " , " + y + ") A = " + grayA + " B = " + grayB);
				}
			}
		}
	}

	static void check( byte []dataA , int strideA , int offsetA , GrayU8 imgB ) {
		// handle a special case where the RGB conversion is screwed
		for (int i = 0; i < imgB.getHeight(); i++) {
			for (int j = 0; j < imgB.getWidth(); j++) {
				int valB = imgB.get(j, i);
				int valA = dataA[ offsetA + i*strideA + j];
				if (!imgB.getDataType().isSigned())
					valA &= 0xFF;

				if (valA != valB)
					throw new RuntimeException("Images are not equal: "+valA+" "+valB);
			}
		}
	}

	/**
	 * Checks to see if the BufferedImage has the same intensity values as the GrayI16
	 *
	 * @param imgA BufferedImage
	 * @param imgB GrayI16
	 */
	public static void checkEquals(BufferedImage imgA, GrayI16 imgB) {

		WritableRaster raster = imgA.getRaster();
		if (raster.getDataBuffer().getDataType() == DataBuffer.TYPE_BYTE &&
				ConvertBufferedImage.isKnownByteFormat(imgA) ) {

			if (raster.getNumBands() == 1) {
				byte []data = ((DataBufferByte)raster.getDataBuffer()).getData();
				int strideA = ConvertRaster.stride(raster);
				int offsetA = ConvertRaster.getOffset(raster);

				// handle a special case where the RGB conversion is screwed
				for (int i = 0; i < imgA.getHeight(); i++) {
					for (int j = 0; j < imgA.getWidth(); j++) {
						int valB = imgB.get(j, i);
						int valA = data[ offsetA + i*strideA + j];
						if (!imgB.getDataType().isSigned())
							valA &= 0xFFFF;

						if (valA != valB)
							throw new RuntimeException("Images are not equal: "+valA+" "+valB);
					}
				}
				return;
			}
		} else if (raster.getDataBuffer().getDataType() == DataBuffer.TYPE_USHORT) {

			if (raster.getNumBands() == 1) {
				short []data = ((DataBufferUShort)raster.getDataBuffer()).getData();
				int strideA = ConvertRaster.stride(raster);
				int offsetA = ConvertRaster.getOffset(raster);

				// handle a special case where the RGB conversion is screwed
				for (int i = 0; i < imgA.getHeight(); i++) {
					for (int j = 0; j < imgA.getWidth(); j++) {
						int valB = imgB.get(j, i);
						int valA = data[ offsetA + i*strideA + j];
						if (!imgB.getDataType().isSigned())
							valA &= 0xFFFF;

						if (valA != valB)
							throw new RuntimeException("Images are not equal: "+valA+" "+valB);
					}
				}
			}
		} else {
			for (int y = 0; y < imgA.getHeight(); y++) {
				for (int x = 0; x < imgA.getWidth(); x++) {
					int rgb = imgA.getRGB(x, y);

					int gray = ((((rgb >>> 16) & 0xFF) + ((rgb >>> 8) & 0xFF) + (rgb & 0xFF)) / 3);
					int grayB = imgB.get(x, y);
					if (!imgB.getDataType().isSigned())
						gray &= 0xFFFF;

					if (Math.abs(gray - grayB) != 0) {
						throw new RuntimeException("images are not equal: (" + x + " , " + y + ") A = " + gray + " B = " + grayB);
					}
				}
			}
		}
	}

	/**
	 * Checks to see if the BufferedImage has the same intensity values as the GrayF32
	 *
	 * @param imgA BufferedImage
	 * @param imgB GrayF32
	 */
	public static void checkEquals(BufferedImage imgA, GrayF32 imgB, float tol) {

		WritableRaster raster = imgA.getRaster();
		if (raster.getDataBuffer().getDataType() == DataBuffer.TYPE_BYTE &&
				ConvertBufferedImage.isKnownByteFormat(imgA)  ) {

			if (raster.getNumBands() == 1) {
				byte []data = ((DataBufferByte)raster.getDataBuffer()).getData();
				int strideA = ConvertRaster.stride(raster);
				int offsetA = ConvertRaster.getOffset(raster);

				// handle a special case where the RGB conversion is screwed
				for (int i = 0; i < imgA.getHeight(); i++) {
					for (int j = 0; j < imgA.getWidth(); j++) {
						float valB = imgB.get(j, i);
						int valA = data[offsetA + i * strideA + j];
						valA &= 0xFF;

						if (Math.abs(valA - valB) > tol)
							throw new RuntimeException("Images are not equal: A = " + valA + " B = " + valB);
					}
				}
				return;
			}
		}

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				int rgb = imgA.getRGB(x, y);

				float gray = (((rgb >>> 16) & 0xFF) + ((rgb >>> 8) & 0xFF) + (rgb & 0xFF)) / 3.0f;
				float grayB = imgB.get(x, y);

				if (Math.abs(gray - grayB) > tol) {
					throw new RuntimeException("images are not equal: A = " + gray + " B = " + grayB);
				}
			}
		}
	}

	public static void checkEquals(WritableRaster imgA, ImageMultiBand imgB, float tol) {

		GImageMultiBand genericB = FactoryGImageMultiBand.wrap(imgB);
		float pixelB[] = new float[ imgB.getNumBands() ];

		if( imgA.getNumBands() != imgB.getNumBands()) {
			throw new RuntimeException("Number of bands not equals");
		}

		if( imgA.getDataBuffer().getDataType() == DataBuffer.TYPE_BYTE ) {

			byte []dataA = ((DataBufferByte)imgA.getDataBuffer()).getData();
			int strideA = ConvertRaster.stride(imgA);
			int offsetA = ConvertRaster.getOffset(imgA);

			// handle a special case where the RGB conversion is screwed
			for (int y = 0; y < imgA.getHeight(); y++) {
				int indexA = offsetA + strideA * y;

				for (int x = 0; x < imgA.getWidth(); x++) {
					genericB.get(x,y,pixelB);
					for( int k = 0; k < imgB.getNumBands(); k++ ) {
						int valueA = dataA[indexA++] & 0xFF;
						double valueB = pixelB[k];
						if( Math.abs(valueA- valueB) > tol )
							throw new RuntimeException("Images are not equal: A = " + valueA + " B = " + valueB);
					}
				}
			}

		} else if( imgA.getDataBuffer().getDataType() == DataBuffer.TYPE_INT) {
			int []dataA = ((DataBufferInt)imgA.getDataBuffer()).getData();
			int strideA = ConvertRaster.stride(imgA);
			int offsetA = ConvertRaster.getOffset(imgA);

			// handle a special case where the RGB conversion is screwed
			for (int y = 0; y < imgA.getHeight(); y++) {
				int indexA = offsetA + strideA * y;

				for (int x = 0; x < imgA.getWidth(); x++) {
					genericB.get(x,y,pixelB);
					int valueA = dataA[indexA++];
					if( imgB.getNumBands() == 4 ) {
						int found0 = (valueA >> 24) & 0xFF;
						int found1 = (valueA >> 16) & 0xFF;
						int found2 = (valueA >> 8) & 0xFF;
						int found3 = valueA & 0xFF;

						if( Math.abs(found0-pixelB[0]) > tol )
							throw new RuntimeException("Images are not equal");
						if( Math.abs(found1-pixelB[1]) > tol )
							throw new RuntimeException("Images are not equal");
						if( Math.abs(found2-pixelB[2]) > tol )
							throw new RuntimeException("Images are not equal");
						if( Math.abs(found3-pixelB[3]) > tol )
							throw new RuntimeException("Images are not equal");
					} else if( imgB.getNumBands() == 3 ) {
						int found0 = (valueA >> 16) & 0xFF;
						int found1 = (valueA >> 8) & 0xFF;
						int found2 = valueA & 0xFF;

						if( Math.abs(found0-pixelB[0]) > tol )
							throw new RuntimeException("Images are not equal");
						if( Math.abs(found1-pixelB[1]) > tol )
							throw new RuntimeException("Images are not equal");
						if( Math.abs(found2-pixelB[2]) > tol )
							throw new RuntimeException("Images are not equal");
					} else {
						throw new RuntimeException("Unexpectd number of bands");
					}
				}
			}
		} else {
			throw new RuntimeException("Add support for raster type "+imgA.getClass().getSimpleName());
		}

	}

	public static void checkEquals(BufferedImage imgA, ImageMultiBand imgB, boolean boofcvBandOrder , float tol) {

		GImageMultiBand genericB = FactoryGImageMultiBand.wrap(imgB);
		float pixelB[] = new float[ imgB.getNumBands() ];

		WritableRaster raster = imgA.getRaster();
		if (raster.getDataBuffer().getDataType() == DataBuffer.TYPE_BYTE &&
				ConvertBufferedImage.isKnownByteFormat(imgA)) {

			if (raster.getNumBands() == 1) {
				byte []dataA = ((DataBufferByte)raster.getDataBuffer()).getData();
				int strideA = ConvertRaster.stride(raster);
				int offsetA = ConvertRaster.getOffset(raster);

				// handle a special case where the RGB conversion is screwed
				for (int i = 0; i < imgA.getHeight(); i++) {
					for (int j = 0; j < imgA.getWidth(); j++) {
						genericB.get(j,i,pixelB);
						double valB = pixelB[0];
						int valA = dataA[offsetA + i * strideA + j];
						valA &= 0xFF;

						if (Math.abs(valA - valB) > tol)
							throw new RuntimeException("Images are not equal: A = " + valA + " B = " + valB);
					}
				}
				return;
			}
		}

		int bandOrder[];

		if( boofcvBandOrder ) {
			if( imgB.getNumBands() == 4 ) {
				bandOrder = new int[]{1,2,3,0};
			} else {
				bandOrder = new int[]{0,1,2};
			}
		} else {
			if( imgA.getType() == BufferedImage.TYPE_INT_RGB ) {
				bandOrder = new int[]{0,1,2};
			} else if( imgA.getType() == BufferedImage.TYPE_INT_BGR ||
					imgA.getType() == BufferedImage.TYPE_3BYTE_BGR ) {
				bandOrder = new int[]{2,1,0};
			} else if( imgA.getType() == BufferedImage.TYPE_4BYTE_ABGR ) {
				bandOrder = new int[]{0,3,2,1};
			} else if( imgA.getType() == BufferedImage.TYPE_INT_ARGB ) {
				bandOrder = new int[]{0,1,2,3};
			}  else {
				bandOrder = new int[]{0,1,2};
			}
		}

		int expected[] = new int[4];

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				// getRGB() automatically converts the band order to ARGB
				int rgb = imgA.getRGB(x, y);

				expected[0] = ((rgb >>> 24) & 0xFF); // alpha
				expected[1] = ((rgb >>> 16) & 0xFF); // red
				expected[2] = ((rgb >>> 8) & 0xFF);  // green
				expected[3] = (rgb & 0xFF);          // blue

				if( imgB.getNumBands() == 4 ) {
					genericB.get(x,y,pixelB);
					for( int i = 0; i < 4; i++ ) {

						double found = pixelB[bandOrder[i]];
						if( Math.abs(Math.exp(expected[i]- found)) > tol ) {
							for( int j = 0; j < 4; j++ ) {
								System.out.println(expected[j]+" "+pixelB[bandOrder[j]]);
							}
							throw new RuntimeException("Images are not equal: band - "+i+" type "+imgA.getType());
						}
					}

				} else if( imgB.getNumBands() == 3 ) {
					genericB.get(x, y, pixelB);
					for (int i = 0; i < 3; i++) {
						double found = pixelB[bandOrder[i]];
						if (Math.abs(expected[i + 1] - found) > tol) {
							for (int j = 0; j < 3; j++) {
								System.out.println(expected[j + 1] + " " + pixelB[bandOrder[j]]);
							}
							throw new RuntimeException("Images are not equal: band - " + i + " type " + imgA.getType());
						}
					}
				} else if( imgB.getNumBands() == 1 ) {
					genericB.get(x, y, pixelB);
					double expectedGray = (expected[1]+expected[2]+expected[3])/3.0;

					if (Math.abs(expectedGray - pixelB[0]) > tol) {
						throw new RuntimeException("Images are not equal:  "+imgA.getType()+" single band multi banded");
					}
				} else {
					throw new RuntimeException("Unexpected number of bands");
				}
			}
		}
	}

}
