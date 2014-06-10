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

package boofcv.processing;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Base class for simplified images.  This provides a more object oriented way of interacting with images.
 *
 * @author Peter Abeles
 */
public class SimpleImage<T extends ImageBase> {
	protected T image;

	public SimpleImage(T image) {
		this.image = image;
	}

	public T getImage() {
		return image;
	}

	public ImageType getImageType() {
		return image.getImageType();
	}
}
