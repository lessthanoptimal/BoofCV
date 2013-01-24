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

package boofcv.abst.feature.tracker;

import georegression.struct.point.Point2D_F64;

/**
 * Current location of feature in a {@link PointTracker}.
 * 
 * @author Peter Abeles
 */
public class PointTrack extends Point2D_F64 {
	/**
	 * Unique ID associated with this feature
	 */
	public long featureId;
	
	/** User specified data */
	public Object cookie;

	/* Description of this feature that is used internally.  Don't mess with this */
	private Object description;

	public PointTrack(double x, double y, long featureId) {
		super(x, y);
		this.featureId = featureId;
	}

	public PointTrack() {
	}
	
	public void set( PointTrack t ) {
		featureId = t.featureId;
		x = t.x;
		y = t.y;
		cookie = t.cookie;
		description = t.description;
	}
	
	public void reset() {
		featureId = -1;
		cookie = null;
		description = null;
	}

	public <T> T getCookie() {
		return (T) cookie;
	}

	public <T> T getDescription() {
		return (T) description;
	}

	public void setDescription( Object description ) {
		this.description = description;
	}

	public void setCookie(Object cookie) {
		this.cookie = cookie;
	}
}
