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

package boofcv.abst.geo.optimization;

import org.ddogleg.fitting.modelset.ModelCodec;
import org.ejml.data.DenseMatrix64F;

/**
 * For use in cases where the model is a matrix and there is a 1-to-1 relationship with model
 * parameters.  Instead of copying the data over it simple puts parameter array reference into
 * the matrix.
 * 
 * @author Peter Abeles
 */
public class ModelCodecSwapData implements ModelCodec<DenseMatrix64F> {
	int paramLength;

	public ModelCodecSwapData(int paramLength) {
		this.paramLength = paramLength;
	}

	@Override
	public void decode(double[] input, DenseMatrix64F outputModel) {
		outputModel.data = input;
	}

	@Override
	public void encode(DenseMatrix64F model, double[] param) {
		System.arraycopy(model.data,0,param,0,paramLength);
	}

	@Override
	public int getParamLength() {
		return paramLength;
	}
}
