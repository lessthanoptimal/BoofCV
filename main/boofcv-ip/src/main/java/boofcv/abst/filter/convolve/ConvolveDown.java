/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.convolve;

import boofcv.struct.image.ImageBase;

/**
 *  Generalized interface for filtering images with convolution kernels while skipping pixels.
 * Can invoke different techniques for handling image borders.  The first pixel sampled is always (0,0) and the
 * sampled pixels are (x*skip,y*skip).
 *
 * @author Peter Abeles
 */
public interface ConvolveDown<Input extends ImageBase<Input>, Output extends ImageBase<Output>>
		extends ConvolveInterface<Input,Output>
{
	int getSkip();

	void setSkip(int skip);
}
