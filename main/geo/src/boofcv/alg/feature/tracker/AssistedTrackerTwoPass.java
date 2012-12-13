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
import boofcv.abst.feature.tracker.TrackGeometryManager;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class AssistedTrackerTwoPass<I extends ImageSingleBand,D extends TupleDesc,Model,Info>
		extends DetectAssociateTracker<I,D>
		implements ModelAssistedTracker<I,Model,Info> {

	TrackGeometryManager<Info> manager;
	PredictFeatureLocation<Info,Model> predictor;

	ModelMatcher<Model, Info> matcherInitial;
	ModelMatcher<Model, Info> matcherFinal;

	ModelFitter<Model, Info> modelRefiner;

	AssociateDescription2D<D> associateCrude;
	AssociateDescription2D<D> associateHint;

	public AssistedTrackerTwoPass(final DetectDescribePoint<I, D> detDesc,
								  final AssociateDescription2D<D> associate,
								  final boolean updateDescription ,
								  ModelMatcher<Model, Info> modelMatcher,
								  ModelFitter<Model, Info> modelRefiner )
	{
		super(detDesc, associate, updateDescription);
	}

	@Override
	protected FastQueue<AssociatedIndex> associateFeatures() {
		// Perform initial crude association

		// Compute a model

		// Use model to predict where features will be and associate there

		return null;
	}

	@Override
	public void setTrackGeometry(TrackGeometryManager<Info> manager) {
		this.manager = manager;
	}

	@Override
	public boolean foundModel() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Model getModel() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public List<Info> getMatchSet() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public int convertMatchToTrackIndex(int matchIndex) {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
