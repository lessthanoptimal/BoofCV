/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import android.graphics.Bitmap;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.feature.detect.edge.EdgeSegment;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.segmentation.ImageSegmentationOps;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static boofcv.android.ConvertBitmap.declareStorage;

/**
 * Visualizes different types of image data into a color image which can be understood by humans.
 *
 * @author Peter Abeles
 */
public class VisualizeImageData {

	/**
	 * Renders a binary image as a B&W bitmap.  Create storage with
	 * {@link ConvertBitmap#declareStorage(android.graphics.Bitmap, byte[])};
	 *
	 * @param binary (Input) Binary image.
	 * @param invert If true it will invert the output image.
	 * @param output (Output) Bitmap ARGB_8888 image.
	 * @param storage Optional working buffer for Bitmap image.
	 */
	public static void binaryToBitmap( GrayU8 binary , boolean invert , Bitmap output , byte[] storage ) {
		shapeShape(binary, output);

		if( storage == null )
			storage = declareStorage(output,null);

		int indexDst = 0;

		if( invert ) {
			for (int y = 0; y < binary.height; y++) {
				int indexSrc = binary.startIndex + y * binary.stride;
				for (int x = 0; x < binary.width; x++) {
					int value = (1-binary.data[indexSrc++]) * 255;

					storage[indexDst++] = (byte) value;
					storage[indexDst++] = (byte) value;
					storage[indexDst++] = (byte) value;
					storage[indexDst++] = (byte) 0xFF;
				}
			}
		} else {
			for (int y = 0; y < binary.height; y++) {
				int indexSrc = binary.startIndex + y * binary.stride;
				for (int x = 0; x < binary.width; x++) {
					int value = binary.data[indexSrc++] * 255;

					storage[indexDst++] = (byte) value;
					storage[indexDst++] = (byte) value;
					storage[indexDst++] = (byte) value;
					storage[indexDst++] = (byte) 0xFF;
				}
			}
		}

		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}

	/**
	 * Renders positive and negative values as two different colors.
	 *
	 * @param input (Input) Image with positive and negative values.
	 * @param maxAbsValue  The largest absolute value of any pixel in the image.  Set to < 0 if not known.
	 * @param output (Output) Bitmap ARGB_8888 image.
	 * @param storage Optional working buffer for Bitmap image.
	 */
	public static void colorizeSign( GrayS16 input , int maxAbsValue , Bitmap output , byte[] storage ) {
		shapeShape(input, output);

		if( storage == null )
			storage = declareStorage(output,null);

		if( maxAbsValue < 0 )
			maxAbsValue = ImageStatistics.maxAbs(input);

		int indexDst = 0;

		for( int y = 0; y < input.height; y++ ) {
			int indexSrc = input.startIndex + y*input.stride;
			for( int x = 0; x < input.width; x++ ) {
				int value = input.data[ indexSrc++ ];
				if( value > 0 ) {
					storage[indexDst++] = (byte) (255*value/maxAbsValue);
					storage[indexDst++] = 0;
					storage[indexDst++] = 0;
				} else {
					storage[indexDst++] = 0;
					storage[indexDst++] = (byte) (-255*value/maxAbsValue);
					storage[indexDst++] = 0;
				}
				storage[indexDst++] = (byte) 0xFF;
			}
		}

		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}

	/**
	 * Renders positive and negative values as two different colors.
	 *
	 * @param input (Input) Image with positive and negative values.
	 * @param maxAbsValue  The largest absolute value of any pixel in the image.  Set to < 0 if not known.
	 * @param output (Output) Bitmap ARGB_8888 image.
	 * @param storage Optional working buffer for Bitmap image.
	 */
	public static void colorizeSign( GrayF32 input , float maxAbsValue , Bitmap output , byte[] storage ) {
		shapeShape(input, output);

		if( storage == null )
			storage = declareStorage(output,null);

		if( maxAbsValue < 0 )
			maxAbsValue = ImageStatistics.maxAbs(input);

		int indexDst = 0;

		for( int y = 0; y < input.height; y++ ) {
			int indexSrc = input.startIndex + y*input.stride;
			for( int x = 0; x < input.width; x++ ) {
				float value = input.data[ indexSrc++ ];
				if( value > 0 ) {
					storage[indexDst++] = (byte) (255f*value/maxAbsValue);
					storage[indexDst++] = 0;
					storage[indexDst++] = 0;
				} else {
					storage[indexDst++] = 0;
					storage[indexDst++] = (byte) (-255f*value/maxAbsValue);
					storage[indexDst++] = 0;
				}
				storage[indexDst++] = (byte) 0xFF;
			}
		}

		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}

	/**
	 * Renders the image using its gray magnitude
	 *
	 * @param input (Input) Image image
	 * @param maxAbsValue (Input) Largest absolute value of a pixel in the image
	 * @param output (Output) Bitmap ARGB_8888 image.
	 * @param storage Optional working buffer for Bitmap image.
	 */
	public static void grayMagnitude(GrayS32 input , int maxAbsValue , Bitmap output , byte[] storage) {
		shapeShape(input, output);

		if( storage == null )
			storage = declareStorage(output,null);

		if( maxAbsValue < 0 )
			maxAbsValue = ImageStatistics.maxAbs(input);

		int indexDst = 0;

		for( int y = 0; y < input.height; y++ ) {
			int indexSrc = input.startIndex + y*input.stride;
			for( int x = 0; x < input.width; x++ ) {
				byte gray = (byte)(255*Math.abs(input.data[ indexSrc++ ])/maxAbsValue);
				storage[indexDst++] = gray;
				storage[indexDst++] = gray;
				storage[indexDst++] = gray;
				storage[indexDst++] = (byte) 0xFF;
			}
		}

		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}

	/**
	 * Renders the image using its gray magnitude
	 *
	 * @param input (Input) Image image
	 * @param maxAbsValue (Input) Largest absolute value of a pixel in the image
	 * @param output (Output) Bitmap ARGB_8888 image.
	 * @param storage Optional working buffer for Bitmap image.
	 */
	public static void grayMagnitude(GrayF32 input , float maxAbsValue , Bitmap output , byte[] storage) {
		shapeShape(input, output);

		if( storage == null )
			storage = declareStorage(output,null);

		if( maxAbsValue < 0 )
			maxAbsValue = ImageStatistics.maxAbs(input);

		int indexDst = 0;

		for( int y = 0; y < input.height; y++ ) {
			int indexSrc = input.startIndex + y*input.stride;
			for( int x = 0; x < input.width; x++ ) {
				byte gray = (byte)(255f*Math.abs(input.data[ indexSrc++ ])/maxAbsValue);
				storage[indexDst++] = gray;
				storage[indexDst++] = gray;
				storage[indexDst++] = gray;
				storage[indexDst++] = (byte) 0xFF;
			}
		}

		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}

	/**
	 * Renders two gradients on the same image using two sets of colors, on for each input image.
	 *
	 * @param derivX (Input) Image with positive and negative values.
	 * @param derivY (Input) Image with positive and negative values.
	 * @param maxAbsValue  The largest absolute value of any pixel in the image.  Set to < 0 if not known.
	 * @param output (Output) Bitmap ARGB_8888 image.
	 * @param storage Optional working buffer for Bitmap image.
	 */
	public static void colorizeGradient( GrayS16 derivX , GrayS16 derivY ,
										 int maxAbsValue , Bitmap output , byte[] storage ) {
		shapeShape(derivX, derivY, output);

		if( storage == null )
			storage = declareStorage(output,null);

		if( maxAbsValue < 0 ) {
			maxAbsValue = ImageStatistics.maxAbs(derivX);
			maxAbsValue = Math.max(maxAbsValue, ImageStatistics.maxAbs(derivY));
		}
		if( maxAbsValue == 0 )
			return;

		int indexDst = 0;

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
				storage[indexDst++] = (byte) r;
				storage[indexDst++] = (byte) g;
				storage[indexDst++] = (byte) b;
				storage[indexDst++] = (byte) 0xFF;
			}
		}

		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}

	/**
	 * Renders two gradients on the same image using two sets of colors, on for each input image.
	 *
	 * @param derivX (Input) Image with positive and negative values.
	 * @param derivY (Input) Image with positive and negative values.
	 * @param maxAbsValue  The largest absolute value of any pixel in the image.  Set to < 0 if not known.
	 * @param output (Output) Bitmap ARGB_8888 image.
	 * @param storage Optional working buffer for Bitmap image.
	 */
	public static void colorizeGradient( GrayF32 derivX , GrayF32 derivY ,
										 float maxAbsValue , Bitmap output , byte[] storage ) {
		shapeShape(derivX, derivY, output);

		if( storage == null )
			storage = declareStorage(output,null);

		if( maxAbsValue < 0 ) {
			maxAbsValue = ImageStatistics.maxAbs(derivX);
			maxAbsValue = Math.max(maxAbsValue, ImageStatistics.maxAbs(derivY));
		}
		if( maxAbsValue == 0 )
			return;

		int indexDst = 0;

		for( int y = 0; y < derivX.height; y++ ) {
			int indexX = derivX.startIndex + y*derivX.stride;
			int indexY = derivY.startIndex + y*derivY.stride;

			for( int x = 0; x < derivX.width; x++ ) {
				float valueX = derivX.data[ indexX++ ];
				float valueY = derivY.data[ indexY++ ];

				int r=0,g=0,b=0;

				if( valueX > 0 ) {
					r = (int)(255f*valueX/maxAbsValue);
				} else {
					g = (int)(-255f*valueX/maxAbsValue);
				}
				if( valueY > 0 ) {
					b = (int)(255f*valueY/maxAbsValue);
				} else {
					int v = (int)(-255f*valueY/maxAbsValue);
					r += v;
					g += v;
					if( r > 255 ) r = 255;
					if( g > 255 ) g = 255;
				}
				storage[indexDst++] = (byte) r;
				storage[indexDst++] = (byte) g;
				storage[indexDst++] = (byte) b;
				storage[indexDst++] = (byte) 0xFF;
			}
		}

		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}

	/**
	 * Colorizes a disparity image.
	 *
	 * @param disparity (Input) disparity image.
	 * @param minValue Minimum possible disparity
	 * @param maxValue Maximum possible disparity
	 * @param invalidColor RGB value of an invalid pixel
	 * @param output (Output) Bitmap ARGB_8888 image.
	 * @param storage Optional working buffer for Bitmap image. Can be null.
	 */
	public static void disparity( GrayI disparity, int minValue, int maxValue,
								  int invalidColor, Bitmap output , byte[] storage ) {
		shapeShape(disparity, output);

		if( storage == null )
			storage = declareStorage(output,null);

		int range = maxValue - minValue;

		int indexDst = 0;

		for (int y = 0; y < disparity.height; y++) {
			for (int x = 0; x < disparity.width; x++) {
				int v = disparity.unsafe_get(x, y);
				int r,g,b;

				if (v > range) {
					r = (invalidColor >> 16) & 0xFF;
					g = (invalidColor >> 8) & 0xFF;
					b = (invalidColor) & 0xFF;
				} else {
					g = 0;
					if (v == 0) {
						r = b = 0;
					} else {
						r = 255 * v / maxValue;
						b = 255 * (maxValue - v) / maxValue;
					}
				}

				storage[indexDst++] = (byte) r;
				storage[indexDst++] = (byte) g;
				storage[indexDst++] = (byte) b;
				storage[indexDst++] = (byte) 0xFF;
			}
		}

		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}

	/**
	 * Colorizes a disparity image.
	 *
	 * @param disparity (Input) disparity image.
	 * @param minValue Minimum possible disparity
	 * @param maxValue Maximum possible disparity
	 * @param invalidColor RGB value of an invalid pixel
	 * @param output (Output) Bitmap ARGB_8888 image.
	 * @param storage Optional working buffer for Bitmap image. Can be null.
	 */
	public static void disparity( GrayF32 disparity, int minValue, int maxValue,
								  int invalidColor, Bitmap output , byte[] storage ) {
		shapeShape(disparity, output);

		if( storage == null )
			storage = declareStorage(output,null);

		int range = maxValue - minValue;

		int indexDst = 0;

		for (int y = 0; y < disparity.height; y++) {
			for (int x = 0; x < disparity.width; x++) {
				float v = disparity.unsafe_get(x, y);
				int r,g,b;

				if (v > range) {
					r = (invalidColor >> 16) & 0xFF;
					g = (invalidColor >> 8) & 0xFF;
					b = (invalidColor) & 0xFF;
				} else {
					g = 0;
					if (v == 0) {
						r = b = 0;
					} else {
						r = (int)(255 * v / maxValue);
						b = (int)(255 * (maxValue - v) / maxValue);
					}
				}

				storage[indexDst++] = (byte) r;
				storage[indexDst++] = (byte) g;
				storage[indexDst++] = (byte) b;
				storage[indexDst++] = (byte) 0xFF;
			}
		}

		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}

	/**
	 * Draws each contour using a unique color.  Each segment of each edge is drawn using the same colors.
	 *
	 * @param contours List of edge contours
	 * @param colors RGB color for each edge
	 * @param output Where the output is written to
	 * @param storage Optional working buffer for Bitmap image. Can be null.
	 */
	public static void drawEdgeContours( List<EdgeContour> contours , int colors[] , Bitmap output , byte[] storage ) {
		if( output.getConfig() != Bitmap.Config.ARGB_8888 )
			throw new IllegalArgumentException("Only ARGB_8888 is supported");

		if( storage == null )
			storage = declareStorage(output,null);
		else
			Arrays.fill(storage,(byte)0);

		for( int i = 0; i < contours.size(); i++ ) {
			EdgeContour e = contours.get(i);

			int c = colors[i];

			for( int j = 0; j < e.segments.size(); j++ ) {
				EdgeSegment s = e.segments.get(j);

				for( int k = 0; k < s.points.size(); k++ ) {
					Point2D_I32 p = s.points.get(k);

					int index = p.y*4*output.getWidth() + p.x*4;

					storage[index++] = (byte)(c >> 16);
					storage[index++] = (byte)(c >> 8);
					storage[index++] = (byte)c;
					storage[index]   = (byte)0xFF;
				}
			}
		}

		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}

	/**
	 * Draws each contour using a single color.
	 *
	 * @param contours List of edge contours
	 * @param color The RGB color that each edge pixel should be drawn
	 * @param output Where the output is written to
	 * @param storage Optional working buffer for Bitmap image. Can be null.
	 */
	public static void drawEdgeContours( List<EdgeContour> contours , int color , Bitmap output , byte[] storage ) {
		if( output.getConfig() != Bitmap.Config.ARGB_8888 )
			throw new IllegalArgumentException("Only ARGB_8888 is supported");

		if( storage == null )
			storage = declareStorage(output,null);
		else
			Arrays.fill(storage,(byte)0);

		byte r = (byte)((color >> 16) & 0xFF);
		byte g = (byte)((color >> 8) & 0xFF);
		byte b = (byte)( color );


		for( int i = 0; i < contours.size(); i++ ) {
			EdgeContour e = contours.get(i);

			for( int j = 0; j < e.segments.size(); j++ ) {
				EdgeSegment s = e.segments.get(j);

				for( int k = 0; k < s.points.size(); k++ ) {
					Point2D_I32 p = s.points.get(k);

					int index = p.y*4*output.getWidth() + p.x*4;

					storage[index++] = b;
					storage[index++] = g;
					storage[index++] = r;
					storage[index]   = (byte)0xFF;
				}
			}
		}

		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}

	/**
	 * Renders a labeled where each region is assigned a random color.
	 *
	 * @param labelImage Labeled image with labels from 0 to numRegions-1
	 * @param numRegions Number of labeled in the image
	 * @param output Where the output is written to
	 * @param storage Optional working buffer for Bitmap image. Can be null.
	 **/
	public static void renderLabeled(GrayS32 labelImage, int numRegions, Bitmap output , byte[] storage) {

		if( storage == null )
			storage = declareStorage(output,null);

		int colors[] = new int[numRegions];

		Random rand = new Random(123);
		for( int i = 0; i < colors.length; i++ ) {
			colors[i] = rand.nextInt();
		}

		int w = labelImage.getWidth();
		int h = labelImage.getHeight();

		int indexOut = 0;
		for( int y = 0; y < h; y++ ) {
			int indexSrc = labelImage.startIndex + y*labelImage.stride;
			for( int x = 0; x < w; x++ ) {
				int rgb = colors[labelImage.data[indexSrc++]];

				storage[indexOut++] = (byte)(rgb & 0xFF);
				storage[indexOut++] = (byte)((rgb >> 8) & 0xFF);
				storage[indexOut++] = (byte)((rgb >> 16) & 0xFF);
				storage[indexOut++] = (byte)0xFF;
			}
		}

		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}

	/**
	 * Draws border pixels of each region using the specified color.
	 *
	 * @param pixelToRegion Conversion from pixel to region
	 * @param borderColor RGB value of border pixel
	 * @param output Where the output is written to
	 * @param storage Optional working buffer for Bitmap image. Can be null.
	 */
	public static void regionBorders( GrayS32 pixelToRegion , int borderColor ,
									  Bitmap output , byte[] storage) {
		if( storage == null )
			storage = declareStorage(output,null);

		GrayU8 binary = new GrayU8(pixelToRegion.width,pixelToRegion.height);
		ImageSegmentationOps.markRegionBorders(pixelToRegion, binary);
		int indexOut = 0;
		for( int y = 0; y < binary.height; y++ ) {
			for( int x = 0; x < binary.width; x++ ) {
				if( binary.unsafe_get(x,y) == 1 )  {
					storage[indexOut++] = (byte)(borderColor & 0xFF);
					storage[indexOut++] = (byte)((borderColor >> 8) & 0xFF);
					storage[indexOut++] = (byte)((borderColor >> 16) & 0xFF);
					storage[indexOut++] = (byte)0xFF;
				} else {
					indexOut += 4;
				}
			}
		}

		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}

	/**
	 * Draws each region using the provided color
	 * @param pixelToRegion Conversion from pixel to region
	 * @param segmentColor Color of each region
	 * @param output Where the output is written to
	 * @param storage Optional working buffer for Bitmap image. Can be null.
	 */
	public static void regionsColor( GrayS32 pixelToRegion ,
									 FastQueue<float[]> segmentColor ,
									 Bitmap output , byte[] storage ) {
		if( storage == null )
			storage = declareStorage(output,null);

		int indexOut = 0;
		for( int y = 0; y < pixelToRegion.height; y++ ) {
			for( int x = 0; x < pixelToRegion.width; x++ ) {
				int index = pixelToRegion.unsafe_get(x,y);
				float []cv = segmentColor.get(index);

				int r,g,b;

				if( cv.length == 3 ) {
					r = (int)cv[0];
					g = (int)cv[1];
					b = (int)cv[2];
				} else {
					r = g = b = (int)cv[0];
				}

				storage[indexOut++] = (byte)r;
				storage[indexOut++] = (byte)g;
				storage[indexOut++] = (byte)b;
				storage[indexOut++] = (byte)0xFF;
			}
		}

		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}

	private static void shapeShape(ImageBase input, Bitmap output) {
		if( output.getConfig() != Bitmap.Config.ARGB_8888 )
			throw new IllegalArgumentException("Only ARGB_8888 is supported");
		if( input.width != output.getWidth() || input.height != output.getHeight() )
			throw new IllegalArgumentException("Input and output must have the same shape");
	}
	private static void shapeShape(ImageBase input0, ImageBase input1, Bitmap output) {
		if( output.getConfig() != Bitmap.Config.ARGB_8888 )
			throw new IllegalArgumentException("Only ARGB_8888 is supported");
		if( input0.width != output.getWidth() || input0.height != output.getHeight() )
			throw new IllegalArgumentException("Input and output must have the same shape");
		if( input1.width != output.getWidth() || input1.height != output.getHeight() )
			throw new IllegalArgumentException("Input and output must have the same shape");
	}
}
