/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.bundle.BundleAdjustmentSchur;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.DogArray;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Computes observations errors/residuals for metric bundle adjustment as implemented using
 * {@link org.ddogleg.optimization.UnconstrainedLeastSquares}. Parameterization is done using
 * the format in {@link CodecSceneStructureMetric}.
 * </p>
 *
 * <p>
 * cost(P) = (1/(m*n))*&sum;<sub>i</sub> &sum;<sub>j</sub> ||x<sub>j</sub> - (1/z)*[R<sub>i</sub>|T<sub>i</sub>]*X<sub>j</sub>||<sup>2</sup>
 * </p>
 *
 * @author Peter Abeles
 * @see SceneStructureMetric
 * @see SceneObservations
 */
@SuppressWarnings({"NullAway.Init"})
public class BundleAdjustmentMetricResidualFunction
		implements BundleAdjustmentSchur.FunctionResiduals<SceneStructureMetric> {
	private SceneStructureMetric structure;
	private SceneObservations observations;

	// feature location in world coordinates
	private final Point3D_F64 worldPt = new Point3D_F64();

	// number of parameters being optimised
	private int numParameters;
	// number of observations. 2 for each point in each view
	private int numObservations;

	// local variable which stores the predicted location of the feature in the camera frame
	private final Point3D_F64 cameraPt = new Point3D_F64();

	// Storage for rendered output
	private final Point2D_F64 predictedPixel = new Point2D_F64();
	private final PointIndex2D_F64 observedPixel = new PointIndex2D_F64();

	// Used to write the "unknown" parameters into the scene
	private final CodecSceneStructureMetric codec = new CodecSceneStructureMetric();

	// Recycled world to view transforms
	private final DogArray<Se3_F64> storageSe3 = new DogArray<>(Se3_F64::new);
	// Look up workspace by view ID when relative view
	private final Map<SceneStructureMetric.View, Se3_F64> mapWorldToView = new HashMap<>();

	// Storage for 3D points in Cartesian and homogenous coordinates
	private final Point3D_F64 p3 = new Point3D_F64();
	private final Point4D_F64 p4 = new Point4D_F64();

	/**
	 * Specifies the scenes structure and observed feature locations
	 */
	@Override
	public void configure( SceneStructureMetric structure,
						   SceneObservations observations ) {
		this.structure = structure;
		this.observations = observations;

		numObservations = observations.getObservationCount();
		numParameters = structure.getParameterCount();
		structure.assignIDsToRigidPoints();

		// declare storage and create a look up table for world to view for all relative views
		mapWorldToView.clear();
		storageSe3.reset();
		for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
			SceneStructureMetric.View v = structure.views.get(viewIdx);
			if (v.parent == null)
				continue;
			Se3_F64 world_to_view = storageSe3.grow();
			mapWorldToView.put(v, world_to_view);
		}
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
	public void process( double[] input, double[] output ) {

		// write the current parameters into the scene's structure
		codec.decode(input, structure);

		// Project the general scene now
		if (structure.isHomogenous())
			project4(output);
		else
			project3(output);
	}

	/**
	 * projection from 3D coordinates
	 */
	private void project3( double[] output ) {
		int observationIndex = 0;
		for (int viewIndex = 0; viewIndex < structure.views.size; viewIndex++) {
			SceneStructureMetric.View view = structure.views.get(viewIndex);
			SceneStructureCommon.Camera camera = structure.cameras.get(view.camera);

			Se3_F64 world_to_view = lookupWorldToView(view);

			//=========== Project General Points in this View
			{
				SceneObservations.View obsView = observations.views.get(viewIndex);
				for (int i = 0; i < obsView.size(); i++) {
					obsView.getPixel(i, observedPixel);
					SceneStructureCommon.Point worldPt = structure.points.data[observedPixel.index];
					worldPt.get(p3);

					SePointOps_F64.transform(world_to_view, p3, cameraPt);

					camera.model.project(cameraPt.x, cameraPt.y, cameraPt.z, predictedPixel);

					int outputIndex = observationIndex*2;
					output[outputIndex] = predictedPixel.x - observedPixel.p.x;
					output[outputIndex + 1] = predictedPixel.y - observedPixel.p.y;
					observationIndex++;
				}
			}

			//=========== Project Rigid Object Points in this View
			if (observations.hasRigid()) {
				SceneObservations.View obsView = observations.viewsRigid.get(viewIndex);
				for (int i = 0; i < obsView.size(); i++) {
					obsView.getPixel(i, observedPixel);

					// Use lookup table to figure out which rigid object it belongs to
					int rigidIndex = structure.lookupRigid[observedPixel.index];
					SceneStructureMetric.Rigid rigid = structure.rigids.get(rigidIndex);
					// Compute the point's index on the rigid object
					int pointIndex = observedPixel.index - rigid.indexFirst;

					// Load the 3D location of point on the rigid body
					SceneStructureCommon.Point objectPt = rigid.points[pointIndex];
					objectPt.get(p3);

					// Transform to world frame and from world to camera
					SePointOps_F64.transform(rigid.object_to_world, p3, worldPt);
					SePointOps_F64.transform(world_to_view, worldPt, cameraPt);

					// Project and compute residual
					camera.model.project(cameraPt.x, cameraPt.y, cameraPt.z, predictedPixel);

					int outputIndex = observationIndex*2;
					output[outputIndex] = predictedPixel.x - observedPixel.p.x;
					output[outputIndex + 1] = predictedPixel.y - observedPixel.p.y;
					observationIndex++;
				}
			}
		}
	}

	/**
	 * projection from homogenous coordinates
	 */
	private void project4( double[] output ) {
		int observationIndex = 0;
		for (int viewIndex = 0; viewIndex < structure.views.size; viewIndex++) {
			SceneStructureMetric.View view = structure.views.get(viewIndex);
			SceneStructureCommon.Camera camera = structure.cameras.get(view.camera);

			Se3_F64 world_to_view = lookupWorldToView(view);

			//=========== Project General Points in this View
			{
				SceneObservations.View obsView = observations.views.get(viewIndex);

				for (int i = 0; i < obsView.size(); i++) {
					obsView.getPixel(i, observedPixel);
					SceneStructureCommon.Point worldPt = structure.points.data[observedPixel.index];
					worldPt.get(p4);

					// TODO Explain why this is correct. The last row is omitted when converted to 3D
					SePointOps_F64.transformV(world_to_view, p4, cameraPt);

					camera.model.project(cameraPt.x, cameraPt.y, cameraPt.z, predictedPixel);

					int outputIndex = observationIndex*2;
					output[outputIndex] = predictedPixel.x - observedPixel.p.x;
					output[outputIndex + 1] = predictedPixel.y - observedPixel.p.y;
					observationIndex++;
				}
			}

			//=========== Project Rigid Object Points in this View
			if (observations.hasRigid()) {
				SceneObservations.View obsView = observations.viewsRigid.get(viewIndex);

				for (int i = 0; i < obsView.size(); i++) {
					obsView.getPixel(i, observedPixel);

					// Use lookup table to figure out which rigid object it belongs to
					int rigidIndex = structure.lookupRigid[observedPixel.index];
					SceneStructureMetric.Rigid rigid = structure.rigids.get(rigidIndex);
					// Compute the point's index on the rigid object
					int pointIndex = observedPixel.index - rigid.indexFirst;

					// Load the 3D location of point on the rigid body
					SceneStructureCommon.Point objectPt = rigid.points[pointIndex];
					objectPt.get(p4);

					// Transform to world frame and from world to camera
					SePointOps_F64.transformV(rigid.object_to_world, p4, worldPt);
					SePointOps_F64.transform(world_to_view, worldPt, cameraPt);

					camera.model.project(cameraPt.x, cameraPt.y, cameraPt.z, predictedPixel);

					int outputIndex = observationIndex*2;
					output[outputIndex] = predictedPixel.x - observedPixel.p.x;
					output[outputIndex + 1] = predictedPixel.y - observedPixel.p.y;
					observationIndex++;
				}
			}
		}
	}

	/**
	 * Returns a transform from the world_to_view. If relative then the parent's world to view is look up and used
	 * to compute this view's transform and the results are saved.
	 */
	protected Se3_F64 lookupWorldToView( SceneStructureMetric.View v ) {
		Se3_F64 parent_to_view = structure.getParentToView(v);
		if (v.parent == null)
			return parent_to_view;

		Se3_F64 world_to_view = Objects.requireNonNull(mapWorldToView.get(v));
		SceneStructureMetric.View parentView = v.parent;

		// See if the parent is relative to the global frame
		if (parentView.parent == null) {
			Se3_F64 world_to_parent = Objects.requireNonNull(structure.getParentToView(v.parent));
			world_to_parent.concat(parent_to_view, world_to_view);
		} else {
			// Since the parent must have a lower index it's transform is already known
			Se3_F64 world_to_parent = Objects.requireNonNull(mapWorldToView.get(v.parent));
			world_to_parent.concat(parent_to_view, world_to_view);
		}
		return world_to_view;
	}
}
