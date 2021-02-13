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

package boofcv.struct.feature;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

/**
 * Basic description of an image feature's attributes using an array.
 *
 * @author Peter Abeles
 */
public class TupleDesc_F32 implements TupleDesc<TupleDesc_F32> {
	public @Getter @Setter float[] data;

	public TupleDesc_F32( int numFeatures ) {
		this.data = new float[numFeatures];
	}

	public TupleDesc_F32( float ...src ) {
		this.data = src.clone();
	}

	protected TupleDesc_F32() {}

	public void setTo( float... value ) {
		System.arraycopy(value, 0, this.data, 0, this.data.length);
	}

	public void fill( float value ) {
		Arrays.fill(this.data, value);
	}

	@Override public void setTo( TupleDesc_F32 source ) {
		System.arraycopy(source.data, 0, data, 0, data.length);
	}

	@Override public double getDouble( int index ) {
		return data[index];
	}

	@Override public int size() {
		return data.length;
	}

	@Override public TupleDesc_F32 newInstance() {
		return new TupleDesc_F32(data.length);
	}
}
