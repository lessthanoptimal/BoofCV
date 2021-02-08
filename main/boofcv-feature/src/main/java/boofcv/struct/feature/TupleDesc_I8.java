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

/**
 * Feature description storage in an array of bytes.
 *
 * @author Peter Abeles
 */
public abstract class TupleDesc_I8<TD extends TupleDesc_I8> implements TupleDesc<TD> {
	public @Getter @Setter byte[] value;

	protected TupleDesc_I8( int numFeatures ) {
		this.value = new byte[numFeatures];
	}

	public void setTo( byte... value ) {
		System.arraycopy(value, 0, this.value, 0, this.value.length);
	}

	@Override
	public void setTo( TD source ) {
		System.arraycopy(source.value, 0, value, 0, value.length);
	}

	@Override
	public int size() {
		return value.length;
	}
}
