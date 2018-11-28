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

import boofcv.abst.geo.bundle.BundleAdjustmentSchur_DSCC;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;

/**
 * <p>
 * Computes observations errors/residuals for projective bundle adjustment as implemented using
 * {@link org.ddogleg.optimization.UnconstrainedLeastSquares}. Parameterization is done using
 * the format in {@link CodecSceneStructureProjective}.
 * </p>
 *
 * <p>
 * cost(P) = (1/(m*n))*&sum;<sub>i</sub> &sum;<sub>j</sub> ||x<sub>j</sub> - (1/z)*P<sub>i</sub>*X<sub>j</sub>||<sup>2</sup>
 * </p>
 *
 * @see SceneStructureProjective
 * @see SceneObservations
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentProjectiveResidualFunction
	implements BundleAdjustmentSchur_DSCC.FunctionResiduals<SceneStructureProjective>
{
	private SceneStructureProjective structure;
	private SceneObservations observations;

	// number of parameters being optimised
	private int numParameters;
	// number of observations.  2 for each point in each view
	private int numObservations;


	// Storage for rendered output
	private Point2D_F64 predictedPixel = new Point2D_F64();
	private PointIndex2D_F64 observedPixel = new PointIndex2D_F64();

	// Used to write the "unknown" paramters into the scene
	private CodecSceneStructureProjective codec = new CodecSceneStructureProjective();

	private Point3D_F64 p3 = new Point3D_F64();
	private Point4D_F64 p4 = new Point4D_F64();

	/**
	 * Specifies the scenes structure and observed feature locations
	 */
	@Override
	public void configure(SceneStructureProjective structure ,
						  SceneObservations observations )
	{
		this.structure = structure;
		this.observations = observations;

		numObservations = observations.getObservationCount();
		numParameters = structure.getParameterCount();
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

		if( structure.homogenous )
			project4(output);
		else
			project3(output);
	}

	public void project3(double[] output) {
		int observationIndex = 0;
		for( int viewIndex = 0; viewIndex < structure.views.length; viewIndex++ ) {
			SceneStructureProjective.View view = structure.views[viewIndex];
			SceneObservations.View obsView = observations.views[viewIndex];
			for (int i = 0; i < obsView.size(); i++) {
				obsView.get(i,observedPixel);
				SceneStructureMetric.Point worldPt = structure.points[observedPixel.index];
				worldPt.get(p3);
				PerspectiveOps.renderPixel(view.worldToView, p3, predictedPixel);
				int outputIndex = observationIndex*2;
				output[outputIndex  ] = predictedPixel.x - observedPixel.x;
				output[outputIndex+1] = predictedPixel.y - observedPixel.y;
				observationIndex++;
			}
		}
	}

	public void project4(double[] output) {
		int observationIndex = 0;
		for( int viewIndex = 0; viewIndex < structure.views.length; viewIndex++ ) {
			SceneStructureProjective.View view = structure.views[viewIndex];
			SceneObservations.View obsView = observations.views[viewIndex];
			for (int i = 0; i < obsView.size(); i++) {
				obsView.get(i,observedPixel);
				SceneStructureMetric.Point worldPt = structure.points[observedPixel.index];
				worldPt.get(p4);
				PerspectiveOps.renderPixel(view.worldToView, p4, predictedPixel);
				int outputIndex = observationIndex*2;
				output[outputIndex  ] = predictedPixel.x - observedPixel.x;
				output[outputIndex+1] = predictedPixel.y - observedPixel.y;
				observationIndex++;
			}
		}
	}
}
