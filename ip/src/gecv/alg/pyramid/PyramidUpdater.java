/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.pyramid;

import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;

/**
 * Updates each layer in the pyramid given the original full resolution image.
 *
 * @author Peter Abeles
 */
public abstract class PyramidUpdater<T extends ImageBase> {

	protected ImagePyramid<T> pyramid;

	public void setPyramid(ImagePyramid<T> pyramid) {
		this.pyramid = pyramid;
	}

	public ImagePyramid<T> getPyramid() {
		return pyramid;
	}

	/**
	 * Given this original image create a pyramidal scale space representation.
	 *
	 * @param original Original full resolution image
	 */
	public void update(T original) {
		if (original.width != pyramid.bottomWidth || original.height != pyramid.bottomHeight)
			throw new IllegalArgumentException("Unexpected dimension");
		_update(original);
	}

	abstract protected void _update(T input);
}
