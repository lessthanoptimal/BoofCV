/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.blur;

import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.BoofTesting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Simplified interface for using a median filter.  Reflections are used to look up a function inside
 * of {@link BlurImageOps} which is then invoked later on.
 *
 * @author Peter Abeles
 */
public class MedianImageFilter<T extends ImageSingleBand> implements BlurFilter<T> {

	// the blur function inside of BlurImageOps being invoked
	private Method m;
	// size of the blur region
	private int radius;

	Class<T> imageType;

	public MedianImageFilter( Class<T> imageType , int radius) {
		this.radius = radius;
		this.imageType = imageType;

		m = BoofTesting.findMethod(BlurImageOps.class,"median",imageType,imageType,int.class);

		if( m == null )
			throw new IllegalArgumentException("Can't find matching function for image type "+imageType.getSimpleName());
	}

	/**
	 * Radius of the square region.  The width is defined as the radius*2 + 1.
	 *
	 * @return Blur region's radius.
	 */
	@Override
	public int getRadius() {
		return radius;
	}

	@Override
	public void setRadius(int radius) {
		this.radius = radius;
	}

	@Override
	public void process(T input, T output) {
		try {
			m.invoke(null,input,output,radius);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getHorizontalBorder() {
		return 0;
	}

	@Override
	public int getVerticalBorder() {
		return 0;
	}

	@Override
	public Class<T> getInputType() {
		return imageType;
	}
}
