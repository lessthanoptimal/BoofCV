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

package boofcv.abst.disparity;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Provides do nothing implementations of all the functions for easy mocking
 *
 * @author Peter Abeles
 */
public abstract class StereoDisparityAbstract<T extends ImageBase<T>, D extends ImageGray<D>>
		implements StereoDisparity<T, D> {

	ImageType<T> imageType;
	Class<D> disparityType;

	protected StereoDisparityAbstract( ImageType<T> imageType, Class<D> disparityType ) {
		this.imageType = imageType;
		this.disparityType = disparityType;
	}

	protected StereoDisparityAbstract() {}

	@Override public void process( T imageLeft, T imageRight ) {}

	@Override public D getDisparity() {return null;}

	@Override public int getDisparityMin() {return 0;}

	@Override public int getDisparityRange() {return 0;}

	@Override public int getInvalidValue() {return 0;}

	@Override public int getBorderX() {return 0;}

	@Override public int getBorderY() {return 0;}

	@Override public ImageType<T> getInputType() {return imageType;}

	@Override public Class<D> getDisparityType() {return disparityType;}
}
