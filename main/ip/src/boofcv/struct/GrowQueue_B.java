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

package boofcv.struct;


/**
 * This is a queue that is composed of booleans.  Elements are added and removed from the tail
 *
 * @author Peter Abeles
 */
public class GrowQueue_B {

	public boolean data[];
	public int size;

	public GrowQueue_B(int maxSize) {
		data = new boolean[ maxSize ];
		this.size = 0;
	}

	public GrowQueue_B() {
		this(10);
	}

	public void reset() {
		size = 0;
	}

	public void add(boolean value) {
		push(value);
	}

	public void push( boolean val ) {
		if( size == data.length ) {
			boolean temp[] = new boolean[ size * 2];
			System.arraycopy(data,0,temp,0,size);
			data = temp;
		}
		data[size++] = val;
	}

	public boolean get( int index ) {
		return data[index];
	}

	public void resize( int size ) {
		if( data.length < size ) {
			data = new boolean[size];
		}
		this.size = size;
	}

	public void setMaxSize( int size ) {
		if( data.length < size ) {
			data = new boolean[size];
		}
	}

	public int getSize() {
		return size;
	}

	public boolean pop() {
		return data[--size];
	}
}
