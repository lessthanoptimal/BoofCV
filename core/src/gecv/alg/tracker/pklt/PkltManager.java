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
import gecv.alg.pyramid.PyramidUpdater;
import gecv.alg.tracker.klt.KltTrackFault;
import gecv.alg.tracker.klt.KltTracker;
import gecv.struct.image.FactoryImage;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.FactoryImagePyramid;
import gecv.struct.pyramid.ImagePyramid;

import java.lang.reflect.Array;
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
	private ImagePyramid<I> pyramid;
	protected D derivX[];
	protected D derivY[];

	InterpolateRectangle<I> interpInput;
	InterpolateRectangle<D> interpDeriv;

	ImageGradient<I,D> gradient;

	GenericPkltFeatSelector<I, D> featureSelector; // todo generalize again
	PyramidUpdater<I> pyramidUpdater;

	public PkltManager(PkltManagerConfig<I,D> config,
					   InterpolateRectangle<I> interpInput,
					   InterpolateRectangle<D> interpDeriv,
					   ImageGradient<I,D> gradient,
					   GenericPkltFeatSelector<I, D> featureSelector,
					   PyramidUpdater<I> pyramidUpdater) {

		this.config = config;
		this.interpInput = interpInput;
		this.interpDeriv = interpDeriv;
		this.gradient = gradient;
		this.featureSelector = featureSelector;
		this.pyramidUpdater = pyramidUpdater;

		declarePyramid(config);

		KltTracker<I, D> klt = new KltTracker<I, D>(interpInput,interpDeriv,config.config);
		tracker = new PyramidKltTracker<I,D>(klt);
		featureSelector.setTracker(tracker);

		// pre-declare image features
		int numLayers = pyramid.getNumLayers();
		for (int i = 0; i < config.maxFeatures; i++) {
			unused.add(new PyramidKltFeature(numLayers, config.featureRadius));
		}
	}

	/**
	 * Declare storage for the image pyramid and its derivatives
	 */
	private void declarePyramid(PkltManagerConfig<I, D> config) {
		pyramid = FactoryImagePyramid.create(config.typeInput,config.imgWidth,config.imgHeight,true);
		pyramid.setScaling(config.pyramidScaling);
		derivX = (D[]) Array.newInstance(config.typeDeriv,pyramid.getNumLayers());
		derivY = (D[])Array.newInstance(config.typeDeriv,pyramid.getNumLayers());

		for( int i = 0; i < pyramid.getNumLayers(); i++ ) {
			int scaling = pyramid.getScalingAtLayer(i);
			int width = config.imgWidth/scaling;
			int height = config.imgHeight/scaling;

			derivX[i] = FactoryImage.create(config.typeDeriv,width,height);
			derivY[i] = FactoryImage.create(config.typeDeriv,width,height);
		}

		pyramidUpdater.setPyramid(pyramid);
	}

	public void processFrame(I image) {

		spawned.clear();
		pyramidUpdater.update(image);

		computeGradient();
		tracker.setImage(pyramid,derivX,derivY);
		
		for( int i = 0; i < active.size(); ) {
			PyramidKltFeature f = active.get(i);
			KltTrackFault result = tracker.track(f);
			if( result != KltTrackFault.SUCCESS ) {
//				System.out.println("Dropping feature: "+result);
				unused.add(f);
				active.remove(i);
			} else {
				tracker.setDescription(f);
				i++;
			}
		}

		// if there are too few features spawn new ones
		if( active.size() < config.minFeatures ) {
			int numBefore = active.size();
			featureSelector.setInputs(pyramid,derivX,derivY);
			featureSelector.compute(active,unused);

			// add new features which were just added
			for( int i = numBefore; i < active.size(); i++ ) {
				spawned.add( active.get(i));
			}
		}
	}

	private void computeGradient() {
		for (int i = 0; i < pyramid.getNumLayers(); i++) {
			I img = pyramid.getLayer(i);
			gradient.process(img, derivX[i], derivY[i]);
		}
	}

	public List<PyramidKltFeature> getFeatures() {
		return active;
	}

	public List<PyramidKltFeature> getSpawned() {
		return spawned;
	}
}
