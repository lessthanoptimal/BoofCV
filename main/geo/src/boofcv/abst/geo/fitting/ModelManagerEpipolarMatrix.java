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

import org.ddogleg.fitting.modelset.ModelManager;
import org.ejml.data.DenseMatrix64F;

/**
 * {@link ModelManager} for 3x3 {@link DenseMatrix64F}.
 *
 * @author Peter Abeles
 */
public class ModelManagerEpipolarMatrix implements ModelManager<DenseMatrix64F> {
	@Override
	public DenseMatrix64F createModelInstance() {
		return new DenseMatrix64F(3,3);
	}

	@Override
	public void copyModel(DenseMatrix64F src, DenseMatrix64F dst) {
		dst.set(src);
	}
}
