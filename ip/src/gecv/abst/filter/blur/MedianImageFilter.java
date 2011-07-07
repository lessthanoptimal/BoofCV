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

package gecv.abst.filter.blur;

import gecv.abst.filter.FilterImageInterface;
import gecv.alg.filter.blur.BlurImageOps;
import gecv.struct.image.ImageBase;
import gecv.testing.GecvTesting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Simplified interface for using a median filter.  Reflections are used to look up a function inside
 * of {@link BlurImageOps} which is then invoked later on.
 *
 * @author Peter Abeles
 */
public class MedianImageFilter<T extends ImageBase> implements FilterImageInterface<T,T> {

	Method m;
	int radius;

	public MedianImageFilter( Class<?> imageType , int radius) {
		this.radius = radius;

		m = GecvTesting.findMethod(BlurImageOps.class,"median",imageType,imageType,int.class);

		if( m == null )
			throw new IllegalArgumentException("Can't find matching function for image type "+imageType.getSimpleName());
	}

	/**
	 * Radius of the square region.  The width is defined as the radius*2 + 1.
	 *
	 * @return Square's radius.
	 */
	public int getRadius() {
		return radius;
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
}
