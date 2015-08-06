/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.core.image.border;

import boofcv.struct.image.ImageBase;

/**
 * A wrapper around a normal image that returns a numeric value if a pixel is requested that is outside of the image
 * boundary.  The additional sanity checks can significantly slow down algorithms and should only be used when needed.
 *
 * @author Peter Abeles
 */
public abstract class ImageBorder<T extends ImageBase> {

	T image;

	protected ImageBorder(T image) {
		setImage(image);
	}

	protected ImageBorder() {
	}

	public void setImage( T image ) {
		this.image = image;
	}

	public T getImage() {
		return image;
	}

	public abstract void getGeneral(int x, int y, double[] pixel);

	public abstract void setGeneral(int x, int y, double[] pixel);
}
