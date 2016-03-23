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

package boofcv.alg.feature.detect.intensity;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Several different types of corner detectors [1,2] all share the same initial processing steps.  First a 2 by 2 deformation
 * matrix D = [ Ixx , Ixy ; Iyx , Iyy] is computed around each pixel.  D is computed by summing up the product of each
 * pixel's gradient inside of a window.  Next how corner like each pixel is computed using the information in the deformation
 * matrix.  In the final step where each of these techniques differ.
 * <p>
 *
 * <p>
 * Ixx = Sum dX*dX<br>
 * Ixy = Iyx = Sum dX*dY<br>
 * Iyy = Sum dY*dY<br>
 * where the Sum is the sum across all the pixels within a rectangular window, and [dX,dY] is a pixel's gradient.
 * </p>
 *
 * <p>
 * Alternative implementations can consider a weighted window around the pixel.  By considering only a uniform set of
 * weights several optimizations are possible.  The runtime is independent of the window size and can be very efficiently
 * computed.
 * </p>
 *
 * <p>
 * [1] Jianbo Shi and Carlo Tomasi. Good Features to Track. IEEE Conference on Computer Vision and Pattern Recognition,
 * pages 593-600, 1994<br>
 * [2] E.R. Davies, "Machine Vision Theory Algorithms Practicalities," 3rd ed. 2005
 * </p>
 *
 * @author Peter Abeles
 */
public interface GradientCornerIntensity<T extends ImageGray> extends FeatureIntensity<T> {

	/**
	 * Computes feature intensity image.
	 *
	 * @param derivX Image derivative along the x-axis.
	 * @param derivY Image derivative along the y-axis.
	 * @param intensity Output intensity image
	 */
	public void process(T derivX, T derivY , GrayF32 intensity );
}
