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

package boofcv.struct.feature;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.struct.FastQueue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * {@link boofcv.struct.FastQueue} for TupleDesc.
 *
 * @author Peter Abeles
 */
public class TupleDescQueue<D extends TupleDesc> extends FastQueue<D> {

	int numFeatures;
	Constructor<D> c;

	public TupleDescQueue( Class<D> descType , int numFeatures, boolean declareInstances) {
		super(descType,declareInstances);
		this.numFeatures = numFeatures;
		growArray(10);
	}

	/**
	 * A more convenient constructor which takes the required information out of the passed in
	 * {@link DescribeRegionPoint}.
	 *
	 * @param describe Contains descriptor information
	 * @param declareInstances Should the list declare new instances or not
	 */
	public TupleDescQueue( DescribeRegionPoint<?,D> describe, boolean declareInstances) {
		super(describe.getDescriptorType(),declareInstances);
		this.numFeatures = describe.getDescriptionLength();
		growArray(10);
	}

	@Override
	protected D createInstance() {
		if( c == null ) {
			try {
				c = type.getConstructor(int.class);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException("Constructor with a parameter for numFeatures expected",e);
			}
		}

		try {
			return c.newInstance(numFeatures);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
