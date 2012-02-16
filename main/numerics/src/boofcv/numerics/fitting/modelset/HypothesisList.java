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

package boofcv.numerics.fitting.modelset;

import java.lang.reflect.Array;

/**
 * List of model hypotheses.  Essentially a glorified array to encourage data reuse and fast access.
 * 
 * @author Peter Abeles
 */
public class HypothesisList<Model> {
	Class<Model> dataType;
	Model h[];
	int N;
	ModelGenerator<Model,?> generator;

	public HypothesisList(ModelGenerator<Model, ?> generator) {
		this.generator = generator;
		Model m = generator.createModelInstance();
		dataType = (Class<Model>)m.getClass();
		h = (Model[])Array.newInstance(dataType,1);
		h[0] = m;
	}

	public Model swap( int index , Model data ) {
		Model ret = h[index];
		h[index] = data;
		
		return ret;
	}
	
	public Model get( int i ) {
		return h[i];
	}
	
	public Model pop() {
		if( N == h.length )
			resize(N*2);
		
		return h[N++];
	}
	
	public void reset() {
		N = 0;
	}
	
	public int size() {
		return N;
	}
	
	public int getMaxSize() {
		return h.length;
	}
	
	public void resize( int size ) {
		Model temp[] = (Model[])Array.newInstance(dataType,size);
		
		int smallestN = Math.min(size,h.length);
		for( int i = 0; i < smallestN; i++ ) {
			temp[i] = h[i];
		}
		for( int i = smallestN; i < size; i++ ) {
			temp[i] = generator.createModelInstance();
		}
		h = temp;
		
		if( N > size )
			N = size;
	}
}
