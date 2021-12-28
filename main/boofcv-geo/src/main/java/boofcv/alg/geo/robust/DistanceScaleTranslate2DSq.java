/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.ScaleTranslate2D;
import org.ddogleg.fitting.modelset.DistanceFromModel;

import java.util.List;

/**
 * Computes distance squared between p1 after applying the {@link ScaleTranslate2D} motion model and p2.
 * </p>
 * ||p2 - (p1*scale+trans)||^2
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DistanceScaleTranslate2DSq implements DistanceFromModel<ScaleTranslate2D, AssociatedPair> {

	ScaleTranslate2D model;

	@Override
	public void setModel( ScaleTranslate2D model ) {
		this.model = model;
	}

	@Override
	public double distance( AssociatedPair pt ) {
		double dx = pt.p2.x - pt.p1.x*model.scale - model.transX;
		double dy = pt.p2.y - pt.p1.y*model.scale - model.transY;

		return dx*dx + dy*dy;
	}

	@Override
	public void distances( List<AssociatedPair> obs, double[] distance ) {
		final int N = obs.size();
		for (int i = 0; i < N; i++) {
			distance[i] = distance(obs.get(i));
		}
	}

	@Override
	public Class<AssociatedPair> getPointType() {
		return AssociatedPair.class;
	}

	@Override
	public Class<ScaleTranslate2D> getModelType() {
		return ScaleTranslate2D.class;
	}
}
