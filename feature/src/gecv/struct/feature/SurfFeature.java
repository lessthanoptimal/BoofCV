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
 * Description of a SURF interest point.  It is composed of a set of image features computed from sub-regions
 * around the interest point as well as the sign of the Laplacian at the interest point.
 *
 * @author Peter Abeles
 */
public class SurfFeature {
	// is the feature light or dark. Can be used to improve lookup performance.
	public boolean laplacianPositive;
	// feature description
	public TupleDesc_F64 features;

	public SurfFeature( int numFeatures ) {
		features = new TupleDesc_F64(numFeatures);
	}


}
