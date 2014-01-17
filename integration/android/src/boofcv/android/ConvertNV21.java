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

	/**
	 * Converts an NV21 image into a gray scale image.  Image type is determined at runtime.
	 *
	 * @param data Input: NV21 image data
	 * @param width Input: NV21 image width
	 * @param height Input: NV21 image height
	 * @param output Output: Optional storage for output image.  Can be null.
	 * @param outputType  Output: Type of output image
	 * @param <T> Output image type
	 * @return Gray scale image
	 */
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

	/**
	 * Converts an NV21 image into a gray scale U8 image.
	 *
	 * @param data Input: NV21 image data
	 * @param width Input: NV21 image width
	 * @param height Input: NV21 image height
	 * @param output Output: Optional storage for output image.  Can be null.
	 * @return Gray scale image
	 */
	public static ImageUInt8 nv21ToGray( byte[] data , int width , int height , ImageUInt8 output ) {
		if( output != null ) {
			if( output.width != width || output.height != height )
				throw new IllegalArgumentException("output width and height must be "+width+" "+height);
		} else {
			output = new ImageUInt8(width,height);
		}

		ImplConvertNV21.nv21ToGray(data, output);

		return output;
	}

	/**
	 * Converts an NV21 image into a gray scale F32 image.
	 *
	 * @param data Input: NV21 image data
	 * @param width Input: NV21 image width
	 * @param height Input: NV21 image height
	 * @param output Output: Optional storage for output image.  Can be null.
	 * @return Gray scale image
	 */
	public static ImageFloat32 nv21ToGray( byte[] data , int width , int height , ImageFloat32 output ) {
		if( output != null ) {
			if( output.width != width || output.height != height )
				throw new IllegalArgumentException("output width and height must be "+width+" "+height);
		} else {
			output = new ImageFloat32(width,height);
		}

		ImplConvertNV21.nv21ToGray(data, output);

		return output;
	}

	/**
	 * Converts an NV21 image into a {@link MultiSpectral} YUV image.
	 *
	 * @param data Input: NV21 image data
	 * @param width Input: NV21 image width
	 * @param height Input: NV21 image height
	 * @param output Output: Optional storage for output image.  Can be null.
	 * @param outputType  Output: Type of output image
	 * @param <T> Output image type
	 */
	public static <T extends ImageSingleBand>
	MultiSpectral<T> nv21ToMsYuv( byte[] data , int width , int height ,
								  MultiSpectral<T> output , Class<T> outputType ) {

		if( outputType == ImageUInt8.class ) {
			return (MultiSpectral)nv21ToMsYuv_U8(data,width,height,(MultiSpectral)output);
		} else if( outputType == ImageFloat32.class ) {
			return (MultiSpectral)nv21ToMsYuv_F32(data,width,height,(MultiSpectral)output);
		} else {
			throw new IllegalArgumentException("Unsupported BoofCV Image Type "+outputType.getSimpleName());
		}
	}

	/**
	 * Converts an NV21 image into a {@link MultiSpectral} YUV image with U8 bands.
	 *
	 * @param data Input: NV21 image data
	 * @param width Input: NV21 image width
	 * @param height Input: NV21 image height
	 * @param output Output: Optional storage for output image.  Can be null.
	 */
	public static MultiSpectral<ImageUInt8> nv21ToMsYuv_U8( byte[] data , int width , int height ,
															MultiSpectral<ImageUInt8> output ) {
		if( output == null ) {
			output = new MultiSpectral<ImageUInt8>(ImageUInt8.class,width,height,3);
		} else if( output.width != width || output.height != height )
			throw new IllegalArgumentException("output width and height must be "+width+" "+height);
		else if( output.getNumBands() != 3 )
			throw new IllegalArgumentException("three bands expected");

		ImplConvertNV21.nv21ToMultiYuv_U8(data,output);

		return output;
	}

	/**
	 * Converts an NV21 image into a {@link MultiSpectral} RGB image with U8 bands.
	 *
	 * @param data Input: NV21 image data
	 * @param width Input: NV21 image width
	 * @param height Input: NV21 image height
	 * @param output Output: Optional storage for output image.  Can be null.
	 */
	public static MultiSpectral<ImageUInt8> nv21ToMsRgb_U8( byte[] data , int width , int height ,
															MultiSpectral<ImageUInt8> output ) {
		if( output == null ) {
			output = new MultiSpectral<ImageUInt8>(ImageUInt8.class,width,height,3);
		} else if( output.width != width || output.height != height )
			throw new IllegalArgumentException("output width and height must be "+width+" "+height);
		else if( output.getNumBands() != 3 )
			throw new IllegalArgumentException("three bands expected");

		ImplConvertNV21.nv21ToMultiRgb_U8(data,output);

		return output;
	}

	/**
	 * Converts an NV21 image into a {@link MultiSpectral} YUV image with F32 bands.
	 *
	 * @param data Input: NV21 image data
	 * @param width Input: NV21 image width
	 * @param height Input: NV21 image height
	 * @param output Output: Optional storage for output image.  Can be null.
	 */
	public static MultiSpectral<ImageFloat32> nv21ToMsYuv_F32( byte[] data , int width , int height ,
															   MultiSpectral<ImageFloat32> output ) {
		if( output == null ) {
			output = new MultiSpectral<ImageFloat32>(ImageFloat32.class,width,height,3);
		} else if( output.width != width || output.height != height )
			throw new IllegalArgumentException("output width and height must be "+width+" "+height);
		else if( output.getNumBands() != 3 )
			throw new IllegalArgumentException("three bands expected");

		ImplConvertNV21.nv21ToMultiYuv_F32(data,output);

		return output;
	}

	/**
	 * Converts an NV21 image into a {@link MultiSpectral} RGB image with F32 bands.
	 *
	 * @param data Input: NV21 image data
	 * @param width Input: NV21 image width
	 * @param height Input: NV21 image height
	 * @param output Output: Optional storage for output image.  Can be null.
	 */
	public static MultiSpectral<ImageFloat32> nv21ToMsRgb_F32( byte[] data , int width , int height ,
															 MultiSpectral<ImageFloat32> output ) {
		if( output == null ) {
			output = new MultiSpectral<ImageFloat32>(ImageFloat32.class,width,height,3);
		} else if( output.width != width || output.height != height )
			throw new IllegalArgumentException("output width and height must be "+width+" "+height);
		else if( output.getNumBands() != 3 )
			throw new IllegalArgumentException("three bands expected");

		ImplConvertNV21.nv21ToMultiRgb_F32(data,output);

		return output;
	}
}
