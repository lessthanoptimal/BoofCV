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

package boofcv.abst.sfm.d2;

import boofcv.struct.image.ImageBase;
import georegression.struct.InvertibleTransform;
import georegression.struct.homography.Homography2D_F64;

/**
 * Estimates the 2D motion of images in a video sequence.  All internal motion models must belong to the
 * {@link Homography2D_F64} transformation family, The returned transformations use the first image as the reference
 * frame, but other images can be converted into the reference frame.  Typically used in image stabilization
 * or image mosaic applications.
 *
 * @param <I> Input image type
 * @param <IT> Internally used image motion model
 *
 * @author Peter Abeles
 */
public interface ImageMotion2D<I extends ImageBase, IT extends InvertibleTransform>
{
	/**
	 * Processes and updates the image transform.  The very first image processed will always return
	 * true and have a transform of no motion.
	 *
	 * @param input Next image in the sequence.  Not modified.
	 * @return true if the transform has been updated and false if not
	 */
	boolean process( I input );

	/**
	 * Resets the class into its initial state and throws away any information on the image sequence
	 */
	void reset();

	/**
	 * Turns the current image into the origin of the coordinate system.
	 */
	void setToFirst();

	/**
	 * Transform from first image into the current image.
	 *
	 * NOTE: Returned transform is owned by this class and can be modified after any function is called.
	 *
	 * @return Image transform
	 */
	IT getFirstToCurrent();

	/**
	 * Type of transform that it estimates
	 *
	 * @return Transform type.
	 */
	Class<IT> getTransformType();
}
