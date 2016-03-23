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

import boofcv.struct.image.GrayI;

/**
 * Child of {@link ImageBorder} for {@link GrayI}.
 *
 * @author Peter Abeles
 */
public abstract class ImageBorder_S32<T extends GrayI> extends ImageBorder<T> {

	public ImageBorder_S32(T image) {
		super(image);
	}

	protected ImageBorder_S32() {
	}

	public int get( int x , int y ) {
		if( image.isInBounds(x,y) )
			return image.get(x,y);

		return getOutside( x , y );
	}

	public abstract int getOutside( int x , int y );

	public void set( int x , int y , int value ) {
		if( image.isInBounds(x,y) )
			image.set(x,y,value);

		setOutside( x , y , value);
	}

	@Override
	public void getGeneral(int x, int y, double[] pixel ) {
		pixel[0] = get(x, y);
	}

	@Override
	public void setGeneral(int x, int y, double[] pixel ) {
		set(x, y, (int)pixel[0]);
	}

	public abstract void setOutside( int x , int y , int value );
}
