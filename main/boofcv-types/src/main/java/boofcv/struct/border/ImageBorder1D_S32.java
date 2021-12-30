/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.border;

import boofcv.struct.image.GrayI;

/**
 * Image border is handled independently along each axis by changing the indexes so that it references a pixel
 * inside the image. For {@link ImageBorder_S32}.
 *
 * @author Peter Abeles
 */
public class ImageBorder1D_S32<T extends GrayI<T>> extends ImageBorder_S32<T> implements ImageBorder1D {
	BorderIndex1D rowWrap;
	BorderIndex1D colWrap;

	public ImageBorder1D_S32( FactoryBorderIndex1D factory ) {
		this.rowWrap = factory.newInstance();
		this.colWrap = factory.newInstance();
	}

	public ImageBorder1D_S32( BorderIndex1D rowWrap, BorderIndex1D colWrap ) {
		this.rowWrap = rowWrap;
		this.colWrap = colWrap;
	}

	public ImageBorder1D_S32( T image, BorderIndex1D rowWrap, BorderIndex1D colWrap ) {
		super(image);
		this.rowWrap = rowWrap;
		this.colWrap = colWrap;
	}

	@Override public BorderIndex1D getRowWrap() {return rowWrap;}

	@Override public BorderIndex1D getColWrap() {return colWrap;}

	@Override
	public void setImage( T image ) {
		super.setImage(image);
		colWrap.setLength(image.width);
		rowWrap.setLength(image.height);
	}

	@Override
	public ImageBorder1D_S32<T> copy() {
		return new ImageBorder1D_S32<>(this.rowWrap.copy(), this.colWrap.copy());
	}

	@Override
	public int getOutside( int x, int y ) {
		return image.get(colWrap.getIndex(x), rowWrap.getIndex(y));
	}

	@Override
	public void setOutside( int x, int y, int val ) {
		image.set(colWrap.getIndex(x), rowWrap.getIndex(y), val);
	}
}
