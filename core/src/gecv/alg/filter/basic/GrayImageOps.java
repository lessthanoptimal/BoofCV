/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.basic;

import gecv.alg.InputSanityCheck;
import gecv.struct.image.ImageInt8;

/**
 * Pixel-wise operations on gray-scale images.
 *
 * @author Peter Abeles
 */
public class GrayImageOps {

	/**
	 * <p>
	 * Inverts the image's intensity:<br>
	 * O<sub>x,y</sub> = 255 - I<sub>x,y</sub><br>
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageInt8 invert(ImageInt8 input, ImageInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				output.set(x, y, (byte) (255 - input.getU(x, y)));
			}
		}

		return output;
	}

	/**
	 * <p>
	 * Brightens the image's intensity:<br>
	 * O<sub>x,y</sub> = I<sub>x,y</sub> + beta<br>
	 * </p>
	 * <p>
	 * The image's intensity is clamped at 0 and 255;
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param beta   How much the image is brightened by.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageInt8 brighten(ImageInt8 input, int beta, ImageInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int val = input.getU(x, y) + beta;
				if (val > 255) val = 255;
				if (val < 0) val = 0;
				output.set(x, y, (byte) val);
			}
		}

		return output;
	}

	/**
	 * <p>
	 * Stretches the image's intensity:<br>
	 * O<sub>x,y</sub> = I<sub>x,y</sub>&gamma + beta<br>
	 * </p>
	 * <p>
	 * The image's intensity is clamped at 0 and 255;
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageInt8 stretch(ImageInt8 input, double gamma, int beta, ImageInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int val = (int) (input.getU(x, y) * gamma + beta);
				if (val > 255) val = 255;
				if (val < 0) val = 0;
				output.set(x, y, (byte) val);
			}
		}

		return output;
	}
}
