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

package boofcv.alg.sfm.structure;

import boofcv.abst.geo.bundle.BundleAdjustmentObservations;
import boofcv.abst.geo.bundle.BundleAdjustmentSceneStructure;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Makes it easy to remove outliers or features which don't meet certain criteria from the
 * scene.
 *
 * @author Peter Abeles
 */
public class PruneStructureFromScene {

	BundleAdjustmentSceneStructure structure;
	BundleAdjustmentObservations observations;

	public PruneStructureFromScene(BundleAdjustmentSceneStructure structure,
								   BundleAdjustmentObservations observations)
	{
		this.structure = structure;
		this.observations = observations;
	}

	/**
	 * Computes reprojection error for all features. Sorts the resulting residuals by magnitude.
	 * Prunes observations which have the largest errors. Then searches for features with less
	 * than two views and prunes them.
	 *
	 * @param inlierFraction
	 */
	public void pruneObservationsByErrorRank( double inlierFraction ) {

		Point2D_F64 observation = new Point2D_F64();
		Point2D_F64 predicted = new Point2D_F64();
		Point3D_F64 X = new Point3D_F64();

		List<Errors> errors = new ArrayList<>();

		for (int viewIndex = 0; viewIndex < observations.views.length; viewIndex++) {
			BundleAdjustmentObservations.View v = observations.views[viewIndex];
			BundleAdjustmentSceneStructure.View view = structure.views[viewIndex];

			for (int featureIndex = 0; featureIndex < v.feature.size; featureIndex++) {
				BundleAdjustmentSceneStructure.Point f = structure.points[v.feature.data[featureIndex]];

				// Get feature location in world
				f.get(X);
				// Get observation in image pixels
				v.get(featureIndex, observation);

				// World to View
				view.worldToView.transform(X, X);

				// predicted pixel
				BundleAdjustmentSceneStructure.Camera camera = structure.cameras[view.camera];
				camera.model.project(X.x, X.y, X.z, predicted);

				Errors e = new Errors();
				e.view = viewIndex;
				e.feature = featureIndex;
				e.error = predicted.distance2(observation);
				errors.add(e);
			}
		}

		errors.sort(Comparator.comparingDouble(a -> a.error));

		// Mark observations which are to be removed. Can't remove yet since the indexes will change
		for (int i = (int) (errors.size() * inlierFraction); i < errors.size(); i++) {
			Errors e = errors.get(i);

			BundleAdjustmentObservations.View v = observations.views[e.view];
			v.set(e.feature, Float.NaN, Float.NaN);
		}

		// Remove all marked features
		for (int viewIndex = 0; viewIndex < observations.views.length; viewIndex++) {
			BundleAdjustmentObservations.View v = observations.views[viewIndex];

			for( int featureIndex = v.feature.size-1; featureIndex >= 0; featureIndex-- ) {
				BundleAdjustmentSceneStructure.Point f = structure.points[v.feature.data[featureIndex]];

				v.get(featureIndex, observation);

				if( !Double.isNaN(observation.x))
					continue;

				// Tell the feature it is no longer visible in this view
				f.removeView(viewIndex);
				// Remove the observation of this feature from the view
				v.remove(featureIndex);
			}
		}
	}

	/**
	 * Prunes features with less than 'count' observations
	 * @param count
	 */
	public void pruneFeatures( int count ) {
		for (int viewIndex = observations.views.length-1; viewIndex >= 0; viewIndex--) {
			BundleAdjustmentObservations.View v = observations.views[viewIndex];

			for( int featureIndex = v.feature.size-1; featureIndex >= 0; featureIndex-- ) {
				BundleAdjustmentSceneStructure.Point f = structure.points[v.feature.data[featureIndex]];

				if( f.views.size < count ) {
					v.remove(featureIndex);
				}
			}
		}

		int oldToNew[] = new int[ structure.points.length ];
		Arrays.fill(oldToNew,-1); // crash is bug

		GrowQueue_I32 prune = new GrowQueue_I32();
		for (int i = 0; i < structure.points.length; i++) {
			if( structure.points[i].views.size < count ) {
				prune.add(i);
			} else {
				oldToNew[i] = i-prune.size;
			}

		}

		structure.removePoints(prune);

		// Update the references from observation to features
		for (int viewIndex = observations.views.length-1; viewIndex >= 0; viewIndex--) {
			BundleAdjustmentObservations.View v = observations.views[viewIndex];

			for( int featureIndex = v.feature.size-1; featureIndex >= 0; featureIndex-- ) {
				v.feature.data[featureIndex] = oldToNew[v.feature.data[featureIndex]];
			}
		}
	}

	/**
	 * Removes views with less than 'count' features visible
	 * @param count minimum number of features
	 */
	public void pruneViews( int count ) {

	}

	private static class Errors {
		int view;
		int feature;
		double error;
	}
}
