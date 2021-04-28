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
import boofcv.alg.distort.brown.RemoveBrownPtoN_F64;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;
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

	PrintStream verbose;

	public TriangulateNViewsMetricH triangulator = FactoryMultiView.triangulateNViewMetricH(null);

	/**
	 * Applies a positive depth constraint on triangulated inlier features for a view.
	 */
	public void inlierTriangulatePositiveDepth( double tolerance,
												LookUpSimilarImages db,
												SceneWorkingGraph workGraph,
												String targetID ) {

		SceneWorkingGraph.View wview1 = workGraph.lookupView(targetID);
		SceneWorkingGraph.InlierInfo inliers = wview1.inliers;

		// see there are inleirs to sanity check
		if (inliers.isEmpty())
			return;

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

			listMotion.add(view1_to_world.concat(w.world_to_view,null));

			var features = new DogArray<>(Point2D_F64::new);
			db.lookupPixelFeats(w.pview.id, features);
			ImageDimension shape = new ImageDimension();
			db.lookupShape(w.pview.id, shape);
			double cx = shape.width/2;
			double cy = shape.height/2;
			features.forEach(p->p.setTo(p.x-cx, p.y-cy));
			listFeatures.add(features);
		}

		List<Point2D_F64> pixelNorms = BoofMiscOps.createListFilled(inliers.views.size, Point2D_F64::new);

		Point4D_F64 foundX = new Point4D_F64();

		int bad = 0;

		int numFeatures = wview1.inliers.getInlierCount();
		for (int inlierIdx = 0; inlierIdx < numFeatures; inlierIdx++) {
			for (int viewIdx = 0; viewIdx < listViews.size(); viewIdx++) {
				Point2D_F64 p = listFeatures.get(viewIdx).get(inliers.observations.get(viewIdx).get(inlierIdx));
				listNormalize.get(viewIdx).compute(p.x, p.y, pixelNorms.get(viewIdx));
			}

			if (!triangulator.triangulate(pixelNorms, listMotion, foundX)) {
				bad++;
				continue;
			}

			// points at infinity are a weird case
			if (foundX.w == 0.0)
				continue;

			if (foundX.z/foundX.w < 0)
				bad++;
		}

		if (verbose != null)
			verbose.println("Depth Sanity: target='"+targetID+"', Depth: "+bad+"/"+numFeatures);

		if (bad > numFeatures*tolerance)
			throw new RuntimeException("Failed positive depth. bad="+bad+"/"+numFeatures);
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
