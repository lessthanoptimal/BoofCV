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

/**
 * Remaps references to elements outside of an array to elements inside of the array.
 *
 * @author Peter Abeles
 */
public abstract class BorderIndex1D {

	public void setLength( int length ) {
		this.length = length;
	}

	public int getLength() {
		return length;
	}

	public abstract int getIndex( int index );

	protected int length;

	public abstract BorderIndex1D copy();
}
