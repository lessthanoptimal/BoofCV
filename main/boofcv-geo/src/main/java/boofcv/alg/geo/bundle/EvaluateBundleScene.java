/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import lombok.Getter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates the quality of a reconstruction based on various factors.
 */
public class EvaluateBundleScene {
	protected BundleAdjustmentMetricResidualFunction functionResiduals = new BundleAdjustmentMetricResidualFunction();

	/**
	 * Statistics for how many inliers were observed at different thresholds
	 */
	@Getter protected List<InlierBucket> inlierStats = new ArrayList<>();

	/**
	 * Add the inlier threshold values where you want to compute how many observations would be considered
	 * an inlier if you had the following thresholds.
	 */
	public void addInliers( double... thresholds ) {
		inlierStats.clear();
		for (int i = 0; i < thresholds.length; i++) {
			inlierStats.add(new InlierBucket(thresholds[i]));
		}
	}

	/**
	 * Evaluates the scene and computes performance statistics
	 */
	public void evaluate( SceneStructureMetric scene, SceneObservations observations ) {
		for (int i = 0; i < inlierStats.size(); i++) {
			inlierStats.get(i).clearCounts();
		}

		functionResiduals.configure(scene, observations);
		var residuals = new double[functionResiduals.getNumOfOutputsM()];

		functionResiduals.process(residuals);

		for (int i = 0; i < residuals.length; i += 2) {
			double rx = residuals[i];
			double ry = residuals[i + 1];
			double errorSq = rx*rx + ry*ry;

			for (int inlierIdx = 0; inlierIdx < inlierStats.size(); inlierIdx++) {
				InlierBucket bucket = inlierStats.get(inlierIdx);
				if (bucket.isMember(errorSq)) {
					bucket.count++;
				}
			}
		}
	}

	public void printSummary( PrintStream out ) {
		// two residuals for every projected points (x,y)
		double N = functionResiduals.getNumOfOutputsM()/2.0;

		out.println("      Inlier Thresholds");
		out.print("       ");
		for (int inlierIdx = 0; inlierIdx < inlierStats.size(); inlierIdx++) {
			InlierBucket bucket = inlierStats.get(inlierIdx);
			out.printf(" | %6.1f", bucket.threshold);
		}
		out.println();
		out.print("percent");
		for (int inlierIdx = 0; inlierIdx < inlierStats.size(); inlierIdx++) {
			InlierBucket bucket = inlierStats.get(inlierIdx);
			out.printf(" | %5.1f%%", 100.0*bucket.count/N);
		}
		out.println();
	}

	public static class InlierBucket {
		/** Inlier threshold */
		public double threshold;

		/** Number of observations below this threshold */
		public int count;

		public InlierBucket( double threshold ) {
			this.threshold = threshold;
		}

		public boolean isMember( double errorSq ) {
			return errorSq <= threshold*threshold;
		}

		public void clearCounts() {
			count = 0;
		}
	}
}
