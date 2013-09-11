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

package boofcv.abst.geo;

import boofcv.struct.geo.GeoModelEstimator1;
import boofcv.struct.geo.GeoModelEstimatorN;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * Wrapper that allows {@link GeoModelEstimator1} to be used as a {@link GeoModelEstimatorN}.
 *
 * @author Peter Abeles
 */
public class GeoModelEstimator1toN<Model,Point> implements GeoModelEstimatorN<Model,Point> {

	private GeoModelEstimator1<Model,Point> alg;

	public GeoModelEstimator1toN(GeoModelEstimator1<Model, Point> alg) {
		this.alg = alg;
	}

	@Override
	public boolean process(List<Point> points, FastQueue<Model> estimatedModels) {
		estimatedModels.reset();

		Model m = estimatedModels.grow();

		if( alg.process(points,m) ) {
			return true;
		} else {
			estimatedModels.reset();
		}

		return false;
	}

	@Override
	public int getMinimumPoints() {
		return alg.getMinimumPoints();
	}
}
