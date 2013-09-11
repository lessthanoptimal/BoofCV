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

import org.ddogleg.struct.FastQueue;


/**
 * {@link org.ddogleg.struct.FastQueue} for {@link NccFeature}.
 *
 * @author Peter Abeles
 */
public class NccFeatureQueue extends FastQueue<NccFeature> {

	int descriptorLength;

	public NccFeatureQueue(int descriptorLength) {
		super(0,NccFeature.class,true);
		this.descriptorLength = descriptorLength;
		growArray(10);
	}

	@Override
	protected NccFeature createInstance() {
		return new NccFeature(descriptorLength);
	}
}
