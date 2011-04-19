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

package gecv.alg.filter.convolve;

import gecv.alg.filter.convolve.impl.ConvolveBox_F32_F32;
import gecv.alg.filter.convolve.impl.ConvolveBox_I8_I16;
import gecv.alg.filter.convolve.impl.ConvolveBox_I8_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt32;
import gecv.struct.image.ImageInt8;

/**
 * @author Peter Abeles
 */
// TODO add description
public class ConvolveBoxImage {

	public static void horizontal(ImageFloat32 input, ImageFloat32 output, int radius, boolean includeBorder) {
		ConvolveBox_F32_F32.horizontal(input, output, radius, includeBorder);
	}

	public static void horizontal(ImageInt8 input, ImageInt16 output, int radius, boolean includeBorder) {
		ConvolveBox_I8_I16.horizontal(input, output, radius, includeBorder);
	}

	public static void horizontal(ImageInt8 input, ImageInt32 output, int radius, boolean includeBorder) {
		ConvolveBox_I8_I32.horizontal(input, output, radius, includeBorder);
	}

	public static void vertical(ImageFloat32 input, ImageFloat32 output, int radius, boolean includeBorder) {
		ConvolveBox_F32_F32.vertical(input, output, radius, includeBorder);
	}

	public static void vertical(ImageInt8 input, ImageInt16 output, int radius, boolean includeBorder) {
		ConvolveBox_I8_I16.vertical(input, output, radius, includeBorder);
	}

	public static void vertical(ImageInt8 input, ImageInt32 output, int radius, boolean includeBorder) {
		ConvolveBox_I8_I32.vertical(input, output, radius, includeBorder);
	}
}
