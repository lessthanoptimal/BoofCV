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

package boofcv.struct.gss;

import boofcv.core.image.border.BorderType;
import boofcv.struct.image.ImageGray;


/**
 * <p>
 * Interface for computing the scale space of an image and its derivatives.  The scale space
 * of an image is computed by convolving a Gaussian kernel across the image.  The image's scale
 * is determined by the Gaussian's standard deviation.  See [1] for a summary of scale-space theory.
 * </p>
 *
 * <p>
 * [1] Tony Lindeberg, "Scale-space: A framework for handling image structures at multiple scales,"
 * In. Proc. CERN School of Computing, Egmond aan Zee, The Netherlands, 8-21 September, 1996
 * </p>
 *
 * @author Peter Abeles
 */
public interface GaussianScaleSpace<T extends ImageGray, D extends ImageGray> {

	/**
	 * Sets the scales/blur magnitudes for which the scale-space should be computed over.
	 *
	 * @param scales All the scales.  These are absolute and not relative to the previous level.
	 */
	public void setScales( double ... scales);

	/**
	 * Returns the scale for the specified layer in the pyramid.  This is equivalent to
	 * the standard deviation of the Gaussian convolved across the original input image.
	 */
	public double getScale( int level );

	/**
	 * Specifies the original un-scaled image.
	 *
	 * @param input Original image.
	 */
	public void setImage( T input );

	/**
	 * Sets the active scale.  Must call {@link #setImage(ImageGray)}
	 * before this function.
	 *
	 * @param index Index of active scale
	 */
	public void setActiveScale( int index );

	/**
	 * Returns number of scaled images inside of this scale space.
	 * @return Number of scales.
	 */
	public int getTotalScales();

	/**
	 * Returns the value of the current active scale.
	 * @return active scale.
	 */
	public double getCurrentScale();

	/**
	 * Returns the scaled image at the active scale.
	 *
	 * @return scaled image.
	 */
	public T getScaledImage();

	/**
	 * Change how image borders are handled.
	 * @param type The BorderType.
	 */
	public void setBorderType( BorderType type );

	/**
	 * Returns how image borders are processed.
	 *
	 * @return how image borders are processed.
	 */
	public BorderType getBorderType();

	/**
	 * <p>
	 * Returns the partial derivative of the image.
	 * </p>
	 *
	 * <p>
	 * Examples:<br>
	 * derivative X  = getDerivative(true)<br>
	 * derivative Y  = getDerivative(false)<br>
	 * derivative XY = getDerivative(true,false)
	 * </p>
	 *
	 * @param isX specifies which partial derivative is to be returned.
	 * @return The image's derivative.
	 */
	public D getDerivative( boolean ...isX );
}
