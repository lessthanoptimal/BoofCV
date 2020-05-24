/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.combined;

import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.struct.feature.TupleDesc;
import georegression.struct.point.Point2D_F64;

/**
 * An image feature track for {@link CombinedTrackerScalePoint}.
 *
 * @author Peter Abeles
 */
public class CombinedTrack<TD extends TupleDesc> {
	// Location of the track in the image in pixels
	public final Point2D_F64 pixel = new Point2D_F64();
	// KLT feature description
	public PyramidKltFeature track;
	// DDA type description
	public TD featureDesc;
	// Feature ID
	public long trackID;
	// The feature set that the feature belongs to
	public int featureSet;

	// user storage
	Object cookie;

	public <T>T getCookie() {
		return (T)cookie;
	}

	public void setCookie(Object cookie) {
		this.cookie = cookie;
	}
}
