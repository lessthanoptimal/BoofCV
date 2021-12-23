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
import boofcv.alg.geo.bundle.jacobians.JacobianSo3;
import boofcv.alg.geo.bundle.jacobians.JacobianSo3Rodrigues;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.ReshapeMatrix;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Computes the Jacobian for bundle adjustment with a Schur implementation. This is the base class
 * for specific types of matrices
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class BundleAdjustmentMetricSchurJacobian<M extends DMatrix>
		implements BundleAdjustmentSchur.Jacobian<SceneStructureMetric, M> {
	private SceneStructureMetric structure;
	private SceneObservations observations;

	// number of views with parameters that are going to be adjusted
	private int numMotionsUnknown;
	private int numRigidUnknown;

	// total number of parameters being optimized
	private int numParameters;

	// length of a 3D point. 3 = regular, 4 = homogenous
	private int lengthPoint;

	/** Specifies method to parameterize rotations, i.e. Rodrigues to SO3 */
	public @Getter @Setter JacobianSo3 jacSO3 = new JacobianSo3Rodrigues();

	// Recycled data structures for use in the maps below
	private final DogArray<Se3_F64> storageSe3 = new DogArray<>(Se3_F64::new);
	private final DogArray<DMatrixRMaj[]> storageSO3Jac = new DogArray<>(this::declareRotJacStorage);
	// Look up workspace by view ID when relative view. Only filled in when a relative view is encountered
	private final Map<SceneStructureMetric.View, Se3_F64> mapWorldToView = new HashMap<>();
	private final TIntObjectMap<DMatrixRMaj[]> mapSO3Jac = new TIntObjectHashMap<>();
	// If only one view will use a particular motion then we don't want to store the Jacobian for future use

	// Workspace for world to view transform
	private final Se3_F64 world_to_view = new Se3_F64();

	// Jacobians for rigid objects
	private JacobianSo3[] jacRigidS03;

	// feature location in world coordinates
	private final Point3D_F64 worldPt3 = new Point3D_F64();
	private final Point4D_F64 worldPt4 = new Point4D_F64();
	// feature location in rigid body coordinates
	private final Point3D_F64 rigidPt3 = new Point3D_F64();
	private final Point4D_F64 rigidPt4 = new Point4D_F64();
	// feature location in camera coordinates
	private final Point3D_F64 cameraPt = new Point3D_F64();

	// Number of parameters to describe SE3 (rotation + translation)
	private int lengthSE3;
	// first index for rigid body parameters
	private int indexFirstRigid;
	// index in parameters of the first point
	private int indexFirstMotion;
	private int indexLastMotion;
	// rigid to parameter index. Left side
	private int[] rigidParameterIndexes;
	// view to parameter index. Right side
	private int[] motionParameterIndexes;
	// first index in input/parameters vector for each camera. Right side
	private int[] cameraParameterIndexes;

	// Jacobian matrix index of x and y partial
	private int jacRowX, jacRowY;

	// Storage for gradients
	private final double[] pointGradX = new double[3];
	private final double[] pointGradY = new double[3];
	private double[] calibGradX = new double[0];
	private double[] calibGradY = new double[0];

	// work space for R2*R1
	DMatrixRMaj RR = new DMatrixRMaj(3, 3);

	// Storage for current view's partials of SO3
	private DMatrixRMaj[] arraySO3 = new DMatrixRMaj[0];
	private final DMatrixRMaj accumulatedR = new DMatrixRMaj(3, 3);
	private final Point4D_F64 worldX = new Point4D_F64();
	private final Point3D_F64 pt3 = new Point3D_F64();
	private final DMatrixRMaj tmp3x3 = new DMatrixRMaj(3, 3);

	@Override
	public void configure( SceneStructureMetric structure, SceneObservations observations ) {
		this.structure = structure;
		this.observations = observations;

		if (!structure.isHomogenous()) {
			lengthPoint = 3;
		} else {
			lengthPoint = 4;
		}

		// 3 for translation + orientation parameterization
		lengthSE3 = 3 + jacSO3.getParameterLength();

		//----- Precompute location of parameters for different structures
		numRigidUnknown = structure.getUnknownRigidCount();
		numMotionsUnknown = structure.getUnknownMotionCount();
		int numCameraParameters = structure.getUnknownCameraParameterCount();

		indexFirstRigid = structure.points.size*lengthPoint;
		indexFirstMotion = indexFirstRigid + numRigidUnknown*lengthSE3;
		indexLastMotion = indexFirstMotion + numMotionsUnknown*lengthSE3;
		numParameters = indexLastMotion + numCameraParameters;

		// precompute index of first parameter in each unknown rigid object. Left side
		jacRigidS03 = new JacobianSo3[structure.rigids.size];
		rigidParameterIndexes = new int[jacRigidS03.length];
		for (int i = 0, index = 0; i < jacRigidS03.length; i++) {
			rigidParameterIndexes[i] = index;
			jacRigidS03[i] = new JacobianSo3Rodrigues();
			if (!structure.rigids.get(i).known) {
				index += lengthSE3;
			}
		}

		// precompute index of first parameter for each unknown view. Right side
		motionParameterIndexes = new int[structure.motions.size];
		for (int i = 0, index = 0; i < structure.motions.size; i++) {
			motionParameterIndexes[i] = index;
			if (!structure.motions.data[i].known) {
				index += lengthSE3;
			}
		}

		// Create a lookup table for each camera. Camera ID to location in parameter vector
		cameraParameterIndexes = new int[structure.cameras.size];
		int largestCameraSize = 0;
		for (int i = 0, index = 0; i < structure.cameras.size; i++) {
			if (!structure.cameras.get(i).known) {
				cameraParameterIndexes[i] = index;
				int count = structure.cameras.data[i].model.getIntrinsicCount();
				largestCameraSize = Math.max(largestCameraSize, count);
				index += count;
			}
		}

		calibGradX = new double[largestCameraSize];
		calibGradY = new double[largestCameraSize];

		// Storage for relative views
		declareStorageWorldToView(structure);
		declareStoragePartialsSE3(structure);
	}

	/**
	 * Declare storage and create a look up table for world to view for all relative views
	 */
	private void declareStorageWorldToView( SceneStructureMetric structure ) {
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

	/**
	 * Pre-declare storage for all the SO3 Jacobians that will need to be retrieved as part of a relative view
	 * in the future. This is needed when relative views are in use. When there are no regular views then
	 * these sparse data structures will be empty.
	 */
	private void declareStoragePartialsSE3( SceneStructureMetric structure ) {
		int lengthParam = storageSO3Jac.grow().length;

		mapSO3Jac.clear();
		// see if the parameterization changed. If so discard all the old data
		if (jacSO3.getParameterLength() != lengthParam) {
			storageSO3Jac.data = new DMatrixRMaj[0][];
			storageSO3Jac.size = 0;
		} else {
			storageSO3Jac.reset();
		}
		arraySO3 = arraySO3.length != lengthParam ? new DMatrixRMaj[lengthParam] : arraySO3;
		for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
			SceneStructureMetric.View v = structure.views.get(viewIdx);
			if (v.parent == null)
				continue;

			// if this view is relative to another we don't need to store the Jacobian since we already have it
			// However, the parent it refers to needs to be stored. We do not need to traverse back any more due
			// since all potential ancestors have already been processed
			SceneStructureMetric.View vp = v.parent;
			SceneStructureMetric.Motion mp = structure.motions.get(vp.parent_to_view);
			if (mp.known || mapSO3Jac.containsKey(vp.parent_to_view))
				continue;

			mapSO3Jac.put(vp.parent_to_view, storageSO3Jac.grow());
		}
	}

	@Override
	public int getNumOfInputsN() {
		return numParameters;
	}

	@Override
	public int getNumOfOutputsM() {
		return observations.getObservationCount()*2;
	}

	private int computeGeneralPoints( DMatrix leftPoint, DMatrix rightView,
									  double[] input, int observationIndex, int viewIndex,
									  SceneStructureCommon.Camera camera,
									  int cameraParamStartIndex ) {
		SceneObservations.View obsView = observations.views.get(viewIndex);
		SceneStructureMetric.View strView = structure.views.get(viewIndex);

		for (int i = 0; i < obsView.size(); i++) {
			int featureIndex = obsView.point.get(i);
			int columnOfPointInJac = featureIndex*lengthPoint;

			if (structure.isHomogenous()) {
				worldPt4.x = input[columnOfPointInJac];
				worldPt4.y = input[columnOfPointInJac + 1];
				worldPt4.z = input[columnOfPointInJac + 2];
				worldPt4.w = input[columnOfPointInJac + 3];

				SePointOps_F64.transformV(world_to_view, worldPt4, cameraPt);
			} else {
				worldPt3.x = input[columnOfPointInJac];
				worldPt3.y = input[columnOfPointInJac + 1];
				worldPt3.z = input[columnOfPointInJac + 2];

				SePointOps_F64.transform(world_to_view, worldPt3, cameraPt);
			}

			jacRowX = observationIndex*2;
			jacRowY = jacRowX + 1;

			//============ Partial of camera parameters
			if (!camera.known) {
				int N = camera.model.getIntrinsicCount();
				camera.model.jacobian(cameraPt.x, cameraPt.y, cameraPt.z,
						pointGradX, pointGradY, true, calibGradX, calibGradY);

				int location = indexLastMotion - indexFirstMotion + cameraParamStartIndex;
				for (int j = 0; j < N; j++) {
					set(rightView, jacRowX, location + j, calibGradX[j]);
					set(rightView, jacRowY, location + j, calibGradY[j]);
				}
			} else {
				camera.model.jacobian(cameraPt.x, cameraPt.y, cameraPt.z, pointGradX, pointGradY,
						false, null, null);
			}
			//============ Partial of worldPt
			if (structure.isHomogenous()) {
				partialPointH(leftPoint, rightView, strView, columnOfPointInJac);
			} else {
				partialPoint3(leftPoint, rightView, strView, columnOfPointInJac);
			}

			observationIndex++;
		}
		return observationIndex;
	}

	/**
	 * Internal matrix type agnostic process function.
	 *
	 * @param input Input parameters describing the current state of the optimization
	 * @param leftPoint Storage for left Jacobian
	 * @param rightView Storage for right Jacobian
	 */
	public void internalProcess( double[] input, DMatrix leftPoint, DMatrix rightView ) {
		int numRows = getNumOfOutputsM();
		// number of parameters on left. All points
		int numPointParam = structure.points.size*lengthPoint + numRigidUnknown*lengthSE3;
		// Number of parameters on right. views + camera
		int numViewParam = numParameters - numPointParam; // view + camera

		((ReshapeMatrix)leftPoint).reshape(numRows, numPointParam);
		((ReshapeMatrix)rightView).reshape(numRows, numViewParam);
		leftPoint.zero();
		rightView.zero();

		// parse parameters for rigid bodies. the translation + rotation is the same for all views
		for (int rigidIndex = 0; rigidIndex < structure.rigids.size; rigidIndex++) {
			if (!structure.rigids.get(rigidIndex).known) {
				jacRigidS03[rigidIndex].setParameters(input, indexFirstRigid + rigidParameterIndexes[rigidIndex]);
			}
		}

		int observationIndex = 0;
		// first decode the transformation
		for (int viewIndex = 0; viewIndex < structure.views.size; viewIndex++) {
			SceneStructureMetric.View view = structure.views.data[viewIndex];
			SceneStructureCommon.Camera camera = structure.cameras.data[view.camera];
			SceneStructureMetric.Motion motion = structure.motions.data[view.parent_to_view];

			if (!motion.known) {
				int paramIndex = motionParameterIndexes[view.parent_to_view] + indexFirstMotion;
				jacSO3.setParameters(input, paramIndex);
				paramIndex += jacSO3.getParameterLength();

				motion.motion.T.x = input[paramIndex];
				motion.motion.T.y = input[paramIndex + 1];
				motion.motion.T.z = input[paramIndex + 2];

				motion.motion.getR().setTo(jacSO3.getRotationMatrix());

				// save the Jacobian if we need to
				DMatrixRMaj[] savedJac = mapSO3Jac.get(view.parent_to_view);
				if (savedJac != null) {
					for (int i = 0; i < savedJac.length; i++) {
						savedJac[i].setTo(jacSO3.getPartial(i));
					}
				}
			}

			lookupWorldToView(view, world_to_view);

			int cameraParamStartIndex = cameraParameterIndexes[view.camera];
			if (!camera.known) {
				camera.model.setIntrinsic(input, indexLastMotion + cameraParamStartIndex);
			}

			observationIndex = computeGeneralPoints(leftPoint, rightView, input, observationIndex, viewIndex, camera, cameraParamStartIndex);
			if (observations.hasRigid())
				observationIndex = computeRigidPoints(leftPoint, rightView, observationIndex, viewIndex, camera, cameraParamStartIndex);
		}
	}

	private int computeRigidPoints( DMatrix leftPoint, DMatrix rightView,
									int observationIndex, int viewIndex,
									SceneStructureCommon.Camera camera,
									int cameraParamStartIndex ) {
		SceneObservations.View obsView = observations.viewsRigid.get(viewIndex);
		SceneStructureMetric.View view = structure.views.data[viewIndex];

		for (int i = 0; i < obsView.size(); i++) {
			int featureIndex = obsView.point.get(i);
			int rigidIndex = structure.lookupRigid[featureIndex];
			SceneStructureMetric.Rigid rigid = structure.rigids.get(rigidIndex);
			int pointIndex = featureIndex - rigid.indexFirst; // index of point in rigid body

			if (structure.isHomogenous()) {
				rigid.getPoint(pointIndex, rigidPt4);
				SePointOps_F64.transformV(rigid.object_to_world, rigidPt4, worldPt3);
			} else {
				rigid.getPoint(pointIndex, rigidPt3);
				SePointOps_F64.transform(rigid.object_to_world, rigidPt3, worldPt3);
			}
			SePointOps_F64.transform(world_to_view, worldPt3, cameraPt);

			jacRowX = observationIndex*2;
			jacRowY = jacRowX + 1;

			//============ Partial of camera parameters
			if (!camera.known) {
				int N = camera.model.getIntrinsicCount();
				camera.model.jacobian(cameraPt.x, cameraPt.y, cameraPt.z,
						pointGradX, pointGradY, true, calibGradX, calibGradY);

				int location = indexLastMotion - indexFirstMotion + cameraParamStartIndex;
				for (int j = 0; j < N; j++) {
					set(rightView, jacRowX, location + j, calibGradX[j]);
					set(rightView, jacRowY, location + j, calibGradY[j]);
				}
			} else {
				camera.model.jacobian(cameraPt.x, cameraPt.y, cameraPt.z, pointGradX, pointGradY,
						false, null, null);
			}

			//============ Partial of world to view
			partialViewSE3(rightView, view, worldPt3.x, worldPt3.y, worldPt3.z, 1);

			//============ Partial of body to world
			// R2*(R1*X+T1)+T2
			// [R1|T1] = object to world. X = fixed point in rigid body
			// [R2|T2] = world to view
			// partial R1 is R2*(@R1*X)
			// partial T1 is R2*(@T1)
			if (!rigid.known) {
				if (structure.isHomogenous()) {
					partialRigidSE3(leftPoint, rigidIndex, rigidPt4.x, rigidPt4.y, rigidPt4.z, rigidPt4.w);
				} else {
					partialRigidSE3(leftPoint, rigidIndex, rigidPt3.x, rigidPt3.y, rigidPt3.z, 1);
				}
			}

			observationIndex++;
		}
		return observationIndex;
	}

	private void partialPoint3( DMatrix leftPoint, DMatrix rightView,
								SceneStructureMetric.View view, int columnOfPointInJac ) {
		// partial of (R*X + T) with respect to X is a 3 by 3 matrix
		// This turns out to be just R
		// grad F(G(X)) = 2 x 3 matrix which is then multiplied by R
		addToJacobian(leftPoint, columnOfPointInJac, pointGradX, pointGradY, world_to_view.R);

		partialViewSE3(rightView, view, worldPt3.x, worldPt3.y, worldPt3.z, 1);
	}

	private void partialPointH( DMatrix leftPoint, DMatrix rightView,
								SceneStructureMetric.View view, int columnOfPointInJac ) {
		// partial of (R*[x,y,z]' + T*w) with respect to X=[x,y,z,w] is a 3 by 4 matrix, [R|T]
		//
		// grad F(G(X)) = 2 x 4 matrix which is then multiplied by R
		addToJacobian(leftPoint, columnOfPointInJac, pointGradX, pointGradY, world_to_view.R);
		addToJacobian(leftPoint, columnOfPointInJac + 3, pointGradX, pointGradY, world_to_view.T);

		partialViewSE3(rightView, view, worldPt4.x, worldPt4.y, worldPt4.z, worldPt4.w);
	}

	/**
	 * Computes the partial for the view's rigid body transform.
	 *
	 * <pre>
	 * When views are not relative it's fairly straight forward:
	 *
	 * Partial of: F(rotation + translation parameters) = R[i]*X + T[i]
	 *             dot(R[i])*X and dot(T[i]) for Jacobian of rotation matrix and translation
	 *
	 * It's more complex when views are relative...
	 *
	 * Current transform from view[i] to world (view[0]) can be written as:
	 *      Tr(i,0) = Tr(i,i-1)*Tr(i-1,0)
	 * where Tr = [R|T]
	 * The Jacobian for R[i] is written as dot(R[i])*Tr(i-1,0)*X. When Generalized for any 'i' in the chain you get
	 *      R[i]*R[i-1]*...*dot(R[j])*Tr(j-1,0)
	 * For T[i] it's similar
	 *       R[i]*R[i-1]*...*dot(T[j])
	 * </pre>
	 * The chained view can be writen as a recursive formula where a rotation matrix is updated each iteration.
	 */
	private void partialViewSE3( DMatrix rightView,
								 SceneStructureMetric.View view,
								 double X, double Y, double Z, double W ) {
		{ // Abort if there is no partial derivative to compute
			SceneStructureMetric.Motion motion = structure.motions.get(view.parent_to_view);
			if (motion.known && view.parent == null)
				return;
		}

		boolean firstView = true;
		worldX.setTo(X, Y, Z, W);

		// Recursively computed rotation R[i]*R[i-1] ... etc
		CommonOps_DDRM.setIdentity(accumulatedR);

		while (true) {
			// Column in output matrix for this view
			SceneStructureMetric.Motion motion = structure.motions.get(view.parent_to_view);
			int col = motionParameterIndexes[view.parent_to_view];

			if (motion.known) {
				// Since this view is known there will be no partial derivative. However, one of it's parents
				// might not be known and will have a Jacobian
				view = view.parent;
				if (view == null)
					break;
				CommonOps_DDRM.mult(accumulatedR, motion.motion.R, tmp3x3);
				accumulatedR.setTo(tmp3x3);
				continue;
			}
			// look up the SO3 Jacobian
			DMatrixRMaj[] jacobianSO3;
			if (firstView) {
				firstView = false;
				// Current view isn't saved in storage and needs to have a reference copied to the array
				for (int i = 0; i < arraySO3.length; i++) {
					arraySO3[i] = jacSO3.getPartial(i);
				}
				jacobianSO3 = arraySO3;
			} else {
				jacobianSO3 = mapSO3Jac.get(view.parent_to_view);
			}

			//============== Partial of view rotation parameters
			final int paramLength = jacSO3.getParameterLength();
			if (view.parent == null) {
				for (int i = 0; i < paramLength; i++) {
					CommonOps_DDRM.mult(accumulatedR, jacobianSO3[i], tmp3x3);
					addToJacobian(rightView, col + i, pointGradX, pointGradY, tmp3x3, X, Y, Z);
				}
			} else {
				Se3_F64 world_to_parent = getWorldToView(view.parent);
				for (int i = 0; i < paramLength; i++) {
					SePointOps_F64.transformV(world_to_parent, worldX, pt3);
					CommonOps_DDRM.mult(accumulatedR, jacobianSO3[i], tmp3x3);
					addToJacobian(rightView, col + i, pointGradX, pointGradY, tmp3x3, pt3.x, pt3.y, pt3.z);
				}
			}

			//============== Partial of view translation parameters
			for (int i = 0; i < 3; i++) {
				double sumX = 0.0;
				double sumY = 0.0;
				for (int j = 0; j < 3; j++) {
					double r_ji = accumulatedR.unsafe_get(j, i);
					sumX += r_ji*pointGradX[j];
					sumY += r_ji*pointGradY[j];
				}
				add(rightView, jacRowX, col + paramLength + i, sumX*W);
				add(rightView, jacRowY, col + paramLength + i, sumY*W);
			}

			// If there is a parent then traverse to it next
			view = view.parent;
			if (view == null)
				break;

			// accumulatedR = R[i,j]*R[j-1]
			CommonOps_DDRM.mult(accumulatedR, motion.motion.R, tmp3x3);
			accumulatedR.setTo(tmp3x3);
		}
	}

	/**
	 * Finds the transform from world to view for the specified view by index
	 */
	private Se3_F64 getWorldToView( SceneStructureMetric.View view ) {
		Se3_F64 world_to_view;
		if (view.parent != null) {
			world_to_view = mapWorldToView.get(view);
		} else {
			world_to_view = structure.motions.get(view.parent_to_view).motion;
		}
		return Objects.requireNonNull(world_to_view);
	}

	private void partialRigidSE3( DMatrix leftPoint, int rigidIndex,
								  double X, double Y, double Z, double W ) {
		int col = rigidParameterIndexes[rigidIndex] + indexFirstRigid;

		JacobianSo3 jac = jacRigidS03[rigidIndex];

		//============== Partial of view rotation parameters
		final int N = jac.getParameterLength();
		for (int i = 0; i < N; i++) {
			CommonOps_DDRM.mult(world_to_view.R, jac.getPartial(i), RR);
			addToJacobian(leftPoint, col + i, pointGradX, pointGradY, RR, X, Y, Z);
		}

		//============== Partial of view translation parameters
		// Apply rotation matrix to gradX and gradY.
		// RX = gradX'*R
		double RX0 = world_to_view.R.data[0]*pointGradX[0] + world_to_view.R.data[3]*pointGradX[1] + world_to_view.R.data[6]*pointGradX[2];
		double RX1 = world_to_view.R.data[1]*pointGradX[0] + world_to_view.R.data[4]*pointGradX[1] + world_to_view.R.data[7]*pointGradX[2];
		double RX2 = world_to_view.R.data[2]*pointGradX[0] + world_to_view.R.data[5]*pointGradX[1] + world_to_view.R.data[8]*pointGradX[2];
		// RY = gradY'*R
		double RY0 = world_to_view.R.data[0]*pointGradY[0] + world_to_view.R.data[3]*pointGradY[1] + world_to_view.R.data[6]*pointGradY[2];
		double RY1 = world_to_view.R.data[1]*pointGradY[0] + world_to_view.R.data[4]*pointGradY[1] + world_to_view.R.data[7]*pointGradY[2];
		double RY2 = world_to_view.R.data[2]*pointGradY[0] + world_to_view.R.data[5]*pointGradY[1] + world_to_view.R.data[8]*pointGradY[2];

		set(leftPoint, jacRowX, col + N, RX0*W);
		set(leftPoint, jacRowY, col + N, RY0*W);
		set(leftPoint, jacRowX, col + N + 1, RX1*W);
		set(leftPoint, jacRowY, col + N + 1, RY1*W);
		set(leftPoint, jacRowX, col + N + 2, RX2*W);
		set(leftPoint, jacRowY, col + N + 2, RY2*W);
	}

	/**
	 * J[rows,col:(col+3)] =  [a;b]*R
	 */
	private void addToJacobian( DMatrix matrix, int col, double[] a, double[] b, DMatrixRMaj R ) {
		set(matrix, jacRowX, col + 0, a[0]*R.data[0] + a[1]*R.data[3] + a[2]*R.data[6]);
		set(matrix, jacRowX, col + 1, a[0]*R.data[1] + a[1]*R.data[4] + a[2]*R.data[7]);
		set(matrix, jacRowX, col + 2, a[0]*R.data[2] + a[1]*R.data[5] + a[2]*R.data[8]);

		set(matrix, jacRowY, col + 0, b[0]*R.data[0] + b[1]*R.data[3] + b[2]*R.data[6]);
		set(matrix, jacRowY, col + 1, b[0]*R.data[1] + b[1]*R.data[4] + b[2]*R.data[7]);
		set(matrix, jacRowY, col + 2, b[0]*R.data[2] + b[1]*R.data[5] + b[2]*R.data[8]);
	}

	private void addToJacobian( DMatrix matrix, int col, double[] a, double[] b,
								DMatrixRMaj R, double X, double Y, double Z ) {

		double x = R.data[0]*X + R.data[1]*Y + R.data[2]*Z;
		double y = R.data[3]*X + R.data[4]*Y + R.data[5]*Z;
		double z = R.data[6]*X + R.data[7]*Y + R.data[8]*Z;

		add(matrix, jacRowX, col, a[0]*x + a[1]*y + a[2]*z);
		add(matrix, jacRowY, col, b[0]*x + b[1]*y + b[2]*z);
	}

	private void addToJacobian( DMatrix matrix, int col, double[] a, double[] b, Vector3D_F64 X ) {
		set(matrix, jacRowX, col, a[0]*X.x + a[1]*X.y + a[2]*X.z);
		set(matrix, jacRowY, col, b[0]*X.x + b[1]*X.y + b[2]*X.z);
	}

	/**
	 * Abstract interface for settings the value of a matrix without knowing the type of matrix
	 */
	protected abstract void set( DMatrix matrix, int row, int col, double value );

	/**
	 * Abstract interface for adding the value of a matrix without knowing the type of matrix. The matrix
	 * is assumed to have been initialized to zero.
	 */
	protected abstract void add( DMatrix matrix, int row, int col, double value );

	/**
	 * Returns a transform from the world_to_view. If relative then the parent's world to view is look up and used
	 * to compute this view's transform and the results are saved.
	 */
	protected void lookupWorldToView( SceneStructureMetric.View v, Se3_F64 world_to_view ) {
		Se3_F64 parent_to_view = structure.getParentToView(v);
		if (v.parent == null) {
			world_to_view.setTo(parent_to_view);
			return;
		}
		Se3_F64 saved_world_to_view = Objects.requireNonNull(mapWorldToView.get(v));
		SceneStructureMetric.View parentView = v.parent;

		if (parentView.parent == null) {
			// Parent is in reference to the world
			Se3_F64 world_to_parent = structure.getParentToView(v.parent);
			world_to_parent.concat(parent_to_view, saved_world_to_view);
		} else {
			// Since the parent must have a lower index it's transform is already known
			Se3_F64 world_to_parent = Objects.requireNonNull(mapWorldToView.get(v.parent));
			world_to_parent.concat(parent_to_view, saved_world_to_view);
		}
		world_to_view.setTo(saved_world_to_view);
	}

	/**
	 * Declare storage SO3 partials in relative views
	 */
	private DMatrixRMaj[] declareRotJacStorage() {
		DMatrixRMaj[] partials = new DMatrixRMaj[jacSO3.getParameterLength()];
		for (int i = 0; i < partials.length; i++) {
			partials[i] = new DMatrixRMaj(3, 3);
		}
		return partials;
	}
}
