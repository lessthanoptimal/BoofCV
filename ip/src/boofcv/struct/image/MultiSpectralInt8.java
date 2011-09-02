/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.struct.image;

/**
 * Multiple spectral image composed {@link ImageUInt8} images.
 *
 * @author Peter Abeles
 */
public class MultiSpectralInt8 extends MultiSpectral<ImageUInt8> {

	public MultiSpectralInt8(int width, int height, int numBands) {
		super(ImageUInt8.class, width, height, numBands);
	}

	public MultiSpectralInt8(int numBands) {
		super(ImageUInt8.class, numBands);
	}

	@Override
	protected ImageUInt8 declareImage(int width, int height) {
		return new ImageUInt8(width, height);
	}

	public byte[] get(int x, int y, byte[] storage) {
		if (storage == null) {
			storage = new byte[bands.length];
		}

		for (int i = 0; i < bands.length; i++) {
			storage[i] = (byte) bands[i].get(x, y);
		}

		return storage;
	}

	public void set(int x, int y, byte[] value) {
		for (int i = 0; i < bands.length; i++) {
			bands[i].set(x, y, value[i]);
		}
	}
}
