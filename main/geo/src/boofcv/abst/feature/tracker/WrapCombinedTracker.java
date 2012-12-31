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

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.tracker.combined.CombinedTrack;
import boofcv.alg.tracker.combined.CombinedTrackerScalePoint;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.struct.pyramid.PyramidUpdaterDiscrete;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
// TODO drop after no associate after X detections
// TODO Speed up combination of respawn and spawn
public class WrapCombinedTracker<I extends ImageSingleBand, D extends ImageSingleBand, TD extends TupleDesc>
		implements ImagePointTracker<I> {

	CombinedTrackerScalePoint<I,D,TD> tracker;

	PyramidUpdaterDiscrete<I> updaterP;

	PyramidDiscrete<I> pyramid;
	PyramidDiscrete<D> derivX;
	PyramidDiscrete<D> derivY;

	ImageGradient<I,D> gradient;

	int reactivateThreshold;
	int previousSpawn;

	boolean detected;

	public WrapCombinedTracker(CombinedTrackerScalePoint<I, D,TD> tracker ,
							   int reactivateThreshold ,
							   Class<I> imageType , Class<D> derivType ) {
		this.tracker = tracker;
		this.reactivateThreshold = reactivateThreshold;

		updaterP = FactoryPyramid.discreteGaussian(imageType, -1, 2);
		gradient = FactoryDerivative.sobel(imageType, derivType);

		int pyramidScaling[] = tracker.getTrackerKlt().pyramidScaling;

		pyramid = new PyramidDiscrete<I>(imageType,true,pyramidScaling);
		derivX = new PyramidDiscrete<D>(derivType,false,pyramidScaling);
		derivY = new PyramidDiscrete<D>(derivType,false,pyramidScaling);

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
		updaterP.update(image,pyramid);
		PyramidOps.gradient(pyramid, gradient, derivX, derivY);

		// pass in filtered inputs
		tracker.updateTracks(image, pyramid, derivX, derivY);

		// if no features have been spawned don't try to reactive anything
		if( previousSpawn < 0 )
			return;

		int numActive = tracker.getPureKlt().size() + tracker.getReactivated().size();

		if( previousSpawn-numActive > reactivateThreshold) {
			detected = true;
			tracker.associateAllToDetected();
			previousSpawn = tracker.getPureKlt().size() + tracker.getReactivated().size();
		}

		for( CombinedTrack<TD> t : tracker.getPureKlt() ) {
			((PointTrack)t.getCookie()).set(t);
		}
		for( CombinedTrack<TD> t : tracker.getReactivated() ) {
			((PointTrack)t.getCookie()).set(t);
		}
	}

	@Override
	public void spawnTracks() {
		if( !detected ) {
			tracker.associateAllToDetected();
		}
		tracker.spawnTracksFromDetected();

		List<CombinedTrack<TD>> spawned = tracker.getSpawned();

		for( CombinedTrack<TD> t : spawned ) {
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
	public void dropTrack(PointTrack track) {
		tracker.dropTrack((CombinedTrack<TD>) track.getDescription());
		// make sure if the user drops a lot of tracks that doesn't force a constant respawn
		previousSpawn--;
	}

	@Override
	public List<PointTrack> getAllTracks(List<PointTrack> list) {
		if( list == null ) {
			list = new ArrayList<PointTrack>();
		}

		addToList(tracker.getReactivated(),list);
		addToList(tracker.getPureKlt(),list);
		addToList(tracker.getDormant(),list);

		return list;
	}

	@Override
	public List<PointTrack> getActiveTracks(List<PointTrack> list) {
		if( list == null ) {
			list = new ArrayList<PointTrack>();
		}

		addToList(tracker.getReactivated(),list);
		addToList(tracker.getPureKlt(),list);

		return list;
	}

	@Override
	public List<PointTrack> getInactiveTracks(List<PointTrack> list) {
		if( list == null ) {
			list = new ArrayList<PointTrack>();
		}

		addToList(tracker.getDormant(),list);

		return list;
	}

	@Override
	public List<PointTrack> getDroppedTracks(List<PointTrack> list) {
		if( list == null ) {
			list = new ArrayList<PointTrack>();
		}

		// it never drops tracks
		return list;
	}

	@Override
	public List<PointTrack> getNewTracks(List<PointTrack> list) {
		if( list == null ) {
			list = new ArrayList<PointTrack>();
		}

		addToList(tracker.getSpawned(),list);

		return list;
	}

	private void addToList( List<CombinedTrack<TD>> in , List<PointTrack> out ) {
		for( int i = 0; i < in.size(); i++ ) {
			out.add( (PointTrack)in.get(i).getCookie() );
		}
	}
}
