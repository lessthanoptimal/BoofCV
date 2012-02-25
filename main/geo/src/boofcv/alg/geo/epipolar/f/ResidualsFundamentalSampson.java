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

package boofcv.alg.geo.epipolar.f;

import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.epipolar.EpipolarResiduals;
import boofcv.numerics.fitting.modelset.ModelCodec;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * <p>
 * Computes the Sampson distance residual for a set of observations given a fundamental matrix.  For use
 * in least-squares non-linear optimization algorithms.
 * </p>
 *
 * <p>
 * Page 287 in: R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </p>
 *
 * @author Peter Abeles
 */
public class ResidualsFundamentalSampson implements EpipolarResiduals {

	// converts parameters to and from the fundamental matrix
	ModelCodec<DenseMatrix64F> param;
	// list of observations
	List<AssociatedPair> obs;

	// pre-declare temporary storage
	Point3D_F64 temp = new Point3D_F64();
	DenseMatrix64F F = new DenseMatrix64F(3,3);

	public ResidualsFundamentalSampson(ModelCodec<DenseMatrix64F> param,
									   List<AssociatedPair> obs) {
		this(param);
		this.obs = obs;
	}

	public ResidualsFundamentalSampson(ModelCodec<DenseMatrix64F> param) {
		this.param = param;
	}

	@Override
	public void setObservations( List<AssociatedPair> obs ) {
		this.obs = obs;
	}

	@Override
	public int getN() {
		return param.getParamLength();
	}

	@Override
	public int getM() {
		return obs.size();
	}

	@Override
	public void process(double[] input, double[] output) {
		param.decode(input, F);

		for( int i = 0; i < obs.size(); i++ ) {
			AssociatedPair p = obs.get(i);

			double bottom = 0;

			GeometryMath_F64.mult(F, p.keyLoc, temp);
			bottom += temp.x*temp.x + temp.y*temp.y;

			GeometryMath_F64.multTran(F, p.currLoc, temp);
			bottom += temp.x*temp.x + temp.y*temp.y;

			if( bottom <= UtilEjml.EPS) {
				output[i] = 0;
			} else {
				GeometryMath_F64.multTran(F,p.currLoc,temp);

				output[i] = (temp.x*p.keyLoc.x + temp.y*p.keyLoc.y + temp.z)/bottom;
			}
		}
	}
}
