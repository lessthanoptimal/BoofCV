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

/**
 * Extends {@link TupleDesc_F64} by adding a boolean which indicates of the object it came
 * from is white or dark.  SURF and SIFT generate these kind of descriptors.  Association
 * can be improve by only trying to match white or dark features exclusively to each other.
 *
 * @author Peter Abeles
 */
public class BrightFeature extends TupleDesc_F64 {
	/**
	 *  true if the feature was white or false if it was dark
	 */
	public boolean white;

	public BrightFeature(int numFeatures ) {
		super(numFeatures);
	}

	protected BrightFeature(){}

	@Override
	public void setTo(TupleDesc_F64 source) {
		BrightFeature f = (BrightFeature)source;
		white = f.white;
		System.arraycopy(f.value,0,value,0,value.length);
	}

	@Override
	public BrightFeature copy() {
		BrightFeature ret = new BrightFeature( value.length );
		ret.setTo(this);
		return ret;
	}

}
