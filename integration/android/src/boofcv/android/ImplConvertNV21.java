package boofcv.android;

import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;

/**
 * @author Peter Abeles
 */
public class ImplConvertNV21 {

	/**
	 * First block contains gray-scale information and UV data can be ignored.
	 */
	public static void nv21ToGray(byte[] dataNV, int yStride, ImageUInt8 output) {

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
	public static void nv21ToGray(byte[] dataNV, int yStride, ImageFloat32 output) {

		for( int y = 0; y < output.height; y++ ) {
			int indexIn = y*yStride;
			int indexOut = output.startIndex + y*output.stride;

			for( int x = 0; x < output.width; x++ ) {
				output.data[ indexOut++ ] = dataNV[ indexIn++ ] & 0xFF;
			}
		}
	}

	public static void nv21ToMultiYuv_U8(byte[] dataNV, int yStride, int uvStride, MultiSpectral<ImageUInt8> output) {

		ImageUInt8 Y = output.getBand(0);
		ImageUInt8 U = output.getBand(1);
		ImageUInt8 V = output.getBand(2);

		nv21ToGray(dataNV, yStride, Y);

		int startUV = yStride*output.height;

		for( int y = 0; y < output.height; y++ ) {
			int indexIn = startUV + y*uvStride;
			int indexOut = output.startIndex + y*output.stride;

			for( int x = 0; x < output.width; x++ , indexOut++ ) {
				U.data[indexOut] = dataNV[ indexIn++ ];
				V.data[indexOut] = dataNV[ indexIn++ ];
			}
		}
	}

	public static void nv21ToMultiYuv_F32(byte[] dataNV, int yStride, int uvStride, MultiSpectral<ImageFloat32> output) {

		ImageFloat32 Y = output.getBand(0);
		ImageFloat32 U = output.getBand(1);
		ImageFloat32 V = output.getBand(2);

		nv21ToGray(dataNV, yStride, Y);

		int startUV = yStride*output.height;

		for( int y = 0; y < output.height; y++ ) {
			int indexIn = startUV + y*uvStride;
			int indexOut = output.startIndex + y*output.stride;

			for( int x = 0; x < output.width; x++ , indexOut++ ) {
				U.data[indexOut] = dataNV[ indexIn++ ] & 0xFF;
				V.data[indexOut] = dataNV[ indexIn++ ] & 0xFF;
			}
		}
	}
}
