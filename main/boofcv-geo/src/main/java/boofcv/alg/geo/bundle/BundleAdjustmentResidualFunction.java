/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.bundle.BundleAdjustmentObservations;
import boofcv.abst.geo.bundle.BundleAdjustmentSceneStructure;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.optimization.functions.FunctionNtoM;

/**
 * <p>
 * Computes observations errors/residuals for bundle adjustment as implemented using
 * {@link org.ddogleg.optimization.UnconstrainedLeastSquares}. Parameterization is done using
 * the format in {@link CodecBundleAdjustmentSceneStructure}.
 * </p>
 *
 * <p>
 * cost(P) = (1/(m*n))*&sum;<sub>i</sub> &sum;<sub>j</sub> ||x<sub>j</sub> - (1/z)*[R<sub>i</sub>|T<sub>i</sub>]*X<sub>j</sub>||<sup>2</sup>
 * </p>
 *
 * @see BundleAdjustmentSceneStructure
 * @see BundleAdjustmentObservations
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentResidualFunction
	implements FunctionNtoM
{
	private BundleAdjustmentSceneStructure structure;
	private BundleAdjustmentObservations observations;

	// feature location in world coordinates
	private Point3D_F64 worldPt = new Point3D_F64();

	// number of parameters being optimised
	private int numParameters;
	// number of observations.  2 for each point in each view
	private int numObservations;

	// local variable which stores the predicted location of the feature in the camera frame
	private Point3D_F64 cameraPt = new Point3D_F64();

	// Storage for rendered output
	private Point2D_F64 predictedPixel = new Point2D_F64();
	private PointIndex2D_F64 observedPixel = new PointIndex2D_F64();

	// Used to write the "unknown" paramters into the scene
	CodecBundleAdjustmentSceneStructure codec = new CodecBundleAdjustmentSceneStructure();

	/**
	 * Specifies the scenes structure and observed feature locations
	 */
	public void configure(BundleAdjustmentSceneStructure structure ,
						  BundleAdjustmentObservations observations )
	{
		this.structure = structure;
		this.observations = observations;

		int numViewsUnknown = structure.getUnknownViewCount();
		int numCameraParameters = structure.getUnknownCameraParameterCount();
		numObservations = observations.getObservationCount();

		numParameters = numViewsUnknown*6 + structure.points.length*3 + numCameraParameters;
	}

	@Override
	public int getNumOfInputsN() {
		return numParameters;
	}

	@Override
	public int getNumOfOutputsM() {
		return numObservations*2;
	}

	@Override
	public void process(double[] input, double[] output) {

		// write the current parameters into the scene's structure
		codec.decode(input,structure);
		int observationIndex = 0;
		for( int viewIndex = 0; viewIndex < structure.views.length; viewIndex++ ) {
			BundleAdjustmentSceneStructure.View view = structure.views[viewIndex];
			BundleAdjustmentSceneStructure.Camera camera = structure.cameras[view.camera];
			BundleAdjustmentObservations.View obsView = observations.views[viewIndex];

			for (int i = 0; i < obsView.size(); i++) {
				obsView.get(i,observedPixel);
				BundleAdjustmentSceneStructure.Point worldPt = structure.points[observedPixel.index];

				SePointOps_F64.transform(view.worldToView,worldPt,cameraPt);

				camera.model.project(cameraPt.x,cameraPt.y,cameraPt.z, predictedPixel);

				int outputIndex = observationIndex*2;
				output[outputIndex  ] = predictedPixel.x - observedPixel.x;
				output[outputIndex+1] = predictedPixel.y - observedPixel.y;
				observationIndex++;
			}
		}
	}

}
