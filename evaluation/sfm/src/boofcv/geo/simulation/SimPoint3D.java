/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.geo.simulation;

import georegression.struct.point.Point3D_F64;

/**
 * @author Peter Abeles
 */
public class SimPoint3D {
	// location of the point in world coordinates
	public Point3D_F64 world = new Point3D_F64();

	// unique ID assigned to this point
	public long id;

	// time at which the point was last viewed
	public long timeLastViewed;

	// additional data
	public Object trackData;

	public <T> T getTrackData() {
		return (T)trackData;
	}
}
