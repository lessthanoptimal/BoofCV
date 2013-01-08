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

package boofcv.alg.geo.pose;

import boofcv.abst.geo.optimization.ResidualsCodecToMatrix;
import boofcv.struct.sfm.Stereo2D3D;
import boofcv.struct.sfm.StereoPose;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.ModelCodec;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;

import java.util.List;

/**
 * Minimizes the projection residual error in a calibrated camera for a pose estimate.
 * Rotation is encoded using rodrigues coordinates.
 *
 * @author Peter Abeles
 */
public class PnPStereoRefineRodrigues implements RefinePnPStereo {

	ModelCodec<StereoPose> paramModel = new Se3ToStereoPoseCodec(new PnPRodriguesCodec());
	ResidualsCodecToMatrix<StereoPose,Stereo2D3D> func;
	PnPStereoJacobianRodrigues jacobian = new PnPStereoJacobianRodrigues();

	StereoPose stereoPose = new StereoPose();

	double param[];
	UnconstrainedLeastSquares minimizer;
	int maxIterations;
	double convergenceTol;

	public PnPStereoRefineRodrigues(double convergenceTol, int maxIterations)
	{
		this.maxIterations = maxIterations;
		this.convergenceTol = convergenceTol;
		this.minimizer = FactoryOptimization.leastSquareLevenberg(1e-3);

		func = new ResidualsCodecToMatrix<StereoPose,Stereo2D3D>(paramModel,new PnPStereoResidualReprojection(),stereoPose);

		param = new double[paramModel.getParamLength()];
	}

	public void setLeftToRight( Se3_F64 leftToRight ) {
		stereoPose.cam0ToCam1 = leftToRight;
		jacobian.setLeftToRight(leftToRight);
	}

	@Override
	public boolean process(Se3_F64 worldToLeft, List<Stereo2D3D> obs, Se3_F64 refinedWorldToLeft ) {

		stereoPose.worldToCam0 = worldToLeft;
		paramModel.encode(stereoPose, param);

		func.setObservations(obs);
		jacobian.setObservations(obs);

		minimizer.setFunction(func,jacobian);

		minimizer.initialize(param,0,convergenceTol*obs.size());

		System.out.println("  error before "+minimizer.getFunctionValue());
		for( int i = 0; i < maxIterations; i++ ) {
			if( minimizer.iterate() )
				break;
		}
		System.out.println("  error after  "+minimizer.getFunctionValue());

		stereoPose.worldToCam0 = refinedWorldToLeft;
		paramModel.decode(minimizer.getParameters(), stereoPose);

		return true;
	}
}
