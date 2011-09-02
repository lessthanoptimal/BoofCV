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

package boofcv.struct;

import java.lang.reflect.Array;


/**
 * Growable array designed for fast access.  It can be configured to declare new instances
 * or just grow the array.
 *
 * @author Peter Abeles
 */
public class FastQueue<T> {
	public T []data;
	public int size;
	public Class<T> type;

	// if true then it will declare new instances automatically
	// if false then
	private boolean declareInstances;

	public FastQueue(int initialMaxSize, Class<T> type, boolean declareInstances) {
		this.size = 0;
		this.type = type;
		this.declareInstances = declareInstances;

		data = (T[])Array.newInstance(type,initialMaxSize);
		if( declareInstances ) {
			for( int i = 0; i < initialMaxSize; i++ ) {
				data[i] = createInstance();
			}
		}
	}

	protected FastQueue(Class<T> type, boolean declareInstances ) {
		this(0,type,declareInstances);
	}

	public void removeTail() {
		if( size > 0 )
			size--;
		else
			throw new IllegalArgumentException("Size is already zero");
	}

	public void reset() {
		size = 0;
	}

	public int getInternalArraySize() {
		return data.length;
	}

	public int size() {
		return size;
	}

	/**
	 * Returns the element at the specified index.  Bounds checking is performed.
	 * @param index
	 * @return
	 */
	public T get( int index ) {
		if( index >= size )
			throw new IllegalArgumentException("Index out of bounds");
		return data[index];
	}

	/**
	 * Returns a new element of data.  If there are new data elements available then array will
	 * automatically grow.
	 *
	 * @return A new instance.
	 */
	public T pop() {
		if( size < data.length ) {
			return data[size++];
		} else {
			growArray((data.length+1)*2);
			return data[size++];
		}
	}

	public void add( T object ) {
		if( size >= data.length ) {
			growArray((data.length+1)*2);
		}
		data[size++] = object;
	}

	public void addAll( FastQueue<T> list ) {
		for( int i = 0; i < list.size; i++ ) {
			add( list.data[i]);
		}
	}

	public void growArray( int length) {
		if( this.data.length >= length)
			throw new IllegalArgumentException("The new size must be larger than the old size.");

		T []data = (T[])Array.newInstance(type, length);
		System.arraycopy(this.data,0,data,0,this.data.length);

		if( declareInstances ) {
			for( int i = this.data.length; i < length; i++ ) {
				data[i] = createInstance();
			}
		}
		this.data = data;
	}

	protected T createInstance() {
		try {
			return type.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
