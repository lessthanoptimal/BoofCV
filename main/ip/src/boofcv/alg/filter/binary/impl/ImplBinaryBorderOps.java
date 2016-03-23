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

package boofcv.alg.filter.binary.impl;

import boofcv.core.image.border.ImageBorderValue;
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.struct.image.GrayU8;

/**
 * Binary operations performed only along the image's border.
 *
 * @author Peter Abeles
 */
public class ImplBinaryBorderOps {
	public static void erode4(GrayU8 input, GrayU8 output) {

		ImageBorder_S32 in = ImageBorderValue.wrap(input,0);
		
		final int h = input.height - 1;
		final int w = input.width - 1;

		for (int x = 0; x < input.width; x++) {
			// check top edge
			if ((in.get( x, 0) + in.get( x - 1, 0) + in.get( x + 1, 0) + in.get( x, 1)) == 4)
				output.set(x, 0, 1);
			else
				output.set(x, 0, 0);

			// check bottom edge
			if ((in.get( x, h) + in.get( x - 1, h) + in.get( x + 1, h) + in.get( x, h - 1)) == 4)
				output.set(x, h, 1);
			else
				output.set(x, h, 0);
		}

		for (int y = 0; y < input.height; y++) {
			// check left edge
			if ((in.get( 0, y) + in.get( 1, y) + in.get( 0, y - 1) + in.get( 0, y + 1)) == 4)
				output.set(0, y, 1);
			else
				output.set(0, y, 0);

			// check right edge
			if ((in.get( w, y) + in.get( w - 1, y) + in.get( w, y - 1) + in.get( w, y + 1)) == 4)
				output.set(w, y, 1);
			else
				output.set(w, y, 0);
		}
	}

	public static void dilate4(GrayU8 input, GrayU8 output) {

		ImageBorder_S32 in = ImageBorderValue.wrap(input,0);

		final int h = input.height - 1;
		final int w = input.width - 1;

		for (int x = 0; x < input.width; x++) {
			// check top edge
			if ((in.get( x, 0) + in.get( x - 1, 0) + in.get( x + 1, 0) + in.get( x, 1)) > 0)
				output.set(x, 0, 1);
			else
				output.set(x, 0, 0);

			// check bottom edge
			if ((in.get( x, h) + in.get( x - 1, h) + in.get( x + 1, h) + in.get( x, h - 1)) > 0)
				output.set(x, h, 1);
			else
				output.set(x, h, 0);
		}

		for (int y = 0; y < input.height; y++) {
			// check left edge
			if ((in.get( 0, y) + in.get( 1, y) + in.get( 0, y - 1) + in.get( 0, y + 1)) > 0)
				output.set(0, y, 1);
			else
				output.set(0, y, 0);

			// check right edge
			if ((in.get( w, y) + in.get( w - 1, y) + in.get( w, y - 1) + in.get( w, y + 1)) > 0)
				output.set(w, y, 1);
			else
				output.set(w, y, 0);
		}
	}

	public static GrayU8 edge4(GrayU8 input, GrayU8 output) {

		ImageBorder_S32 in = ImageBorderValue.wrap(input,1);
		
		final int h = input.height - 1;
		final int w = input.width - 1;

		for (int x = 0; x < input.width; x++) {
			// check top edge
			if ((in.get( x - 1, 0) + in.get( x + 1, 0) + in.get( x, 1)) == 3)
				output.set(x, 0, 0);
			else
				output.set(x, 0, input.get(x, 0));

			// check bottom edge
			if ((in.get( x - 1, h) + in.get( x + 1, h) + in.get( x, h - 1)) == 3)
				output.set(x, h, 0);
			else
				output.set(x, h, input.get(x, h));
		}

		for (int y = 0; y < input.height; y++) {
			// check left edge
			if ((in.get( 1, y) + in.get( 0, y - 1) + in.get( 0, y + 1)) == 3)
				output.set(0, y, 0);
			else
				output.set(0, y, input.get(0, y));

			// check right edge
			if ((in.get( w - 1, y) + in.get( w, y - 1) + in.get( w, y + 1)) == 3)
				output.set(w, y, 0);
			else
				output.set(w, y, input.get(w, y));
		}

		return output;
	}

	public static void erode8(GrayU8 input, GrayU8 output) {

		ImageBorder_S32 in = ImageBorderValue.wrap(input,1);
		
		final int h = input.height - 1;
		final int w = input.width - 1;

		for (int x = 0; x < input.width; x++) {
			// check top edge
			if ((in.get( x, 0) + in.get( x - 1, 0) + in.get( x + 1, 0) +
					in.get( x - 1, 1) + in.get( x, 1) + in.get( x + 1, 1)) == 6)
				output.set(x, 0, 1);
			else
				output.set(x, 0, 0);

			// check bottom edge
			if ((in.get( x, h) + in.get( x - 1, h) + in.get( x + 1, h) +
					in.get( x - 1, h - 1) + in.get( x, h - 1) + in.get( x + 1, h - 1)) == 6)
				output.set(x, h, 1);
			else
				output.set(x, h, 0);
		}

		for (int y = 0; y < input.height; y++) {
			// check left edge
			if ((in.get( 0, y) + in.get( 1, y) + in.get( 0, y - 1) +
					in.get( 1, y - 1) + in.get( 0, y + 1) + in.get( 1, y + 1)) == 6)
				output.set(0, y, 1);
			else
				output.set(0, y, 0);

			// check right edge
			if ((in.get( w - 1, y) + in.get( w, y) + in.get( w - 1, y - 1) +
					in.get( w, y - 1) + in.get( w - 1, y + 1) + in.get( w, y + 1)) == 6)
				output.set(w, y, 1);
			else
				output.set(w, y, 0);
		}
	}

	public static void dilate8(GrayU8 input, GrayU8 output) {

		ImageBorder_S32 in = ImageBorderValue.wrap(input,0);

		final int h = input.height - 1;
		final int w = input.width - 1;

		for (int x = 0; x < input.width; x++) {
			// check top edge
			if ((in.get( x, 0) + in.get( x - 1, 0) + in.get( x + 1, 0) +
					in.get( x - 1, 1) + in.get( x, 1) + in.get( x + 1, 1)) > 0)
				output.set(x, 0, 1);
			else
				output.set(x, 0, 0);

			// check bottom edge
			if ((in.get( x, h) + in.get( x - 1, h) + in.get( x + 1, h) +
					in.get( x - 1, h - 1) + in.get( x, h - 1) + in.get( x + 1, h - 1)) > 0)
				output.set(x, h, 1);
			else
				output.set(x, h, 0);
		}

		for (int y = 0; y < input.height; y++) {
			// check left edge
			if ((in.get( 0, y) + in.get( 1, y) + in.get( 0, y - 1) +
					in.get( 1, y - 1) + in.get( 0, y + 1) + in.get( 1, y + 1)) > 0)
				output.set(0, y, 1);
			else
				output.set(0, y, 0);

			// check right edge
			if ((in.get( w - 1, y) + in.get( w, y) + in.get( w - 1, y - 1) +
					in.get( w, y - 1) + in.get( w - 1, y + 1) + in.get( w, y + 1)) > 0)
				output.set(w, y, 1);
			else
				output.set(w, y, 0);
		}
	}

	public static void edge8(GrayU8 input, GrayU8 output) {

		ImageBorder_S32 in = ImageBorderValue.wrap(input,1);
		
		final int h = input.height - 1;
		final int w = input.width - 1;

		for (int x = 0; x < input.width; x++) {
			// check top edge
			if ((in.get( x - 1, 0) + in.get( x + 1, 0) +
					in.get( x - 1, 1) + in.get( x, 1) + in.get( x + 1, 1)) == 5)
				output.set(x, 0, 0);
			else
				output.set(x, 0, input.get(x, 0));

			// check bottom edge
			if ((in.get( x - 1, h) + in.get( x + 1, h) +
					in.get( x - 1, h - 1) + in.get( x, h - 1) + in.get( x + 1, h - 1)) == 5)
				output.set(x, h, 0);
			else
				output.set(x, h, input.get(x, h));
		}

		for (int y = 0; y < input.height; y++) {
			// check left edge
			if ((in.get( 1, y) + in.get( 0, y - 1) +
					in.get( 1, y - 1) + in.get( 0, y + 1) + in.get( 1, y + 1)) == 5)
				output.set(0, y, 0);
			else
				output.set(0, y, input.get(0, y));

			// check right edge
			if ((in.get( w - 1, y) + in.get( w - 1, y - 1) +
					in.get( w, y - 1) + in.get( w - 1, y + 1) + in.get( w, y + 1)) == 5)
				output.set(w, y, 0);
			else
				output.set(w, y, input.get(w, y));
		}
	}

	public static void removePointNoise(GrayU8 input, GrayU8 output) {

		ImageBorder_S32 in = ImageBorderValue.wrap(input,0);

		final int h = input.height - 1;
		final int w = input.width - 1;

		for (int x = 0; x < input.width; x++) {
			// check top edge
			int total = in.get( x - 1, 0) + in.get( x + 1, 0) +
					in.get( x - 1, 1) + in.get( x, 1) + in.get( x + 1, 1);
			if (total < 2)
				output.set(x, 0, 0);
			else
				output.set(x, 0, input.get(x, 0));

			// check bottom edge
			total = in.get( x - 1, h) + in.get( x + 1, h) +
					in.get( x - 1, h - 1) + in.get( x, h - 1) + in.get( x + 1, h - 1);
			if (total < 2)
				output.set(x, h, 0);
			else
				output.set(x, h, input.get(x, h));
		}

		for (int y = 0; y < input.height; y++) {
			// check left edge
			int total = in.get( 1, y) + in.get( 0, y - 1) +
					in.get( 1, y - 1) + in.get( 0, y + 1) + in.get( 1, y + 1);
			if (total < 2)
				output.set(0, y, 0);
			else
				output.set(0, y, input.get(0, y));

			// check right edge
			total = in.get( w - 1, y) + in.get( w - 1, y - 1) +
					in.get( w, y - 1) + in.get( w - 1, y + 1) + in.get( w, y + 1);
			if (total < 2)
				output.set(w, y, 0);
			else
				output.set(w, y, input.get(w, y));
		}
	}
}
