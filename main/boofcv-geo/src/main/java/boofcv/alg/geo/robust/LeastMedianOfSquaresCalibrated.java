/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.robust;

import boofcv.alg.geo.DistanceFromModelMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import lombok.Getter;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelMatcherPost;
import org.ddogleg.fitting.modelset.lmeds.LeastMedianOfSquares;
import org.ddogleg.struct.Factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Extension of {@link LeastMedianOfSquares} for two calibrated camera views. Input point must be
 * normalized image coordinates.
 *
 * @author Peter Abeles
 */
public class LeastMedianOfSquaresCalibrated<Model, Point>
		implements ModelMatcherMultiview<Model, Point>, ModelMatcherPost<Model, Point> {
	private @Getter final LeastMedianOfSquares<Model, Point> fitter;
	// All the passed in camera intrinsics
	final List<CameraPinhole> listIntrinsics = new ArrayList<>();

	public LeastMedianOfSquaresCalibrated( LeastMedianOfSquares<Model, Point> fitter ) {
		this.fitter = fitter;

		fitter.setInitializeModels(( generator, distance ) -> {
			DistanceFromModelMultiView dist = (DistanceFromModelMultiView)distance;
			BoofMiscOps.checkEq(dist.getNumberOfViews(), listIntrinsics.size(),
					"Must first call setModel()");
			for (int viewIdx = 0; viewIdx < listIntrinsics.size(); viewIdx++) {
				CameraPinhole intrinscs = Objects.requireNonNull(listIntrinsics.get(viewIdx),
						"Must first specify intrinsics for each camera");
				dist.setIntrinsic(viewIdx, intrinscs);
			}
		});
	}

	@Override
	public void setIntrinsic( int view, CameraPinhole intrinsic ) {
		if (listIntrinsics.size() == 0)
			throw new IllegalArgumentException("You must call setModel() first");
		listIntrinsics.set(view, intrinsic);
	}

	@Override
	public int getNumberOfViews() {
		return listIntrinsics.size();
	}

	@Override public boolean process( List<Point> list ) {
		return fitter.process(list);
	}

	@Override public Model getModelParameters() {
		return fitter.getModelParameters();
	}

	@Override public List<Point> getMatchSet() {
		return fitter.getMatchSet();
	}

	@Override public int getInputIndex( int i ) {
		return fitter.getInputIndex(i);
	}

	@Override public double getFitQuality() {
		return fitter.getFitQuality();
	}

	@Override public int getMinimumSize() {
		return fitter.getMinimumSize();
	}

	@Override public void reset() {
		fitter.reset();
	}

	@Override public Class<Point> getPointType() {
		return fitter.getPointType();
	}

	@Override public Class<Model> getModelType() {
		return fitter.getModelType();
	}

	@Override
	public void setModel( Factory<ModelGenerator<Model, Point>> factoryGenerator,
						  Factory<DistanceFromModel<Model, Point>> factoryDistance ) {
		// Make sure the list is large enough to store calibration for all the cameras
		int numViews = ((DistanceFromModelMultiView)factoryDistance.newInstance()).getNumberOfViews();
		listIntrinsics.clear();
		for (int i = 0; i < numViews; i++) {
			listIntrinsics.add(null);
		}

		fitter.setModel(factoryGenerator, factoryDistance);
	}
}
