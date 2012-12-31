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

package boofcv.struct.sparse;

import boofcv.struct.image.ImageBase;

/**
 * Interface for operations which are applied to a single pixel or region around
 * a single pixel
 *
 * @author Peter Abeles
 */
public interface SparseImageOperator <T extends ImageBase>
{
	/**
	 * Specifies the image being processed.
	 *
	 * @param input Image being processed
	 */
	public void setImage(T input );

	/**
	 * Checks to see if the entire sample region is contained inside the image or not.
	 * Depending on the implementation it might be able to handle out of bounds pixels or not.
	 */
	public boolean isInBounds( int x , int y );
}
