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

import boofcv.abst.feature.detect.interest.GeneralFeatureDetector;
import boofcv.abst.feature.tracker.*;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.klt.KltTrackFault;
import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidUpdaterDiscrete;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO DOcument
 *
 * @author Peter Abeles
 */
public class AssistedPyramidKltTracker<I extends ImageSingleBand, D extends ImageSingleBand,Model,Info>
		extends PointTrackerKltPyramid<I,D>
		implements ModelAssistedTracker<I,Model,Info>
{
	TrackGeometryManager<Model, Info> manager;

	ModelMatcher<Model, Info> matcherInitial;
	ModelMatcher<Model, Info> matcherFinal;

	ModelFitter<Model, Info> modelRefiner;

	// list of location information stored in each track
	protected List<Info> locationInfo = new ArrayList<Info>();

	Model refinedModel;
	boolean foundModel;

	/**
	 * Constructor which specified the KLT track manager and how the image pyramids are computed.
	 *
	 * @param config              KLT tracker configuration
	 * @param inputPyramidUpdater Computes the main image pyramid.
	 * @param gradient            Computes gradient image pyramid.
	 */
	public AssistedPyramidKltTracker(PkltConfig<I, D> config, PyramidUpdaterDiscrete<I> inputPyramidUpdater,
									 GeneralFeatureDetector<I, D> detector,
									 ImageGradient<I, D> gradient,
									 InterpolateRectangle<I> interpInput,
									 InterpolateRectangle<D> interpDeriv,
									 ModelMatcher<Model, Info> matcherInitial,
									 ModelMatcher<Model, Info> matcherFinal,
									 ModelFitter<Model, Info> modelRefiner )
	{
		super(config, inputPyramidUpdater, detector, gradient, interpInput, interpDeriv);

		this.matcherInitial = matcherInitial;
		this.matcherFinal = matcherFinal;
		this.modelRefiner = modelRefiner;

		if( modelRefiner != null )
			refinedModel = modelRefiner.createModelInstance();
	}

	@Override
	public void process(I image) {
		foundModel = true;
		spawned.clear();
		dropped.clear();

		// update image pyramids
		inputPyramidUpdater.update(image,basePyramid);
		PyramidOps.gradient(basePyramid, gradient, derivX, derivY);
		tracker.setImage(basePyramid,derivX,derivY);

		// track feature and add successful tracks to location info list
		locationInfo.clear();
		for( int i = 0; i < active.size(); i++ ) {
			PyramidKltFeature t = active.get(i);
			KltTrackFault ret = tracker.track(t);

			if( ret == KltTrackFault.SUCCESS ) {
				// don't update the track description yet or remove failed tracks
				PointTrack p = t.getCookie();
				p.set(t.x,t.y);
				locationInfo.add(manager.extractGeometry(p));
			}
		}

		// fit the motion model to the feature tracks
		if( !matcherInitial.process(locationInfo) ) {
			foundModel = false;
			return;
		}

		// adjust location of each track using the found model
		Model found = matcherInitial.getModel();

//		System.out.println("Inliers first: "+matcherInitial.getMatchSet().size());

		Point2D_F64 predicted = new Point2D_F64();
		for( PyramidKltFeature pt : active ) {
			PointTrack t = pt.getCookie();
			manager.predict(found, t,predicted);
			pt.setPosition((float)predicted.x,(float)predicted.y);
		}

		// track features again using new initial location
		locationInfo.clear();
		for( int i = 0; i < active.size(); ) {
			PyramidKltFeature t = active.get(i);
			KltTrackFault ret = tracker.track(t);

			if( ret == KltTrackFault.SUCCESS ) {
				tracker.setDescription(t);
				PointTrack p = t.getCookie();
				p.set(t.x,t.y);
				locationInfo.add(manager.extractGeometry(p));
				i++;
			} else {
				active.remove(i);
				dropped.add( t );
				unused.add( t );
			}
		}

		// recompute the model using improved matches
		if( !matcherFinal.process(locationInfo) ) {
			foundModel = false;
			return;
		}

//		System.out.println("Inliers second: "+matcherFinal.getMatchSet().size());

		// refine the motion estimate
		if( modelRefiner != null )
			modelRefiner.fitModel(matcherFinal.getMatchSet(), matcherFinal.getModel(),refinedModel);
	}

	@Override
	protected boolean checkValidSpawn( PointTrack p ) {
		return manager.handleSpawnedTrack(p);
	}

	@Override
	public void setTrackGeometry(TrackGeometryManager<Model,Info> manager) {
		this.manager = manager;
	}

	@Override
	public boolean foundModel() {
		return foundModel;
	}

	@Override
	public Model getModel() {
		if( modelRefiner == null )
			return matcherFinal.getModel();
		else
			return refinedModel;
	}

	@Override
	public List<Info> getMatchSet() {
		return matcherFinal.getMatchSet();
	}

	@Override
	public int convertMatchToActiveIndex(int matchIndex) {
		return matcherFinal.getInputIndex(matchIndex);
	}
}
