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

import boofcv.struct.image.ImageBase;

import java.util.List;

/**
 * Wrapper class that allows {@link PointTracker} to be used as a {@link PointTrackerTwoPass}.  Since
 * {@link PointTrack} does not have all the same capabilities of {@link PointTrackerTwoPass} not all operations
 * are supported or behave in exactly the same way. Use of this interface is only appropriate when
 * a two pass tracker is being used as a regular point tracker.
 *
 * @author Peter Abeles
 */
public class PointTrackerToTwoPass<T extends ImageBase>
		implements PointTrackerTwoPass<T>
{
	PointTracker<T> tracker;

	public PointTrackerToTwoPass(PointTracker<T> tracker) {
		this.tracker = tracker;
	}

	@Override
	public void process(T image) {
		tracker.process(image);
	}

	@Override
	public void reset() {
		tracker.reset();
	}

	@Override
	public void dropAllTracks() {
		tracker.dropAllTracks();
	}

	@Override
	public boolean dropTrack(PointTrack track) {
		return tracker.dropTrack(track);
	}

	@Override
	public List<PointTrack> getAllTracks(List<PointTrack> list) {
		return tracker.getAllTracks(list);
	}

	@Override
	public List<PointTrack> getActiveTracks(List<PointTrack> list) {
		return tracker.getActiveTracks(list);
	}

	@Override
	public List<PointTrack> getInactiveTracks(List<PointTrack> list) {
		return tracker.getInactiveTracks(list);
	}

	@Override
	public List<PointTrack> getDroppedTracks(List<PointTrack> list) {
		return tracker.getDroppedTracks(list);
	}

	@Override
	public List<PointTrack> getNewTracks(List<PointTrack> list) {
		return tracker.getNewTracks(list);
	}

	@Override
	public void spawnTracks() {
		tracker.spawnTracks();
	}

	@Override
	public void performSecondPass() {
		throw new RuntimeException("Operation not supported.  Need a real two pass tracker.");
	}

	@Override
	public void finishTracking() {
	}

	@Override
	public void setHint(double pixelX, double pixelY, PointTrack track) {
		throw new RuntimeException("Operation not supported.  Need a real two pass tracker.");
	}
}
