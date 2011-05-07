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

package gecv.alg.filter.basic.impl;

import gecv.alg.InputSanityCheck;
import gecv.struct.image.ImageInt8;

/**
 * Simple unoptimized implementations of binary operations.
 *
 * @author Peter Abeles
 * @see gecv.alg.filter.basic.BinaryImageOps
 */
public class BinaryNaiveOps {
	public static void erode4(ImageInt8 input, ImageInt8 output) {
		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				if (input.get(x, y) != 0 &&
						getT(input, x - 1, y) && getT(input, x + 1, y) &&
						getT(input, x, y - 1) && getT(input, x, y + 1))
					output.set(x, y, 1);
				else
					output.set(x, y, 0);
			}
		}
	}

	public static void dilate4(ImageInt8 input, ImageInt8 output) {
		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				if (input.get(x, y) != 0 || getF(input, x - 1, y) || getF(input, x + 1, y) ||
						getF(input, x, y - 1) || getF(input, x, y + 1))
					output.set(x, y, 1);
				else
					output.set(x, y, 0);
			}
		}
	}

	public static void edge4(ImageInt8 input, ImageInt8 output) {
		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				if (getT(input, x - 1, y) && getT(input, x + 1, y) &&
						getT(input, x, y - 1) && getT(input, x, y + 1))
					output.set(x, y, 0);
				else
					output.set(x, y, input.get(x, y));
			}
		}
	}

	public static void erode8(ImageInt8 input, ImageInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				if (input.get(x, y) != 0 &&
						getT(input, x - 1, y) && getT(input, x + 1, y) &&
						getT(input, x, y - 1) && getT(input, x, y + 1) &&
						getT(input, x - 1, y + 1) && getT(input, x + 1, y + 1) &&
						getT(input, x - 1, y - 1) && getT(input, x + 1, y - 1))
					output.set(x, y, 1);
				else
					output.set(x, y, 0);
			}
		}
	}

	public static void dilate8(ImageInt8 input, ImageInt8 output) {
		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				if (input.get(x, y) != 0 ||
						getF(input, x - 1, y) || getF(input, x + 1, y) ||
						getF(input, x, y - 1) || getF(input, x, y + 1) ||
						getF(input, x - 1, y + 1) || getF(input, x + 1, y + 1) ||
						getF(input, x - 1, y - 1) || getF(input, x + 1, y - 1))
					output.set(x, y, 1);
				else
					output.set(x, y, 0);
			}
		}
	}

	public static void edge8(ImageInt8 input, ImageInt8 output) {
		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				if (getT(input, x - 1, y) && getT(input, x + 1, y) &&
						getT(input, x, y - 1) && getT(input, x, y + 1) &&
						getT(input, x - 1, y + 1) && getT(input, x + 1, y + 1) &&
						getT(input, x - 1, y - 1) && getT(input, x + 1, y - 1))
					output.set(x, y, 0);
				else
					output.set(x, y, input.get(x, y));
			}
		}
	}

	/**
	 * Binary operation which is designed to remove small bits of spurious noise.  An 8-neighborhood is used.
	 * If a pixel is connected to less than 2 neighbors then its value zero.  If connected to more than 6 then
	 * its value is one.  Otherwise it retains its original value.
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static void removePointNoise(ImageInt8 input, ImageInt8 output) {
		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int num = 0;
				if (getF(input, x - 1, y + 1)) num++;
				if (getF(input, x, y + 1)) num++;
				if (getF(input, x + 1, y + 1)) num++;
				if (getF(input, x + 1, y)) num++;
				if (getF(input, x + 1, y - 1)) num++;
				if (getF(input, x, y - 1)) num++;
				if (getF(input, x - 1, y - 1)) num++;
				if (getF(input, x - 1, y)) num++;

				if (num < 2)
					output.set(x, y, 0);
				else if (num > 6)
					output.set(x, y, 1);
				else
					output.set(x, y, input.get(x, y));
			}
		}
	}

	/**
	 * If a point is inside the image true is returned if its value is not zero, otherwise true is returned.
	 */
	public static boolean getT(ImageInt8 image, int x, int y) {
		if (image.isInBounds(x, y)) {
			return image.get(x, y) != 0;
		} else {
			return true;
		}
	}

	/**
	 * If a point is inside the image true is returned if its value is not zero, otherwise false is returned.
	 */
	public static boolean getF(ImageInt8 image, int x, int y) {
		if (image.isInBounds(x, y)) {
			return image.get(x, y) != 0;
		} else {
			return false;
		}
	}
}
