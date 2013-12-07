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

import boofcv.alg.geo.ModelObservationResidualN;
import org.ddogleg.fitting.modelset.ModelCodec;
import org.ddogleg.optimization.functions.FunctionNtoM;

import java.util.List;

/**
 * Computes residual errors for a set of observations from a model.
 *
 * @author Peter Abeles
 */
public class ResidualsCodecToMatrix<Model,Observation> implements FunctionNtoM {
	// converts parameters to and from the motion
	protected ModelCodec<Model> param;
	// list of observations
	protected List<Observation> obs;
	// error function
	protected ModelObservationResidualN<Model,Observation> residual;

	// pre-declare temporary storage
	protected Model pose;

	/**
	 * Configures algorithm
	 *
	 * @param param Converts parameters into epipolar matrix
	 * @param residual Function for computing the residuals
	 * @param storage Storage for converted model.  Will be modified.
	 */
	public ResidualsCodecToMatrix(ModelCodec<Model> param,
								  ModelObservationResidualN<Model, Observation> residual,
								  Model storage) {
		this.param = param;
		this.residual = residual;
		this.pose = storage;
	}

	public void setObservations( List<Observation> obs ) {
		this.obs = obs;
	}

	@Override
	public int getNumOfInputsN() {
		return param.getParamLength();
	}

	@Override
	public int getNumOfOutputsM() {
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
