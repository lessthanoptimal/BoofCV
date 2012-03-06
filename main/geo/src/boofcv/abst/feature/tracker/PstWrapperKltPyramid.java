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

package boofcv.abst.feature.tracker;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.tracker.pklt.PkltManager;
import boofcv.alg.tracker.pklt.PkltManagerConfig;
import boofcv.alg.tracker.pklt.PyramidKltFeature;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.alg.transform.pyramid.PyramidUpdateIntegerDown;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.image.ImageSingleBand;
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
public class PstWrapperKltPyramid <I extends ImageSingleBand,D extends ImageSingleBand>
		implements ImagePointTracker<I>
{

	PkltManager<I,D> trackManager;
	PyramidUpdaterDiscrete<I>  inputPyramidUpdater;
	ImageGradient<I,D> gradient;

	PyramidDiscrete<I> basePyramid;
	ImagePyramid<D> derivX;
	ImagePyramid<D> derivY;

	List<PointTrack> active = new ArrayList<PointTrack>();
	List<PointTrack> spawned = new ArrayList<PointTrack>();
	List<PointTrack> dropped = new ArrayList<PointTrack>();

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
		PointTrack p = new PointTrack(t.x,t.y,totalFeatures++);
		p.setDescription(t);
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
			dropped.add( (PointTrack)t.cookie);
			// todo slow way to remove features from a large list
			if( !active.remove(t.cookie) )
				throw new IllegalArgumentException("Feature dropped not in active list");
			t.cookie = null;
		}

		// update the position of all active tracks
		for( PointTrack t : active ) {
			PyramidKltFeature p = t.getDescription();
			t.set(p.x,p.y);
		}
	}

	@Override
	public void dropTrack(PointTrack track) {
		if( !active.remove(track) ) {
			throw new RuntimeException("Not in active list!");
		}
		trackManager.dropTrack( (PyramidKltFeature)track.getDescription() );
		dropped.add(track);
	}

	@Override
	public List<PointTrack> getActiveTracks() {
		return active;
	}

	@Override
	public List<PointTrack> getDroppedTracks() {
		return dropped;
	}

	@Override
	public List<PointTrack> getNewTracks() {
		return spawned;
	}

	public PkltManager<I, D> getTrackManager() {
		return trackManager;
	}
}
