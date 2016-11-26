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

package boofcv.core.image.border;

import boofcv.struct.image.InterleavedInteger;

/**
 * Child of {@link ImageBorder} for {@link InterleavedInteger}.
 *
 * @author Peter Abeles
 */
public abstract class ImageBorder_IL_S32<T extends InterleavedInteger> extends ImageBorder<T> {

	public ImageBorder_IL_S32(T image) {
		super(image);
	}

	protected ImageBorder_IL_S32() {
	}

	public void set( int x , int y , int[] pixel ) {
		if (image.isInBounds(x, y)) {
			image.unsafe_set(x, y, pixel);
		} else {
			setOutside(x, y, pixel);
		}
	}

	public void get( int x , int y , int[] pixel ) {
		if( image.isInBounds(x,y) ) {
			image.unsafe_get(x, y, pixel);
		} else {
			getOutside(x, y, pixel);
		}
	}

	public abstract void getOutside( int x , int y , int[] pixel);

	public abstract void setOutside( int x , int y , int[] pixel);

	@Override
	public void getGeneral(int x, int y, double[] pixel ) {
		int[] tmp = new int[pixel.length];
		get(x,y,tmp);
		for (int i = 0; i < pixel.length; i++) {
			pixel[i] = tmp[i];
		}
	}

	@Override
	public void setGeneral(int x, int y, double[] pixel ) {
		int[] tmp = new int[pixel.length];
		for (int i = 0; i < pixel.length; i++) {
			tmp[i] = (int)pixel[i];
		}
		set(x, y, tmp);
	}
}
