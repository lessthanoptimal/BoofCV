/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.geo;

/**
 * Provides information object a specific object and creates new instances of it.  Intended for use in highly
 * abstracted code.
 *
 * @author Peter Abeles
 */
public interface ObjectManager<O> {


	/**
	 * Creates a copy of 'src' in 'dst'
	 *
	 * @param src Input: Object being copied.
	 * @param dst Output: Object that is being written into.
	 */
	public void copy( O src, O dst);

	/**
	 * Creates a new instance of the class.
	 *
	 * @return New instance of Type.
	 */
	public O createInstance();

	/**
	 * Class type of the object being created.
	 *
	 * @return Class type
	 */
	public Class<O> getType();
}
