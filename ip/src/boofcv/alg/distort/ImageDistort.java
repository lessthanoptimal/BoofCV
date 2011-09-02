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

package boofcv.alg.distort;

import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.ImageBase;


/**
 * Copies an image onto another image while applying a transform to the pixel coordinates. Pixels
 * which have no corresponding mapping can either be skipped or set to a default value.
 *
 * @author Peter Abeles
 */
public interface ImageDistort<T extends ImageBase> {

	/**
	 * Specifies how pixel coordinates are transformed from the destination
	 * to source images.
	 *
	 * @param dstToSrc Pixel coordinate transformation.
	 */
	public void setModel( PixelTransform dstToSrc );

	/**
	 * Applies the transform while skipping over pixels without a match.
	 *
	 * @param srcImg Input image. Not modified.
	 * @param dstImg Output image. Modified.
	 */
	public void apply( T srcImg , T dstImg );

	/**
	 * Applies the transform while setting pixels to the specified default value if there is
	 * no match.
	 *
	 * @param srcImg Input image. Not modified.
	 * @param dstImg Output image. Modified.
	 */
	public void apply( T srcImg , T dstImg , Number value );
}
