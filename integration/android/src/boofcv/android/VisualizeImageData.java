package boofcv.android;

import android.graphics.Bitmap;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;

import java.nio.ByteBuffer;

import static boofcv.android.ConvertBitmap.declareStorage;

/**
 * @author Peter Abeles
 */
public class VisualizeImageData {

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
	 *
	 * @param input
	 * @param maxAbsValue
	 * @param output
	 * @param storage
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

	public static void colorizeGradient( ImageSInt16 derivX , ImageSInt16 derivY ,
										 int maxAbsValue , Bitmap output , byte[] storage ) {
		shapeShape(derivX, derivY, output);

		if( storage == null )
			storage = declareStorage(output,null);

		if( maxAbsValue < 0 ) {
			maxAbsValue = ImageStatistics.maxAbs(derivX);
			maxAbsValue = Math.max(maxAbsValue, ImageStatistics.maxAbs(derivY));
		}

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
