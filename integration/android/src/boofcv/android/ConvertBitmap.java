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

package boofcv.android;

import android.graphics.Bitmap;
import boofcv.struct.image.*;

import java.nio.ByteBuffer;

/**
 * Functions for converting Android Bitmap images into BoofCV formats.  In earlier versions of Android
 * there is no way to directly access the internal array used by Bitmap.  You have to provide an array for it to
 * be copied into.  This is why the storage array is provided.
 * 
 * @author Peter Abeles
 *
 */
public class ConvertBitmap {
	
	/**
	 * Declares a storage array for the provided Bitmap image.  The array will be
	 * of size = width*height*byteCount where byteCount is the number of bytes per
	 * pixel.
	 * 
	 * @param input Bitmap image.
	 * @param storage If not null then the array size will be checked, if too small a new array will be returned.
	 * @return An array of appropriate size.
	 */
	public static byte[] declareStorage( Bitmap input , byte[] storage ) {
		int byteCount = input.getConfig() == Bitmap.Config.ARGB_8888 ? 4 : 2;
		int length = input.getWidth()*input.getHeight()*byteCount;
		
		if( storage == null || storage.length < length )
			return new byte[ length ];
		return storage;
	}

	/**
	 * Converts a {@link Bitmap} into a BoofCV image.  Type is determined at runtime.
	 * @param input Bitmap image.
	 * @param output Output image
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 */
	public static <T extends ImageBase>
	void bitmapToBoof( Bitmap input , T output , byte[] storage) {
		switch( output.getImageType().getFamily() ) {
			case GRAY: {
				if( output.getClass() == GrayF32.class )
					bitmapToGray(input, (GrayF32) output, storage);
				else if( output.getClass() == GrayU8.class )
					bitmapToGray(input,(GrayU8)output,storage);
				else
					throw new IllegalArgumentException("Unsupported BoofCV Image Type");
			} break;

			case PLANAR:
				Planar pl = (Planar)output;
				bitmapToMS(input,pl,pl.getBandType(),storage);
			break;

			default:
				throw new IllegalArgumentException("Unsupported BoofCV Image Type");
		}
	}

	/**
	 * Converts Bitmap image into a single band image of arbitrary type.
	 *
	 * @see #declareStorage(android.graphics.Bitmap, byte[])
	 * 
	 * @param input Input Bitmap image.
	 * @param output Output single band image.  If null a new one will be declared.
	 * @param imageType Type of single band image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally. 
	 * @return The converted gray scale image.
	 */
	public static <T extends ImageGray>
	T bitmapToGray( Bitmap input , T output , Class<T> imageType , byte[] storage) {
		if( imageType == GrayF32.class )
			return (T)bitmapToGray(input,(GrayF32)output,storage);
		else if( imageType == GrayU8.class )
			return (T)bitmapToGray(input,(GrayU8)output,storage);
		else
			throw new IllegalArgumentException("Unsupported BoofCV Image Type");
		
	}
	
	/**
	 * Converts Bitmap image into GrayU8.
	 * 
	 * @param input Input Bitmap image.
	 * @param output Output image.  If null a new one will be declared.
	 * @param storage Byte array used for internal storage. If null it will be declared internally. 
	 * @return The converted gray scale image.
	 */
	public static GrayU8 bitmapToGray( Bitmap input , GrayU8 output , byte[] storage ) {
		if( output == null ) {
			output = new GrayU8( input.getWidth() , input.getHeight() );
		} else if( output.getWidth() != input.getWidth() || output.getHeight() != input.getHeight() ) {
			throw new IllegalArgumentException("Image shapes are not the same");
		}
		
		if( storage == null )
			storage = declareStorage(input,null);
		input.copyPixelsToBuffer(ByteBuffer.wrap(storage));
		
		ImplConvertBitmap.arrayToGray(storage, input.getConfig(), output);
		
		return output;
	}
	
	/**
	 * Converts Bitmap image into GrayF32.
	 *
	 * @see #declareStorage(android.graphics.Bitmap, byte[])
	 * 
	 * @param input Input Bitmap image.
	 * @param output Output image.  If null a new one will be declared.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 * @return The converted gray scale image.
	 */
	public static GrayF32 bitmapToGray( Bitmap input , GrayF32 output , byte[] storage) {
		if( output == null ) {
			output = new GrayF32( input.getWidth() , input.getHeight() );
		} else if( output.getWidth() != input.getWidth() || output.getHeight() != input.getHeight() ) {
			throw new IllegalArgumentException("Image shapes are not the same");
		}
		
		if( storage == null )
			storage = declareStorage(input,null);
		input.copyPixelsToBuffer(ByteBuffer.wrap(storage));
		
		ImplConvertBitmap.arrayToGray(storage, input.getConfig(), output);
		
		return output;
	}
	
	/**
	 * Converts Bitmap image into Planar image of the appropriate type.
	 *
	 * @see #declareStorage(android.graphics.Bitmap, byte[])
	 * 
	 * @param input Input Bitmap image.
	 * @param output Output image.  If null a new one will be declared.
	 * @param type The type of internal single band image used in the Planar image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 * @return The converted Planar image.
	 */
	public static <T extends ImageGray>
	Planar<T> bitmapToMS( Bitmap input , Planar<T> output , Class<T> type , byte[] storage ) {
		if( output == null ) {
			output = new Planar<>(type, input.getWidth(), input.getHeight(), 4);
		} else if( output.getWidth() != input.getWidth() || output.getHeight() != input.getHeight() ) {
			throw new IllegalArgumentException("Image shapes are not the same");
		}

		if( storage == null )
			storage = declareStorage(input,null);
		input.copyPixelsToBuffer(ByteBuffer.wrap(storage));
		
		if( type == GrayU8.class )
			ImplConvertBitmap.arrayToMulti_U8(storage, input.getConfig(), (Planar)output);
		else if( type == GrayF32.class )
			ImplConvertBitmap.arrayToMulti_F32(storage, input.getConfig(), (Planar)output);
		else
			throw new IllegalArgumentException("Unsupported BoofCV Type");

		return output;
	}

	/**
	 * Converts many BoofCV image types into a Bitmap.
	 *
	 * @see #declareStorage(android.graphics.Bitmap, byte[])
	 *
	 * @param input Input BoofCV image.
	 * @param output Output Bitmap image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 */
	public static void boofToBitmap( ImageBase input , Bitmap output , byte[] storage) {
		if( input instanceof Planar ) {
			multiToBitmap((Planar)input,output,storage);
		} else if( input instanceof ImageGray ) {
			grayToBitmap((ImageGray)input,output,storage);
		} else if( input instanceof ImageInterleaved ) {
			interleavedToBitmap((ImageInterleaved) input, output, storage);
		} else {
			throw new IllegalArgumentException("Unsupported input image type");
		}
	}

	/**
	 * Converts ImageGray into Bitmap.
	 *
	 * @see #declareStorage(android.graphics.Bitmap, byte[])
	 * 
	 * @param input Input gray scale image.
	 * @param output Output Bitmap image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 */
	public static void grayToBitmap( ImageGray input , Bitmap output , byte[] storage) {
		if( input instanceof GrayU8 )
			grayToBitmap((GrayU8)input,output,storage);
		else if( input instanceof GrayF32 )
			grayToBitmap((GrayF32)input,output,storage);
		else
			throw new IllegalArgumentException("Unsupported BoofCV Type: "+input);
	}
	
	/**
	 * Converts ImageGray into Bitmap.
	 *
	 * @see #declareStorage(android.graphics.Bitmap, byte[])
	 * 
	 * @param input Input gray scale image.
	 * @param output Output Bitmap image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 */
	public static void grayToBitmap( GrayU8 input , Bitmap output , byte[] storage) {
		if( output.getWidth() != input.getWidth() || output.getHeight() != input.getHeight() ) {
			throw new IllegalArgumentException("Image shapes are not the same");
		}
		
		if( storage == null )
			storage = declareStorage(output,null);
		
		ImplConvertBitmap.grayToArray(input, storage,output.getConfig());
		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}
	
	/**
	 * Converts ImageGray into Bitmap.
	 *
	 * @see #declareStorage(android.graphics.Bitmap, byte[])
	 * 
	 * @param input Input gray scale image.
	 * @param output Output Bitmap image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 */
	public static void grayToBitmap( GrayF32 input , Bitmap output , byte[] storage ) {
		if( output.getWidth() != input.getWidth() || output.getHeight() != input.getHeight() ) {
			throw new IllegalArgumentException("Image shapes are not the same");
		}
		
		if( storage == null )
			storage = declareStorage(output,null);
		
		ImplConvertBitmap.grayToArray(input, storage,output.getConfig());
		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}
	
	/**
	 * Converts Planar image into Bitmap.
	 *
	 * @see #declareStorage(android.graphics.Bitmap, byte[])
	 *
	 * @param input Input Planar image.
	 * @param output Output Bitmap image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 */
	public static <T extends ImageGray>
	void multiToBitmap(  Planar<T> input , Bitmap output , byte[] storage ) {
		if( output.getWidth() != input.getWidth() || output.getHeight() != input.getHeight() ) {
			throw new IllegalArgumentException("Image shapes are not the same");
		}
		
		if( storage == null )
			storage = declareStorage(output,null);
		
		if( input.getBandType() == GrayU8.class )
			ImplConvertBitmap.multiToArray_U8((Planar)input, storage,output.getConfig());
		else if( input.getBandType() == GrayF32.class )
			ImplConvertBitmap.multiToArray_F32((Planar)input, storage,output.getConfig());
		else
			throw new IllegalArgumentException("Unsupported BoofCV Type");
		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}

	/**
	 * Converts {@link ImageInterleaved} image into Bitmap.
	 *
	 * @see #declareStorage(android.graphics.Bitmap, byte[])
	 *
	 * @param input Input Planar image.
	 * @param output Output Bitmap image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 */
	public static <T extends ImageInterleaved>
	void interleavedToBitmap(T input, Bitmap output, byte[] storage) {
		if( output.getWidth() != input.getWidth() || output.getHeight() != input.getHeight() ) {
			throw new IllegalArgumentException("Image shapes are not the same");
		}

		if( storage == null )
			storage = declareStorage(output,null);

		if( input.getImageType().getDataType() == ImageDataType.U8 )
			ImplConvertBitmap.interleavedToArray((InterleavedU8) input, storage, output.getConfig());
		else if( input.getImageType().getDataType() == ImageDataType.F32 )
			ImplConvertBitmap.interleavedToArray((InterleavedF32) input, storage, output.getConfig());
		else
			throw new IllegalArgumentException("Unsupported BoofCV Type");
		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage));
	}
	
	/**
	 * Converts GrayU8 into a new Bitmap.
	 * 
	 * @param input Input gray scale image.
	 * @param config Type of Bitmap image to create.
	 */
	public static Bitmap grayToBitmap( GrayU8 input , Bitmap.Config config ) {
		Bitmap output = Bitmap.createBitmap(input.width, input.height, config);
		
		grayToBitmap(input,output,null);
		
		return output;
	}
	
	/**
	 * Converts GrayF32 into a new Bitmap.
	 * 
	 * @param input Input gray scale image.
	 * @param config Type of Bitmap image to create.
	 */
	public static Bitmap grayToBitmap( GrayF32 input , Bitmap.Config config ) {
		Bitmap output = Bitmap.createBitmap(input.width, input.height, config);
		
		grayToBitmap(input,output,null);
		
		return output;
	}
}
