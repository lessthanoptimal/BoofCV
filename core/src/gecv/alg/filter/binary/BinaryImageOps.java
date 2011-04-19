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

package gecv.alg.filter.binary;

import gecv.struct.image.ImageInt8;

/**
 * <p>
 * DESIGN NOTE: 8-bit integer images ({@link ImageInt8}) are used instead of images composed of boolean values because
 * there is no performance advantage.  According to the virtual machines specification binary arrays are stored as
 * byte arrays with 1 representing true and 0 representing false.
 * </p>
 *
 * @author Peter Abeles
 */
// todo benchmark byte and boolean images to see which one is fastest to work with
// stronger typing of a binary image would be good...
public class BinaryImageOps {

	public static ImageInt8 erode(ImageInt8 input, ImageInt8 output) {
		return null;
	}

	public static ImageInt8 dilate(ImageInt8 input, ImageInt8 output) {
		return null;
	}

	public static ImageInt8 removePointNoise(ImageInt8 input, ImageInt8 output) {
		return null;
	}
}
