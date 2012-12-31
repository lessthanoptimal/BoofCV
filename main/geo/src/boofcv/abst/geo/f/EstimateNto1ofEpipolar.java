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

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.GeoModelEstimatorNto1;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.GeoModelEstimatorN;
import boofcv.struct.geo.QueueMatrix;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ejml.data.DenseMatrix64F;

/**
 * Implementation of {@link GeoModelEstimatorNto1} for epipolar matrices.
 *
 * @author Peter Abeles
 */
public class EstimateNto1ofEpipolar
		extends GeoModelEstimatorNto1<DenseMatrix64F,AssociatedPair>
		implements Estimate1ofEpipolar
{
	public EstimateNto1ofEpipolar(GeoModelEstimatorN<DenseMatrix64F, AssociatedPair> alg,
								  DistanceFromModel<DenseMatrix64F, AssociatedPair> distance,
								  int numTest) {
		super(alg, distance, new QueueMatrix(3,3), numTest);
	}

	@Override
	protected void copy(DenseMatrix64F src, DenseMatrix64F dst) {
		dst.set(src);
	}
}
