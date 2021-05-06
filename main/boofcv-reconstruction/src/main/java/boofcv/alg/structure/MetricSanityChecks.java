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
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageDimension;
import georegression.geometry.ConvertRotation3D_F64;
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
import java.util.Set;

/**
 * Sanity checks on results
 *
 * @author Peter Abeles
 */
public class MetricSanityChecks implements VerbosePrint {

	@Nullable PrintStream verbose;

	public TriangulateNViewsMetricH triangulator = FactoryMultiView.triangulateNViewMetricH(null);

	public double maxFractionFail = 0.15;
	public double maxReprojectionErrorSq = 100.0;
	// If true then the feature was marked as bad
	public DogArray_B badFeatures = new DogArray_B();

	/**
	 * Applies a positive depth constraint on triangulated inlier features for a view.
	 */
	public void inlierTriangulatePositiveDepth( double tolerance,
												LookUpSimilarImages db,
												SceneWorkingGraph workGraph,
												String targetID ) {

		SceneWorkingGraph.View wview1 = workGraph.lookupView(targetID);

		for (int infoIdx = 0; infoIdx < wview1.inliers.size; infoIdx++) {
			SceneWorkingGraph.InlierInfo inliers = wview1.inliers.get(infoIdx);
			checkInlierSet(tolerance, db, workGraph, targetID, wview1, inliers);
		}
	}

	private void checkInlierSet( double tolerance, LookUpSimilarImages db,
								 SceneWorkingGraph workGraph, String targetID,
								 SceneWorkingGraph.View wview1,
								 SceneWorkingGraph.InlierInfo inliers ) {
		List<SceneWorkingGraph.View> listViews = new ArrayList<>();
		List<RemoveBrownPtoN_F64> listNormalize = new ArrayList<>();
		List<Se3_F64> listMotion = new ArrayList<>();
		List<DogArray<Point2D_F64>> listFeatures = new ArrayList<>();

		Se3_F64 view1_to_world = wview1.world_to_view.invert(null);

		for (int i = 0; i < inliers.views.size; i++) {
			SceneWorkingGraph.View w = workGraph.lookupView(inliers.views.get(i).id);
			listViews.add(w);
			var normalize = new RemoveBrownPtoN_F64();
			normalize.setK(w.intrinsic.f, w.intrinsic.f, 0, 0, 0).setDistortion(w.intrinsic.k1, w.intrinsic.k2);
			listNormalize.add(normalize);

			listMotion.add(view1_to_world.concat(w.world_to_view, null));

			var features = new DogArray<>(Point2D_F64::new);
			db.lookupPixelFeats(w.pview.id, features);
			ImageDimension shape = new ImageDimension();
			db.lookupShape(w.pview.id, shape);
			double cx = shape.width/2;
			double cy = shape.height/2;
			features.forEach(p -> p.setTo(p.x - cx, p.y - cy));
			listFeatures.add(features);
		}

		List<Point2D_F64> pixelNorms = BoofMiscOps.createListFilled(inliers.views.size, Point2D_F64::new);

		Point4D_F64 foundX = new Point4D_F64();

		int bad = 0;

		int numFeatures = inliers.getInlierCount();
		for (int inlierIdx = 0; inlierIdx < numFeatures; inlierIdx++) {
			for (int viewIdx = 0; viewIdx < listViews.size(); viewIdx++) {
				Point2D_F64 p = listFeatures.get(viewIdx).get(inliers.observations.get(viewIdx).get(inlierIdx));
				listNormalize.get(viewIdx).compute(p.x, p.y, pixelNorms.get(viewIdx));
			}

			if (!triangulator.triangulate(pixelNorms, listMotion, foundX)) {
				bad++;
				continue;
			}

			if (foundX.z*foundX.w < 0)
				bad++;
		}

		if (verbose != null) {
			verbose.print("Depth Sanity: target='" + targetID + "', Bad Depth: " + bad + "/" + numFeatures + ", ");
			verbose.print("inlier.views={ ");
			for (int i = 0; i < inliers.views.size(); i++) {
				verbose.print("'" + inliers.views.get(i).id + "' ");
			}
			verbose.println("}");
		}

		if (bad > numFeatures*tolerance) {
			if (verbose != null) {
				for (int i = 0; i < listMotion.size(); i++) {
					Se3_F64 m = listMotion.get(i);
					double theta = ConvertRotation3D_F64.matrixToRodrigues(m.R, null).theta;
					BundlePinholeSimplified intrinsic = listViews.get(i).intrinsic;
					verbose.printf("_ view['%s'] T=(%.2f %.2f %.2f) R=%.4f f=%.2f k1=%.2e k2=%.2e\n",
							listViews.get(i).pview.id, m.T.x, m.T.y, m.T.z, theta,
							intrinsic.f, intrinsic.k1, intrinsic.k2);
				}
			}
			throw new RuntimeException("Failed positive depth. bad=" + bad + "/" + numFeatures);
		}
	}

	public boolean checkPhysicalConstraints( MetricBundleAdjustmentUtils bundle,
											 List<ImageDimension> listDimensions ) {
		return checkPhysicalConstraints(bundle.structure, bundle.observations, listDimensions);
	}

	public boolean checkPhysicalConstraints( SceneStructureMetric structure,
											 SceneObservations observations,
											 List<ImageDimension> listDimensions ) {
		for (int i = 0; i < structure.cameras.size; i++) {
			BundlePinholeSimplified pinhole = (BundlePinholeSimplified)structure.cameras.get(i).model;
			if (pinhole.f < 0.0f) {
				if (verbose != null) verbose.println("Bad focal length. f=" + pinhole.f);
				return false;
			}
		}

		badFeatures.resetResize(structure.points.size, false);

		boolean success = true;

		var worldP = new Point4D_F64(0, 0, 0, 1);
		var viewP = new Point4D_F64();
		var observedPixel = new Point2D_F64();
		var predictdPixel = new Point2D_F64();

		for (int viewIdx = 0; viewIdx < observations.views.size; viewIdx++) {
			ImageDimension dimension = listDimensions.get(viewIdx);
			int width = dimension.width;
			int height = dimension.height;

			float cx = (float)(width/2);
			float cy = (float)(height/2);

			int failedBehind = 0;
			int failedImageBounds = 0;
			int failedReprojection = 0;

			Se3_F64 world_to_view = structure.getParentToView(viewIdx);
			SceneObservations.View oview = observations.views.get(viewIdx);

			int threshold = (int)(oview.size()*maxFractionFail);

			for (int i = 0; i < oview.size(); i++) {
				boolean badObservation = false;

				oview.getPixel(i, observedPixel);
				SceneStructureCommon.Point p = structure.points.get(oview.getPointId(i));
				worldP.x = p.getX();
				worldP.y = p.getY();
				worldP.z = p.getZ();
				if (structure.isHomogenous()) {
					worldP.w = p.getW();
					// ignore points at infinity
					if (worldP.w == 0.0)
						continue;
				}

				SePointOps_F64.transform(world_to_view, worldP, viewP);
				if (viewP.z/viewP.w < 0.0) {
					badObservation = true;
					failedBehind++;
				}

				structure.cameras.get(viewIdx).model.project(viewP.x, viewP.y, viewP.z, predictdPixel);

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

			if (verbose != null) verbose.printf("view[%d] errors: behind=%d bounds=%d reprojection=%d. obs=%d\n",
					viewIdx, failedBehind, failedImageBounds, failedReprojection, oview.size());

			if (failedBehind > threshold || failedImageBounds > threshold || failedReprojection > threshold)
				success = false;
		}

		return success;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
