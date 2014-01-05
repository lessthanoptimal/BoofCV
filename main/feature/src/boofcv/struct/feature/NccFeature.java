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
 * Description for normalized cross correlation (NCC).  The descriptor's value
 * in a NCC feature is the pixel intensity value minus the mean pixel intensity value.
 * </p>
 * value[i] = I(x,y) - mean
 *
 * @author Peter Abeles
 */
public class NccFeature extends TupleDesc_F64 {

	/** Mean pixel intensity   Can be used to reconstruct the original values of the template.*/
	public double mean;
	/** standard deviation of pixel intensity*/
	public double sigma;

	public NccFeature(int numFeatures) {
		super(numFeatures);
	}

	protected NccFeature() {
	}

	@Override
	public NccFeature copy() {
		NccFeature ret = new NccFeature( value.length );
		ret.setTo(this);
		return ret;
	}

	@Override
	public void setTo(TupleDesc_F64 source) {
		super.setTo(source);
		NccFeature ncc = (NccFeature)source;
		this.mean = ncc.mean;
		this.sigma = ncc.sigma;
	}
}
