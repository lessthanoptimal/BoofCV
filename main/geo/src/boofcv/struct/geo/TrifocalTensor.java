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
 * The trifocal tensor describes the projective relationship between three different camera views and is
 * analogous to the Fundamental matrix for two views. The trifocal tensor is composed of three matrices
 * which are 3x3.
 *
 * @author Peter Abeles
 */
public class TrifocalTensor {
	public DenseMatrix64F T1 = new DenseMatrix64F(3,3);
	public DenseMatrix64F T2 = new DenseMatrix64F(3,3);
	public DenseMatrix64F T3 = new DenseMatrix64F(3,3);

	public DenseMatrix64F getT( int index ) {
		switch( index ) {
			case 0:
				return T1;

			case 1:
				return T2;

			case 2:
				return T3;
		}

		throw new IllegalArgumentException("Invalid index");
	}

	/**
	 * Returns a new copy of the TrifocalTensor
	 *
	 * @return Copy of the trifocal tensor
	 */
	public TrifocalTensor copy() {
		TrifocalTensor ret = new TrifocalTensor();
		ret.T1.set(T1);
		ret.T2.set(T2);
		ret.T3.set(T3);
		return ret;
	}
}
