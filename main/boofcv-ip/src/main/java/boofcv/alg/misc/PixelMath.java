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

package boofcv.alg.misc;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.misc.PixelMathLambdas.*;
import boofcv.alg.misc.impl.ImplPixelMath;
import boofcv.alg.misc.impl.ImplPixelMath_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.*;

import javax.annotation.Generated;

/**
 * Functions which perform basic arithmetic (e.g. addition, subtraction, multiplication, or division) on a pixel by pixel basis.
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GeneratePixelMath</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.misc.GeneratePixelMath")
public class PixelMath {
	/**
	 * If an image has fewer pixels than this it will not run a concurrent algorithm. The overhead makes it slower.
	 */
	public static int SMALL_IMAGE = 100*100;

	/**
	 * Applies the lambda operation to each element in the input image. output[i] = function(input[i])
	 *
	 * @param input The input image. Not modified.
	 * @param function The function to apply.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void operator1( GrayI8 input, Function1_I8 function, GrayI8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		} else {
			ImplPixelMath.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the input image. output[i] = function(input[i])
	 *
	 * @param input The input image. Not modified.
	 * @param function The function to apply.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void operator1( InterleavedI8 input, Function1_I8 function, InterleavedI8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		} else {
			ImplPixelMath.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the input image. output[i] = function(input[i])
	 *
	 * @param input The input image. Not modified.
	 * @param function The function to apply.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void operator1( GrayI16 input, Function1_I16 function, GrayI16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		} else {
			ImplPixelMath.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the input image. output[i] = function(input[i])
	 *
	 * @param input The input image. Not modified.
	 * @param function The function to apply.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void operator1( InterleavedI16 input, Function1_I16 function, InterleavedI16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		} else {
			ImplPixelMath.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the input image. output[i] = function(input[i])
	 *
	 * @param input The input image. Not modified.
	 * @param function The function to apply.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void operator1( GrayS32 input, Function1_S32 function, GrayS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		} else {
			ImplPixelMath.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the input image. output[i] = function(input[i])
	 *
	 * @param input The input image. Not modified.
	 * @param function The function to apply.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void operator1( InterleavedS32 input, Function1_S32 function, InterleavedS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		} else {
			ImplPixelMath.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the input image. output[i] = function(input[i])
	 *
	 * @param input The input image. Not modified.
	 * @param function The function to apply.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void operator1( GrayS64 input, Function1_S64 function, GrayS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		} else {
			ImplPixelMath.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the input image. output[i] = function(input[i])
	 *
	 * @param input The input image. Not modified.
	 * @param function The function to apply.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void operator1( InterleavedS64 input, Function1_S64 function, InterleavedS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		} else {
			ImplPixelMath.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the input image. output[i] = function(input[i])
	 *
	 * @param input The input image. Not modified.
	 * @param function The function to apply.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void operator1( GrayF32 input, Function1_F32 function, GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		} else {
			ImplPixelMath.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the input image. output[i] = function(input[i])
	 *
	 * @param input The input image. Not modified.
	 * @param function The function to apply.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void operator1( InterleavedF32 input, Function1_F32 function, InterleavedF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		} else {
			ImplPixelMath.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the input image. output[i] = function(input[i])
	 *
	 * @param input The input image. Not modified.
	 * @param function The function to apply.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void operator1( GrayF64 input, Function1_F64 function, GrayF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		} else {
			ImplPixelMath.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the input image. output[i] = function(input[i])
	 *
	 * @param input The input image. Not modified.
	 * @param function The function to apply.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void operator1( InterleavedF64 input, Function1_F64 function, InterleavedF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		} else {
			ImplPixelMath.operator1(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the two input images. output[i] = function(imA[i],imgB[i])
	 *
	 * @param imgA Input image. Not modified.
	 * @param function The function to apply.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void operator2( GrayI8 imgA, Function2_I8 function, GrayI8 imgB, GrayI8 output ) {

		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int columns = imgA.width;
		int N = imgA.width*imgA.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		} else {
			ImplPixelMath.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the two input images. output[i] = function(imA[i],imgB[i])
	 *
	 * @param imgA Input image. Not modified.
	 * @param function The function to apply.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void operator2( InterleavedI8 imgA, Function2_I8 function, InterleavedI8 imgB, InterleavedI8 output ) {

		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int columns = imgA.width*imgA.numBands;
		int N = imgA.width*imgA.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		} else {
			ImplPixelMath.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the two input images. output[i] = function(imA[i],imgB[i])
	 *
	 * @param imgA Input image. Not modified.
	 * @param function The function to apply.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void operator2( GrayI16 imgA, Function2_I16 function, GrayI16 imgB, GrayI16 output ) {

		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int columns = imgA.width;
		int N = imgA.width*imgA.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		} else {
			ImplPixelMath.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the two input images. output[i] = function(imA[i],imgB[i])
	 *
	 * @param imgA Input image. Not modified.
	 * @param function The function to apply.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void operator2( InterleavedI16 imgA, Function2_I16 function, InterleavedI16 imgB, InterleavedI16 output ) {

		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int columns = imgA.width*imgA.numBands;
		int N = imgA.width*imgA.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		} else {
			ImplPixelMath.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the two input images. output[i] = function(imA[i],imgB[i])
	 *
	 * @param imgA Input image. Not modified.
	 * @param function The function to apply.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void operator2( GrayS32 imgA, Function2_S32 function, GrayS32 imgB, GrayS32 output ) {

		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int columns = imgA.width;
		int N = imgA.width*imgA.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		} else {
			ImplPixelMath.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the two input images. output[i] = function(imA[i],imgB[i])
	 *
	 * @param imgA Input image. Not modified.
	 * @param function The function to apply.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void operator2( InterleavedS32 imgA, Function2_S32 function, InterleavedS32 imgB, InterleavedS32 output ) {

		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int columns = imgA.width*imgA.numBands;
		int N = imgA.width*imgA.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		} else {
			ImplPixelMath.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the two input images. output[i] = function(imA[i],imgB[i])
	 *
	 * @param imgA Input image. Not modified.
	 * @param function The function to apply.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void operator2( GrayS64 imgA, Function2_S64 function, GrayS64 imgB, GrayS64 output ) {

		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int columns = imgA.width;
		int N = imgA.width*imgA.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		} else {
			ImplPixelMath.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the two input images. output[i] = function(imA[i],imgB[i])
	 *
	 * @param imgA Input image. Not modified.
	 * @param function The function to apply.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void operator2( InterleavedS64 imgA, Function2_S64 function, InterleavedS64 imgB, InterleavedS64 output ) {

		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int columns = imgA.width*imgA.numBands;
		int N = imgA.width*imgA.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		} else {
			ImplPixelMath.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the two input images. output[i] = function(imA[i],imgB[i])
	 *
	 * @param imgA Input image. Not modified.
	 * @param function The function to apply.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void operator2( GrayF32 imgA, Function2_F32 function, GrayF32 imgB, GrayF32 output ) {

		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int columns = imgA.width;
		int N = imgA.width*imgA.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		} else {
			ImplPixelMath.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the two input images. output[i] = function(imA[i],imgB[i])
	 *
	 * @param imgA Input image. Not modified.
	 * @param function The function to apply.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void operator2( InterleavedF32 imgA, Function2_F32 function, InterleavedF32 imgB, InterleavedF32 output ) {

		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int columns = imgA.width*imgA.numBands;
		int N = imgA.width*imgA.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		} else {
			ImplPixelMath.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the two input images. output[i] = function(imA[i],imgB[i])
	 *
	 * @param imgA Input image. Not modified.
	 * @param function The function to apply.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void operator2( GrayF64 imgA, Function2_F64 function, GrayF64 imgB, GrayF64 output ) {

		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int columns = imgA.width;
		int N = imgA.width*imgA.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		} else {
			ImplPixelMath.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		}
	}

	/**
	 * Applies the lambda operation to each element in the two input images. output[i] = function(imA[i],imgB[i])
	 *
	 * @param imgA Input image. Not modified.
	 * @param function The function to apply.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void operator2( InterleavedF64 imgA, Function2_F64 function, InterleavedF64 imgB, InterleavedF64 output ) {

		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int columns = imgA.width*imgA.numBands;
		int N = imgA.width*imgA.height;
		if (BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		} else {
			ImplPixelMath.operator2(imgA.data, imgA.startIndex, imgA.stride,
					imgB.data, imgB.startIndex, imgB.stride,
					output.data, output.startIndex, output.stride,
					imgA.height, columns, function);
		}
	}

	/**
	 * Sets each pixel in the output image to be the absolute value of the input image.
	 * Both the input and output image can be the same instance.
	 * 
	 * @param input The input image. Not modified.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void abs( GrayS8 input , GrayS8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Sets each pixel in the output image to be the absolute value of the input image.
	 * Both the input and output image can be the same instance.
	 * 
	 * @param input The input image. Not modified.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void abs( InterleavedS8 input , InterleavedS8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Sets each pixel in the output image to be the absolute value of the input image.
	 * Both the input and output image can be the same instance.
	 * 
	 * @param input The input image. Not modified.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void abs( GrayS16 input , GrayS16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Sets each pixel in the output image to be the absolute value of the input image.
	 * Both the input and output image can be the same instance.
	 * 
	 * @param input The input image. Not modified.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void abs( InterleavedS16 input , InterleavedS16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Sets each pixel in the output image to be the absolute value of the input image.
	 * Both the input and output image can be the same instance.
	 * 
	 * @param input The input image. Not modified.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void abs( GrayS32 input , GrayS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Sets each pixel in the output image to be the absolute value of the input image.
	 * Both the input and output image can be the same instance.
	 * 
	 * @param input The input image. Not modified.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void abs( InterleavedS32 input , InterleavedS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Sets each pixel in the output image to be the absolute value of the input image.
	 * Both the input and output image can be the same instance.
	 * 
	 * @param input The input image. Not modified.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void abs( GrayS64 input , GrayS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Sets each pixel in the output image to be the absolute value of the input image.
	 * Both the input and output image can be the same instance.
	 * 
	 * @param input The input image. Not modified.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void abs( InterleavedS64 input , InterleavedS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Sets each pixel in the output image to be the absolute value of the input image.
	 * Both the input and output image can be the same instance.
	 * 
	 * @param input The input image. Not modified.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void abs( GrayF32 input , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Sets each pixel in the output image to be the absolute value of the input image.
	 * Both the input and output image can be the same instance.
	 * 
	 * @param input The input image. Not modified.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void abs( InterleavedF32 input , InterleavedF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Sets each pixel in the output image to be the absolute value of the input image.
	 * Both the input and output image can be the same instance.
	 * 
	 * @param input The input image. Not modified.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void abs( GrayF64 input , GrayF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Sets each pixel in the output image to be the absolute value of the input image.
	 * Both the input and output image can be the same instance.
	 * 
	 * @param input The input image. Not modified.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static void abs( InterleavedF64 input , InterleavedF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.abs(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Changes the sign of every pixel in the image: output[x,y] = -input[x,y]
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the negated image is written to. Modified.
	 */
	public static void negative( GrayS8 input , GrayS8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Changes the sign of every pixel in the image: output[x,y] = -input[x,y]
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the negated image is written to. Modified.
	 */
	public static void negative( InterleavedS8 input , InterleavedS8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Changes the sign of every pixel in the image: output[x,y] = -input[x,y]
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the negated image is written to. Modified.
	 */
	public static void negative( GrayS16 input , GrayS16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Changes the sign of every pixel in the image: output[x,y] = -input[x,y]
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the negated image is written to. Modified.
	 */
	public static void negative( InterleavedS16 input , InterleavedS16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Changes the sign of every pixel in the image: output[x,y] = -input[x,y]
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the negated image is written to. Modified.
	 */
	public static void negative( GrayS32 input , GrayS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Changes the sign of every pixel in the image: output[x,y] = -input[x,y]
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the negated image is written to. Modified.
	 */
	public static void negative( InterleavedS32 input , InterleavedS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Changes the sign of every pixel in the image: output[x,y] = -input[x,y]
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the negated image is written to. Modified.
	 */
	public static void negative( GrayS64 input , GrayS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Changes the sign of every pixel in the image: output[x,y] = -input[x,y]
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the negated image is written to. Modified.
	 */
	public static void negative( InterleavedS64 input , InterleavedS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Changes the sign of every pixel in the image: output[x,y] = -input[x,y]
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the negated image is written to. Modified.
	 */
	public static void negative( GrayF32 input , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Changes the sign of every pixel in the image: output[x,y] = -input[x,y]
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the negated image is written to. Modified.
	 */
	public static void negative( InterleavedF32 input , InterleavedF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Changes the sign of every pixel in the image: output[x,y] = -input[x,y]
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the negated image is written to. Modified.
	 */
	public static void negative( GrayF64 input , GrayF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Changes the sign of every pixel in the image: output[x,y] = -input[x,y]
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the negated image is written to. Modified.
	 */
	public static void negative( InterleavedF64 input , InterleavedF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		} else {
			ImplPixelMath.negative(input.data, input.startIndex, input.stride,
					output.data, output.startIndex, output.stride,
					input.height, columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayU8 input , double value , GrayU8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedU8 input , double value , InterleavedU8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayS8 input , double value , GrayS8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedS8 input , double value , InterleavedS8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayU16 input , double value , GrayU16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedU16 input , double value , InterleavedU16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayS16 input , double value , GrayS16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedS16 input , double value , InterleavedS16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayS32 input , double value , GrayS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedS32 input , double value , InterleavedS32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayS64 input , double value , GrayS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedS64 input , double value , InterleavedS64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayF32 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedF32 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayF64 input , double value , GrayF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedF64 input , double value , InterleavedF64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayU8 input , double value , int lower , int upper , GrayU8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiplyU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiplyU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedU8 input , double value , int lower , int upper , InterleavedU8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiplyU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiplyU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayS8 input , double value , int lower , int upper , GrayS8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedS8 input , double value , int lower , int upper , InterleavedS8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayU16 input , double value , int lower , int upper , GrayU16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiplyU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiplyU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedU16 input , double value , int lower , int upper , InterleavedU16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiplyU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiplyU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayS16 input , double value , int lower , int upper , GrayS16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedS16 input , double value , int lower , int upper , InterleavedS16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayS32 input , double value , int lower , int upper , GrayS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedS32 input , double value , int lower , int upper , InterleavedS32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayS64 input , double value , long lower , long upper , GrayS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedS64 input , double value , long lower , long upper , InterleavedS64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayF32 input , float value , float lower , float upper , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedF32 input , float value , float lower , float upper , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayF64 input , double value , double lower , double upper , GrayF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedF64 input , double value , double lower , double upper , InterleavedF64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayU8 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedU8 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayS8 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedS8 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayU16 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedU16 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiplyU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayS16 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedS16 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayS32 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedS32 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( GrayS64 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static void multiply( InterleavedS64 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.multiply_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayU8 input , double denominator , GrayU8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedU8 input , double denominator , InterleavedU8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayS8 input , double denominator , GrayS8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedS8 input , double denominator , InterleavedS8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayU16 input , double denominator , GrayU16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedU16 input , double denominator , InterleavedU16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayS16 input , double denominator , GrayS16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedS16 input , double denominator , InterleavedS16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayS32 input , double denominator , GrayS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedS32 input , double denominator , InterleavedS32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayS64 input , double denominator , GrayS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedS64 input , double denominator , InterleavedS64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayF32 input , float denominator , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedF32 input , float denominator , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayF64 input , double denominator , GrayF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedF64 input , double denominator , InterleavedF64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayU8 input , double denominator , int lower , int upper , GrayU8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divideU_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divideU_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedU8 input , double denominator , int lower , int upper , InterleavedU8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divideU_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divideU_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayS8 input , double denominator , int lower , int upper , GrayS8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedS8 input , double denominator , int lower , int upper , InterleavedS8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayU16 input , double denominator , int lower , int upper , GrayU16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divideU_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divideU_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedU16 input , double denominator , int lower , int upper , InterleavedU16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divideU_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divideU_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayS16 input , double denominator , int lower , int upper , GrayS16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedS16 input , double denominator , int lower , int upper , InterleavedS16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayS32 input , double denominator , int lower , int upper , GrayS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedS32 input , double denominator , int lower , int upper , InterleavedS32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayS64 input , double denominator , long lower , long upper , GrayS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedS64 input , double denominator , long lower , long upper , InterleavedS64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayF32 input , float denominator , float lower , float upper , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedF32 input , float denominator , float lower , float upper , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayF64 input , double denominator , double lower , double upper , GrayF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedF64 input , double denominator , double lower , double upper , InterleavedF64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayU8 input , float denominator , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedU8 input , float denominator , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayS8 input , float denominator , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedS8 input , float denominator , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayU16 input , float denominator , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedU16 input , float denominator , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divideU_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayS16 input , float denominator , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedS16 input , float denominator , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayS32 input , float denominator , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedS32 input , float denominator , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( GrayS64 input , float denominator , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static void divide( InterleavedS64 input , float denominator , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.divide_A(input.data,input.startIndex,input.stride,denominator , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayU8 input , int value , GrayU8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedU8 input , int value , InterleavedU8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayS8 input , int value , GrayS8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedS8 input , int value , InterleavedS8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayU16 input , int value , GrayU16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedU16 input , int value , InterleavedU16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayS16 input , int value , GrayS16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedS16 input , int value , InterleavedS16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayS32 input , int value , GrayS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedS32 input , int value , InterleavedS32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayS64 input , long value , GrayS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedS64 input , long value , InterleavedS64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayF32 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedF32 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayF64 input , double value , GrayF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedF64 input , double value , InterleavedF64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayU8 input , int value , int lower , int upper , GrayU8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedU8 input , int value , int lower , int upper , InterleavedU8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayS8 input , int value , int lower , int upper , GrayS8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedS8 input , int value , int lower , int upper , InterleavedS8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayU16 input , int value , int lower , int upper , GrayU16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedU16 input , int value , int lower , int upper , InterleavedU16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayS16 input , int value , int lower , int upper , GrayS16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedS16 input , int value , int lower , int upper , InterleavedS16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayS32 input , int value , int lower , int upper , GrayS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedS32 input , int value , int lower , int upper , InterleavedS32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayS64 input , long value , long lower , long upper , GrayS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedS64 input , long value , long lower , long upper , InterleavedS64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayF32 input , float value , float lower , float upper , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedF32 input , float value , float lower , float upper , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayF64 input , double value , double lower , double upper , GrayF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedF64 input , double value , double lower , double upper , InterleavedF64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayU8 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedU8 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayS8 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedS8 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayU16 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedU16 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayS16 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedS16 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayS32 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedS32 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( GrayS64 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Adds a scalar value to each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static void plus( InterleavedS64 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.plus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayU8 input , int value , GrayU8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedU8 input , int value , InterleavedU8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayS8 input , int value , GrayS8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedS8 input , int value , InterleavedS8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayU16 input , int value , GrayU16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedU16 input , int value , InterleavedU16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayS16 input , int value , GrayS16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedS16 input , int value , InterleavedS16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayS32 input , int value , GrayS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedS32 input , int value , InterleavedS32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayS64 input , long value , GrayS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedS64 input , long value , InterleavedS64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayF32 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedF32 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayF64 input , double value , GrayF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedF64 input , double value , InterleavedF64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayU8 input , int value , int lower , int upper , GrayU8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedU8 input , int value , int lower , int upper , InterleavedU8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayS8 input , int value , int lower , int upper , GrayS8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedS8 input , int value , int lower , int upper , InterleavedS8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayU16 input , int value , int lower , int upper , GrayU16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedU16 input , int value , int lower , int upper , InterleavedU16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayS16 input , int value , int lower , int upper , GrayS16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedS16 input , int value , int lower , int upper , InterleavedS16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayS32 input , int value , int lower , int upper , GrayS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedS32 input , int value , int lower , int upper , InterleavedS32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayS64 input , long value , long lower , long upper , GrayS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedS64 input , long value , long lower , long upper , InterleavedS64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayF32 input , float value , float lower , float upper , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedF32 input , float value , float lower , float upper , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayF64 input , double value , double lower , double upper , GrayF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedF64 input , double value , double lower , double upper , InterleavedF64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayU8 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedU8 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayS8 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedS8 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayU16 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedU16 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayS16 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedS16 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayS32 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedS32 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( GrayS64 input , float value , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element.
	 * @param output The output image. Modified.
	 */
	public static void minus( InterleavedS64 input , float value , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_A(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , GrayU8 input , GrayU8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , InterleavedU8 input , InterleavedU8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , GrayS8 input , GrayS8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , InterleavedS8 input , InterleavedS8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , GrayU16 input , GrayU16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , InterleavedU16 input , InterleavedU16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , GrayS16 input , GrayS16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , InterleavedS16 input , InterleavedS16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , GrayS32 input , GrayS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , InterleavedS32 input , InterleavedS32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( long value , GrayS64 input , GrayS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( long value , InterleavedS64 input , InterleavedS64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , GrayF32 input , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , InterleavedF32 input , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( double value , GrayF64 input , GrayF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( double value , InterleavedF64 input , InterleavedF64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , GrayU8 input , int lower , int upper , GrayU8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , InterleavedU8 input , int lower , int upper , InterleavedU8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , GrayS8 input , int lower , int upper , GrayS8 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , InterleavedS8 input , int lower , int upper , InterleavedS8 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , GrayU16 input , int lower , int upper , GrayU16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , InterleavedU16 input , int lower , int upper , InterleavedU16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , GrayS16 input , int lower , int upper , GrayS16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , InterleavedS16 input , int lower , int upper , InterleavedS16 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , GrayS32 input , int lower , int upper , GrayS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( int value , InterleavedS32 input , int lower , int upper , InterleavedS32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( long value , GrayS64 input , long lower , long upper , GrayS64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( long value , InterleavedS64 input , long lower , long upper , InterleavedS64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , GrayF32 input , float lower , float upper , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , InterleavedF32 input , float lower , float upper , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( double value , GrayF64 input , double lower , double upper , GrayF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( double value , InterleavedF64 input , double lower , double upper , InterleavedF64 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value, lower, upper ,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , GrayU8 input , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , InterleavedU8 input , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , GrayS8 input , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , InterleavedS8 input , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , GrayU16 input , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , InterleavedU16 input , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minusU_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , GrayS16 input , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , InterleavedS16 input , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , GrayS32 input , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , InterleavedS32 input , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , GrayS64 input , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Subtracts each element's value from a scalar. Both input and output images can be the same instance.
	 *
	 * @param value Scalar value
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static void minus( float value , InterleavedS64 input , InterleavedF32 output ) {

		output.reshape(input.width,input.height,input.numBands);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.minus_B(input.data,input.startIndex,input.stride,value , 
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Bounds image pixels to be between these two values
	 * 
	 * @param img Image
	 * @param min minimum value.
	 * @param max maximum value.
	 */
	public static void boundImage( GrayU8 img , int min , int max ) {
		ImplPixelMath.boundImage(img,min,max);
	}

	/**
	 * <p>
	 * Computes the absolute value of the difference between each pixel in the two images.<br>
	 * d(x,y) = |img1(x,y) - img2(x,y)|
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Absolute value of difference image. Can be either input. Modified.
	 */
	public static void diffAbs( GrayU8 imgA , GrayU8 imgB , GrayU8 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.diffAbs(imgA, imgB, output);
		} else {
			ImplPixelMath.diffAbs(imgA, imgB, output);
		}
	}

	/**
	 * Bounds image pixels to be between these two values
	 * 
	 * @param img Image
	 * @param min minimum value.
	 * @param max maximum value.
	 */
	public static void boundImage( GrayS8 img , int min , int max ) {
		ImplPixelMath.boundImage(img,min,max);
	}

	/**
	 * <p>
	 * Computes the absolute value of the difference between each pixel in the two images.<br>
	 * d(x,y) = |img1(x,y) - img2(x,y)|
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Absolute value of difference image. Can be either input. Modified.
	 */
	public static void diffAbs( GrayS8 imgA , GrayS8 imgB , GrayS8 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.diffAbs(imgA, imgB, output);
		} else {
			ImplPixelMath.diffAbs(imgA, imgB, output);
		}
	}

	/**
	 * Bounds image pixels to be between these two values
	 * 
	 * @param img Image
	 * @param min minimum value.
	 * @param max maximum value.
	 */
	public static void boundImage( GrayU16 img , int min , int max ) {
		ImplPixelMath.boundImage(img,min,max);
	}

	/**
	 * <p>
	 * Computes the absolute value of the difference between each pixel in the two images.<br>
	 * d(x,y) = |img1(x,y) - img2(x,y)|
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Absolute value of difference image. Can be either input. Modified.
	 */
	public static void diffAbs( GrayU16 imgA , GrayU16 imgB , GrayU16 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.diffAbs(imgA, imgB, output);
		} else {
			ImplPixelMath.diffAbs(imgA, imgB, output);
		}
	}

	/**
	 * Bounds image pixels to be between these two values
	 * 
	 * @param img Image
	 * @param min minimum value.
	 * @param max maximum value.
	 */
	public static void boundImage( GrayS16 img , int min , int max ) {
		ImplPixelMath.boundImage(img,min,max);
	}

	/**
	 * <p>
	 * Computes the absolute value of the difference between each pixel in the two images.<br>
	 * d(x,y) = |img1(x,y) - img2(x,y)|
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Absolute value of difference image. Can be either input. Modified.
	 */
	public static void diffAbs( GrayS16 imgA , GrayS16 imgB , GrayS16 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.diffAbs(imgA, imgB, output);
		} else {
			ImplPixelMath.diffAbs(imgA, imgB, output);
		}
	}

	/**
	 * Bounds image pixels to be between these two values
	 * 
	 * @param img Image
	 * @param min minimum value.
	 * @param max maximum value.
	 */
	public static void boundImage( GrayS32 img , int min , int max ) {
		ImplPixelMath.boundImage(img,min,max);
	}

	/**
	 * <p>
	 * Computes the absolute value of the difference between each pixel in the two images.<br>
	 * d(x,y) = |img1(x,y) - img2(x,y)|
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Absolute value of difference image. Can be either input. Modified.
	 */
	public static void diffAbs( GrayS32 imgA , GrayS32 imgB , GrayS32 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.diffAbs(imgA, imgB, output);
		} else {
			ImplPixelMath.diffAbs(imgA, imgB, output);
		}
	}

	/**
	 * Bounds image pixels to be between these two values
	 * 
	 * @param img Image
	 * @param min minimum value.
	 * @param max maximum value.
	 */
	public static void boundImage( GrayS64 img , long min , long max ) {
		ImplPixelMath.boundImage(img,min,max);
	}

	/**
	 * <p>
	 * Computes the absolute value of the difference between each pixel in the two images.<br>
	 * d(x,y) = |img1(x,y) - img2(x,y)|
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Absolute value of difference image. Can be either input. Modified.
	 */
	public static void diffAbs( GrayS64 imgA , GrayS64 imgB , GrayS64 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.diffAbs(imgA, imgB, output);
		} else {
			ImplPixelMath.diffAbs(imgA, imgB, output);
		}
	}

	/**
	 * Bounds image pixels to be between these two values
	 * 
	 * @param img Image
	 * @param min minimum value.
	 * @param max maximum value.
	 */
	public static void boundImage( GrayF32 img , float min , float max ) {
		ImplPixelMath.boundImage(img,min,max);
	}

	/**
	 * <p>
	 * Computes the absolute value of the difference between each pixel in the two images.<br>
	 * d(x,y) = |img1(x,y) - img2(x,y)|
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Absolute value of difference image. Can be either input. Modified.
	 */
	public static void diffAbs( GrayF32 imgA , GrayF32 imgB , GrayF32 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.diffAbs(imgA, imgB, output);
		} else {
			ImplPixelMath.diffAbs(imgA, imgB, output);
		}
	}

	/**
	 * Bounds image pixels to be between these two values
	 * 
	 * @param img Image
	 * @param min minimum value.
	 * @param max maximum value.
	 */
	public static void boundImage( GrayF64 img , double min , double max ) {
		ImplPixelMath.boundImage(img,min,max);
	}

	/**
	 * <p>
	 * Computes the absolute value of the difference between each pixel in the two images.<br>
	 * d(x,y) = |img1(x,y) - img2(x,y)|
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Absolute value of difference image. Can be either input. Modified.
	 */
	public static void diffAbs( GrayF64 imgA , GrayF64 imgB , GrayF64 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.diffAbs(imgA, imgB, output);
		} else {
			ImplPixelMath.diffAbs(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise addition<br>
	 * output(x,y) = imgA(x,y) + imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void add( GrayU8 imgA , GrayU8 imgB , GrayU16 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.add(imgA, imgB, output);
		} else {
			ImplPixelMath.add(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise subtraction.<br>
	 * output(x,y) = imgA(x,y) - imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void subtract( GrayU8 imgA , GrayU8 imgB , GrayI16 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.subtract(imgA, imgB, output);
		} else {
			ImplPixelMath.subtract(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise addition<br>
	 * output(x,y) = imgA(x,y) + imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void add( GrayS8 imgA , GrayS8 imgB , GrayS16 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.add(imgA, imgB, output);
		} else {
			ImplPixelMath.add(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise subtraction.<br>
	 * output(x,y) = imgA(x,y) - imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void subtract( GrayS8 imgA , GrayS8 imgB , GrayS16 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.subtract(imgA, imgB, output);
		} else {
			ImplPixelMath.subtract(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise addition<br>
	 * output(x,y) = imgA(x,y) + imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void add( GrayU16 imgA , GrayU16 imgB , GrayS32 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.add(imgA, imgB, output);
		} else {
			ImplPixelMath.add(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise subtraction.<br>
	 * output(x,y) = imgA(x,y) - imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void subtract( GrayU16 imgA , GrayU16 imgB , GrayS32 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.subtract(imgA, imgB, output);
		} else {
			ImplPixelMath.subtract(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise addition<br>
	 * output(x,y) = imgA(x,y) + imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void add( GrayS16 imgA , GrayS16 imgB , GrayS32 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.add(imgA, imgB, output);
		} else {
			ImplPixelMath.add(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise subtraction.<br>
	 * output(x,y) = imgA(x,y) - imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void subtract( GrayS16 imgA , GrayS16 imgB , GrayS32 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.subtract(imgA, imgB, output);
		} else {
			ImplPixelMath.subtract(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise addition<br>
	 * output(x,y) = imgA(x,y) + imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void add( GrayS32 imgA , GrayS32 imgB , GrayS32 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.add(imgA, imgB, output);
		} else {
			ImplPixelMath.add(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise subtraction.<br>
	 * output(x,y) = imgA(x,y) - imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void subtract( GrayS32 imgA , GrayS32 imgB , GrayS32 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.subtract(imgA, imgB, output);
		} else {
			ImplPixelMath.subtract(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise addition<br>
	 * output(x,y) = imgA(x,y) + imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void add( GrayS64 imgA , GrayS64 imgB , GrayS64 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.add(imgA, imgB, output);
		} else {
			ImplPixelMath.add(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise subtraction.<br>
	 * output(x,y) = imgA(x,y) - imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void subtract( GrayS64 imgA , GrayS64 imgB , GrayS64 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.subtract(imgA, imgB, output);
		} else {
			ImplPixelMath.subtract(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise addition<br>
	 * output(x,y) = imgA(x,y) + imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void add( GrayF32 imgA , GrayF32 imgB , GrayF32 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.add(imgA, imgB, output);
		} else {
			ImplPixelMath.add(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise subtraction.<br>
	 * output(x,y) = imgA(x,y) - imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void subtract( GrayF32 imgA , GrayF32 imgB , GrayF32 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.subtract(imgA, imgB, output);
		} else {
			ImplPixelMath.subtract(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise multiplication<br>
	 * output(x,y) = imgA(x,y) * imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void multiply( GrayF32 imgA , GrayF32 imgB , GrayF32 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply(imgA, imgB, output);
		} else {
			ImplPixelMath.multiply(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise division<br>
	 * output(x,y) = imgA(x,y) / imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void divide( GrayF32 imgA , GrayF32 imgB , GrayF32 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide(imgA,imgB,output);
		} else {
			ImplPixelMath.divide(imgA,imgB,output);
		}
	}

	/**
	 * Sets each pixel in the output image to log( val + input(x,y)) of the input image.
	 * Both the input and output image can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the log image is written to. Modified.
	 */
	public static void log( GrayF32 input , final float val, GrayF32 output ) {

		output.reshape(input.width,input.height);

		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.log(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		} else {
			ImplPixelMath.log(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		}
	}

	/**
	 * Sets each pixel in the output image to sgn*log( val + sgn*input(x,y)) of the input image.
	 * where sng is the sign of input(x,y). 
	 * Both the input and output image can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the log image is written to. Modified.
	 */
	public static void logSign( GrayF32 input , final float val, GrayF32 output ) {

		output.reshape(input.width,input.height);

		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.logSign(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		} else {
			ImplPixelMath.logSign(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		}
	}

	/**
	 * Computes the square root of each pixel in the input image. Both the input and output image can be the
	 * same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the sqrt() image is written to. Can be same as input. Modified.
	 */
	public static void sqrt( GrayF32 input , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.sqrt(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		} else {
			ImplPixelMath.sqrt(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		}
	}

	/**
	 * Sets each pixel in the output image to log( val + input(x,y)) of the input image.
	 * Both the input and output image can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the log image is written to. Modified.
	 */
	public static void log( InterleavedF32 input , final float val, InterleavedF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.log(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.log(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Sets each pixel in the output image to sgn*log( val + sgn*input(x,y)) of the input image.
	 * where sng is the sign of input(x,y). 
	 * Both the input and output image can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the log image is written to. Modified.
	 */
	public static void logSign( InterleavedF32 input , final float val, InterleavedF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.logSign(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.logSign(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Computes the square root of each pixel in the input image. Both the input and output image can be the
	 * same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the sqrt() image is written to. Can be same as input. Modified.
	 */
	public static void sqrt( InterleavedF32 input , InterleavedF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.sqrt(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.sqrt(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise addition<br>
	 * output(x,y) = imgA(x,y) + imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void add( GrayF64 imgA , GrayF64 imgB , GrayF64 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.add(imgA, imgB, output);
		} else {
			ImplPixelMath.add(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise subtraction.<br>
	 * output(x,y) = imgA(x,y) - imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void subtract( GrayF64 imgA , GrayF64 imgB , GrayF64 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.subtract(imgA, imgB, output);
		} else {
			ImplPixelMath.subtract(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise multiplication<br>
	 * output(x,y) = imgA(x,y) * imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void multiply( GrayF64 imgA , GrayF64 imgB , GrayF64 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.multiply(imgA, imgB, output);
		} else {
			ImplPixelMath.multiply(imgA, imgB, output);
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise division<br>
	 * output(x,y) = imgA(x,y) / imgB(x,y)
	 * </p>
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Can be either input. Modified.
	 */
	public static void divide( GrayF64 imgA , GrayF64 imgB , GrayF64 output ) {
		InputSanityCheck.checkSameShape(imgA,imgB);
		output.reshape(imgA.width,imgA.height);

		int N = imgA.width*imgA.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.divide(imgA,imgB,output);
		} else {
			ImplPixelMath.divide(imgA,imgB,output);
		}
	}

	/**
	 * Sets each pixel in the output image to log( val + input(x,y)) of the input image.
	 * Both the input and output image can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the log image is written to. Modified.
	 */
	public static void log( GrayF64 input , final double val, GrayF64 output ) {

		output.reshape(input.width,input.height);

		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.log(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		} else {
			ImplPixelMath.log(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		}
	}

	/**
	 * Sets each pixel in the output image to sgn*log( val + sgn*input(x,y)) of the input image.
	 * where sng is the sign of input(x,y). 
	 * Both the input and output image can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the log image is written to. Modified.
	 */
	public static void logSign( GrayF64 input , final double val, GrayF64 output ) {

		output.reshape(input.width,input.height);

		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.logSign(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		} else {
			ImplPixelMath.logSign(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		}
	}

	/**
	 * Computes the square root of each pixel in the input image. Both the input and output image can be the
	 * same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the sqrt() image is written to. Can be same as input. Modified.
	 */
	public static void sqrt( GrayF64 input , GrayF64 output ) {

		output.reshape(input.width,input.height);

		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.sqrt(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		} else {
			ImplPixelMath.sqrt(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		}
	}

	/**
	 * Sets each pixel in the output image to log( val + input(x,y)) of the input image.
	 * Both the input and output image can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the log image is written to. Modified.
	 */
	public static void log( InterleavedF64 input , final double val, InterleavedF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.log(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.log(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Sets each pixel in the output image to sgn*log( val + sgn*input(x,y)) of the input image.
	 * where sng is the sign of input(x,y). 
	 * Both the input and output image can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the log image is written to. Modified.
	 */
	public static void logSign( InterleavedF64 input , final double val, InterleavedF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.logSign(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.logSign(
					input.data,input.startIndex,input.stride,val,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Computes the square root of each pixel in the input image. Both the input and output image can be the
	 * same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the sqrt() image is written to. Can be same as input. Modified.
	 */
	public static void sqrt( InterleavedF64 input , InterleavedF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.sqrt(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.sqrt(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Raises each pixel in the input image to the power of two. Both the input and output image can be the 
	 * same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the pow2 image is written to. Can be same as input. Modified.
	 */
	public static void pow2( GrayU8 input , GrayU16 output ) {

		output.reshape(input.width,input.height);

		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		} else {
			ImplPixelMath.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		}
	}

	/**
	 * Raises each pixel in the input image to the power of two. Both the input and output image can be the 
	 * same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the pow2 image is written to. Can be same as input. Modified.
	 */
	public static void pow2( InterleavedU8 input , InterleavedU16 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Computes the standard deviation of each pixel in a local region.
	 *
	 * @param mean (Input) Image with local mean
	 * @param pow2 (Input) Image with local mean pixel-wise power of 2 
	 * @param stdev (Output) standard deviation of each pixel. Can be same instance as either input.
	 */
	public static void stdev( GrayU8 mean , GrayU16 pow2 , GrayU8 stdev) {

		InputSanityCheck.checkSameShape(mean,pow2);
		stdev.reshape(mean.width,mean.height);

		int N = mean.width*mean.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.stdev(mean,pow2,stdev);
		} else {
			ImplPixelMath.stdev(mean,pow2,stdev);
		}
	}

	/**
	 * Raises each pixel in the input image to the power of two. Both the input and output image can be the 
	 * same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the pow2 image is written to. Can be same as input. Modified.
	 */
	public static void pow2( GrayU16 input , GrayS32 output ) {

		output.reshape(input.width,input.height);

		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		} else {
			ImplPixelMath.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		}
	}

	/**
	 * Raises each pixel in the input image to the power of two. Both the input and output image can be the 
	 * same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the pow2 image is written to. Can be same as input. Modified.
	 */
	public static void pow2( InterleavedU16 input , InterleavedS32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Computes the standard deviation of each pixel in a local region.
	 *
	 * @param mean (Input) Image with local mean
	 * @param pow2 (Input) Image with local mean pixel-wise power of 2 
	 * @param stdev (Output) standard deviation of each pixel. Can be same instance as either input.
	 */
	public static void stdev( GrayU16 mean , GrayS32 pow2 , GrayU16 stdev) {

		InputSanityCheck.checkSameShape(mean,pow2);
		stdev.reshape(mean.width,mean.height);

		int N = mean.width*mean.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.stdev(mean,pow2,stdev);
		} else {
			ImplPixelMath.stdev(mean,pow2,stdev);
		}
	}

	/**
	 * Raises each pixel in the input image to the power of two. Both the input and output image can be the 
	 * same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the pow2 image is written to. Can be same as input. Modified.
	 */
	public static void pow2( GrayF32 input , GrayF32 output ) {

		output.reshape(input.width,input.height);

		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		} else {
			ImplPixelMath.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		}
	}

	/**
	 * Raises each pixel in the input image to the power of two. Both the input and output image can be the 
	 * same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the pow2 image is written to. Can be same as input. Modified.
	 */
	public static void pow2( InterleavedF32 input , InterleavedF32 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Computes the standard deviation of each pixel in a local region.
	 *
	 * @param mean (Input) Image with local mean
	 * @param pow2 (Input) Image with local mean pixel-wise power of 2 
	 * @param stdev (Output) standard deviation of each pixel. Can be same instance as either input.
	 */
	public static void stdev( GrayF32 mean , GrayF32 pow2 , GrayF32 stdev) {

		InputSanityCheck.checkSameShape(mean,pow2);
		stdev.reshape(mean.width,mean.height);

		int N = mean.width*mean.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.stdev(mean,pow2,stdev);
		} else {
			ImplPixelMath.stdev(mean,pow2,stdev);
		}
	}

	/**
	 * Raises each pixel in the input image to the power of two. Both the input and output image can be the 
	 * same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the pow2 image is written to. Can be same as input. Modified.
	 */
	public static void pow2( GrayF64 input , GrayF64 output ) {

		output.reshape(input.width,input.height);

		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		} else {
			ImplPixelMath.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,input.width);
		}
	}

	/**
	 * Raises each pixel in the input image to the power of two. Both the input and output image can be the 
	 * same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the pow2 image is written to. Can be same as input. Modified.
	 */
	public static void pow2( InterleavedF64 input , InterleavedF64 output ) {

		output.reshape(input.width,input.height);

		int columns = input.width*input.numBands;
		int N = input.width*input.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		} else {
			ImplPixelMath.pow2(
					input.data,input.startIndex,input.stride,
					output.data,output.startIndex,output.stride,
					input.height,columns);
		}
	}

	/**
	 * Computes the standard deviation of each pixel in a local region.
	 *
	 * @param mean (Input) Image with local mean
	 * @param pow2 (Input) Image with local mean pixel-wise power of 2 
	 * @param stdev (Output) standard deviation of each pixel. Can be same instance as either input.
	 */
	public static void stdev( GrayF64 mean , GrayF64 pow2 , GrayF64 stdev) {

		InputSanityCheck.checkSameShape(mean,pow2);
		stdev.reshape(mean.width,mean.height);

		int N = mean.width*mean.height;
		if( BoofConcurrency.USE_CONCURRENT && N > SMALL_IMAGE) {
			ImplPixelMath_MT.stdev(mean,pow2,stdev);
		} else {
			ImplPixelMath.stdev(mean,pow2,stdev);
		}
	}

}
