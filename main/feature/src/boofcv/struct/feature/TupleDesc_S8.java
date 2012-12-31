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

package boofcv.struct.feature;


/**
 * Feature description storage in an array of signed bytes.
 *
 * @author Peter Abeles
 */
public class TupleDesc_S8 extends TupleDesc_I8<TupleDesc_S8> {

	public TupleDesc_S8(int numFeatures) {
		super(numFeatures);
	}

	public TupleDesc_S8 copy() {
		TupleDesc_S8 ret = new TupleDesc_S8( value.length );
		System.arraycopy(value,0,ret.value,0,value.length);
		return ret;
	}

	@Override
	public double getDouble(int index) {
		return value[index];
	}
}
