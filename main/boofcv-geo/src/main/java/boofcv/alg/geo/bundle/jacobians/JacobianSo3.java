/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle.jacobians;

import org.ejml.data.DMatrixRMaj;

/**
 * Generalized computation for jacobian of 3D rotation matrix
 *
 * @author Peter Abeles
 */
public interface JacobianSo3 {

	/**
	 * Converts the 3x3 rotation matrix into encoded parameters
	 *
	 * @param R 3x3 (Input) rotation matrix
	 * @param parameters (Output) storage for encoded rotation matrix
	 * @param offset index in parameters array
	 */
	void getParameters( DMatrixRMaj R, double[] parameters, int offset );

	void setParameters( double[] parameters, int offset );

	int getParameterLength();

	DMatrixRMaj getRotationMatrix();

	DMatrixRMaj getPartial( int param );
}
