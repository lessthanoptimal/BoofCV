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
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.klt.KltConfig;
import boofcv.alg.tracker.klt.KltTrackFault;
import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.PyramidDiscrete;

import java.util.ArrayList;
import java.util.List;

/**
 * Changes behavior of {@link PointTrackerKltPyramid} so that it conforms to the {@link PointTrackerTwoPass} interface.
 *
 * @author Peter Abeles
 */
public class PointTrackerTwoPassKltPyramid<I extends ImageGray,D extends ImageGray>
	extends PointTrackerKltPyramid<I,D> implements PointTrackerTwoPass<I>
{
	// list of active tracks before the current image is processed
	List<PyramidKltFeature> originalActive = new ArrayList<>();

	// list of tracks that were dropped, but won't really be dropped until tracking finishes
	List<PyramidKltFeature> candidateDrop = new ArrayList<>();

	// has finished tracking been called
	boolean finishedTracking;

	public PointTrackerTwoPassKltPyramid(KltConfig config,
										 int templateRadius ,
										 PyramidDiscrete<I> pyramid,
										 GeneralFeatureDetector<I, D> detector,
										 ImageGradient<I, D> gradient,
										 InterpolateRectangle<I> interpInput,
										 InterpolateRectangle<D> interpDeriv)
	{
		super(config, templateRadius, pyramid , detector, gradient, interpInput, interpDeriv,
				gradient.getDerivativeType().getImageClass());
	}

	@Override
	public void process(I image) {
		this.input = image;

		finishedTracking = false;
		spawned.clear();
		dropped.clear();

		// update image pyramids
		basePyramid.process(image);
		declareOutput();
		PyramidOps.gradient(basePyramid, gradient, derivX, derivY);

		// setup active list
		originalActive.clear();
		originalActive.addAll( active );

		// track features
		candidateDrop.clear();
		active.clear();

		tracker.setImage(basePyramid,derivX,derivY);
		for( int i = 0; i < originalActive.size(); i++ ) {
			PyramidKltFeature t = originalActive.get(i);
			KltTrackFault ret = tracker.track(t);

			boolean success = false;

			if( ret == KltTrackFault.SUCCESS ) {
				// discard a track if its center drifts outside the image.
				if( BoofMiscOps.checkInside(input, t.x, t.y)) {
					active.add(t);
					PointTrack p = t.getCookie();
					p.set(t.x,t.y);
					success = true;
				}
			}

			if( !success ) {
				candidateDrop.add(t);
			}
		}
	}

	@Override
	public void performSecondPass() {
		candidateDrop.clear();
		active.clear();

		for( int i = 0; i < originalActive.size(); i++ ) {
			PyramidKltFeature t = originalActive.get(i);
			KltTrackFault ret = tracker.track(t);

			boolean success = false;

			if( ret == KltTrackFault.SUCCESS ) {
				// discard a track if its center drifts outside the image.
				if(BoofMiscOps.checkInside(input, t.x, t.y)) {
					active.add(t);
					PointTrack p = t.getCookie();
					p.set(t.x,t.y);
					success = true;
				}
			}
			if( !success) {
				candidateDrop.add(t);
			}
		}
	}

	@Override
	public void finishTracking() {
		for( int i = 0; i < active.size(); ) {
			PyramidKltFeature t = active.get(i);
			if( tracker.setDescription(t) ) {
				i++;
			} else {
				candidateDrop.add(t);
				active.remove(i);
			}
		}

		for( int i = 0; i < candidateDrop.size(); i++ ) {
			PyramidKltFeature t = candidateDrop.get(i);
			dropped.add( t );
			unused.add( t );
		}

		finishedTracking = true;
	}

	@Override
	public void setHint(double pixelX, double pixelY, PointTrack track) {
		PyramidKltFeature kltTrack = track.getDescription();
		kltTrack.setPosition((float)pixelX,(float)pixelY);
	}

	@Override
	public List<PointTrack> getAllTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<>();

		if( finishedTracking )
			addToList(active,list);
		else
			addToList(originalActive,list);

		return list;
	}
}
