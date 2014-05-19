/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo;

import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.fitting.modelset.ModelCodec;
import org.ddogleg.optimization.functions.FunctionNtoS;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * <p>
 * Computes the Sampson distance for a set of observations given a fundamental matrix.  For use
 * in non-linear optimization algorithms.
 * </p>
 *
 * <p>
 * Page 287 in: R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </p>
 *
 * @author Peter Abeles
 */
public class FunctionSampsonFundamental implements FunctionNtoS {
	// converts parameters to and from the fundamental matrix
	ModelCodec<DenseMatrix64F> param;
	// list of observations
	List<AssociatedPair> obs;

	// pre-declare temporary storage
	Point3D_F64 temp = new Point3D_F64();
	DenseMatrix64F F = new DenseMatrix64F(3,3);

	public FunctionSampsonFundamental(ModelCodec<DenseMatrix64F> param,
									  List<AssociatedPair> obs) {
		set(param,obs);
	}

	public FunctionSampsonFundamental() {
	}

	public void set( ModelCodec<DenseMatrix64F> param , List<AssociatedPair> obs ) {
		this.param = param;
		this.obs = obs;
	}

	@Override
	public int getNumOfInputsN() {
		return param.getParamLength();
	}

	@Override
	public double process(double[] input) {
		param.decode(input, F);
		
		double sum = 0;
		
		for( int i = 0; i < obs.size(); i++ ) {
			AssociatedPair p = obs.get(i);

			double top = GeometryMath_F64.innerProd(p.p2,F,p.p1);
			top *= top;
			
			double bottom = 0;
			
			GeometryMath_F64.mult(F,p.p1,temp);
			bottom += temp.x * temp.x + temp.y*temp.y;

			GeometryMath_F64.multTran(F, p.p2, temp);
			bottom += temp.x * temp.x + temp.y*temp.y;

			if( bottom <= 1e-12 )
				continue;

			sum += top/bottom;
		}
		
		return sum;
	}
}
