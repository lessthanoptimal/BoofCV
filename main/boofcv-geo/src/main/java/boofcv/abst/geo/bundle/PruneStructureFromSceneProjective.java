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

package boofcv.abst.geo.bundle;

import boofcv.alg.geo.PerspectiveOps;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.struct.DogArray_I32;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Makes it easy to removing elements from bundle adjustment's input scene structure. Different criteria can
 * be specified for each type of element you wish to remove.
 *
 * @author Peter Abeles
 */
public class PruneStructureFromSceneProjective {
	SceneStructureProjective structure;
	SceneObservations observations;

	public PruneStructureFromSceneProjective( SceneStructureProjective structure,
											  SceneObservations observations ) {
		this.structure = structure;
		this.observations = observations;
	}

	/**
	 * Computes reprojection error for all features. Sorts the resulting residuals by magnitude.
	 * Prunes observations which have the largest errors first. After calling this function you should
	 * call {@link #prunePoints(int)} and {@link #pruneViews(int)} to ensure the scene is still valid.
	 *
	 * @param inlierFraction Fraction of observations to keep. 0 to 1. 1 = no change. 0 = everything is pruned.
	 */
	public void pruneObservationsByErrorRank( double inlierFraction ) {

		Point2D_F64 observation = new Point2D_F64();
		Point2D_F64 predicted = new Point2D_F64();
		Point3D_F64 X3 = new Point3D_F64();
		Point4D_F64 X4 = new Point4D_F64();

		// Create a list of observation errors
		List<Errors> errors = new ArrayList<>();
		for (int viewIndex = 0; viewIndex < observations.views.size; viewIndex++) {
			SceneObservations.View v = observations.views.data[viewIndex];
			SceneStructureProjective.View view = structure.views.data[viewIndex];

			for (int indexInView = 0; indexInView < v.point.size; indexInView++) {
				int pointID = v.point.data[indexInView];
				SceneStructureCommon.Point f = structure.points.data[pointID];

				// Get observation in image pixels
				v.getPixel(indexInView, observation);

				// Get feature location in world and predict the pixel observation
				if (structure.homogenous) {
					f.get(X4);
					PerspectiveOps.renderPixel(view.worldToView, X4, predicted);
				} else {
					f.get(X3);
					PerspectiveOps.renderPixel(view.worldToView, X3, predicted);
				}

				Errors e = new Errors();
				e.view = viewIndex;
				e.pointIndexInView = indexInView;
				e.error = predicted.distance2(observation);
				errors.add(e);
			}
		}

		errors.sort(Comparator.comparingDouble(a -> a.error));

		// Mark observations which are to be removed. Can't remove yet since the indexes will change
		int index0 = (int)(errors.size()*inlierFraction + 0.5);
		for (int i = index0; i < errors.size(); i++) {
			Errors e = errors.get(i);

			SceneObservations.View v = observations.views.data[e.view];
			v.setPixel(e.pointIndexInView, Float.NaN, Float.NaN);
		}

		// Remove all marked features
		removeMarkedObservations();
	}

	/**
	 * Prunes Points/features with less than 'count' observations. Observations of the points are also removed.
	 *
	 * Call {@link #pruneViews(int)} to ensure that all views have points in view
	 *
	 * @param count Minimum number of observations
	 */
	public boolean prunePoints( int count ) {
		// create a lookup table from old to new point indexes
		int oldToNew[] = new int[structure.points.size];

		// list of remaining points
		DogArray_I32 removeIdx = new DogArray_I32();
//		List<SceneStructureProjective.Point> remainingP = new ArrayList<>();
		for (int pointIdx = 0; pointIdx < structure.points.size; pointIdx++) {
			SceneStructureCommon.Point sp = structure.points.data[pointIdx];

			if (sp.views.size < count) {
				removeIdx.add(pointIdx);
				// remove observations of this point from each view
				for (int i = 0; i < sp.views.size; i++) {
					int viewIdx = sp.views.data[i];
					SceneObservations.View ov = observations.views.get(viewIdx);
					int localIdx = ov.point.indexOf(pointIdx);
					if (localIdx == -1)
						throw new RuntimeException("Point not in view's observation!?");
					ov.remove(localIdx);
				}
			} else {
				oldToNew[pointIdx] = pointIdx - removeIdx.size();
			}
		}

		// if all of them are still remaining nothing changed
		if (removeIdx.size == 0)
			return false;

		// update point references
		for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
			SceneObservations.View so = observations.views.get(viewIdx);

			for (int i = 0; i < so.point.size; i++) {
				so.point.data[i] = oldToNew[so.point.data[i]];
			}
		}

		// Remove the pruned points
		structure.removePoints(removeIdx);
		return true;
	}

	/**
	 * Removes views with less than 'count' features visible. Observations of features in removed views are also
	 * removed. Features are not automatically removed even if there are zero observations of them.
	 *
	 * @param count Prune if it has this number of views or less
	 * @return true if views were pruned or false if not
	 */
	public boolean pruneViews( int count ) {
		DogArray_I32 pruneIdx = new DogArray_I32();

		// count number of observations in each view
		int[] counts = new int[structure.views.size];
		for (int pointIdx = 0; pointIdx < structure.points.size; pointIdx++) {
			DogArray_I32 viewsIn = structure.points.data[pointIdx].views;
			for (int i = 0; i < viewsIn.size; i++) {
				counts[viewsIn.get(i)]++;
			}
		}

		// TODO Add a list of points to each view reducing number of iterations through all the points
		// mark views with too few points for removal
		for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
			if (counts[viewIdx] <= count) {
				pruneIdx.add(viewIdx);
				structure.views.data[viewIdx].width = -2;
			}
		}

		// remove the view from points
		for (int pointIdx = 0; pointIdx < structure.points.size; pointIdx++) {
			DogArray_I32 viewsIn = structure.points.data[pointIdx].views;
			for (int i = viewsIn.size - 1; i >= 0; i--) {
				SceneStructureProjective.View v = structure.views.data[viewsIn.get(i)];
				if (v.width == -2) {
					viewsIn.remove(i);
				}
			}
		}

		if (pruneIdx.size() == 0) {
			return false;
		}

		// Create new arrays with the views that were not pruned
		structure.views.remove(pruneIdx.data, 0, pruneIdx.size, null);
		observations.views.remove(pruneIdx.data, 0, pruneIdx.size, null);
		return true;
	}

	/**
	 * Removes observations which have been marked with NaN
	 */
	private void removeMarkedObservations() {
		Point2D_F64 observation = new Point2D_F64();

		for (int viewIndex = 0; viewIndex < observations.views.size; viewIndex++) {
//			System.out.println("ViewIndex="+viewIndex);
			SceneObservations.View v = observations.views.data[viewIndex];
			for (int indexInView = v.point.size - 1; indexInView >= 0; indexInView--) {
				int pointID = v.getPointId(indexInView);
				SceneStructureCommon.Point f = structure.points.data[pointID];
//				System.out.println("   pointIndex="+pointIndex+" pointID="+pointID+" hash="+f.hashCode());
				v.getPixel(indexInView, observation);

				if (!Double.isNaN(observation.x))
					continue;

				// Tell the feature it is no longer visible in this view
				f.removeView(viewIndex);
				// Remove the observation of this feature from the view
				v.remove(indexInView);
			}
		}
	}

	private static class Errors {
		int view;
		int pointIndexInView;
		double error;
	}
}
