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

package gecv.struct.image;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Image class for images that are composed of multiple bands
 *
 * @author Peter Abeles
 */
public abstract class MultiSpectral<T extends ImageBase> {

	Class<T> type;

	public int width;
	public int height;
	public T bands[];

	public MultiSpectral(Class<T> type, int width, int height, int numBands) {
		this.type = type;
		this.width = width;
		this.height = height;
		bands = (T[]) Array.newInstance(type, numBands);

		for (int i = 0; i < numBands; i++) {
			bands[i] = declareImage(width, height);
		}
	}

	protected MultiSpectral(Class<T> type, int numBands) {
		this.type = type;
		bands = (T[]) Array.newInstance(type, numBands);
	}

	public Class<T> getType() {
		return type;
	}

	protected abstract T declareImage(int width, int height);

	public int getNumBands() {
		return bands.length;
	}

	public T getBand(int band) {
		if (band >= bands.length || band < 0)
			throw new IllegalArgumentException("The specified band is out of bounds");

		return bands[band];
	}
}
