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

package gecv.abst.feature.describe;

import gecv.struct.feature.TupleDesc_F64;
import gecv.struct.image.ImageBase;


/**
 * Given a point in an image compute a descriptor of the point
 *
 * @author Peter Abeles
 */
public interface ExtractFeatureDescription<T extends ImageBase> {

	/**
	 * Specified the image which is to be processed.
	 * 
	 * @param image The image which contains the features.
	 */
	public void setImage( T image );

	/**
	 * Extract feature information from point at the specified scale.
	 *
	 * @param x Coordinate of the point.
	 * @param y Coordinate of the point.
	 * @param scale Scale at which the feature was found.
	 * @return  Description of the point.  If one could not be computed then null is returned.
	 */
	TupleDesc_F64 process( int x , int y , double scale );
}
