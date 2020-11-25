/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.misc;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDimension;

/** Used to look up images as needed for disparity calculation. */
public interface LookUpImages {
	/**
	 * Loads the shape for an image
	 *
	 * @param name (Input) Name of the image
	 * @param shape (Output) shape of the image
	 * @return true if the image was found or false if not
	 */
	boolean loadShape( String name, ImageDimension shape );

	/**
	 * Loads an image. If a multi change image is passed in it will load the image as a RGB image.
	 *
	 * @param name (Input) Name of the image
	 * @param output (Output) Storage for the image
	 * @return true if the image was found or false if not
	 */
	<LT extends ImageBase<LT>> boolean loadImage( String name, LT output );
}
