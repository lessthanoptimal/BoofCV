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

package boofcv.struct;


/**
 * This is a queue that is composed of integers.  Elements are added and removed from the tail
 *
 * @author Peter Abeles
 */
public class GrowQueue_F32 {

    public float queue[];
    public int size;

    public GrowQueue_F32( int maxSize ) {
        queue = new float[ maxSize ];
        this.size = 0;
    }

	public void reset() {
		size = 0;
	}

    public void push( float val ) {
        if( size == queue.length ) {
			float temp[] = new float[ size * 2];
			System.arraycopy(queue,0,temp,0,size);
			queue = temp;
		}
		queue[size++] = val;
    }

	public float get( int index ) {
		return queue[index];
	}

	public int getSize() {
		return size;
	}

    public float pop() {
        return queue[--size];
    }
}
