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

package gecv.alg.tracker.pklt;

import gecv.abst.filter.derivative.ImageGradient;
import gecv.alg.interpolate.InterpolateRectangle;
import gecv.alg.tracker.klt.KltTrackFault;
import gecv.alg.tracker.klt.KltTracker;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Peter Abeles
 */
// todo flag to save old feature locations
// todo assign a unique ID to each feature
@SuppressWarnings({"unchecked"})
public class PkltManager<I extends ImageBase, D extends ImageBase> {
	PkltManagerConfig<I,D> config;

	private List<PyramidKltFeature> active = new ArrayList<PyramidKltFeature>();
	private List<PyramidKltFeature> spawned = new ArrayList<PyramidKltFeature>();
	private List<PyramidKltFeature> unused = new ArrayList<PyramidKltFeature>();

	PyramidKltTracker<I, D> tracker;
	protected D derivX[];
	protected D derivY[];

	InterpolateRectangle<I> interpInput;
	InterpolateRectangle<D> interpDeriv;

	ImageGradient<I,D> gradient;

	GenericPkltFeatSelector<I, D> featureSelector; // todo generalize again

	public PkltManager(PkltManagerConfig<I,D> config,
					   InterpolateRectangle<I> interpInput,
					   InterpolateRectangle<D> interpDeriv,
					   ImageGradient<I,D> gradient,
					   GenericPkltFeatSelector<I, D> featureSelector) {

		this.config = config;
		this.interpInput = interpInput;
		this.interpDeriv = interpDeriv;
		this.gradient = gradient;
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

	public void processFrame( ImagePyramid<I> image ,
							  ImagePyramid<D> derivX ,
							  ImagePyramid<D> derivY) {

		spawned.clear();
		tracker.setImage(image,derivX,derivY);
		
		for( int i = active.size()-1; i >= 0; i-- ) {
			PyramidKltFeature f = active.get(i);
			KltTrackFault result = tracker.track(f);
			if( result != KltTrackFault.SUCCESS ) {
//				System.out.println("Dropping feature: "+result);
				unused.add(f);
				active.remove(i);
			} else {
				tracker.setDescription(f);
			}
		}

		// if there are too few features spawn new ones
		if( active.size() < config.minFeatures ) {
			int numBefore = active.size();
			featureSelector.setInputs(image,derivX,derivY);
			featureSelector.compute(active,unused);

			// add new features which were just added
			for( int i = numBefore; i < active.size(); i++ ) {
				spawned.add( active.get(i));
			}
		}
	}

	public List<PyramidKltFeature> getFeatures() {
		return active;
	}

	public List<PyramidKltFeature> getSpawned() {
		return spawned;
	}

	public PkltManagerConfig<I, D> getConfig() {
		return config;
	}
}
