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


/**
 * Factory for creating common image types
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryImageGenerator {

	public static <T extends ImageGray> ImageGenerator<T> create(Class<T> type )
	{
		return new SingleBandGenerator(type);
	}

	public static <T extends ImageGray> ImageGenerator<T> create(T original )
	{
		return new WrapImage(original);
	}

	public static class WrapImage<T extends ImageGray> implements ImageGenerator<T>
	{
		T original;

		public WrapImage(T original) {
			this.original = original;
		}

		@Override
		public T[] createArray(int number) {
			return (T[])Array.newInstance(original.getClass(),number);
		}

		@Override
		public T createInstance(int width, int height) {
			return (T)original.createNew(width,height);
		}

		@Override
		public Class<T> getType() {
			return (Class<T>)original.getClass();
		}
	}
}
