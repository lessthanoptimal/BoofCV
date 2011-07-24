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

package gecv.alg;

import gecv.core.image.GeneralizedImageOps;
import gecv.struct.image.ImageBase;

/**
 * @author Peter Abeles
 */
// todo move to misc?
public class InputSanityCheck {

	/**
	 * If the output has not been declared a new instance is declared.  If an instance of the output
	 * is provided its bounds are checked.
	 */
	public static <T extends ImageBase> T checkDeclare(T input, T output) {
		if (output == null) {
			output = (T) input._createNew(input.width, input.height);
		} else if (output.width != input.width || output.height != input.height)
			throw new IllegalArgumentException("Width and/or height of input and output do not match.");
		return output;
	}

	public static <T extends ImageBase> T checkDeclare(ImageBase<?> input, T output , Class<T> outputType ) {
		if (output == null) {
			output = (T) GeneralizedImageOps.createImage(outputType,input.width, input.height);
		} else if (output.width != input.width || output.height != input.height)
			throw new IllegalArgumentException("Width and/or height of input and output do not match.");
		return output;
	}

	public static void checkSameShape(ImageBase<?> imgA, ImageBase<?> imgB) {
		if (imgA.width != imgB.width)
			throw new IllegalArgumentException("Image widths do not match.");
		if (imgA.height != imgB.height)
			throw new IllegalArgumentException("Image heights do not match.");
	}

	public static void checkSameShape(ImageBase<?> imgA, ImageBase<?> imgB, ImageBase<?> imgC) {
		if (imgA.width != imgB.width || imgA.width != imgC.width)
			throw new IllegalArgumentException("Image widths do not match.");
		if (imgA.height != imgB.height || imgA.height != imgC.height)
			throw new IllegalArgumentException("Image heights do not match.");
	}

	public static void checkSameShape(ImageBase<?> imgA, ImageBase<?> imgB, ImageBase<?> imgC , ImageBase<?> imgD) {
		if (imgA.width != imgB.width || imgA.width != imgC.width || imgA.width != imgD.width )
			throw new IllegalArgumentException("Image widths do not match.");
		if (imgA.height != imgB.height || imgA.height != imgC.height || imgA.height != imgD.height )
			throw new IllegalArgumentException("Image heights do not match.");
	}

	public static void checkSameShape(ImageBase<?> imgA, ImageBase<?> imgB, ImageBase<?> imgC , ImageBase<?> imgD , ImageBase<?> imgE) {
		if (imgA.width != imgB.width || imgA.width != imgC.width || imgA.width != imgD.width || imgA.width != imgE.width )
			throw new IllegalArgumentException("Image widths do not match.");
		if (imgA.height != imgB.height || imgA.height != imgC.height || imgA.height != imgD.height || imgA.height != imgE.height)
			throw new IllegalArgumentException("Image heights do not match.");
	}
}
