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

package boofcv.alg.geo.d3.epipolar.f;

import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.d3.epipolar.EpipolarResiduals;
import boofcv.numerics.fitting.modelset.ModelCodec;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * <p>
 * Computes the residual just using the fundamental matrix constraint
 *
 * @author Peter Abeles
 */
public class ResidualsFundamentalSimple implements EpipolarResiduals {

	// converts parameters to and from the fundamental matrix
	ModelCodec<DenseMatrix64F> param;
	// list of observations
	List<AssociatedPair> obs;

	// pre-declare temporary storage
	Point3D_F64 temp = new Point3D_F64();
	DenseMatrix64F F = new DenseMatrix64F(3,3);

	public ResidualsFundamentalSimple(ModelCodec<DenseMatrix64F> param,
									  List<AssociatedPair> obs) {
		this(param);
		this.obs = obs;
	}

	public ResidualsFundamentalSimple(ModelCodec<DenseMatrix64F> param) {
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

			GeometryMath_F64.multTran(F,p.currLoc,temp);

			output[i] = temp.x*p.keyLoc.x + temp.y*p.keyLoc.y + temp.z;
		}
	}
}
