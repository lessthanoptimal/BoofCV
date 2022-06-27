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
import boofcv.struct.calib.CameraPinhole;
import lombok.Getter;
import org.ddogleg.fitting.modelset.ransac.Ransac;

import java.util.ArrayList;
import java.util.List;

/**
 * Extension of {@link Ransac} for two calibrated camera views. Input point will be in normalized image coordinates
 *
 * @author Peter Abeles
 */
public class RansacCalibrated<Model, Point> implements ModelMatcherMultiview<Model, Point> {

	/** Underlying RANSAC implementation */
	@Getter Ransac<Model, Point> ransac;

	// All the passed in camera intrinsics
	final List<CameraPinhole> listIntrinsics = new ArrayList<>();

	public RansacCalibrated( Ransac<Model, Point> ransac, int numberOfViews ) {
		this.ransac = ransac;

		// Initialize the list
		for (int i = 0; i < numberOfViews; i++) {
			listIntrinsics.add(null);
		}

		ransac.setInitializeModels(( generator, distance ) -> {
			DistanceFromModelMultiView dist = (DistanceFromModelMultiView)distance;
			for (int viewIdx = 0; viewIdx < listIntrinsics.size(); viewIdx++) {
				dist.setIntrinsic(viewIdx, listIntrinsics.get(viewIdx));
			}
		});
	}

	@Override
	public void setIntrinsic( int view, CameraPinhole intrinsic ) {
		listIntrinsics.set(view, intrinsic);
	}

	@Override
	public int getNumberOfViews() {
		return listIntrinsics.size();
	}

	@Override public boolean process( List<Point> dataSet ) {
		return ransac.process(dataSet);
	}

	@Override public Model getModelParameters() {
		return ransac.getModelParameters();
	}

	@Override public List<Point> getMatchSet() {
		return ransac.getMatchSet();
	}

	@Override public int getInputIndex( int matchIndex ) {
		return ransac.getInputIndex(matchIndex);
	}

	@Override public double getFitQuality() {
		return ransac.getFitQuality();
	}

	@Override public int getMinimumSize() {
		return ransac.getMinimumSize();
	}

	@Override public void reset() {
		ransac.reset();
	}

	@Override public Class<Point> getPointType() {
		return ransac.getPointType();
	}

	@Override public Class<Model> getModelType() {
		return ransac.getModelType();
	}
}
