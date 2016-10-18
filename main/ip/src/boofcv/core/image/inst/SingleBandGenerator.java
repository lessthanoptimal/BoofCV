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

package boofcv.core.image.inst;

import boofcv.core.image.ImageGenerator;
import boofcv.struct.image.ImageGray;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * Used to create images with a single band
 *
 * @author Peter Abeles
 */
public class SingleBandGenerator<T extends ImageGray> implements ImageGenerator<T> {

	Class<T> type;

	Constructor<T> constructor;

	public SingleBandGenerator(Class<T> type) {
		this.type = type;

		try {
			constructor = type.getConstructor(int.class,int.class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public T[] createArray(int number) {
		return (T[])Array.newInstance(type,number);
	}

	@Override
	public T createInstance(int width, int height) {
		try {
			return constructor.newInstance(width,height);
		} catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Class<T> getType() {
		return type;
	}
}
