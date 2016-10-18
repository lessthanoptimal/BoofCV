/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.tracker.combined.CombinedTrack;
import boofcv.alg.tracker.combined.CombinedTrackerScalePoint;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.PyramidDiscrete;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link CombinedTrackerScalePoint} for {@link PointTracker}. Features are respawned when the
 * number of active tracks drops below a threshold automatically.  This threshold is realtive to the number
 * of tracks spawned previously and is adjusted when the user requests that tracks are dropped.
 *
 * @author Peter Abeles
 */
// TODO drop after no associate after X detections
// TODO Speed up combination of respawn and spawn
public class PointTrackerCombined<I extends ImageGray, D extends ImageGray, Desc extends TupleDesc>
		implements PointTracker<I> {

	CombinedTrackerScalePoint<I,D, Desc> tracker;

	PyramidDiscrete<I> pyramid;
	D[] derivX;
	D[] derivY;
	Class<D> derivType;

	ImageGradient<I,D> gradient;

	int reactivateThreshold;
	int previousSpawn;

	boolean detected;

	public PointTrackerCombined(CombinedTrackerScalePoint<I, D, Desc> tracker,
								int reactivateThreshold,
								Class<I> imageType, Class<D> derivType) {
		this.tracker = tracker;
		this.reactivateThreshold = reactivateThreshold;
		this.derivType = derivType;

		int pyramidScaling[] = tracker.getTrackerKlt().pyramidScaling;
		pyramid = FactoryPyramid.discreteGaussian(pyramidScaling,-1,2,true,imageType);
		gradient = FactoryDerivative.sobel(imageType, derivType);

		reset();
	}

	@Override
	public void reset() {
		tracker.reset();
		previousSpawn = 0;
		detected = false;
	}

	@Override
	public void process(I image) {
		detected = false;

		// update the image pyramid
		pyramid.process(image);
		if( derivX == null ) {
			derivX = PyramidOps.declareOutput(pyramid, derivType);
			derivY = PyramidOps.declareOutput(pyramid, derivType);
		}
		PyramidOps.gradient(pyramid, gradient, derivX, derivY);

		// pass in filtered inputs
		tracker.updateTracks(image, pyramid, derivX, derivY);

		int numActive = tracker.getPureKlt().size() + tracker.getReactivated().size();

		if( previousSpawn-numActive > reactivateThreshold) {
			detected = true;
			tracker.associateAllToDetected();
			previousSpawn = tracker.getPureKlt().size() + tracker.getReactivated().size();
		}

		// Update the PointTrack state for KLT tracks
		for( CombinedTrack<Desc> t : tracker.getPureKlt() ) {
			((PointTrack)t.getCookie()).set(t);
		}

		for( CombinedTrack<Desc> t : tracker.getReactivated() ) {
			((PointTrack)t.getCookie()).set(t);
		}
	}

	@Override
	public void spawnTracks() {
		if( !detected ) {
			tracker.associateAllToDetected();
		}
		tracker.spawnTracksFromDetected();

		List<CombinedTrack<Desc>> spawned = tracker.getSpawned();

		for( CombinedTrack<Desc> t : spawned ) {
			PointTrack p = t.getCookie();
			if( p == null ) {
				p = new PointTrack();
				t.setCookie(p);
			}

			p.set(t);
			p.setDescription(t);
			p.featureId = t.featureId;
		}

		previousSpawn = tracker.getPureKlt().size() + tracker.getReactivated().size();
	}

	@Override
	public void dropAllTracks() {
		tracker.dropAllTracks();
	}

	@Override
	public boolean dropTrack(PointTrack track) {
		if( tracker.dropTrack((CombinedTrack<Desc>) track.getDescription()) ) {
			// make sure if the user drops a lot of tracks that doesn't force a constant respawn
			previousSpawn--;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public List<PointTrack> getAllTracks(List<PointTrack> list) {
		if( list == null ) {
			list = new ArrayList<>();
		}

		addToList(tracker.getReactivated(),list);
		addToList(tracker.getPureKlt(),list);
		addToList(tracker.getDormant(),list);

		return list;
	}

	@Override
	public List<PointTrack> getActiveTracks(List<PointTrack> list) {
		if( list == null ) {
			list = new ArrayList<>();
		}

		addToList(tracker.getReactivated(),list);
		addToList(tracker.getPureKlt(),list);

		return list;
	}

	@Override
	public List<PointTrack> getInactiveTracks(List<PointTrack> list) {
		if( list == null ) {
			list = new ArrayList<>();
		}

		addToList(tracker.getDormant(),list);

		return list;
	}

	@Override
	public List<PointTrack> getDroppedTracks(List<PointTrack> list) {
		if( list == null ) {
			list = new ArrayList<>();
		}

		// it never drops tracks
		return list;
	}

	@Override
	public List<PointTrack> getNewTracks(List<PointTrack> list) {
		if( list == null ) {
			list = new ArrayList<>();
		}

		addToList(tracker.getSpawned(),list);

		return list;
	}

	private void addToList( List<CombinedTrack<Desc>> in , List<PointTrack> out ) {
		for( int i = 0; i < in.size(); i++ ) {
			out.add( (PointTrack)in.get(i).getCookie() );
		}
	}
}
