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

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.tracker.DetectAssociateTracker;
import boofcv.abst.feature.tracker.ModelAssistedTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.TrackGeometryManager;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO document
 *
 * @author Peter Abeles
 */
public class AssistedTrackerTwoPass<I extends ImageSingleBand,D extends TupleDesc,Model,Info>
		extends DetectAssociateTracker<I,D>
		implements ModelAssistedTracker<I,Model,Info> {

	TrackGeometryManager<Model,Info> manager;

	ModelMatcher<Model, Info> fitterInitial;
	ModelMatcher<Model, Info> fitterFinal;

	ModelFitter<Model, Info> modelRefiner;

	AssociateDescription2D<D> associateFinal;

	Model refinedModel;
	boolean foundModel;

	// list of location information stored in each track
	protected List<Info> locationInfo = new ArrayList<Info>();

	public AssistedTrackerTwoPass(final DetectDescribePoint<I, D> detDesc,
								  AssociateDescription2D<D> associateInitial,
								  AssociateDescription2D<D> associateFinal,
								  final boolean updateDescription ,
								  ModelMatcher<Model, Info> fitterInitial,
								  ModelMatcher<Model, Info> fitterFinal,
								  ModelFitter<Model, Info> modelRefiner )
	{
		super(detDesc, associateInitial, updateDescription);

		this.associateFinal = associateFinal;
		this.fitterInitial = fitterInitial;
		this.fitterFinal = fitterFinal;
		this.modelRefiner = modelRefiner;

		if( modelRefiner != null ){
			refinedModel = modelRefiner.createModelInstance();
		}
	}

	@Override
	protected void performTracking() {
		foundModel = true;

		if (firstPass())
			return;

		System.out.println("Tracks "+tracksAll.size());
		System.out.println("First pass. matches "+matches.size());
		System.out.println("            inliers "+fitterInitial.getMatchSet().size());
		secondPass();
		System.out.println("Second pass. matches "+matches.size());
		System.out.println("             inliers "+fitterFinal.getMatchSet().size());
	}

	private boolean firstPass() {
		// Perform initial crude association
		associateFeatures();

		// update associated track locations and create a list of information for model fitting
		locationInfo.clear();
		matches = associate.getMatches();
		for( int i = 0; i < matches.size; i++ ) {
			AssociatedIndex indexes = matches.data[i];
			PointTrack track = tracksAll.get(indexes.src);
			Point2D_F64 loc = locDst.data[indexes.dst];
			track.set(loc.x, loc.y);

			locationInfo.add(manager.extractGeometry(track));
		}

		// fit the motion model to the feature tracks
		if( !fitterInitial.process(locationInfo) ) {
			foundModel = false;
			return true;
		}
		return false;
	}

	private void secondPass() {
		// put each track's newly predicted location into the source list
		locSrc.reset();
		for( int i = 0; i < tracksAll.size(); i++ ) {
			PointTrack t = tracksAll.get(i);
			locSrc.add(manager.predict(fitterInitial.getModel(), t));
		}

		// associate again
		associateFinal.setSource(locSrc, featSrc);
		associateFinal.setDestination(locDst, featDst);
		associateFinal.associate();

		// Update tracks using the second association
		updateTrackState(associateFinal.getMatches());

		// extract the updated track info
		locationInfo.clear();
		matches = associateFinal.getMatches();
		for( int i = 0; i < matches.size; i++ ) {
			AssociatedIndex indexes = matches.data[i];
			PointTrack track = tracksAll.get(indexes.src);
			locationInfo.add(manager.extractGeometry(track));
		}

		// recompute the model using improved matches
		if( !fitterFinal.process(locationInfo) ) {
			foundModel = false;
			return;
		}

		// refine the motion estimate
		if( modelRefiner != null )
			modelRefiner.fitModel(fitterFinal.getMatchSet(), fitterFinal.getModel(),refinedModel);

		foundModel = true;
	}

	@Override
	public void spawnTracks() {
		// TODO maximize reuse here
		// setup data structures
		if( isAssociated.length < featDst.size ) {
			isAssociated = new boolean[ featDst.size ];
		}

		// see which features are associated in the dst list
		for( int i = 0; i < featDst.size; i++ ) {
			isAssociated[i] = false;
		}

		if( matches != null ) {
			for( int i = 0; i < matches.size; i++ ) {
				isAssociated[matches.data[i].dst] = true;
			}
		}

		// create new tracks from latest unassociated detected features
		for( int i = 0; i < featDst.size; i++ ) {
			if( isAssociated[i]  )
				continue;

			PointTrack p = getUnused();
			Point2D_F64 loc = locDst.get(i);
			p.set(loc.x,loc.y);
			((D)p.getDescription()).setTo(featDst.get(i));

			if( manager.handleSpawnedTrack(p) ) {
				p.featureId = featureID++;

				tracksNew.add(p);
				tracksActive.add(p);
				tracksAll.add(p);
			} else {
				unused.add(p);
			}
		}
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
			return fitterFinal.getModel();
		else
			return refinedModel;
	}

	@Override
	public List<Info> getMatchSet() {
		return fitterFinal.getMatchSet();
	}

	@Override
	public int convertMatchToActiveIndex(int matchIndex) {
		return fitterFinal.getInputIndex(matchIndex);
	}
}
