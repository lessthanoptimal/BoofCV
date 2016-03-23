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

package boofcv.struct.sparse;

import boofcv.struct.image.ImageGray;


/**
 * Computes the image gradient on a per pixel basis.
 *
 * @author Peter Abeles
 */
public interface SparseImageGradient<T extends ImageGray, G extends GradientValue>
	extends SparseImageOperator<T>
{
	/**
	 * Computes the gradient at the specified point.
	 *
	 * @param x x-axis pixel coordinate
	 * @param y y-axis pixel coordinate
	 * @return Gradient at that point.
	 */
	public G compute( int x , int y );
	
	public Class<G> getGradientType();
}
