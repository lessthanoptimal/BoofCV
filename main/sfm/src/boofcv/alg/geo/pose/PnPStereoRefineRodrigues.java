/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
 * Minimizes the reprojection residual error for a pose estimate (left camera) in a calibrated stereo camera.
 * Rotation is encoded using rodrigues coordinates.  Transform between left and right camera
 * is assumed to be known and must be specified by the user.  Observations are in normalized image coordinates.
 *
 * @author Peter Abeles
 */
public class PnPStereoRefineRodrigues implements RefinePnPStereo {

	// converts to and from rodrigues coordinates
	private ModelCodec<Se3_F64> motionCodec = new PnPRodriguesCodec();
	// computes residual and Jacobian for optimization
	private ResidualsCodecToMatrix<StereoPose,Stereo2D3D> func;
	private PnPStereoJacobianRodrigues jacobian = new PnPStereoJacobianRodrigues();

	// parameters that specify the stereo camera and location of left camera
	private StereoPose stereoPose = new StereoPose();

	// encoded model that is optimized
	private double param[];
	// optimizer and settings
	protected UnconstrainedLeastSquares minimizer;
	private int maxIterations;
	private double convergenceTol;

	public PnPStereoRefineRodrigues(double convergenceTol, int maxIterations)
	{
		this.maxIterations = maxIterations;
		this.convergenceTol = convergenceTol;
		this.minimizer = FactoryOptimization.leastSquareLevenberg(1e-3);

		// decodes StereoPose
		ModelCodec<StereoPose> paramModel = new Se3ToStereoPoseCodec(motionCodec);

		// since a reference is saved, stereoPose.worldToCam0 will be modified by the optimization
		// algorithm internally
		func = new ResidualsCodecToMatrix<>(
				paramModel, new PnPStereoResidualReprojection(), stereoPose);

		param = new double[paramModel.getParamLength()];
	}

	public void setLeftToRight( Se3_F64 leftToRight ) {
		// cam0toCam1 is not modified during optimization since it is assumed to be known/constant
		stereoPose.cam0ToCam1 = leftToRight;
		jacobian.setLeftToRight(leftToRight);
	}

	@Override
	public boolean fitModel(List<Stereo2D3D> obs, Se3_F64 worldToLeft, Se3_F64 refinedWorldToLeft) {

		// put into a parameterized format
		motionCodec.encode(worldToLeft, param);

		// setup the optimization
		func.setObservations(obs);
		jacobian.setObservations(obs);

		minimizer.setFunction(func,jacobian);
		minimizer.initialize(param,0,convergenceTol*obs.size());

		// iterate until it converges
		for( int i = 0; i < maxIterations; i++ ) {
			if( minimizer.iterate() )
				break;
		}

		// decode the solution
		motionCodec.decode(minimizer.getParameters(),refinedWorldToLeft);

		return true;
	}
}
