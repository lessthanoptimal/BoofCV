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

package boofcv.alg.feature.tracker;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.ModelAssistedTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.TrackGeometryManager;
import boofcv.struct.image.ImageBase;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO COmment
 *
 *
 * @param <T>
 * @param <Model>
 * @param <Info> Geometric information used by model fitting
 *
 * @author Peter Abeles
 */
public class PointToAssistedTracker<T extends ImageBase,Model,Info>
		implements ModelAssistedTracker<T,Model,Info>
{
	// feature tracker
	protected ImagePointTracker<T> tracker;
	// Fits a model to the tracked features
	protected ModelMatcher<Model,Info> modelMatcher;
	// Refines the model using the complete inlier set
	protected ModelFitter<Model,Info> modelRefiner;

	// storage for refined model
	protected Model refinedModel;

	// was it successful in computing a model?
	protected boolean foundModel;

	// list of location information stored in each track
	protected List<Info> locationInfo = new ArrayList<Info>();

	// list of active tracks
	protected List<PointTrack> activeTracks = new ArrayList<PointTrack>();

	// storage for spawned tracks
	protected List<PointTrack> spawnedTracks = new ArrayList<PointTrack>();

	protected TrackGeometryManager<Info> manager;

	/**
	 *
	 * @param tracker
	 * @param modelMatcher
	 * @param modelRefiner Optional model refinement.  Can be null.
	 */
	public PointToAssistedTracker(ImagePointTracker<T> tracker,
								  ModelMatcher<Model, Info> modelMatcher,
								  ModelFitter<Model, Info> modelRefiner )
	{
		this.tracker = tracker;
		this.modelMatcher = modelMatcher;
		this.modelRefiner = modelRefiner;

		if( modelRefiner != null )
			refinedModel = modelRefiner.createModelInstance();
	}

	@Override
	public void setTrackGeometry(TrackGeometryManager<Info> manager) {
		this.manager = manager;
	}

	@Override
	public boolean foundModel() {
		return foundModel;
	}

	@Override
	public Model getModel() {
		if( modelRefiner == null )
			return modelMatcher.getModel();
		else
			return refinedModel;
	}

	@Override
	public List<Info> getMatchSet() {
		return modelMatcher.getMatchSet();
	}

	@Override
	public int convertMatchToTrackIndex(int matchIndex) {
		return modelMatcher.getInputIndex(matchIndex);
	}

	@Override
	public void reset() {
		System.out.println(" reset");
		tracker.reset();
		foundModel = false;
	}

	@Override
	public void process(T image) {
		// update the feature tracker
		tracker.process(image);

		activeTracks.clear();
		tracker.getActiveTracks(activeTracks);

		if( activeTracks.size() == 0 ) {
			foundModel = false;
			return;
		}

		// extract location information from tracks
		locationInfo.clear();
		for( PointTrack t : activeTracks ) {
			locationInfo.add(manager.extractGeometry(t));
		}

		// fit the motion model to the feature tracks
		if( !modelMatcher.process(locationInfo) ) {
			foundModel = false;
			return;
		}

		// refine the motion estimate
		if( modelRefiner != null )
			modelRefiner.fitModel(modelMatcher.getMatchSet(),modelMatcher.getModel(),refinedModel);

		foundModel = true;
	}

	@Override
	public void spawnTracks() {
		tracker.spawnTracks();

		tracker.getNewTracks(spawnedTracks);
		for( PointTrack t : spawnedTracks ) {
			manager.handleSpawnedTrack(t);
		}
		spawnedTracks.clear();
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
