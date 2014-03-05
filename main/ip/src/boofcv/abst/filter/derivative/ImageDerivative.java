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

package boofcv.abst.filter.derivative;

import boofcv.core.image.border.BorderType;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;


/**
 * @author Peter Abeles
 */
public interface ImageDerivative<T extends ImageBase, D extends ImageBase> {

	/**
	 * Overrides the default border behavior.  See {@link boofcv.factory.filter.derivative.FactoryDerivative} for a discussion
	 * of the pros and cons of each border type.
	 *
	 * @param type How image borders are handled.
	 */
	public void setBorderType( BorderType type );

	/**
	 * Returns how the image borders are handled.
	 *
	 * @return Image border type.
	 */
	public BorderType getBorderType();

	/**
	 * How many pixels wide is the region that is not processed along the outside
	 * border of the image.
	 *
	 * @return number of pixels.
	 */
	public int getBorder();

	/**
	 * Image type for derivative output
	 */
	public ImageType<D> getDerivType();
}
