/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;


/**
 * Generalized interface for processing images.
 *
 * @author Peter Abeles
 */
public interface FilterImageInterface<Input extends ImageBase<Input>, Output extends ImageBase<Output>>
{
	/**
	 * Processes the input image and writes the results to the output image.
	 *
	 * @param input Input image.
	 * @param output Output image.
	 */
	void process( Input input , Output output );

	/**
	 * How many pixels are not processed along the x-axis border, left and right.
	 *
	 * @return Border size in pixels.
	 */
	int getBorderX();

	/**
	 * How many pixels are not processed along the y-axis border, top and bottom.
	 *
	 * @return Border size in pixels.
	 */
	int getBorderY();

	/**
	 * Specifies the input image type
	 *
	 * @return Input image type.
	 */
	ImageType<Input> getInputType();

	/**
	 * Specifies the output image type
	 *
	 * @return Output image type.
	 */
	ImageType<Output> getOutputType();
}
