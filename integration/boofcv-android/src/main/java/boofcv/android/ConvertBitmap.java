/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.color.ColorFormat;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_I8;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * Functions for converting Android Bitmap images into BoofCV formats. In earlier versions of Android
 * there is no way to directly access the internal array used by Bitmap. You have to provide an array for it to
 * be copied into. This is why the storage array is provided.
 *
 * @author Peter Abeles
 */
public class ConvertBitmap {

	/**
	 * Checks to see if the bitmap is the same shape as the input image. if not a new instance is returned which is
	 * otherwise the same instance is returned.
	 */
	public static Bitmap checkDeclare( ImageBase input, @Nullable Bitmap bitmap ) {
		if (bitmap == null) {
			return Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888);
		} else if (input.width != bitmap.getWidth() || input.height != bitmap.getHeight()) {
			return Bitmap.createBitmap(input.width, input.height, bitmap.getConfig());
		} else {
			return bitmap;
		}
	}

	public static DogArray_I8 resizeStorage( Bitmap input, @Nullable DogArray_I8 storage ) {
		int byteCount = input.getConfig() == Bitmap.Config.ARGB_8888 ? 4 : 2;
		int length = input.getWidth()*input.getHeight()*byteCount;

		if (storage == null)
			return new DogArray_I8(length);
		else {
			storage.resize(length);
			return storage;
		}
	}

	/**
	 * Converts a {@link Bitmap} into a BoofCV image. Type is determined at runtime.
	 *
	 * @param input Bitmap image.
	 * @param output Output image. Automatically resized to match input shape.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 */
	public static <T extends ImageBase<T>>
	void bitmapToBoof( Bitmap input, T output, @Nullable DogArray_I8 storage ) {
		storage = resizeStorage(input, storage);
		if (BOverrideConvertAndroid.invokeBitmapToBoof(input, output, storage.data))
			return;

		switch (output.getImageType().getFamily()) {
			case GRAY: {
				if (output.getClass() == GrayF32.class)
					bitmapToGray(input, (GrayF32)output, storage);
				else if (output.getClass() == GrayU8.class)
					bitmapToGray(input, (GrayU8)output, storage);
				else
					throw new IllegalArgumentException("Unsupported BoofCV Image Type");
			}
			break;

			case PLANAR:
				Planar pl = (Planar)output;
				bitmapToPlanar(input, pl, pl.getBandType(), storage);
				break;

			case INTERLEAVED:
				if (output.getClass() == InterleavedU8.class)
					bitmapToInterleaved(input, (InterleavedU8)output, storage);
				else if (output.getClass() == InterleavedF32.class)
					bitmapToInterleaved(input, (InterleavedF32)output, storage);
				else
					throw new IllegalArgumentException("Unsupported BoofCV Image Type");
				break;

			default:
				throw new IllegalArgumentException("Unsupported BoofCV Image Type");
		}
	}

	/**
	 * Converts Bitmap image into a single band image of arbitrary type.
	 *
	 * @param input Input Bitmap image.
	 * @param output Output single band image. If null a new one will be declared.
	 * @param imageType Type of single band image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 * @return The converted gray scale image.
	 */
	public static <T extends ImageGray<T>>
	T bitmapToGray( Bitmap input, T output, Class<T> imageType, @Nullable DogArray_I8 storage ) {
		storage = resizeStorage(input, storage);
		if (imageType == GrayF32.class)
			return (T)bitmapToGray(input, (GrayF32)output, storage);
		else if (imageType == GrayU8.class)
			return (T)bitmapToGray(input, (GrayU8)output, storage);
		else
			throw new IllegalArgumentException("Unsupported BoofCV Image Type");
	}

	/**
	 * Converts Bitmap image into GrayU8.
	 *
	 * @param input Input Bitmap image.
	 * @param output Output image. If null a new one will be declared.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 * @return The converted gray scale image.
	 */
	public static GrayU8 bitmapToGray( Bitmap input, GrayU8 output, @Nullable DogArray_I8 storage ) {
		storage = resizeStorage(input, storage);
		if (output == null) {
			output = new GrayU8(input.getWidth(), input.getHeight());
		} else {
			output.reshape(input.getWidth(), input.getHeight());
		}

		input.copyPixelsToBuffer(ByteBuffer.wrap(storage.data));

		ImplConvertBitmap.arrayToGray(storage.data, input.getConfig(), output);

		return output;
	}

	/**
	 * Converts Bitmap image into GrayF32.
	 *
	 * @param input Input Bitmap image.
	 * @param output Output image. If null a new one will be declared.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 * @return The converted gray scale image.
	 */
	public static GrayF32 bitmapToGray( Bitmap input, GrayF32 output, @Nullable DogArray_I8 storage ) {
		if (output == null) {
			output = new GrayF32(input.getWidth(), input.getHeight());
		} else {
			output.reshape(input.getWidth(), input.getHeight());
		}

		storage = resizeStorage(input, storage);
		input.copyPixelsToBuffer(ByteBuffer.wrap(storage.data));

		ImplConvertBitmap.arrayToGray(storage.data, input.getConfig(), output);

		return output;
	}

	/**
	 * Converts Bitmap image into Planar image of the appropriate type.
	 *
	 * @param input Input Bitmap image.
	 * @param output Output image. If null a new one will be declared.
	 * @param type The type of internal single band image used in the Planar image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 * @return The converted Planar image.
	 */
	public static <T extends ImageGray<T>>
	Planar<T> bitmapToPlanar( Bitmap input, Planar<T> output, Class<T> type, @Nullable DogArray_I8 storage ) {
		if (output == null) {
			output = new Planar<>(type, input.getWidth(), input.getHeight(), 3);
		} else {
			int numBands = Math.min(4, Math.max(3, output.getNumBands()));
			output.reshape(input.getWidth(), input.getHeight(), numBands);
		}

		storage = resizeStorage(input, storage);
		input.copyPixelsToBuffer(ByteBuffer.wrap(storage.data));

		if (type == GrayU8.class)
			ImplConvertBitmap.arrayToPlanar_U8(storage.data, input.getConfig(), (Planar)output);
		else if (type == GrayF32.class)
			ImplConvertBitmap.arrayToPlanar_F32(storage.data, input.getConfig(), (Planar)output);
		else
			throw new IllegalArgumentException("Unsupported BoofCV Type");

		return output;
	}

	/**
	 * Converts Bitmap image into InterleavedU8.
	 *
	 * @param input Input Bitmap image.
	 * @param output Output image. If null a new one will be declared.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 * @return The converted gray scale image.
	 */
	public static InterleavedU8 bitmapToInterleaved( Bitmap input, InterleavedU8 output, @Nullable DogArray_I8 storage ) {
		if (output == null) {
			output = new InterleavedU8(input.getWidth(), input.getHeight(), 3);
		} else {
			if (output.getNumBands() < 3 || output.getNumBands() > 4)
				output.reshape(input.getWidth(), input.getHeight(), 3);
			else
				output.reshape(input.getWidth(), input.getHeight());
		}

		storage = resizeStorage(input, storage);
		input.copyPixelsToBuffer(ByteBuffer.wrap(storage.data));

		ImplConvertBitmap.arrayToInterleaved_U8(storage.data, input.getConfig(), output);

		return output;
	}

	/**
	 * Converts Bitmap image into 	public static InterleavedF32 bitmapToInterleaved( Bitmap input, InterleavedU8 output, @Nullable DogArray_I8 storage ) {.
	 *
	 * @param input Input Bitmap image.
	 * @param output Output image. If null a new one will be declared.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 * @return The converted gray scale image.
	 */
	public static InterleavedF32 bitmapToInterleaved( Bitmap input, InterleavedF32 output, @Nullable DogArray_I8 storage ) {
		if (output == null) {
			output = new InterleavedF32(input.getWidth(), input.getHeight(), 3);
		} else {
			if (output.getNumBands() < 3 || output.getNumBands() > 4)
				output.reshape(input.getWidth(), input.getHeight(), 3);
			else
				output.reshape(input.getWidth(), input.getHeight());
		}

		storage = resizeStorage(input, storage);
		input.copyPixelsToBuffer(ByteBuffer.wrap(storage.data));

		ImplConvertBitmap.arrayToInterleaved_F32(storage.data, input.getConfig(), output);

		return output;
	}

	/**
	 * Converts many BoofCV image types into a Bitmap.
	 *
	 * @param input Input BoofCV image.
	 * @param output Output Bitmap image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 */
	public static void boofToBitmap( ImageBase input, Bitmap output, @Nullable DogArray_I8 storage ) {
		storage = resizeStorage(output, storage);
		if (BOverrideConvertAndroid.invokeBoofToBitmap(ColorFormat.RGB, input, output, storage.data))
			return;

		if (input instanceof Planar) {
			planarToBitmap((Planar)input, output, storage);
		} else if (input instanceof ImageGray) {
			grayToBitmap((ImageGray)input, output, storage);
		} else if (input instanceof ImageInterleaved) {
			interleavedToBitmap((ImageInterleaved)input, output, storage);
		} else {
			throw new IllegalArgumentException("Unsupported input image type");
		}
	}

	public static void boofToBitmap( ColorFormat color, ImageBase input, Bitmap output, @Nullable DogArray_I8 storage ) {
		storage = resizeStorage(output, storage);
		if (BOverrideConvertAndroid.invokeBoofToBitmap(color, input, output, storage.data))
			return;

		if (input instanceof ImageGray) {
			grayToBitmap((ImageGray)input, output, storage);
			return;
		}

		switch (color) {
			case RGB -> {
				boofToBitmap(input, output, storage);
				return;
			}
			case YUV -> {
				if (input instanceof ImageInterleaved) {
					interleavedYuvToBitmap((ImageInterleaved)input, output, storage);
					return;
				}
			}
			default -> {}
		}
		throw new IllegalArgumentException("Unsupported input image type");
	}

	/**
	 * Converts ImageGray into Bitmap.
	 *
	 * @param input Input gray scale image.
	 * @param output Output Bitmap image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 */
	public static void grayToBitmap( ImageGray input, Bitmap output, @Nullable DogArray_I8 storage ) {
		if (input instanceof GrayU8)
			grayToBitmap((GrayU8)input, output, storage);
		else if (input instanceof GrayF32)
			grayToBitmap((GrayF32)input, output, storage);
		else
			throw new IllegalArgumentException("Unsupported BoofCV Type: " + input);
	}

	/**
	 * Converts ImageGray into Bitmap.
	 *
	 * @param input Input gray scale image.
	 * @param output Output Bitmap image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 */
	public static void grayToBitmap( GrayU8 input, Bitmap output, @Nullable DogArray_I8 storage ) {
		if (output.getWidth() != input.getWidth() || output.getHeight() != input.getHeight()) {
			throw new IllegalArgumentException("Image shapes are not the same");
		}

		storage = resizeStorage(output, storage);

		ImplConvertBitmap.grayToArray(input, storage.data, output.getConfig());
		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage.data));
	}

	/**
	 * Converts ImageGray into Bitmap.
	 *
	 * @param input Input gray scale image.
	 * @param output Output Bitmap image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 */
	public static void grayToBitmap( GrayF32 input, Bitmap output, @Nullable DogArray_I8 storage ) {
		if (output.getWidth() != input.getWidth() || output.getHeight() != input.getHeight()) {
			throw new IllegalArgumentException("Image shapes are not the same");
		}

		storage = resizeStorage(output, storage);

		ImplConvertBitmap.grayToArray(input, storage.data, output.getConfig());
		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage.data));
	}

	/**
	 * Converts Planar image into Bitmap.
	 *
	 * @param input Input Planar image.
	 * @param output Output Bitmap image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 */
	public static <T extends ImageGray<T>>
	void planarToBitmap( Planar<T> input, Bitmap output, @Nullable DogArray_I8 storage ) {
		if (output.getWidth() != input.getWidth() || output.getHeight() != input.getHeight()) {
			throw new IllegalArgumentException("Image shapes are not the same");
		}

		storage = resizeStorage(output, storage);

		if (input.getBandType() == GrayU8.class)
			ImplConvertBitmap.planarToArray_U8((Planar)input, storage.data, output.getConfig());
		else if (input.getBandType() == GrayF32.class)
			ImplConvertBitmap.planarToArray_F32((Planar)input, storage.data, output.getConfig());
		else
			throw new IllegalArgumentException("Unsupported BoofCV Type");
		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage.data));
	}

	/**
	 * Converts {@link ImageInterleaved} image into Bitmap.
	 *
	 * @param input Input Planar image.
	 * @param output Output Bitmap image.
	 * @param storage Byte array used for internal storage. If null it will be declared internally.
	 */
	public static <T extends ImageInterleaved<T>>
	void interleavedToBitmap( T input, Bitmap output, @Nullable DogArray_I8 storage ) {
		if (output.getWidth() != input.getWidth() || output.getHeight() != input.getHeight()) {
			throw new IllegalArgumentException("Image shapes are not the same");
		}

		storage = resizeStorage(output, storage);

		if (input.getImageType().getDataType() == ImageDataType.U8)
			ImplConvertBitmap.interleavedToArray((InterleavedU8)input, storage.data, output.getConfig());
		else if (input.getImageType().getDataType() == ImageDataType.F32)
			ImplConvertBitmap.interleavedToArray((InterleavedF32)input, storage.data, output.getConfig());
		else
			throw new IllegalArgumentException("Unsupported BoofCV Type");
		output.copyPixelsFromBuffer(ByteBuffer.wrap(storage.data));
	}

	public static <T extends ImageInterleaved<T>>
	void interleavedYuvToBitmap( T input, Bitmap output, @Nullable DogArray_I8 storage ) {
		if (output.getWidth() != input.getWidth() || output.getHeight() != input.getHeight()) {
			throw new IllegalArgumentException("Image shapes are not the same");
		}

		storage = resizeStorage(output, storage);

		if (input.getImageType().getDataType() == ImageDataType.U8) {
			switch (output.getConfig()) {
				case ARGB_8888 -> {
					ImplConvertBitmap.interleavedYuvToArgb8888((InterleavedU8)input, storage.data);
					output.copyPixelsFromBuffer(ByteBuffer.wrap(storage.data));
					return;
				}
				case RGB_565 -> {
					ImplConvertBitmap.interleavedYuvToRGB565((InterleavedU8)input, storage.data);
					output.copyPixelsFromBuffer(ByteBuffer.wrap(storage.data));
					return;
				}
				default -> {
				}
			}
		}
		throw new IllegalArgumentException("Unsupported BoofCV Type");
	}

	/**
	 * Converts GrayU8 into a new Bitmap.
	 *
	 * @param input Input gray scale image.
	 * @param config Type of Bitmap image to create.
	 */
	public static Bitmap grayToBitmap( GrayU8 input, Bitmap.Config config ) {
		Bitmap output = Bitmap.createBitmap(input.width, input.height, config);

		grayToBitmap(input, output, null);

		return output;
	}

	/**
	 * Converts GrayF32 into a new Bitmap.
	 *
	 * @param input Input gray scale image.
	 * @param config Type of Bitmap image to create.
	 */
	public static Bitmap grayToBitmap( GrayF32 input, Bitmap.Config config ) {
		Bitmap output = Bitmap.createBitmap(input.width, input.height, config);

		grayToBitmap(input, output, null);

		return output;
	}
}
