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

package gecv.struct;

/**
 * @author Peter Abeles
 */
public class GrowingArrayInt {
	public int data[];
	public int size;

	public GrowingArrayInt( int initialMaxSize ) {
		this.size = 0;
		data = new int[initialMaxSize];
	}

	public GrowingArrayInt() {
		this(20);
	}

	public void reset() {
		this.size = 0;
	}

	public void add( int value ) {
		if( size >= data.length ) {
			int temp[] = new int[ size*size ];
			System.arraycopy(data,0,temp,0,size);
			data = temp;
		}
		data[size++] = value;
	}

}
