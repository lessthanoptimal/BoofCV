/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.ImagePyramid;

/**
 * @author Peter Abeles
 */
// todo move to misc?
public class InputSanityCheck {

	/**
	 * Checks to see if the target image is null or if it is a different size than
	 * the test image.  If it is null then a new image is returned, otherwise
	 * target is reshaped and returned.
	 *
	 * @param target
	 * @param testImage
	 * @param targetType
	 * @param <T>
	 * @return
	 */
	public static <T extends ImageSingleBand> T checkReshape( T target , ImageSingleBand testImage , Class<T> targetType )
	{
		if( target == null ) {
			return GeneralizedImageOps.createSingleBand(targetType, testImage.width, testImage.height);
		} else if( target.width != testImage.width || target.height != testImage.height ) {
			target.reshape(testImage.width,testImage.height);
		}
		return  target;
	}

	/**
	 * If the output has not been declared a new instance is declared.  If an instance of the output
	 * is provided its bounds are checked.
	 */
	public static <T extends ImageSingleBand> T checkDeclare(T input, T output) {
		if (output == null) {
			output = (T) input._createNew(input.width, input.height);
		} else if (output.width != input.width || output.height != input.height)
			throw new IllegalArgumentException("Width and/or height of input and output do not match.");
		return output;
	}

	public static <T extends ImageSingleBand> T checkDeclare(ImageSingleBand<?> input, T output , Class<T> outputType ) {
		if (output == null) {
			output = (T) GeneralizedImageOps.createSingleBand(outputType, input.width, input.height);
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

	public static void checkSameShape(ImagePyramid<?> imgA, ImagePyramid<?> imgB) {
		if (imgA.getNumLayers() != imgB.getNumLayers())
			throw new IllegalArgumentException("Number of layers do not match");
		int N = imgA.getNumLayers();
		for( int i = 0; i < N; i++ ) {
			if( imgA.getScale(i) != imgB.getScale(i) )
				throw new IllegalArgumentException("Scales do not match at layer "+i);
		}
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

	/**
	 * Makes sure the input image is not a sub-image
	 */
	public static void checkSubimage( ImageBase image ) {
		if( image.isSubimage() )
			throw new IllegalArgumentException("Input image cannot be a subimage");
	}
}
