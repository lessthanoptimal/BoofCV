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

package boofcv.abst.filter.blur.impl;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.testing.BoofTesting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Simplified interface for using a blur filter that requires storage.  Reflections are used to look up a function inside
 * of {@link boofcv.alg.filter.blur.BlurImageOps} which is then invoked later on.
 *
 * @author Peter Abeles
 */
public class BlurStorageFilter<T extends ImageBase> implements FilterImageInterface<T,T> {

	// the blur function inside of BlurImageOps being invoked
	private Method m;
	// the Gaussians standard deviation
	private double sigma;
	// size of the blur region
	private int radius;
	// stores intermediate results
	private ImageBase storage;
	// if sigma is an input or not
	private boolean hasSigma;

	public BlurStorageFilter( String functionName , Class<?> imageType , int radius) {
		this.radius = radius;

		hasSigma = false;
		m = BoofTesting.findMethod(BlurImageOps.class,functionName,imageType,imageType,int.class,imageType);

		if( m == null )
			throw new IllegalArgumentException("Can't find matching function for image type "+imageType.getSimpleName());
	}

	public BlurStorageFilter( String functionName , Class<?> imageType , double sigma , int radius) {
		this.radius = radius;
		this.sigma = sigma;

		hasSigma = true;
		m = BoofTesting.findMethod(BlurImageOps.class,functionName,imageType,imageType,double.class,int.class,imageType);

		if( m == null )
			throw new IllegalArgumentException("Can't find matching function for image type "+imageType.getSimpleName());
	}

	/**
	 * Radius of the square region.  The width is defined as the radius*2 + 1.
	 *
	 * @return Blur region's radius.
	 */
	public int getRadius() {
		return radius;
	}

	@Override
	public void process(T input, T output) {
		try {
			if( storage == null ) {
				storage = output._createNew(output.width,output.height);
			} else {
				storage.reshape(output.width,output.height);
			}
			if( hasSigma )
				m.invoke(null,input,output,sigma,radius,storage);
			else
				m.invoke(null,input,output,radius,storage);
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
