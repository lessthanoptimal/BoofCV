/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;

/**
 * <p>
 * Extracts features from the intensity image by finding local maximums.  Previously found corners are automatically
 * excluded and not added again.  Features below an intensity threshold are automatically ignored and no two
 * features can be closer than the minSeparation apart.
 * </p>
 *
 * <p>
 * Strict versus Not Strict.  Standard non-maximum suppression requires that the central point be a true maximum.
 * There are situations where symmetry in the image causes a couple of local pixels to have the exact same value
 * and what happens is that no points are found in those situations.  In these situations it might be appropriate
 * use the not strict rule which just requires that no local pixel have a higher value.<br>
 * <br>
 * strict:     I(x_c,y_x) > I(x,y)  for all (x,y) around (x_c,y_c) <br>
 * not struct: I(x_c,y_x) >= I(x,y) for all (x,y) around (x_c,y_c) <br>
 * </p>
 *
 * @author Peter Abeles
 */
public interface NonMaxExtractor {

	/**
	 * Sets the minimum distance two features can be.  This is the local region which is searched in non-max
	 * suppression.
	 *
	 * @param minSeparation How close two features can be.
	 */
	public void setMinSeparation(int minSeparation);

	/**
	 * The minimum intensity a feature can have.
	 *
	 * @param thresh Minimum intensity a feature can have to be valid.
	 */
	public void setThresh(float thresh);

	/**
	 * Specifies the size of the input image border which is ignored.
	 *
	 * @param border Size of image border.
	 */
	public void setInputBorder(int border);

	public int getInputBorder();

	/**
	 * Does it use the strict non-max rule or not?
	 *
	 * @return true if strict.
	 */
	public boolean isStrict();

	/**
	 * Returns the current feature selection threshold.
	 */
	public float getThresh();

	/**
	 * Detects corners in the image.  If a pixel has an intensity less than the minimum threshold or
	 * has a value equal to Float.MAX_VALUE it cannot be an image feature.  Typically Float.MAX_VALUE
	 * is given to features which have already been detected and should not be returned again.
	 *
	 * @param intensityImage Feature intensity image. Can be modified.
	 * @param corners	Where found corners are stored.
	 */
	public void process(ImageFloat32 intensityImage, QueueCorner corners);
}
