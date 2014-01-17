package boofcv.android;

import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;

/**
 * NV21:  The format is densely packed.  Y is full resolution and UV are interlaced and 1/2 resolution.
 *        So same UV values within a 2x2 square
 *
 * @author Peter Abeles
 */
public class ImplConvertNV21 {

	/**
	 * First block contains gray-scale information and UV data can be ignored.
	 */
	public static void nv21ToGray(byte[] dataNV, ImageUInt8 output) {

		final int yStride = output.width;

		// see if the whole thing can be copied as one big block to maximize speed
		if( yStride == output.width && !output.isSubimage() ) {
			System.arraycopy(dataNV,0,output.data,0,output.width*output.height);
		} else {
			// copy one row at a time
			for( int y = 0; y < output.height; y++ ) {
				int indexOut = output.startIndex + y*output.stride;

				System.arraycopy(dataNV,y*yStride,output.data,indexOut,output.width);
			}
		}
	}

	/**
	 * First block contains gray-scale information and UV data can be ignored.
	 */
	public static void nv21ToGray(byte[] dataNV, ImageFloat32 output) {

		for( int y = 0; y < output.height; y++ ) {
			int indexIn = y*output.width;
			int indexOut = output.startIndex + y*output.stride;

			for( int x = 0; x < output.width; x++ ) {
				output.data[ indexOut++ ] = dataNV[ indexIn++ ] & 0xFF;
			}
		}
	}

	public static void nv21ToMultiYuv_U8(byte[] dataNV, MultiSpectral<ImageUInt8> output) {

		ImageUInt8 Y = output.getBand(0);
		ImageUInt8 U = output.getBand(1);
		ImageUInt8 V = output.getBand(2);

		final int uvStride = output.width/2;

		nv21ToGray(dataNV, Y);

		int startUV = output.width*output.height;

		for( int row = 0; row < output.height; row++ ) {
			int indexUV = startUV + (row/2)*(2*uvStride);
			int indexOut = output.startIndex + row*output.stride;

			for( int col = 0; col < output.width; col++ , indexOut++ ) {
				U.data[indexOut] = dataNV[ indexUV     ];
				V.data[indexOut] = dataNV[ indexUV + 1 ];

				indexUV += 2*(col&0x1);
			}
		}
	}

	public static void nv21ToMultiYuv_F32(byte[] dataNV, MultiSpectral<ImageFloat32> output) {

		ImageFloat32 Y = output.getBand(0);
		ImageFloat32 U = output.getBand(1);
		ImageFloat32 V = output.getBand(2);

		final int uvStride = output.width/2;

		nv21ToGray(dataNV, Y);

		final int startUV = output.width*output.height;

		for( int row = 0; row < output.height; row++ ) {
			int indexUV = startUV + (row/2)*(2*uvStride);
			int indexOut = output.startIndex + row*output.stride;

			for( int col = 0; col < output.width; col++ , indexOut++ ) {
				U.data[indexOut] = (dataNV[ indexUV     ]&0xFF)-128;
				V.data[indexOut] = (dataNV[ indexUV + 1 ]&0xFF)-128;

				indexUV += 2*(col&0x1);
			}
		}
	}

	public static void nv21ToMultiRgb_U8(byte[] dataNV, MultiSpectral<ImageUInt8> output) {

		ImageUInt8 R = output.getBand(0);
		ImageUInt8 G = output.getBand(1);
		ImageUInt8 B = output.getBand(2);

		final int yStride = output.width;
		final int uvStride = output.width/2;

		final int startUV = yStride*output.height;

		for( int row = 0; row < output.height; row++ ) {
			int indexY = row*yStride;
			int indexUV = startUV + (row/2)*(2*uvStride);
			int indexOut = output.startIndex + row*output.stride;

			for( int col = 0; col < output.width; col++ , indexOut++ ) {
				int y = 1191*(dataNV[indexY++] & 0xFF) - 16;
				int cr = (dataNV[ indexUV ] & 0xFF) - 128;
				int cb = (dataNV[ indexUV+1] & 0xFF) - 128;

				if( y < 0 ) y = 0;

				int r = (y + 1836*cr) >> 10;
				int g = (y - 547*cr - 218*cb) >> 10;
				int b = (y + 2165*cb) >> 10;

				if( r < 0 ) r = 0; else if( r > 255 ) r = 255;
				if( g < 0 ) g = 0; else if( g > 255 ) g = 255;
				if( b < 0 ) b = 0; else if( b > 255 ) b = 255;

				R.data[indexOut] = (byte)r;
				G.data[indexOut] = (byte)g;
				B.data[indexOut] = (byte)b;

				indexUV += 2*(col&0x1);
			}
		}
	}

	public static void nv21ToMultiRgb_F32(byte[] dataNV, MultiSpectral<ImageFloat32> output) {

		ImageFloat32 R = output.getBand(0);
		ImageFloat32 G = output.getBand(1);
		ImageFloat32 B = output.getBand(2);

		final int yStride = output.width;
		final int uvStride = output.width/2;

		final int startUV = yStride*output.height;

		for( int row = 0; row < output.height; row++ ) {
			int indexY = row*yStride;
			int indexUV = startUV + (row/2)*(2*uvStride);
			int indexOut = output.startIndex + row*output.stride;

			for( int col = 0; col < output.width; col++ , indexOut++ ) {
				int y = 1191*(dataNV[indexY++] & 0xFF) - 16;
				int cr = (dataNV[ indexUV ] & 0xFF) - 128;
				int cb = (dataNV[ indexUV+1] & 0xFF) - 128;

				if( y < 0 ) y = 0;

				int r = (y + 1836*cr) >> 10;
				int g = (y - 547*cr - 218*cb) >> 10;
				int b = (y + 2165*cb) >> 10;

				if( r < 0 ) r = 0; else if( r > 255 ) r = 255;
				if( g < 0 ) g = 0; else if( g > 255 ) g = 255;
				if( b < 0 ) b = 0; else if( b > 255 ) b = 255;

				R.data[indexOut] = r;
				G.data[indexOut] = g;
				B.data[indexOut] = b;

				indexUV += 2*(col&0x1);
			}
		}
	}
}
