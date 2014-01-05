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
 * Description of a SURF interest point.  It is composed of a set of image features computed from sub-regions
 * around the interest point as well as the sign of the Laplacian at the interest point.
 *
 * @author Peter Abeles
 */
public class SurfFeature extends TupleDesc_F64 {
	// is the feature light or dark. Can be used to improve lookup performance.
	public boolean laplacianPositive;

	public SurfFeature( int numFeatures ) {
		super(numFeatures);
	}

	protected SurfFeature(){}

	@Override
	public void setTo(TupleDesc_F64 source) {
		SurfFeature f = (SurfFeature)source;
		laplacianPositive = f.laplacianPositive;
		System.arraycopy(f.value,0,value,0,value.length);
	}

	@Override
	public SurfFeature copy() {
		SurfFeature ret = new SurfFeature( value.length );
		ret.setTo(this);
		return ret;
	}

}
