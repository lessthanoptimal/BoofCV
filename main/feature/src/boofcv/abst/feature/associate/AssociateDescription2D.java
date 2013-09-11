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

package boofcv.abst.feature.associate;

import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

/**
 * Associates features from two images together using both 2D location and descriptor information.  Each
 * source feature is paired up with a single feature in the destination.  If a match is not found then it
 * is added to the unassociated list.
 *
 * @param <Desc> Feature description type.
 *
 * @author Peter Abeles
 */
public interface AssociateDescription2D<Desc> extends Associate {

	/**
	 * Provide the location and descriptions for source features.
	 *
	 * @param location Feature locations.
	 * @param descriptions Feature descriptions.
	 */
	public void setSource( FastQueue<Point2D_F64> location , FastQueue<Desc> descriptions );

	/**
	 * Provide the location and descriptions for destination features.
	 *
	 * @param location Feature locations.
	 * @param descriptions Feature descriptions.
	 */
	public void setDestination( FastQueue<Point2D_F64> location , FastQueue<Desc> descriptions );
}
