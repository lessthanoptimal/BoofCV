/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.robust;

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.sfm.ScaleTranslate2D;
import org.ddogleg.fitting.modelset.DistanceFromModel;

import java.util.List;

/**
 * Computes distance squared between p1 after applying the {@link boofcv.struct.sfm.ScaleTranslate2D} motion model and p2.
 * </p>
 * ||p2 - (p1*scale+trans)||^2
 *
 * @author Peter Abeles
 */
public class DistanceScaleTranslate2DSq implements DistanceFromModel<ScaleTranslate2D,AssociatedPair> {

	ScaleTranslate2D model;

	@Override
	public void setModel(ScaleTranslate2D model) {
		this.model = model;
	}

	@Override
	public double computeDistance(AssociatedPair pt)
	{
		double dx = pt.p2.x - pt.p1.x*model.scale - model.transX;
		double dy = pt.p2.y - pt.p1.y*model.scale - model.transY;

		return dx*dx + dy*dy;
	}

	@Override
	public void computeDistance(List<AssociatedPair> obs, double[] distance) {
		final int N = obs.size();
		for( int i = 0; i < N; i++ ) {
			distance[i] = computeDistance(obs.get(i));
		}
	}
}
