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

import boofcv.abst.geo.TriangulateNViewsMetricH;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.ConfigConverge;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Set;

/**
 * Contains everything you need to do metric bundle adjustment in one location
 *
 * @author Peter Abeles
 */
public class MetricBundleAdjustmentUtils implements VerbosePrint {
	/** Configures convergence criteria for SBA */
	public final @Getter ConfigConverge configConverge = new ConfigConverge(1e-5, 1e-5, 30);
	/** Toggles on and off scaling parameters */
	public @Getter @Setter boolean configScale = false;

	/** Optional second pass where outliers observations. Fraction specifies that the best X fraction are kept. */
	public double keepFraction = 1.0;

	/** The estimated scene structure. This the final estimated scene state */
	public final @Getter SceneStructureMetric structure;
	public final @Getter SceneObservations observations = new SceneObservations();
	public @Getter @Setter BundleAdjustment<SceneStructureMetric> sba = FactoryMultiView.bundleSparseMetric(null);
	public @Getter @Setter TriangulateNViewsMetricH triangulator;
	public @Getter ScaleSceneStructure scaler = new ScaleSceneStructure();

	@Nullable PrintStream verbose;

	public MetricBundleAdjustmentUtils( @Nullable ConfigTriangulation triangulation, boolean homogenous ) {
		triangulator = FactoryMultiView.triangulateNViewMetricH(triangulation);
		structure = new SceneStructureMetric(homogenous);
	}

	public MetricBundleAdjustmentUtils() {
		this(null, true);
	}

	/**
	 * Uses the already configured structure and observations to perform bundle adjustment
	 *
	 * @return true if successful
	 */
	public boolean process() {
		if (configConverge.maxIterations == 0)
			return true;
		if (configScale)
			scaler.applyScale(structure, observations);
		sba.configure(configConverge.ftol, configConverge.gtol, configConverge.maxIterations);

		sba.setParameters(structure, observations);
		if (verbose != null) printAverageError("BEFORE", verbose);
		if (!sba.optimize(structure))
			return false;
		if (verbose != null) printAverageError("AFTER", verbose);

		if (keepFraction < 1.0) {
			// don't prune views since they might be required
			prune(keepFraction, -1, 1);
			sba.setParameters(structure, observations);
			if (!sba.optimize(structure))
				return false;
			if (verbose != null) printAverageError("PRUNED-AFTER", verbose);
		}

		if (configScale)
			scaler.undoScale(structure, observations);
		return true;
	}

	private void printAverageError( String location, PrintStream out ) {
		double averageError = Math.sqrt(sba.getFitScore())/observations.getObservationCount();
		out.printf("SBA %13s average error=%.2e\n", location, averageError);
	}

	/**
	 * Prunes outliers and views/points with too few points/observations
	 *
	 * @param keepFraction Only keeps features which have the best reprojection error. 0.95 will keep 95%
	 * @param pruneViews Prunes views if less than or equal to this many features
	 * @param prunePoints Prunes points if less than this number of observations
	 */
	public void prune( double keepFraction, int pruneViews, int prunePoints ) {
		prunePoints = Math.max(1, prunePoints);

		PruneStructureFromSceneMetric pruner = new PruneStructureFromSceneMetric(structure, observations);
		pruner.pruneObservationsByErrorRank(keepFraction);
		if (pruneViews > 0) {
			if (pruner.pruneViews(pruneViews))
				pruner.pruneUnusedMotions();
		}
		pruner.prunePoints(prunePoints);
	}

	/**
	 * Prints the number of different data structures in the scene
	 */
	public void printCounts( PrintStream out ) {
		out.println("Bundle: Points=" + structure.points.size + " Views=" + structure.views.size + " Cameras=" + structure.cameras.size);
		for (int viewIdx = 0; viewIdx < observations.views.size; viewIdx++) {
			out.println("view[" + viewIdx + "].observations.size=" + observations.views.get(viewIdx).size());
		}
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
