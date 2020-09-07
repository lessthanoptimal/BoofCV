/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.concurrency;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;

/**
 * An array of objects which grows and recycles its elements automatically.
 *
 * @author Peter Abeles
 */
public class GrowArray<D> {
    BoofConcurrency.NewInstance<D> factory;
    BoofConcurrency.Reset<D> reset;

    D[] array;
    int size;

    public GrowArray( BoofConcurrency.NewInstance<D> factory ) {
        this(factory,(o)->{});
    }

    public GrowArray( BoofConcurrency.NewInstance<D> factory , BoofConcurrency.Reset<D> reset ) {
        this.factory = factory;
        this.reset = reset;

        array = createArray(0);
        size = 0;
    }

    @NotNull
    private D[] createArray(int length) {
        return (D[]) Array.newInstance(factory.newInstance().getClass(), length);
    }

    public void reset() {
        size = 0;
    }

    /**
     * Increases the size of the array so that it contains the specified number of elements. If the new length
     * is bigger than the old size then reset is called on the new elements
     */
    public void resize( int length ) {
        if( length >= array.length) {
            D[] tmp = createArray(length);
            System.arraycopy(array,0,tmp,0,array.length);
            for (int i = array.length; i < tmp.length; i++) {
                tmp[i] = factory.newInstance();
            }
            this.array = tmp;
        }
        for (int i = size; i < length; i++) {
            reset.reset(array[i]);
        }
        this.size = length;
    }

    /**
     * Add a new element to the array. Reset is called on it and it's then returned.
     */
    public D grow() {
        if( size == array.length ) {
            int length = Math.max(10,size<1000?size*2:size*5/3);
            D[] tmp = createArray(length);
            System.arraycopy(array,0,tmp,0,array.length);
            for (int i = array.length; i < tmp.length; i++) {
                tmp[i] = factory.newInstance();
            }
            this.array = tmp;
        }
        D ret = array[size++];
        reset.reset(ret);
        return ret;
    }

    public D get( int index ) {
        return array[index];
    }

    public int size() {
        return size;
    }

}
