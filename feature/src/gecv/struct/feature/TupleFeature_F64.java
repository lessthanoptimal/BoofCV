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

package gecv.struct.feature;


/**
 * @author Peter Abeles
 */
public class TupleFeature_F64 {
	public double value[];

	public TupleFeature_F64( int numFeatures ) {
		this.value = new double[ numFeatures ];
	}

	public TupleFeature_F64 copy() {
		TupleFeature_F64 ret = new TupleFeature_F64( value.length );
		System.arraycopy(value,0,ret.value,0,value.length);
		return ret;
	}
}
