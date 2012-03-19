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

package boofcv.abst.geo.optimization;

import boofcv.alg.geo.ModelObservationResidualN;
import boofcv.alg.geo.PointPositionPair;
import boofcv.numerics.fitting.modelset.ModelCodec;
import boofcv.numerics.optimization.functions.FunctionNtoM;
import georegression.struct.se.Se3_F64;

import java.util.List;

/**
 * Computes residual errors for a set of observations from a Se3 motion.
 *
 * @author Peter Abeles
 */
public class ResidualsPoseMatrix implements FunctionNtoM {
	// converts parameters to and from the motion
	protected ModelCodec<Se3_F64> param;
	// list of observations
	protected List<PointPositionPair> obs;
	// error function
	protected ModelObservationResidualN<Se3_F64,PointPositionPair> residual;

	// pre-declare temporary storage
	protected Se3_F64 pose = new Se3_F64();

	/**
	 * Configures algorithm
	 *
	 * @param param Converts parameters into epipolar matrix
	 * @param residual Function for computing the residuals
	 */
	public ResidualsPoseMatrix(ModelCodec<Se3_F64> param,
							   ModelObservationResidualN<Se3_F64,PointPositionPair> residual) {
		this.param = param;
		this.residual = residual;
	}

	public void setObservations( List<PointPositionPair> obs ) {
		this.obs = obs;
	}

	@Override
	public int getN() {
		return param.getParamLength();
	}

	@Override
	public int getM() {
		return obs.size()*residual.getN();
	}

	@Override
	public void process(double[] input, double[] output) {
		param.decode(input, pose);

		residual.setModel(pose);
		int index = 0;
		for( int i = 0; i < obs.size(); i++ ) {
			index = residual.computeResiduals(obs.get(i),output,index);
		}
	}
}
