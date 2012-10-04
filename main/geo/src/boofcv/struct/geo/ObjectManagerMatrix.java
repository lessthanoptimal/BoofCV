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

package boofcv.struct.geo;

import org.ejml.data.DenseMatrix64F;

/**
 * {@link ObjectManager} for {@link DenseMatrix64F} of a fixed shape.
 *
 * @author Peter Abeles
 */
public class ObjectManagerMatrix implements ObjectManager<DenseMatrix64F> {

	int numRows,numCols;

	public ObjectManagerMatrix(int numRows, int numCols) {
		this.numRows = numRows;
		this.numCols = numCols;
	}

	@Override
	public void copy(DenseMatrix64F src, DenseMatrix64F dst) {
		dst.set(src);
	}

	@Override
	public DenseMatrix64F createInstance() {
		return new DenseMatrix64F(numRows,numCols);
	}

	@Override
	public Class<DenseMatrix64F> getType() {
		return DenseMatrix64F.class;
	}
}
