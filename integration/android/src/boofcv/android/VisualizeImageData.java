package boofcv.android;

import android.graphics.Bitmap;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.feature.detect.edge.EdgeSegment;
import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_I32;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

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
	 * @param output (Output) Bitmap ARGB_8888 image.
	 * @param storage Optional working buffer for Bitmap image.
	 */
	public static void binaryToBitmap( ImageUInt8 binary , Bitmap output , byte[] storage ) {
		shapeShape(binary, output);

		if( storage == null )
			storage = declareStorage(output,null);

		int indexDst = 0;

		for( int y = 0; y < binary.height; y++ ) {
			int indexSrc = binary.startIndex + y*binary.stride;
			for( int x = 0; x < binary.width; x++ ) {
				int value = binary.data[ indexSrc++ ] * 255;

				storage[indexDst++] = (byte) value;
				storage[indexDst++] = (byte) value;
				storage[indexDst++] = (byte) value;
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
	public static void colorizeSign( ImageSInt16 input , int maxAbsValue , Bitmap output , byte[] storage ) {
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
	public static void colorizeSign( ImageFloat32 input , float maxAbsValue , Bitmap output , byte[] storage ) {
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
	public static void grayMagnitude(ImageSInt32 input , int maxAbsValue , Bitmap output , byte[] storage) {
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
	public static void grayMagnitude(ImageFloat32 input , float maxAbsValue , Bitmap output , byte[] storage) {
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
	public static void colorizeGradient( ImageSInt16 derivX , ImageSInt16 derivY ,
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
	public static void colorizeGradient( ImageFloat32 derivX , ImageFloat32 derivY ,
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
	public static void disparity( ImageInteger disparity, int minValue, int maxValue,
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
	public static void disparity( ImageFloat32 disparity, int minValue, int maxValue,
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
