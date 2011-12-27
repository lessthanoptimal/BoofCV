/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.d2;


import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.d3.epipolar.HomographyLinear4;
import boofcv.numerics.fitting.modelset.ModelFitter;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.homo.UtilHomography;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Fits a homography to the observed points using linear algebra.  This provides an approximate solution.
 *
 * @author Peter Abeles
 */
public class ModelFitterLinearHomography implements ModelFitter<Homography2D_F64,AssociatedPair> {

	HomographyLinear4 alg = new HomographyLinear4(true);

	@Override
	public Homography2D_F64 declareModel() {
		return new Homography2D_F64();
	}

	@Override
	public boolean fitModel(List<AssociatedPair> dataSet,
							Homography2D_F64 initParam,
							Homography2D_F64 foundModel)
	{
		if( !alg.process(dataSet) )
			return false;

		DenseMatrix64F m = alg.getHomography();
		UtilHomography.convert(m,foundModel);

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return 4;
	}
}
