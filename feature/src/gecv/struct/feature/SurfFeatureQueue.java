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

import gecv.struct.FastQueue;


/**
 * {@link gecv.struct.FastQueue} for {@link SurfFeature}.
 *
 * @author Peter Abeles
 */
public class SurfFeatureQueue extends FastQueue<SurfFeature> {

	int numFeatures;

	public SurfFeatureQueue( int numFeatures ) {
		super(SurfFeature.class,true);
		this.numFeatures = numFeatures;
		growArray(10);
	}

	@Override
	protected SurfFeature createInstance() {
		return new SurfFeature(numFeatures);
	}
}
