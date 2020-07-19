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

import boofcv.struct.image.ImageBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides default implementations of functions in a {@link PointTrack}. Primary to reduce verbosity in writing
 * mock implementations for testing
 *
 * @author Peter Abeles
 */
public class PointTrackerDefault<T extends ImageBase<T>> implements PointTracker<T> {
	public long frameID;

	public PointTrackerDefault() {
		reset();
	}

	@Override
	public void process(T image) {
		frameID++;
	}

	@Override
	public void reset() {
		frameID = -1;
	}

	@Override
	public long getFrameID() {
		return frameID;
	}

	@Override
	public int getTotalActive() {
		return 0;
	}

	@Override
	public int getTotalInactive() {
		return 0;
	}

	@Override
	public void dropAllTracks() {
	}

	@Override
	public int getMaxSpawn() {
		return 0;
	}

	@Override
	public boolean dropTrack(PointTrack track) {
		return false;
	}

	@Override
	public void dropTracks(Dropper dropper) {
	}

	@Override
	public List<PointTrack> getAllTracks(List<PointTrack> list) {
		if( list != null )
			return list;
		return new ArrayList<>();
	}

	@Override
	public List<PointTrack> getActiveTracks(List<PointTrack> list) {
		if( list != null )
			return list;
		return new ArrayList<>();
	}

	@Override
	public List<PointTrack> getInactiveTracks(List<PointTrack> list) {
		if( list != null )
			return list;
		return new ArrayList<>();
	}

	@Override
	public List<PointTrack> getDroppedTracks(List<PointTrack> list) {
		if( list != null )
			return list;
		return new ArrayList<>();
	}

	@Override
	public List<PointTrack> getNewTracks(List<PointTrack> list) {
		return null;
	}

	@Override
	public void spawnTracks() {

	}
}
