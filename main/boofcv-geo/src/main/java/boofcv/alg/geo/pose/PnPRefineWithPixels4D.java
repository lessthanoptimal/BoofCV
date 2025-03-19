/*
 * Copyright (c) 2025, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.bundle.*;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.Point2D4D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.VerbosePrint;
import org.ddogleg.util.VerboseUtils;
import org.ejml.UtilEjml;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;

/**
 * Refines the PnP estimate using pixel observations and known homogeneous points. Internally, the optimization is done
 * using bundle adjustment and should be optimal.
 */
public class PnPRefineWithPixels4D implements VerbosePrint {
	@Getter @Setter
	BundleAdjustment<SceneStructureMetric> bundleAdjustment = FactoryMultiView.bundleDenseMetric(false, null);
	@Getter SceneStructureMetric structure = new SceneStructureMetric(true);
	@Getter SceneObservations sceneObs = new SceneObservations();
	@Nullable BundleCameraState cameraState;
	@Nullable BundleAdjustmentCamera camera;

	Se3_F64 identity = new Se3_F64();

	@Nullable PrintStream verbose = null;

	public PnPRefineWithPixels4D() {
		bundleAdjustment.configure(1e-8, 1e-8, 20);
	}

	/**
	 * Specifies the camera model it should use. Must be called first
	 */
	public void setCamera( BundleAdjustmentCamera camera, @Nullable BundleCameraState state ) {
		this.camera = camera;
		this.cameraState = state;
	}

	/**
	 * Refines the passed in SE3 using the provided obseravations
	 *
	 * @param observations Pixel observations with known 3D location
	 * @param initial Initial estimate of world to view transform
	 * @param output Refined estimate of world to view transform
	 */
	public boolean refine( List<Point2D4D> observations, Se3_F64 initial, Se3_F64 output ) {
		if (camera == null)
			throw new RuntimeException("Must first call setCamera");

		structure.initialize(1, 1, 1, 0, 1);
		sceneObs.initialize(1, true);
		structure.setCamera(0, true, camera);

		// Define the planar scene
		structure.setRigid(0, true, identity, observations.size());
		for (int i = 0; i < observations.size(); i++) {
			Point4D_F64 p = observations.get(i).location;
			structure.getRigid(0).setPoint(i, p.x, p.y, p.z, p.w);
		}

		structure.setView(0, 0, false, initial);
		structure.assignIDsToRigidPoints();

		// Attach pixel observations
		for (int i = 0; i < observations.size(); i++) {
			Point2D_F64 p = observations.get(i).observation;
			structure.getRigid(0).connectPointToView(i, 0, (float)p.x, (float)p.y, sceneObs);
		}

		// Attach the camera state
		sceneObs.getView(0).cameraState = cameraState;

		// Optimize the scene. If it fails to work then it's probably already optimal and return the initial conditions
		bundleAdjustment.setParameters(structure, sceneObs);
		if (bundleAdjustment.optimize(structure))
			output.setTo(structure.getParentToView(0));
		else
			output.setTo(initial);

		return !UtilEjml.isUncountable(bundleAdjustment.getFitScore());
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = VerboseUtils.addPrefix(this, out);
		VerboseUtils.verboseChildren(verbose, configuration, bundleAdjustment);
	}
}
