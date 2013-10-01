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

package boofcv.abst.geo.fitting;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.struct.geo.AssociatedPair;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Wrapper around {@link boofcv.abst.geo.Estimate1ofEpipolar} for {@link ModelGenerator}.  Used for robust model
 * fitting with outliers.
 * 
 * @author Peter Abeles
 */
public class GenerateEpipolarMatrix implements ModelGenerator<DenseMatrix64F,AssociatedPair> {

	Estimate1ofEpipolar alg;

	public GenerateEpipolarMatrix(Estimate1ofEpipolar alg) {
		this.alg = alg;
	}

	@Override
	public boolean generate(List<AssociatedPair> dataSet, DenseMatrix64F model ) {
		if( alg.process(dataSet,model) ) {
			return true;
		}
		return false;
	}

	@Override
	public int getMinimumPoints() {
		return alg.getMinimumPoints();
	}
}
