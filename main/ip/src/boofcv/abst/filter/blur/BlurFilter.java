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

package boofcv.abst.filter.blur;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.struct.image.ImageGray;


/**
 * Interface for filters which blur the image.
 *
 * @author Peter Abeles
 */
public interface BlurFilter<T extends ImageGray> extends FilterImageInterface<T,T> {

	/**
	 * Radius of the square region.  The width is defined as the radius*2 + 1.
	 *
	 * @return Blur region's radius.
	 */
	public int getRadius();

	public void setRadius(int radius);
}
