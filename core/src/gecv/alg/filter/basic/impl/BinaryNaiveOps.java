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
 */
public class BinaryNaiveOps {
	/**
	 * <p>
	 * Erodes an image according to a 4-neighborhood.  Unless a pixel is connected to all its neighbors its value
	 * is set to zero.
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageInt8 erode4(ImageInt8 input, ImageInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		for (int y = 1; y < input.height - 1; y++) {
			for (int x = 1; x < input.width - 1; x++) {
				if (input.get(x, y) != 0 &&
						input.get(x - 1, y) != 0 && input.get(x + 1, y) != 0 &&
						input.get(x, y - 1) != 0 && input.get(x, y + 1) != 0)
					output.set(x, y, 1);
				else
					output.set(x, y, 0);
			}
		}
		return output;
	}

	/**
	 * <p>
	 * Dilates an image according to a 4-neighborhood.  If a pixel is connected to any other pixel then its output
	 * value will be one.
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageInt8 dilate4(ImageInt8 input, ImageInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		for (int y = 1; y < input.height - 1; y++) {
			for (int x = 1; x < input.width - 1; x++) {
				if (input.get(x, y) != 0 || input.get(x - 1, y) != 0 || input.get(x + 1, y) != 0 ||
						input.get(x, y - 1) != 0 || input.get(x, y + 1) != 0)
					output.set(x, y, 1);
				else
					output.set(x, y, 0);
			}
		}
		return output;
	}

	/**
	 * <p>
	 * Binary operation which is designed to remove all pixels but ones which are on the edge of an object.
	 * The edge is defined as lying on the object and not being entirely surrounded by pixels in a 4-neighborhood.
	 * </p>
	 * <p/>
	 * <p>
	 * NOTE: There are many ways to define an edge, this is just one of them.
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageInt8 edge4(ImageInt8 input, ImageInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		for (int y = 1; y < input.height - 1; y++) {
			for (int x = 1; x < input.width - 1; x++) {
				if (input.get(x - 1, y) != 0 && input.get(x + 1, y) != 0 &&
						input.get(x, y - 1) != 0 && input.get(x, y + 1) != 0)
					output.set(x, y, 0);
				else
					output.set(x, y, input.get(x, y));
			}
		}
		return output;
	}

	/**
	 * <p>
	 * Erodes an image according to a 8-neighborhood.  Unless a pixel is connected to all its neighbors its value
	 * is set to zero.
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageInt8 erode8(ImageInt8 input, ImageInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		for (int y = 1; y < input.height - 1; y++) {
			for (int x = 1; x < input.width - 1; x++) {
				if (input.get(x, y) != 0 &&
						input.get(x - 1, y) != 0 && input.get(x + 1, y) != 0 &&
						input.get(x, y - 1) != 0 && input.get(x, y + 1) != 0 &&
						input.get(x - 1, y + 1) != 0 && input.get(x + 1, y + 1) != 0 &&
						input.get(x - 1, y - 1) != 0 && input.get(x + 1, y - 1) != 0)
					output.set(x, y, 1);
				else
					output.set(x, y, 0);
			}
		}
		return output;
	}

	/**
	 * <p>
	 * Dilates an image according to a 8-neighborhood.  If a pixel is connected to any other pixel then its output
	 * value will be one.
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageInt8 dilate8(ImageInt8 input, ImageInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		for (int y = 1; y < input.height - 1; y++) {
			for (int x = 1; x < input.width - 1; x++) {
				if (input.get(x, y) != 0 ||
						input.get(x - 1, y) != 0 || input.get(x + 1, y) != 0 ||
								input.get(x, y - 1) != 0 || input.get(x, y + 1) != 0 ||
								input.get(x - 1, y + 1) != 0 || input.get(x + 1, y + 1) != 0 ||
								input.get(x - 1, y - 1) != 0 || input.get(x + 1, y - 1) != 0)
					output.set(x, y, 1);
				else
					output.set(x, y, 0);
			}
		}
		return output;
	}

	/**
	 * <p>
	 * Binary operation which is designed to remove all pixels but ones which are on the edge of an object.
	 * The edge is defined as lying on the object and not being surrounded by 8 pixels.
	 * </p>
	 * <p/>
	 * <p>
	 * NOTE: There are many ways to define an edge, this is just one of them.
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageInt8 edge8(ImageInt8 input, ImageInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		for (int y = 1; y < input.height - 1; y++) {
			for (int x = 1; x < input.width - 1; x++) {
				if (input.get(x - 1, y) != 0 && input.get(x + 1, y) != 0 &&
						input.get(x, y - 1) != 0 && input.get(x, y + 1) != 0 &&
						input.get(x - 1, y + 1) != 0 && input.get(x + 1, y + 1) != 0 &&
						input.get(x - 1, y - 1) != 0 && input.get(x + 1, y - 1) != 0)
					output.set(x, y, 0);
				else
					output.set(x, y, input.get(x, y));
			}
		}
		return output;
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
	public static ImageInt8 removePointNoise(ImageInt8 input, ImageInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		for (int y = 1; y < input.height - 1; y++) {
			for (int x = 1; x < input.width - 1; x++) {
				int num = 0;
				if (input.get(x - 1, y + 1) != 0) num++;
				if (input.get(x, y + 1) != 0) num++;
				if (input.get(x + 1, y + 1) != 0) num++;
				if (input.get(x + 1, y) != 0) num++;
				if (input.get(x + 1, y - 1) != 0) num++;
				if (input.get(x, y - 1) != 0) num++;
				if (input.get(x - 1, y - 1) != 0) num++;
				if (input.get(x - 1, y) != 0) num++;

				if (num < 2)
					output.set(x, y, 0);
				else if (num > 6)
					output.set(x, y, 1);
				else
					output.set(x, y, input.get(x, y));
			}
		}

		return output;
	}
}
