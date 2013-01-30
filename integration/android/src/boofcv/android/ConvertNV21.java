package boofcv.android;

import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;

/**
 * Used to convert NV21 image format used in Android into BoofCV standard image types. NV21 is an encoding of a
 * YUV image [1] (more specifically YUV 4:2:0) where Y is encoded in the first block and UV are interlaced together.
 *
 * @author Peter Abeles
 */
public class ConvertNV21 {

	public static <T extends ImageSingleBand>
	T nv21ToGray( byte[] data , int width , int height ,
				  T output , Class<T> outputType ) {

		if( outputType == ImageUInt8.class ) {
			return (T)nv21ToGray(data,width,height,(ImageUInt8)output);
		} else if( outputType == ImageFloat32.class ) {
			return (T)nv21ToGray(data,width,height,(ImageFloat32)output);
		} else {
			throw new IllegalArgumentException("Unsupported BoofCV Image Type "+outputType.getSimpleName());
		}
	}

	public static ImageUInt8 nv21ToGray( byte[] data , int width , int height , ImageUInt8 output ) {
		if( output != null ) {
			if( output.width != width || output.height != height )
				throw new IllegalArgumentException("output width and height must be "+width+" "+height);
		} else {
			output = new ImageUInt8(width,height);
		}

		int yStride = (int) Math.ceil(width / 16.0) * 16;
		ImplConvertNV21.nv21ToGray(data, yStride, output);

		return output;
	}

	public static ImageFloat32 nv21ToGray( byte[] data , int width , int height , ImageFloat32 output ) {
		if( output != null ) {
			if( output.width != width || output.height != height )
				throw new IllegalArgumentException("output width and height must be "+width+" "+height);
		} else {
			output = new ImageFloat32(width,height);
		}

		int yStride = (int) Math.ceil(width / 16.0) * 16;
		ImplConvertNV21.nv21ToGray(data, yStride, output);

		return output;
	}


	public static <T extends ImageSingleBand>
	T nv21ToMsYuv( byte[] data , int width , int height ,
				   MultiSpectral<T> output , Class<T> outputType ) {
		return null;
	}
}
