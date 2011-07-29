/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.geo.trackers;

import gecv.abst.filter.derivative.FactoryDerivative;
import gecv.abst.filter.derivative.ImageGradient;
import gecv.alg.filter.convolve.FactoryKernelGaussian;
import gecv.alg.geo.AssociatedPair;
import gecv.alg.geo.PointSequentialTracker;
import gecv.alg.geo.SingleImageInput;
import gecv.alg.tracker.pklt.PkltManager;
import gecv.alg.tracker.pklt.PkltManagerConfig;
import gecv.alg.tracker.pklt.PyramidKltFeature;
import gecv.alg.transform.pyramid.ConvolutionPyramid;
import gecv.alg.transform.pyramid.GradientPyramid;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.ImagePyramidFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * Wrapper around {@link gecv.alg.tracker.pklt.PyramidKltTracker} for {@link gecv.alg.geo.PointSequentialTracker}
 *
 * @author Peter Abeles
 */
public class PstWrapperKltPyramid <I extends ImageBase,D extends ImageBase>
		implements PointSequentialTracker<I>, SingleImageInput<I>
{

	PkltManager<I,D> trackManager;
	ConvolutionPyramid<I> inputPyramidUpdater;
	GradientPyramid<I,D> gradientPyramidUpdater;

	ImagePyramid<I> basePyramid;
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
	 * @param gradientPyramidUpdater Computes gradient image pyramid.
	 */
	public PstWrapperKltPyramid(PkltManager<I, D> trackManager,
								ConvolutionPyramid<I> inputPyramidUpdater,
								GradientPyramid<I,D> gradientPyramidUpdater) {
		setup(trackManager, inputPyramidUpdater, gradientPyramidUpdater);
	}

	private void setup(PkltManager<I, D> trackManager, ConvolutionPyramid<I> inputPyramidUpdater, GradientPyramid<I, D> gradientPyramidUpdater) {
		this.trackManager = trackManager;
		this.inputPyramidUpdater = inputPyramidUpdater;
		this.gradientPyramidUpdater = gradientPyramidUpdater;

		PkltManagerConfig<I, D> config = trackManager.getConfig();

		// force the min number of features to be zero to prevent automatic feature spawning
		config.minFeatures = 0;

		// declare the image pyramid
		basePyramid = ImagePyramidFactory.create(
				config.imgWidth,config.imgHeight,true,config.typeInput);
		derivX = ImagePyramidFactory.create(
				config.imgWidth,config.imgHeight,false,config.typeDeriv);
		derivY = ImagePyramidFactory.create(
				config.imgWidth,config.imgHeight,false,config.typeDeriv);

		basePyramid.setScaling(config.pyramidScaling);
		derivX.setScaling(config.pyramidScaling);
		derivY.setScaling(config.pyramidScaling);

		inputPyramidUpdater.setPyramid(basePyramid);
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

		GradientPyramid<I,D> gradientUpdater = new GradientPyramid<I,D>(gradient);

		setup(trackManager,
				new ConvolutionPyramid<I>(FactoryKernelGaussian.gaussian1D(typeInput,2),typeInput),
				gradientUpdater);
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
	public boolean addTrack(float x, float y) {
		if( trackManager.addTrack(x,y) ) {
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
		inputPyramidUpdater.update(image);
		gradientPyramidUpdater.update(basePyramid,derivX,derivY);

		// track features
		trackManager.processFrame(basePyramid,derivX,derivY);

		// remove dropped features
		for( PyramidKltFeature t : trackManager.getDropped() ) {
			dropped.add( (AssociatedPair)t.cookie);
			if( !active.remove(t.cookie) )
				throw new IllegalArgumentException("Feature droped not in active list");
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
