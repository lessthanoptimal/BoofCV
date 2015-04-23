/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.geo;

import boofcv.struct.geo.GeoModelEstimator1;
import org.ddogleg.fitting.modelset.ModelGenerator;

import java.util.List;

/**
 * Wrapper class for converting {@link GeoModelEstimator1} into {@link ModelGenerator}.
 *
 * @author Peter Abeles
 */
public class EstimatorToGenerator<Model,Point> implements ModelGenerator<Model,Point> {

	GeoModelEstimator1<Model,Point> alg;

	public EstimatorToGenerator(GeoModelEstimator1<Model, Point> alg ) {
		this.alg = alg;
	}

	@Override
	public boolean generate(List<Point> dataSet, Model out) {
		return alg.process(dataSet,out);
	}

	@Override
	public int getMinimumPoints() {
		return alg.getMinimumPoints();
	}
}
