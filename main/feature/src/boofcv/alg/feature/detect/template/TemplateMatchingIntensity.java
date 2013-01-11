/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.template;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

/**
 * <p>
 * Moves an image template over the image and for each pixel computes a metric for how similar
 * that region is to template.  An intensity image is thus computed for the entire image.  Better
 * matches always have a more positive value.  If a metric inheritally has a small value for
 * better matches then its sign will be adjusted so that poor matches have a negative value.
 * </p>
 * <p/>
 * <p>
 * A pixel in the intensity image is the result of evaluating the template with its center over
 * that pixel.  Given pixel (x,y) in the intensity image the template's top left corner (x',y') can be
 * found at: x' = x - getOffsetX() and y' = y - getOffsetY().
 * </p>
 * <p/>
 * <p>
 * IMAGE BORDER: If the image border is processed or not depends on the implementation.  If the border
 * is processed then partial templates are considered.  If the border is not processed then the
 * value of the intensity along the border is not defined and should not be processed.
 * </p>
 *
 * @author Peter Abeles
 */
public interface TemplateMatchingIntensity<T extends ImageBase> {

	/**
	 * Matches the template to the image and computes an intensity image.
	 *
	 * @param image    Input image. Not modified.
	 * @param template Template image.  Must be equal to or smaller than the input image. Not modified.
	 */
	public void process(T image, T template);

	/**
	 * Contains results of template matching.  Higher intensity values correspond to a better match.
	 * Local matches can be found using {@link boofcv.abst.feature.detect.extract.NonMaxSuppression}.
	 * See comment about processing the image border.
	 *
	 * @return Feature intensity
	 */
	public ImageFloat32 getIntensity();

	/**
	 * Does this algorithm process the image's border.  If it does not process the border
	 *
	 * @return true if the border is processed and false otherwise.
	 */
	public boolean isBorderProcessed();

	/**
	 * Offset from template's top left corner x-coordinate
	 *
	 * @return Offset in pixels
	 */
	public int getOffsetX();

	/**
	 * Offset from template's top left corner y-coordinate
	 *
	 * @return Offset in pixels
	 */
	public int getOffsetY();
}
