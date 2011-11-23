/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.tracker.pklt.PkltManager;
import boofcv.alg.tracker.pklt.PkltManagerConfig;
import boofcv.alg.tracker.pklt.PyramidKltFeature;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.alg.transform.pyramid.PyramidUpdateIntegerDown;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.image.ImageBase;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.struct.pyramid.PyramidUpdaterDiscrete;

import java.util.ArrayList;
import java.util.List;


/**
 * Wrapper around {@link boofcv.alg.tracker.pklt.PyramidKltTracker} for {@link ImagePointTracker}
 *
 * @author Peter Abeles
 */
public class PstWrapperKltPyramid <I extends ImageBase,D extends ImageBase>
		implements ImagePointTracker<I>
{

	PkltManager<I,D> trackManager;
	PyramidUpdaterDiscrete<I>  inputPyramidUpdater;
	ImageGradient<I,D> gradient;

	PyramidDiscrete<I> basePyramid;
	ImagePyramid<D> derivX;
	ImagePyramid<D> derivY;

	List<AssociatedPair> active = new ArrayList<AssociatedPair>();
	List<AssociatedPair> spawned = new ArrayList<AssociatedPair>();
	List<AssociatedPair> dropped = new ArrayList<AssociatedPair>();

	long totalFeatures = 0;

	/**
	 * Constructor which specified the KLT track manager and how the image pyramids are computed.
	 *
	 * @param trackManager KLT tracker
	 * @param inputPyramidUpdater Computes the main image pyramid.
	 * @param gradient Computes gradient image pyramid.
	 */
	public PstWrapperKltPyramid(PkltManager<I, D> trackManager,
								PyramidUpdateIntegerDown<I> inputPyramidUpdater,
								ImageGradient<I,D> gradient) {
		setup(trackManager, inputPyramidUpdater, gradient);
	}

	private void setup(PkltManager<I, D> trackManager,
					   PyramidUpdaterDiscrete<I> inputPyramidUpdater,
					   ImageGradient<I,D> gradient ) {
		this.trackManager = trackManager;
		this.gradient = gradient;
		this.inputPyramidUpdater = inputPyramidUpdater;

		PkltManagerConfig<I, D> config = trackManager.getConfig();

		// declare the image pyramid
		basePyramid = new PyramidDiscrete<I>(config.typeInput,true,config.pyramidScaling);
		derivX = new PyramidDiscrete<D>(config.typeDeriv,false,config.pyramidScaling);
		derivY = new PyramidDiscrete<D>(config.typeDeriv,false,config.pyramidScaling);
	}

	/**
	 * Uses a default algorithm for computing image pyramids.
	 *
	 * @param trackManager
	 */
	public PstWrapperKltPyramid(PkltManager<I, D> trackManager ) {
		Class<I> typeInput = trackManager.getConfig().typeInput;
		Class<D> typeDeriv = trackManager.getConfig().typeDeriv;

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(typeInput,typeDeriv);

		PyramidUpdaterDiscrete<I> pyrUpdater = FactoryPyramid.discreteGaussian(typeInput,-1,2);

		setup(trackManager,pyrUpdater, gradient);
	}

	@Override
	public void spawnTracks() {
		spawned.clear();
		
		trackManager.spawnTracks(basePyramid,derivX,derivY);

		// add new ones
		for( PyramidKltFeature t : trackManager.getSpawned() ) {
			// create the AssociatedPair, add it to the active and spawned track lists
			addSpawnedFeature(t);
		}
	}

	@Override
	public void dropTracks() {
		trackManager.dropAllTracks();
		dropped.addAll(active);
		active.clear();
	}

	@Override
	public boolean addTrack(double x, double y) {
		if( trackManager.addTrack((float)x,(float)y) ) {
			List<PyramidKltFeature> spawnList = trackManager.getSpawned();
			PyramidKltFeature t = spawnList.get( spawnList.size()-1 );
			addSpawnedFeature(t);
			return true;
		}
		return false;
	}

	private void addSpawnedFeature(PyramidKltFeature t) {
		AssociatedPair p = new AssociatedPair(totalFeatures++,t.x,t.y,t.x,t.y);
		p.description = t;
		t.setCookie(p);

		active.add(p);
		spawned.add(p);
	}

	@Override
	public void process(I image) {
		spawned.clear();
		dropped.clear();
		
		// update image pyramids
		inputPyramidUpdater.update(image,basePyramid);
		PyramidOps.gradient(basePyramid, gradient, derivX,derivY);

		// track features
		trackManager.processFrame(basePyramid,derivX,derivY);

		// remove dropped features
		for( PyramidKltFeature t : trackManager.getDropped() ) {
			dropped.add( (AssociatedPair)t.cookie);
			if( !active.remove(t.cookie) )
				throw new IllegalArgumentException("Feature dropped not in active list");
			t.cookie = null;
		}

		// update the position of all active tracks
		for( AssociatedPair t : active ) {
			PyramidKltFeature p = t.getDescription();
			t.currLoc.set(p.x,p.y);
		}

		if( !trackManager.getSpawned().isEmpty() )
			throw new RuntimeException("Bug.  nothing should be spawned here");

	}

	@Override
	public void setCurrentToKeyFrame() {
		// update the position of all active tracks
		for( AssociatedPair t : active ) {
			t.keyLoc.set(t.currLoc);
		}
	}

	@Override
	public void dropTrack(AssociatedPair track) {
		trackManager.dropTrack( (PyramidKltFeature)track.description );
	}

	@Override
	public List<AssociatedPair> getActiveTracks() {
		return active;
	}

	@Override
	public List<AssociatedPair> getDroppedTracks() {
		return dropped;
	}

	@Override
	public List<AssociatedPair> getNewTracks() {
		return spawned;
	}

	public PkltManager<I, D> getTrackManager() {
		return trackManager;
	}
}
