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

package boofcv.abst.sfm;

import boofcv.abst.feature.tracker.ModelAssistedTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.TrackGeometryManager;
import boofcv.struct.image.ImageBase;

import java.util.List;

/**
 * Abstract class which handles all the wrapped function for converting a {@link ModelAssistedTracker} into
 * a {@link ModelAssistedTrackerCalibrated}.
 *
 * @author Peter Abeles
 */
public abstract class ModelAssistedToCalibrated<T extends ImageBase,Model,Info>
	implements ModelAssistedTrackerCalibrated<T,Model,Info>
{
	ModelAssistedTracker<T,Model,Info> tracker;

	protected ModelAssistedToCalibrated(ModelAssistedTracker<T, Model, Info> tracker) {
		this.tracker = tracker;
	}

	@Override
	public void setTrackGeometry(TrackGeometryManager<Model, Info> manager) {
		tracker.setTrackGeometry(manager);
	}

	@Override
	public boolean foundModel() {
		return tracker.foundModel();
	}

	@Override
	public Model getModel() {
		return tracker.getModel();
	}

	@Override
	public List<Info> getMatchSet() {
		return tracker.getMatchSet();
	}

	@Override
	public int convertMatchToActiveIndex(int matchIndex) {
		return tracker.convertMatchToActiveIndex(matchIndex);
	}

	@Override
	public void reset() {
		tracker.reset();
	}

	@Override
	public void process(T image) {
		tracker.process(image);
	}

	@Override
	public void spawnTracks() {
		tracker.spawnTracks();
	}

	@Override
	public void dropAllTracks() {
		tracker.dropAllTracks();
	}

	@Override
	public void dropTrack(PointTrack track) {
		tracker.dropTrack(track);
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
}
