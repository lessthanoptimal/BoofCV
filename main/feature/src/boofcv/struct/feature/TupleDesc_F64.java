/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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


import java.util.Arrays;

/**
 * Basic description of an image feature's attributes using an array.
 *
 * @author Peter Abeles
 */
public class TupleDesc_F64 implements TupleDesc<TupleDesc_F64> {
	public double value[];

	public TupleDesc_F64( int numFeatures ) {
		this.value = new double[ numFeatures ];
	}

	protected TupleDesc_F64() {
	}

	public void set( double ...value ) {
		System.arraycopy(value,0,this.value,0,this.value.length);
	}

	public TupleDesc_F64 copy() {
		TupleDesc_F64 ret = new TupleDesc_F64( value.length );
		System.arraycopy(value,0,ret.value,0,value.length);
		return ret;
	}

	public void set( TupleDesc_F64 src ) {
		System.arraycopy(src.value,0,this.value,0,this.value.length);
	}

	public double[] getValue() {
		return value;
	}

	public void setValue(double[] value) {
		this.value = value;
	}

	public void fill( double value ) {
		Arrays.fill(this.value,value);
	}

	@Override
	public void setTo(TupleDesc_F64 source) {
		System.arraycopy(source.value,0,value,0,value.length);
	}

	@Override
	public double getDouble(int index) {
		return value[index];
	}

	@Override
	public int size() {
		return value.length;
	}
}
