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

package boofcv.abst.geo.fitting;

import boofcv.abst.geo.PerspectiveNPoint;
import boofcv.numerics.fitting.modelset.ModelGenerator;
import boofcv.struct.geo.GeoModelEstimator1;
import boofcv.struct.geo.PointPosePair;
import georegression.struct.se.Se3_F64;

import java.util.List;

/**
 * Wrapper around {@link PerspectiveNPoint} for {@link ModelGenerator}.
 * 
 * @author Peter Abeles
 */
public class GenerateMotionPnP implements ModelGenerator<Se3_F64,PointPosePair> {

	GeoModelEstimator1<Se3_F64,PointPosePair> alg;

	public GenerateMotionPnP(GeoModelEstimator1<Se3_F64,PointPosePair> alg) {
		this.alg = alg;
	}

	@Override
	public Se3_F64 createModelInstance() {
		return new Se3_F64();
	}

	@Override
	public boolean generate(List<PointPosePair> dataSet, Se3_F64 model ) {
		return alg.process(dataSet,model);
	}

	@Override
	public int getMinimumPoints() {
		return alg.getMinimumPoints();
	}
}
