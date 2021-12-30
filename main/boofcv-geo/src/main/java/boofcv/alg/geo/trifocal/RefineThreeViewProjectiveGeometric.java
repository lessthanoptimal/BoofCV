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

package boofcv.alg.geo.trifocal;

import boofcv.abst.geo.TriangulateNViewsProjective;
import boofcv.abst.geo.bundle.*;
import boofcv.factory.geo.ConfigBundleAdjustment;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.ConfigConverge;
import boofcv.struct.geo.AssociatedTriple;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.ArrayList;
import java.util.List;

/**
 * Improves the estimated camera projection matrices for three views, with the first view assumed to be P1 = [I|0].
 * It's recommended that the observations be normalized to ensure that they are zero mean with a standard deviation
 * of 1, or similar, first.
 *
 * This is known as the Gold Standard algorithm in [1]
 *
 * <ul>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ul>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class RefineThreeViewProjectiveGeometric {

	// Computes initial location of points
	TriangulateNViewsProjective triangulator;

	// data structures for SBA
	SceneStructureProjective structure;
	SceneObservations observations;

	// first view is assumed to be [I|0]
	DMatrixRMaj P1 = CommonOps_DDRM.identity(3, 4);

	// convergence criteria for SBA
	ConfigConverge converge = new ConfigConverge(1e-8, 1e-8, 200);
	BundleAdjustment<SceneStructureProjective> sba;

	// if true scaling is done before running bundle adjustment. Only set to false if pixel coordinate have
	// already been scaled
	boolean scale = true;

	ScaleSceneStructure scaler = new ScaleSceneStructure();

	/**
	 * Creates a constructor using default triangulation and SBA configuration
	 */
	public RefineThreeViewProjectiveGeometric() {
		triangulator = FactoryMultiView.triangulateNViewProj(ConfigTriangulation.GEOMETRIC());

		ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
		configLM.hessianScaling = false; // seems to do better without this. Reconsider later on

		ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
		configSBA.configOptimizer = configLM;

		sba = FactoryMultiView.bundleSparseProjective(configSBA);
	}

	public RefineThreeViewProjectiveGeometric( TriangulateNViewsProjective triangulator,
											   BundleAdjustment<SceneStructureProjective> sba ) {
		this.triangulator = triangulator;
		this.sba = sba;
	}

	/**
	 * Refines P2 and P3 using SBA. View 1 is assumed to be P1 = [I|0]
	 *
	 * @param listObs Observations from the three views
	 * @param P2 camera matrix for view 2. modified
	 * @param P3 camera matrix for view 3. modified
	 * @return true if successful
	 */
	public boolean refine( List<AssociatedTriple> listObs, DMatrixRMaj P2, DMatrixRMaj P3 ) {
		CommonOps_DDRM.setIdentity(P1);
		initializeStructure(listObs, P2, P3);

		if (scale) {
			scaler.applyScale(structure, observations);
		}

//		sba.setVerbose(System.out,0);
		sba.setParameters(structure, observations);
		sba.configure(converge.ftol, converge.gtol, converge.maxIterations);

		if (!sba.optimize(structure)) {
			return false;
		}

		// save the results
		P2.setTo(structure.views.data[1].worldToView);
		P3.setTo(structure.views.data[2].worldToView);

		if (scale) {
			// don't use built in unscaling function because it undoes scaling on points. Those are disposable
			scaler.pixelScaling.get(1).remove(P2, P2);
			scaler.pixelScaling.get(2).remove(P3, P3);
		}

		return true;
	}

	/**
	 * Sets up data structures for SBA
	 */
	private void initializeStructure( List<AssociatedTriple> listObs, DMatrixRMaj P2, DMatrixRMaj P3 ) {
		List<DMatrixRMaj> cameraMatrices = new ArrayList<>();
		cameraMatrices.add(P1);
		cameraMatrices.add(P2);
		cameraMatrices.add(P3);

		List<Point2D_F64> triangObs = new ArrayList<>();
		triangObs.add(null);
		triangObs.add(null);
		triangObs.add(null);

		structure = new SceneStructureProjective(true);
		structure.initialize(3, listObs.size());
		observations = new SceneObservations();
		observations.initialize(3);

		structure.setView(0, true, P1, 0, 0);
		structure.setView(1, false, P2, 0, 0);
		structure.setView(2, false, P3, 0, 0);

		boolean needsPruning = false;
		Point4D_F64 X = new Point4D_F64();
		for (int i = 0; i < listObs.size(); i++) {
			AssociatedTriple t = listObs.get(i);

			triangObs.set(0, t.p1);
			triangObs.set(1, t.p2);
			triangObs.set(2, t.p3);

			// triangulation can fail if all 3 views have the same pixel value. This has been observed in
			// simulated 3D scenes
			if (triangulator.triangulate(triangObs, cameraMatrices, X)) {
				observations.getView(0).add(i, (float)t.p1.x, (float)t.p1.y);
				observations.getView(1).add(i, (float)t.p2.x, (float)t.p2.y);
				observations.getView(2).add(i, (float)t.p3.x, (float)t.p3.y);

				structure.points.get(i).set(X.x, X.y, X.z, X.w);
			} else {
				needsPruning = true;
			}
		}

		if (needsPruning) {
			PruneStructureFromSceneProjective pruner = new PruneStructureFromSceneProjective(structure, observations);
			pruner.prunePoints(1);
		}
	}

	public TriangulateNViewsProjective getTriangulator() {
		return triangulator;
	}

	public SceneStructureProjective getStructure() {
		return structure;
	}

	public SceneObservations getObservations() {
		return observations;
	}

	public BundleAdjustment<SceneStructureProjective> getSba() {
		return sba;
	}

	public boolean isScale() {
		return scale;
	}

	public void setScale( boolean scale ) {
		this.scale = scale;
	}

	public ConfigConverge getConverge() {
		return converge;
	}
}
