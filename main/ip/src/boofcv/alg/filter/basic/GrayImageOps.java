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

package boofcv.alg.filter.basic;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.basic.impl.ImplGrayImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;

/**
 * Pixel-wise operations on gray-scale images.
 *
 * @author Peter Abeles
 */
public class GrayImageOps {

	/**
	 * <p>
	 * Inverts the image's intensity:<br>
	 * O<sub>x,y</sub> = max - I<sub>x,y</sub><br>
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static GrayU8 invert(GrayU8 input, int max , GrayU8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		ImplGrayImageOps.invert(input,max,output);

		return output;
	}

	/**
	 * <p>
	 * Brightens the image's intensity:<br>
	 * O<sub>x,y</sub> = I<sub>x,y</sub> + beta<br>
	 * </p>
	 * <p>
	 * The image's intensity is clamped at 0 and max;
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param beta   How much the image is brightened by.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static GrayU8 brighten(GrayU8 input, int beta, int max , GrayU8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		ImplGrayImageOps.brighten(input,beta,max,output);

		return output;
	}

	/**
	 * <p>
	 * Stretches the image's intensity:<br>
	 * O<sub>x,y</sub> = I<sub>x,y</sub>&gamma; + beta<br>
	 * </p>
	 * <p>
	 * The image's intensity is clamped at 0 and max;
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static GrayU8 stretch(GrayU8 input, double gamma, int beta, int max , GrayU8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		ImplGrayImageOps.stretch(input,gamma,beta,max,output);

		return output;
	}

	/**
	 * <p>
	 * Inverts the image's intensity:<br>
	 * O<sub>x,y</sub> = max - I<sub>x,y</sub><br>
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static GrayS16 invert(GrayS16 input, int max , GrayS16 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		ImplGrayImageOps.invert(input,max,output);

		return output;
	}

	/**
	 * <p>
	 * Brightens the image's intensity:<br>
	 * O<sub>x,y</sub> = I<sub>x,y</sub> + beta<br>
	 * </p>
	 * <p>
	 * The image's intensity is clamped at 0 and max;
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param beta   How much the image is brightened by.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static GrayS16 brighten(GrayS16 input, int beta, int max , GrayS16 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		ImplGrayImageOps.brighten(input,beta,max,output);

		return output;
	}

	/**
	 * <p>
	 * Stretches the image's intensity:<br>
	 * O<sub>x,y</sub> = I<sub>x,y</sub>&gamma; + beta<br>
	 * </p>
	 * <p>
	 * The image's intensity is clamped at 0 and max;
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static GrayS16 stretch(GrayS16 input, double gamma, int beta, int max , GrayS16 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		ImplGrayImageOps.stretch(input,gamma,beta,max,output);

		return output;
	}

	/**
	 * <p>
	 * Inverts the image's intensity:<br>
	 * O<sub>x,y</sub> = max - I<sub>x,y</sub><br>
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static GrayF32 invert(GrayF32 input, float max , GrayF32 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		ImplGrayImageOps.invert(input,max,output);

		return output;
	}

	/**
	 * <p>
	 * Brightens the image's intensity:<br>
	 * O<sub>x,y</sub> = I<sub>x,y</sub> + beta<br>
	 * </p>
	 * <p>
	 * The image's intensity is clamped at 0 and max;
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param beta   How much the image is brightened by.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static GrayF32 brighten(GrayF32 input, float beta, float max , GrayF32 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		ImplGrayImageOps.brighten(input,beta,max,output);

		return output;
	}

	/**
	 * <p>
	 * Stretches the image's intensity:<br>
	 * O<sub>x,y</sub> = I<sub>x,y</sub>&gamma; + beta<br>
	 * </p>
	 * <p>
	 * The image's intensity is clamped at 0 and max;
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static GrayF32 stretch(GrayF32 input, double gamma, float beta, float max , GrayF32 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		ImplGrayImageOps.stretch(input,gamma,beta,max,output);

		return output;
	}
}
