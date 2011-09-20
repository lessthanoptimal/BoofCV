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

package boofcv.alg.tracker.pklt;

import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.klt.KltTrackFault;
import boofcv.alg.tracker.klt.KltTracker;
import boofcv.factory.feature.detect.interest.FactoryCornerDetector;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageBase;
import boofcv.struct.pyramid.ImagePyramid;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>
 * Provides a simplified interface for working with {@link boofcv.alg.tracker.pklt.PyramidKltTracker}
 * by performing basic track management,
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class PkltManager<I extends ImageBase, D extends ImageBase> {
	// configuration for the track manager
	protected PkltManagerConfig<I,D> config;

	// list of features which are actively being tracked
	protected List<PyramidKltFeature> active = new ArrayList<PyramidKltFeature>();
	// list of features which were just spawned
	protected List<PyramidKltFeature> spawned = new ArrayList<PyramidKltFeature>();
	// list of features which were just dropped
	protected List<PyramidKltFeature> dropped = new ArrayList<PyramidKltFeature>();
	// feature data available for future tracking
	protected List<PyramidKltFeature> unused = new ArrayList<PyramidKltFeature>();

	// the tracker
	protected PyramidKltTracker<I, D> tracker;

	// used to automatically select new features in the image
	protected PyramidKltFeatureSelector<I, D> featureSelector;

	public PkltManager(PkltManagerConfig<I,D> config,
					   InterpolateRectangle<I> interpInput,
					   InterpolateRectangle<D> interpDeriv,
					   GenericPkltFeatSelector<I, D> featureSelector) {

		this.config = config;
		this.featureSelector = featureSelector;

		KltTracker<I, D> klt = new KltTracker<I, D>(interpInput,interpDeriv,config.config);
		tracker = new PyramidKltTracker<I,D>(klt);
		featureSelector.setTracker(tracker);

		// pre-declare image features
		int numLayers = config.pyramidScaling.length;
		for (int i = 0; i < config.maxFeatures; i++) {
			unused.add(new PyramidKltFeature(numLayers, config.featureRadius));
		}
	}

	/**
	 * Creates a PkltManager with a default interpolation and feature selector
	 *
	 * @param config Configuration for the tracker/manager.
	 */
	public PkltManager(PkltManagerConfig<I,D> config )
	{
		this(config,
				FactoryInterpolation.<I>bilinearRectangle(config.typeInput),
				FactoryInterpolation.<D>bilinearRectangle(config.typeDeriv),
				new GenericPkltFeatSelector<I,D>(
				(GeneralFeatureDetector<I,D>)
						FactoryCornerDetector.createKlt(config.featureRadius,config.config.minDeterminant,config.maxFeatures,config.typeDeriv),null));
	}

	/**
	 * Adds a new feature at the specified location.
	 *
	 * @return if the new feature was added or not.
	 */
	public boolean addTrack( float x , float y ) {
		if( unused.isEmpty() )
			return false;

		PyramidKltFeature f = unused.remove( unused.size()-1 );
		f.setPosition(x,y);
		tracker.setDescription(f);
		if( f.maxLayer == -1 ) {
			unused.add(f);
			return false;
		}

		spawned.add(f);
		active.add(f);

		return true;
	}

	/**
	 * Processes the next image in the sequence.  Updates tracks.
	 *
	 * @param image Image
	 * @param derivX Image derivative x-axis
	 * @param derivY Image derivative y-axis
	 */
	public void processFrame( ImagePyramid<I> image ,
							  ImagePyramid<D> derivX ,
							  ImagePyramid<D> derivY ) {

		spawned.clear();
		dropped.clear();
		tracker.setImage(image,derivX,derivY);
		
		for( int i = active.size()-1; i >= 0; i-- ) {
			PyramidKltFeature f = active.get(i);
			KltTrackFault result = tracker.track(f);
			if( result != KltTrackFault.SUCCESS ) {
//				System.out.println("Dropping feature: "+result);
				unused.add(f);
				active.remove(i);
				dropped.add(f);
			} else {
				tracker.setDescription(f);
			}
		}

		// if there are too few features spawn new ones
		if( active.size() < config.minFeatures ) {
			spawnTracks( image , derivX , derivY );
		}
	}

	/**
	 * Drops all the active tracks.
	 */
	public void dropAllTracks() {
		unused.addAll(active);
		active.clear();
	}

	/**
	 * Returns a list of features being actively tracked.
	 *
	 * @return List of features.
	 */
	public List<PyramidKltFeature> getTracks() {
		return active;
	}

	/**
	 * Returns a list of features that were recently spawned tracked.
	 *
	 * @return List of features.
	 */
	public List<PyramidKltFeature> getSpawned() {
		return spawned;
	}

	/**
	 * Returns a list of features that were dropped on the last call to {@link #processFrame}.
	 *
	 * @return List of features.
	 */
	public List<PyramidKltFeature> getDropped() {
		return dropped;
	}

	public PkltManagerConfig<I, D> getConfig() {
		return config;
	}

	public void dropTrack(PyramidKltFeature feature) {
		if( !active.remove(feature))
			throw new IllegalArgumentException("Feature not in active list");
		dropped.add(feature);
	}

	/**
	 * Automatically detects and creates new tracks.
	 */
	public void spawnTracks( ImagePyramid<I> image ,
							 ImagePyramid<D> derivX ,
							 ImagePyramid<D> derivY ) {
		int numBefore = active.size();
		featureSelector.setInputs(image,derivX,derivY);
		featureSelector.compute(active,unused);

		// add new features which were just added
		for( int i = numBefore; i < active.size(); i++ ) {
			spawned.add( active.get(i));
		}
	}
}
