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

package boofcv.abst.tracker;

import georegression.struct.point.Point2D_F64;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Current location of feature in a {@link PointTracker}.
 * 
 * @author Peter Abeles
 */
public class PointTrack {
	/** Location of the track in the image */
	public final Point2D_F64 pixel = new Point2D_F64();
	/**
	 * The type of feature it belongs to
	 */
	public int setId;
	/**
	 * Unique ID associated with this feature
	 */
	public long featureId;
	
	/** User specified data */
	public Object cookie;

	/** The frame the track was spawned at */
	public long spawnFrameID;

	/** Description of this feature that is used internally.  Don't mess with this */
	private Object description;

	public PointTrack(double x, double y, long featureId) {
		this.pixel.set(x,y);
		this.featureId = featureId;
	}

	public PointTrack() {
	}

	public static List<Point2D_F64> extractTrackPixels(@Nullable List<Point2D_F64> storage,
													   List<PointTrack> tracks)
	{
		if( storage == null )
			storage = new ArrayList<>();
		else
			storage.clear();

		for (int i = 0; i < tracks.size(); i++) {
			storage.add(tracks.get(i).pixel);
		}

		return storage;
	}

	public void set( PointTrack t ) {
		featureId = t.featureId;
		spawnFrameID = t.spawnFrameID;
		pixel.set(t.pixel);
		cookie = t.cookie;
		description = t.description;
	}
	
	public void reset() {
		spawnFrameID = -1;
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

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}
}
