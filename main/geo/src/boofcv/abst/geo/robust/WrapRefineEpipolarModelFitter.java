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

package boofcv.abst.geo.robust;

import boofcv.abst.geo.epipolar.RefineEpipolarMatrix;
import boofcv.alg.geo.AssociatedPair;
import boofcv.numerics.fitting.modelset.ModelFitter;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.homo.UtilHomography;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class WrapRefineEpipolarModelFitter implements ModelFitter<Homography2D_F64,AssociatedPair> {

	DenseMatrix64F F = new DenseMatrix64F(3,3);
	RefineEpipolarMatrix alg;

	public WrapRefineEpipolarModelFitter(RefineEpipolarMatrix alg) {
		this.alg = alg;
	}

	@Override
	public Homography2D_F64 createModelInstance() {
		return new Homography2D_F64();
	}

	@Override
	public boolean fitModel(List<AssociatedPair> dataSet,
							Homography2D_F64 initial, Homography2D_F64 found )
	{
		UtilHomography.convert(initial,F);
		if( !alg.process(F,dataSet) )
			return false;

		UtilHomography.convert(alg.getRefinement(), found);

		return true;
	}
}
