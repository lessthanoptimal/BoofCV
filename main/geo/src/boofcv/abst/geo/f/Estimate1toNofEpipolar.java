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

package boofcv.abst.geo.f;

import boofcv.abst.geo.EstimateNofEpipolar;
import boofcv.abst.geo.GeoModelEstimator1toN;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.GeoModelEstimator1;
import org.ejml.data.DenseMatrix64F;

/**
 * Implementation of {@link GeoModelEstimator1toN} for epipolar matrices.
 *
 * @author Peter Abeles
 */
public class Estimate1toNofEpipolar extends GeoModelEstimator1toN<DenseMatrix64F,AssociatedPair>
	implements EstimateNofEpipolar
{
	public Estimate1toNofEpipolar(GeoModelEstimator1<DenseMatrix64F, AssociatedPair> alg) {
		super(alg);
	}
}
