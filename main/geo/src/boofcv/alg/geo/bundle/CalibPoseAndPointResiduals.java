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

package boofcv.alg.geo.bundle;

import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.ModelCodec;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * <p>
 * Computes residuals for bundle adjustment given known camera calibration.
 * Cost function being optimized is specified below.
 * </p>
 *
 * <p>
 * cost(P) = (1/(m*n))*&sum;<sub>i</sub> &sum;<sub>j</sub> ||x<sub>j</sub> - (1/z)*[R<sub>i</sub>|T<sub>i</sub>]*X<sub>j</sub>||<sup>2</sup>
 * </p>
 *
 * @author Peter Abeles
 */
public class CalibPoseAndPointResiduals
	implements FunctionNtoM
{
	// decodes parameterized model
	ModelCodec<CalibratedPoseAndPoint> codec;

	// expanded model for fast computations
	CalibratedPoseAndPoint model;
	// observed location of features in each view
	List<ViewPointObservations> observations;

	// number of observations.  2 for each point in each view
	int numObservations;

	// local variable which stores the predicted location of the feature in the camera frame
	Point3D_F64 cameraPt = new Point3D_F64();

	/**
	 * Configures the residual function.
	 *
	 * @param model Model being processed
	 * @param codec Decodes parameterized version of parameters
	 * @param obs Contains camera observations of each point.
	 */
	public void configure( ModelCodec<CalibratedPoseAndPoint> codec ,
						   CalibratedPoseAndPoint model,
						   List<ViewPointObservations> obs ) {
		this.model = model;
		this.codec = codec;
		this.observations = obs;

		numObservations = 0;
		for( int view = 0; view < model.getNumViews(); view++ ) {
			numObservations += obs.get(view).getPoints().size()*2;
		}
	}

	@Override
	public int getNumOfInputsN() {
		return codec.getParamLength();
	}

	@Override
	public int getNumOfOutputsM() {
		return numObservations;
	}

	@Override
	public void process(double[] input, double[] output) {

		codec.decode(input,model);

		process(model,output);
	}

	public void process(CalibratedPoseAndPoint model , double[] output) {
		int outputIndex = 0;

		for( int view = 0; view < model.getNumViews(); view++) {
			Se3_F64 worldToCamera = model.getWorldToCamera(view);

			FastQueue<PointIndexObservation> observedPts = observations.get(view).getPoints();

			for( int i = 0; i < observedPts.size; i++ ) {
				PointIndexObservation o = observedPts.data[i];

				Point3D_F64 worldPt = model.getPoint(o.pointIndex);

				SePointOps_F64.transform(worldToCamera, worldPt, cameraPt);

				output[outputIndex++] = cameraPt.x/cameraPt.z - o.obs.x;
				output[outputIndex++] = cameraPt.y/cameraPt.z - o.obs.y;
			}
		}
	}
}
