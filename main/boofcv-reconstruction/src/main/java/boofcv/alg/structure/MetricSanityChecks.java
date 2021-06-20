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

package boofcv.alg.structure;

import boofcv.abst.geo.TriangulateNViewsMetricH;
import boofcv.abst.geo.bundle.MetricBundleAdjustmentUtils;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.distort.brown.RemoveBrownPtoN_F64;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinholeBrown;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Performs various checks to see if a scene is physically possible. These constraints include: 1) objects
 * appear in front of the camera, 2) objects appear inside the image. 3) Reprojection error isn't massive. 4)
 * focal length isn't negative. A negative focal length is considered a fatal error and processing
 * is aborted early on.
 *
 * Multiple functions are provided to enable these checks when the scene is represented using different data structures.
 *
 * @author Peter Abeles
 */
public class MetricSanityChecks implements VerbosePrint {
	/** Used to triangulate a feature's 3D coordinate */
	public TriangulateNViewsMetricH triangulator = FactoryMultiView.triangulateNViewMetricH(null);

	/** If this value is exceeded then the point is considered to have a bad reprojection. Pixels squared. */
	public double maxReprojectionErrorSq = 100.0;

	/**
	 * Bit field which indicates if a specific 3D feature was found to have passed (true) or failed (false)
	 * the consistency checks.
	 */
	public DogArray_B badFeatures = new DogArray_B();

	// Number of failures in a single inlier set
	int failedTriangulate;
	int failedBehind;
	int failedImageBounds;
	int failedReprojection;

	@Nullable PrintStream verbose;

	/**
	 * Checks physical constraints for one inlier set in a {@link SceneWorkingGraph}. Features are triangulated
	 * directly from observations. Raw counts for each type of error can be found for this function.
	 *
	 * @param dbSimilar Use to get feature locations in the image
	 * @param scene The scene
	 * @param wview The view to check
	 * @param setIdx Which inlier set in the view
	 * @return true if nothing went wrong or false if a very nasty error was detected
	 */
	public boolean checkPhysicalConstraints( LookUpSimilarImages dbSimilar,
											 SceneWorkingGraph scene, SceneWorkingGraph.View wview, int setIdx ) {
		failedTriangulate = 0;
		failedBehind = 0;
		failedImageBounds = 0;
		failedReprojection = 0;

		SceneWorkingGraph.InlierInfo inliers = wview.inliers.get(setIdx);

		int numFeatures = inliers.getInlierCount();
		badFeatures.resetResize(numFeatures, false);

		List<SceneWorkingGraph.View> listViews = new ArrayList<>();
		List<RemoveBrownPtoN_F64> listNormalize = new ArrayList<>();
		List<Se3_F64> listMotion = new ArrayList<>();
		List<DogArray<Point2D_F64>> listFeatures = new ArrayList<>();
		List<Point2D_F64> listViewPixels = new ArrayList<>();

		Se3_F64 view1_to_world = wview.world_to_view.invert(null);

		for (int i = 0; i < inliers.views.size; i++) {
			SceneWorkingGraph.View w = scene.lookupView(inliers.views.get(i).id);
			SceneWorkingGraph.Camera c = scene.getViewCamera(w);
			if (c.intrinsic.f <= 0.0) {
				if (verbose != null) verbose.println("Negative focal length. view='" + w.pview.id + "'");
				return false;
			}

			listViews.add(w);

			// TODO switch to known camera if available

			var normalize = new RemoveBrownPtoN_F64();
			normalize.setK(c.intrinsic.f, c.intrinsic.f, 0, 0, 0).setDistortion(c.intrinsic.k1, c.intrinsic.k2);
			listNormalize.add(normalize);

			listMotion.add(view1_to_world.concat(w.world_to_view, null));

			SceneWorkingGraph.Camera wcamera = scene.getViewCamera(w);
			var features = new DogArray<>(Point2D_F64::new);
			dbSimilar.lookupPixelFeats(w.pview.id, features);
			double cx = wcamera.prior.cx;
			double cy = wcamera.prior.cy;
			features.forEach(p -> p.setTo(p.x - cx, p.y - cy));
			listFeatures.add(features);
		}

		List<Point2D_F64> pixelNorms = BoofMiscOps.createListFilled(inliers.views.size, Point2D_F64::new);

		Point4D_F64 foundX = new Point4D_F64();
		Point4D_F64 viewX = new Point4D_F64();
		Point2D_F64 predictdPixel = new Point2D_F64();

		SceneWorkingGraph.Camera wviewCamera = scene.getViewCamera(wview);

		for (int inlierIdx = 0; inlierIdx < numFeatures; inlierIdx++) {
			listViewPixels.clear();
			for (int viewIdx = 0; viewIdx < listViews.size(); viewIdx++) {
				Point2D_F64 p = listFeatures.get(viewIdx).get(inliers.observations.get(viewIdx).get(inlierIdx));
				listViewPixels.add(p);
				listNormalize.get(viewIdx).compute(p.x, p.y, pixelNorms.get(viewIdx));
			}

			if (!triangulator.triangulate(pixelNorms, listMotion, foundX)) {
				failedTriangulate++;
				badFeatures.set(inlierIdx, true);
				continue;
			}

			boolean badObservation = false;

			for (int viewIdx = 0; viewIdx < listViews.size(); viewIdx++) {
				Se3_F64 view1_to_view = listMotion.get(viewIdx);
				SceneWorkingGraph.View w = listViews.get(viewIdx);

				SePointOps_F64.transform(view1_to_view, foundX, viewX);
				if (PerspectiveOps.isBehindCamera(viewX)) {
					badObservation = true;
					failedBehind++;
				}

				wviewCamera.intrinsic.project(viewX.x, viewX.y, viewX.z, predictdPixel);
				double reprojectionError = predictdPixel.distance2(listViewPixels.get(viewIdx));
				if (reprojectionError > maxReprojectionErrorSq) {
					badObservation = true;
					failedReprojection++;
				}

				SceneWorkingGraph.Camera wcamera = scene.getViewCamera(w);
				int width = wcamera.prior.width;
				int height = wcamera.prior.height;
				double cx = wcamera.prior.cx;
				double cy = wcamera.prior.cy;
				if (!BoofMiscOps.isInside(width, height, predictdPixel.x + cx, predictdPixel.y + cy)) {
					badObservation = true;
					failedImageBounds++;
				}
			}

			badFeatures.set(inlierIdx, badObservation);
		}

		if (verbose != null)
			verbose.printf("view.id='%s' inlierIdx=%d, errors: behind=%d bounds=%d reprojection=%d tri=%d, obs=%d\n",
					wview.pview.id, setIdx,
					failedBehind, failedImageBounds, failedReprojection, failedTriangulate, numFeatures);

		return true;
	}

	/**
	 * Checks that every points passes the physical constraints in every view it is seen from. If a point fails
	 * then it is marked as false in a {@link #badFeatures}.
	 *
	 * @param bundle Scene in bundle adjustment format
	 * @param listDimensions Shape of every image
	 * @return true if a fatal catastrophic error is detected. E.g. negative focal length.
	 */
	public boolean checkPhysicalConstraints( MetricBundleAdjustmentUtils bundle,
											 List<CameraPinholeBrown> listDimensions ) {
		return checkPhysicalConstraints(bundle.structure, bundle.observations, listDimensions);
	}

	public boolean checkPhysicalConstraints( SceneStructureMetric structure,
											 SceneObservations observations,
											 List<CameraPinholeBrown> listPriors ) {
		BoofMiscOps.checkEq(listPriors.size(), structure.views.size);

		for (int i = 0; i < structure.cameras.size; i++) {
			BundlePinholeSimplified pinhole = (BundlePinholeSimplified)structure.cameras.get(i).model;
			if (pinhole.f < 0.0f) {
				if (verbose != null) verbose.println("Bad focal length. f=" + pinhole.f);
				return false;
			}
		}

		badFeatures.resetResize(structure.points.size, false);

		var worldP = new Point4D_F64(0, 0, 0, 1);
		var viewP = new Point4D_F64();
		var observedPixel = new Point2D_F64();
		var predictdPixel = new Point2D_F64();

		for (int viewIdx = 0; viewIdx < observations.views.size; viewIdx++) {
			int cameraIdx = structure.views.get(viewIdx).camera;
			BundlePinholeSimplified pinhole = (BundlePinholeSimplified)
					Objects.requireNonNull(structure.cameras.get(cameraIdx).model);
			CameraPinholeBrown priorCamera = listPriors.get(viewIdx);
			int width = priorCamera.width;
			int height = priorCamera.height;

			// Used to compensates for the lens model having its origin at the image center
			float cx = (float)priorCamera.cx;
			float cy = (float)priorCamera.cy;

			// Number of times each test failed in this particular view
			int failedBehind = 0;
			int failedImageBounds = 0;
			int failedReprojection = 0;

			Se3_F64 world_to_view = structure.getParentToView(viewIdx);
			SceneObservations.View oview = observations.views.get(viewIdx);

			for (int i = 0; i < oview.size(); i++) {
				// If true then this feature failed one of the constraints test in tis value
				boolean badObservation = false;

				oview.getPixel(i, observedPixel);
				SceneStructureCommon.Point p = structure.points.get(oview.getPointId(i));
				worldP.x = p.getX();
				worldP.y = p.getY();
				worldP.z = p.getZ();
				if (structure.isHomogenous()) {
					worldP.w = p.getW();
				}
				// worldP.w = 1 was already set for 3D points

				SePointOps_F64.transform(world_to_view, worldP, viewP);
				if (PerspectiveOps.isBehindCamera(viewP)) {
					badObservation = true;
					failedBehind++;
				}

				pinhole.project(viewP.x, viewP.y, viewP.z, predictdPixel);

				double reprojectionError = predictdPixel.distance2(observedPixel);
				if (reprojectionError > maxReprojectionErrorSq) {
					badObservation = true;
					failedReprojection++;
				}

				if (!BoofMiscOps.isInside(width, height, predictdPixel.x + cx, predictdPixel.y + cy)) {
					badObservation = true;
					failedImageBounds++;
				}

				if (badObservation) {
					badFeatures.set(oview.getPointId(i), true);
				}
			}

			if (verbose != null) verbose.printf("view[%d] errors: behind=%d bounds=%d reprojection=%d, obs=%d\n",
					viewIdx, failedBehind, failedImageBounds, failedReprojection, oview.size());
		}

		return true;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
