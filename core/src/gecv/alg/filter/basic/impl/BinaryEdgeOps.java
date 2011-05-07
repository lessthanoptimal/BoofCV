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

import gecv.struct.image.ImageInt8;

/**
 * Binary operations performed only along the image's edge.
 *
 * @author Peter Abeles
 */
public class BinaryEdgeOps {
	public static void erode4(ImageInt8 input, ImageInt8 output) {

		final int h = input.height - 1;
		final int w = input.width - 1;

		for (int x = 0; x < input.width; x++) {
			// check top edge
			if ((get1(input, x, 0) + get1(input, x - 1, 0) + get1(input, x + 1, 0) + get1(input, x, 1)) == 4)
				output.set(x, 0, 1);
			else
				output.set(x, 0, 0);

			// check bottom edge
			if ((get1(input, x, h) + get1(input, x - 1, h) + get1(input, x + 1, h) + get1(input, x, h - 1)) == 4)
				output.set(x, h, 1);
			else
				output.set(x, h, 0);
		}

		for (int y = 0; y < input.height; y++) {
			// check left edge
			if ((get1(input, 0, y) + get1(input, 1, y) + get1(input, 0, y - 1) + get1(input, 0, y + 1)) == 4)
				output.set(0, y, 1);
			else
				output.set(0, y, 0);

			// check right edge
			if ((get1(input, w, y) + get1(input, w - 1, y) + get1(input, w, y - 1) + get1(input, w, y + 1)) == 4)
				output.set(w, y, 1);
			else
				output.set(w, y, 0);
		}
	}

	public static void dilate4(ImageInt8 input, ImageInt8 output) {

		final int h = input.height - 1;
		final int w = input.width - 1;

		for (int x = 0; x < input.width; x++) {
			// check top edge
			if ((get0(input, x, 0) + get0(input, x - 1, 0) + get0(input, x + 1, 0) + get0(input, x, 1)) > 0)
				output.set(x, 0, 1);
			else
				output.set(x, 0, 0);

			// check bottom edge
			if ((get0(input, x, h) + get0(input, x - 1, h) + get0(input, x + 1, h) + get0(input, x, h - 1)) > 0)
				output.set(x, h, 1);
			else
				output.set(x, h, 0);
		}

		for (int y = 0; y < input.height; y++) {
			// check left edge
			if ((get0(input, 0, y) + get0(input, 1, y) + get0(input, 0, y - 1) + get0(input, 0, y + 1)) > 0)
				output.set(0, y, 1);
			else
				output.set(0, y, 0);

			// check right edge
			if ((get0(input, w, y) + get0(input, w - 1, y) + get0(input, w, y - 1) + get0(input, w, y + 1)) > 0)
				output.set(w, y, 1);
			else
				output.set(w, y, 0);
		}
	}

	public static ImageInt8 edge4(ImageInt8 input, ImageInt8 output) {

		final int h = input.height - 1;
		final int w = input.width - 1;

		for (int x = 0; x < input.width; x++) {
			// check top edge
			if ((get1(input, x - 1, 0) + get1(input, x + 1, 0) + get1(input, x, 1)) == 3)
				output.set(x, 0, 0);
			else
				output.set(x, 0, input.get(x, 0));

			// check bottom edge
			if ((get1(input, x - 1, h) + get1(input, x + 1, h) + get1(input, x, h - 1)) == 3)
				output.set(x, h, 0);
			else
				output.set(x, h, input.get(x, h));
		}

		for (int y = 0; y < input.height; y++) {
			// check left edge
			if ((get1(input, 1, y) + get1(input, 0, y - 1) + get1(input, 0, y + 1)) == 3)
				output.set(0, y, 0);
			else
				output.set(0, y, input.get(0, y));

			// check right edge
			if ((get1(input, w - 1, y) + get1(input, w, y - 1) + get1(input, w, y + 1)) == 3)
				output.set(w, y, 0);
			else
				output.set(w, y, input.get(w, y));
		}

		return output;
	}

	public static void erode8(ImageInt8 input, ImageInt8 output) {

		final int h = input.height - 1;
		final int w = input.width - 1;

		for (int x = 0; x < input.width; x++) {
			// check top edge
			if ((get1(input, x, 0) + get1(input, x - 1, 0) + get1(input, x + 1, 0) +
					get1(input, x - 1, 1) + get1(input, x, 1) + get1(input, x + 1, 1)) == 6)
				output.set(x, 0, 1);
			else
				output.set(x, 0, 0);

			// check bottom edge
			if ((get1(input, x, h) + get1(input, x - 1, h) + get1(input, x + 1, h) +
					get1(input, x - 1, h - 1) + get1(input, x, h - 1) + get1(input, x + 1, h - 1)) == 6)
				output.set(x, h, 1);
			else
				output.set(x, h, 0);
		}

		for (int y = 0; y < input.height; y++) {
			// check left edge
			if ((get1(input, 0, y) + get1(input, 1, y) + get1(input, 0, y - 1) +
					get1(input, 1, y - 1) + get1(input, 0, y + 1) + get1(input, 1, y + 1)) == 6)
				output.set(0, y, 1);
			else
				output.set(0, y, 0);

			// check right edge
			if ((get1(input, w - 1, y) + get1(input, w, y) + get1(input, w - 1, y - 1) +
					get1(input, w, y - 1) + get1(input, w - 1, y + 1) + get1(input, w, y + 1)) == 6)
				output.set(w, y, 1);
			else
				output.set(w, y, 0);
		}
	}

	public static void dilate8(ImageInt8 input, ImageInt8 output) {

		final int h = input.height - 1;
		final int w = input.width - 1;

		for (int x = 0; x < input.width; x++) {
			// check top edge
			if ((get0(input, x, 0) + get0(input, x - 1, 0) + get0(input, x + 1, 0) +
					get0(input, x - 1, 1) + get0(input, x, 1) + get0(input, x + 1, 1)) > 0)
				output.set(x, 0, 1);
			else
				output.set(x, 0, 0);

			// check bottom edge
			if ((get0(input, x, h) + get0(input, x - 1, h) + get0(input, x + 1, h) +
					get0(input, x - 1, h - 1) + get0(input, x, h - 1) + get0(input, x + 1, h - 1)) > 0)
				output.set(x, h, 1);
			else
				output.set(x, h, 0);
		}

		for (int y = 0; y < input.height; y++) {
			// check left edge
			if ((get0(input, 0, y) + get0(input, 1, y) + get0(input, 0, y - 1) +
					get0(input, 1, y - 1) + get0(input, 0, y + 1) + get0(input, 1, y + 1)) > 0)
				output.set(0, y, 1);
			else
				output.set(0, y, 0);

			// check right edge
			if ((get0(input, w - 1, y) + get0(input, w, y) + get0(input, w - 1, y - 1) +
					get0(input, w, y - 1) + get0(input, w - 1, y + 1) + get0(input, w, y + 1)) > 0)
				output.set(w, y, 1);
			else
				output.set(w, y, 0);
		}
	}

	public static void edge8(ImageInt8 input, ImageInt8 output) {

		final int h = input.height - 1;
		final int w = input.width - 1;

		for (int x = 0; x < input.width; x++) {
			// check top edge
			if ((get1(input, x - 1, 0) + get1(input, x + 1, 0) +
					get1(input, x - 1, 1) + get1(input, x, 1) + get1(input, x + 1, 1)) == 5)
				output.set(x, 0, 0);
			else
				output.set(x, 0, input.get(x, 0));

			// check bottom edge
			if ((get1(input, x - 1, h) + get1(input, x + 1, h) +
					get1(input, x - 1, h - 1) + get1(input, x, h - 1) + get1(input, x + 1, h - 1)) == 5)
				output.set(x, h, 0);
			else
				output.set(x, h, input.get(x, h));
		}

		for (int y = 0; y < input.height; y++) {
			// check left edge
			if ((get1(input, 1, y) + get1(input, 0, y - 1) +
					get1(input, 1, y - 1) + get1(input, 0, y + 1) + get1(input, 1, y + 1)) == 5)
				output.set(0, y, 0);
			else
				output.set(0, y, input.get(0, y));

			// check right edge
			if ((get1(input, w - 1, y) + get1(input, w - 1, y - 1) +
					get1(input, w, y - 1) + get1(input, w - 1, y + 1) + get1(input, w, y + 1)) == 5)
				output.set(w, y, 0);
			else
				output.set(w, y, input.get(w, y));
		}
	}

	public static void removePointNoise(ImageInt8 input, ImageInt8 output) {

		final int h = input.height - 1;
		final int w = input.width - 1;

		for (int x = 0; x < input.width; x++) {
			// check top edge
			int total = get0(input, x - 1, 0) + get0(input, x + 1, 0) +
					get0(input, x - 1, 1) + get0(input, x, 1) + get0(input, x + 1, 1);
			if (total < 2)
				output.set(x, 0, 0);
			else
				output.set(x, 0, input.get(x, 0));

			// check bottom edge
			total = get0(input, x - 1, h) + get0(input, x + 1, h) +
					get0(input, x - 1, h - 1) + get0(input, x, h - 1) + get0(input, x + 1, h - 1);
			if (total < 2)
				output.set(x, h, 0);
			else
				output.set(x, h, input.get(x, h));
		}

		for (int y = 0; y < input.height; y++) {
			// check left edge
			int total = get0(input, 1, y) + get0(input, 0, y - 1) +
					get0(input, 1, y - 1) + get0(input, 0, y + 1) + get0(input, 1, y + 1);
			if (total < 2)
				output.set(0, y, 0);
			else
				output.set(0, y, input.get(0, y));

			// check right edge
			total = get0(input, w - 1, y) + get0(input, w - 1, y - 1) +
					get0(input, w, y - 1) + get0(input, w - 1, y + 1) + get0(input, w, y + 1);
			if (total < 2)
				output.set(w, y, 0);
			else
				output.set(w, y, input.get(w, y));
		}
	}

	/**
	 * If a point is inside the image true is returned if its value is not zero, otherwise true is returned.
	 */
	public static int get1(ImageInt8 image, int x, int y) {
		if (image.isInBounds(x, y)) {
			return image.data[image.startIndex + y * image.stride + x];
		} else {
			return 1;
		}
	}

	/**
	 * If a point is inside the image true is returned if its value is not zero, otherwise false is returned.
	 */
	public static int get0(ImageInt8 image, int x, int y) {
		if (image.isInBounds(x, y)) {
			return image.data[image.startIndex + y * image.stride + x];
		} else {
			return 0;
		}
	}
}
