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

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.tracker.combined.CombinedTrack;
import boofcv.alg.tracker.combined.CombinedTrackerScalePoint;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
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
public class PointTrackerCombined<I extends ImageGray<I>, D extends ImageGray<D>, Desc extends TupleDesc>
		implements PointTracker<I> {

	CombinedTrackerScalePoint<I,D, Desc> tracker;

	// ID of the most recently processed frame
	protected long frameID=-1;

	PyramidDiscrete<I> pyramid;
	D[] derivX;
	D[] derivY;
	ImageType<D> derivType;

	ImageGradient<I,D> gradient;

	int reactivateThreshold;
	int previousSpawn;

	boolean detected;

	public PointTrackerCombined(CombinedTrackerScalePoint<I, D, Desc> tracker,
								ConfigDiscreteLevels configLevels,
								int reactivateThreshold,
								Class<I> imageType, Class<D> derivType) {
		this.tracker = tracker;
		this.reactivateThreshold = reactivateThreshold;
		this.derivType = ImageType.single(derivType);

		pyramid = FactoryPyramid.discreteGaussian(configLevels,-1,2,true, ImageType.single(imageType));
		gradient = FactoryDerivative.sobel(imageType, derivType);

		reset();
	}

	@Override
	public void reset() {
		tracker.reset();
		previousSpawn = 0;
		detected = false;
		frameID=-1;
	}

	@Override
	public long getFrameID() {
		return frameID;
	}

	@Override
	public int getTotalActive() {
		return tracker.getReactivated().size() + tracker.getPureKlt().size();
	}

	@Override
	public int getTotalInactive() {
		return tracker.getDormant().size();
	}

	@Override
	public void process(I image) {
		frameID++;
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
			((PointTrack)t.getCookie()).pixel.set(t.pixel);
		}

		for( CombinedTrack<Desc> t : tracker.getReactivated() ) {
			((PointTrack)t.getCookie()).pixel.set(t.pixel);
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

			p.pixel.set(t.pixel);
			p.setDescription(t);
			p.featureId = t.trackID;
			p.spawnFrameID = frameID;
		}

		previousSpawn = tracker.getPureKlt().size() + tracker.getReactivated().size();
	}

	@Override
	public void dropAllTracks() {
		tracker.dropAllTracks();
	}

	@Override
	public int getMaxSpawn() {
		return 0;
		// returning zero here since there is no good answer.
		// The detector employed doesn't have a normal hard limit
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
	public void dropTracks(Dropper dropper) {
		dropTracks(dropper, tracker.getPureKlt());
		dropTracks(dropper, tracker.getReactivated());
		dropTracks(dropper, tracker.getDormant());
	}

	private void dropTracks(Dropper dropper, List<CombinedTrack<Desc>> tracks) {
		for (int i = tracks.size()-1; i >= 0; i--) {
			PointTrack track = tracks.get(i).getCookie();
			if( dropper.shouldDropTrack(track) ) {
				tracker.addUnused(tracks.remove(i));
				previousSpawn--;
			}
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
