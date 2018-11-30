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

import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.alg.geo.PerspectiveOps;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.struct.GrowQueue_I32;

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

	public PruneStructureFromSceneProjective(SceneStructureProjective structure,
											 SceneObservations observations) {
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
	public void pruneObservationsByErrorRank(double inlierFraction) {

		Point2D_F64 observation = new Point2D_F64();
		Point2D_F64 predicted = new Point2D_F64();
		Point3D_F64 X3 = new Point3D_F64();
		Point4D_F64 X4 = new Point4D_F64();

		// Create a list of observation errors
		List<Errors> errors = new ArrayList<>();
		for (int viewIndex = 0; viewIndex < observations.views.length; viewIndex++) {
			SceneObservations.View v = observations.views[viewIndex];
			SceneStructureProjective.View view = structure.views[viewIndex];

			for (int pointIndex = 0; pointIndex < v.point.size; pointIndex++) {
				int pointID = v.point.data[pointIndex];
				SceneStructureMetric.Point f = structure.points[pointID];

				// Get observation in image pixels
				v.get(pointIndex, observation);

				// Get feature location in world and predict the pixel observation
				if( structure.homogenous ) {

					f.get(X3);
					PerspectiveOps.renderPixel(view.worldToView,X3,predicted);
				} else {
					f.get(X4);
					PerspectiveOps.renderPixel(view.worldToView,X4,predicted);
				}


				Errors e = new Errors();
				e.view = viewIndex;
				e.pointIndexInView = pointIndex;
				e.error = predicted.distance2(observation);
				errors.add(e);
			}
		}

		errors.sort(Comparator.comparingDouble(a -> a.error));

		// Mark observations which are to be removed. Can't remove yet since the indexes will change
		for (int i = (int) (errors.size() * inlierFraction); i < errors.size(); i++) {
			Errors e = errors.get(i);

			SceneObservations.View v = observations.views[e.view];
			v.set(e.pointIndexInView, Float.NaN, Float.NaN);
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
	public boolean prunePoints(int count) {
		List<SceneStructureProjective.Point> remainingP = new ArrayList<>();
		for (int pointIdx = 0; pointIdx < structure.points.length; pointIdx++) {
			SceneStructureProjective.Point sp = structure.points[pointIdx];

			if( sp.views.size < count) {
				// remove observations of this point from each view
				for (int i = 0; i < sp.views.size; i++) {
					SceneObservations.View ov = observations.views[sp.views.data[i]];
					int localIdx = ov.point.indexOf(pointIdx);
					if( localIdx == -1 )
						throw new RuntimeException("Point not in view's observation!?");
					ov.remove(localIdx);
				}
			} else {
				remainingP.add(sp);
			}
		}

		// if all of them are still remaining nothing changed
		if( remainingP.size() == structure.points.length )
			return false;

		// create a new array with just the remaining points
		structure.points = new SceneStructureProjective.Point[remainingP.size()];
		for (int i = 0; i < remainingP.size(); i++) {
			structure.points[i] = remainingP.get(i);
		}
		return true;
	}

	/**
	 * Removes views with less than 'count' features visible. Observations of features in removed views are also
	 * removed.
	 *
	 * @param count Prune if it has this number of views or less
	 * @return true if views were pruned or false if not
	 */
	public boolean pruneViews(int count) {
		List<SceneStructureProjective.View> remainingS = new ArrayList<>();
		List<SceneObservations.View> remainingO = new ArrayList<>();

		// count number of observations in each view
		int counts[] = new int[structure.views.length];
		for (int pointIdx = 0; pointIdx < structure.points.length; pointIdx++) {
			GrowQueue_I32 viewsIn = structure.points[pointIdx].views;
			for (int i = 0; i < viewsIn.size; i++) {
				counts[viewsIn.get(i)]++;
			}
		}

		// TODO Add a list of points to each view reducing number of iterations through all the points
		// mark views with too few points for removal
		for (int viewIdx = 0; viewIdx < structure.views.length; viewIdx++) {
			if( counts[viewIdx] > count) {
				remainingS.add(structure.views[viewIdx]);
				remainingO.add(observations.views[viewIdx]);
			} else {
				structure.views[viewIdx].width = -2;
			}
		}

		// remove the view from points
		for (int pointIdx = 0; pointIdx < structure.points.length; pointIdx++) {
			GrowQueue_I32 viewsIn = structure.points[pointIdx].views;
			for (int i = viewsIn.size-1; i >= 0; i--) {
				SceneStructureProjective.View v = structure.views[viewsIn.get(i)];
				if( v.width == -2 ) {
					viewsIn.remove(i);
				}
			}
		}

		if( structure.views.length == remainingS.size() ) {
			return false;
		}

		// Create new arrays with the views that were not pruned
		structure.views = new SceneStructureProjective.View[remainingS.size()];
		observations.views = new SceneObservations.View[remainingO.size()];

		for (int i = 0; i < structure.views.length; i++) {
			structure.views[i] = remainingS.get(i);
			observations.views[i] = remainingO.get(i);
		}
		return true;
	}

	/**
	 * Removes observations which have been marked with NaN
	 */
	private void removeMarkedObservations() {
		Point2D_F64 observation = new Point2D_F64();

		for (int viewIndex = 0; viewIndex < observations.views.length; viewIndex++) {
//			System.out.println("ViewIndex="+viewIndex);
			SceneObservations.View v = observations.views[viewIndex];
			for(int pointIndex = v.point.size-1; pointIndex >= 0; pointIndex-- ) {
				int pointID = v.getPointId(pointIndex);
				SceneStructureProjective.Point f = structure.points[pointID];
//				System.out.println("   pointIndex="+pointIndex+" pointID="+pointID+" hash="+f.hashCode());
				v.get(pointIndex, observation);

				if( !Double.isNaN(observation.x))
					continue;

				if( !f.views.contains(viewIndex))
					throw new RuntimeException("BUG!");

				// Tell the feature it is no longer visible in this view
				f.removeView(viewIndex);
				// Remove the observation of this feature from the view
				v.remove(pointIndex);
			}
		}
	}

	private static class Errors {
		int view;
		int pointIndexInView;
		double error;
	}
}