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

package boofcv.alg.tracker.meanshift;

import boofcv.struct.image.ImageBase;
import boofcv.struct.sparse.SparseImageSample_F32;
import georegression.struct.shapes.RectangleLength2D_I32;

/**
 * Computes the likelihood that a pixel belongs to the target.
 *
 * @author Peter Abeles
 */
public interface PixelLikelihood<T extends ImageBase> extends SparseImageSample_F32<T> {

	/**
	 * Sets the input image
	 */
	public void setImage( T image );

	/**
	 * Specifies where the initial location of the target is in the image and computes the model using pixels
	 * inside the rectangle
	 * @param target Location of target inside the image
	 */
	public void createModel( RectangleLength2D_I32 target );

}
