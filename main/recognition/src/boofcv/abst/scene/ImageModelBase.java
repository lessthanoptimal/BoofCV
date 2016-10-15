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

package boofcv.abst.scene;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.io.File;
import java.io.IOException;

/**
 * Base class for algorithms which process an image and load a model to do so
 *
 * @author Peter Abeles
 */
public interface ImageModelBase<T extends ImageBase<T>> {
	/**
	 * Loads the model at the specified location.  See documentation of the classifier for what needs to be
	 * passed in here.
	 *
	 * @param path Path to directory or file containing the model
	 */
	void loadModel( File path ) throws IOException;

	/**
	 * Returns the type of input image
	 * @return input image type
	 */
	ImageType<T> getInputType();
}
