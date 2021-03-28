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

package boofcv.alg.structure.score3d;

import boofcv.alg.structure.EpipolarScore3D;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.homography.Homography2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.data.DMatrixRMaj;
import org.ejml.ops.DConvertMatrixStruct;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;

/**
 * If there is a geometric relationship or not is determined by the number of inliers. The minimum number is specified
 * by {@link #minimumInliers}. A threshold is used for classifying an edge as 3D or not {@link #ratio3D} len(F)/len(H)
 * a value of 1 just requires equality, greater than one means there must be more features from F (fundamental) than
 * H (homography). See [1] for more details on this test.
 *
 * <p>[1] Pollefeys, Marc, et al. "Visual modeling with a hand-held camera." International Journal of Computer
 * Vision 59.3 (2004): 207-232.</p>
 *
 * @author Peter Abeles
 */
public class ScoreRatioFundamentalHomography implements EpipolarScore3D {

	@Getter ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D;
	@Getter ModelMatcher<Homography2D_F64, AssociatedPair> ransacH;

	/**
	 * The minimum number of inliers for an edge to be accepted
	 */
	public @Getter @Setter int minimumInliers = 30;

	/**
	 * If number of matches from fundamental divided by homography is more than this then it is considered a 3D scene
	 */
	public @Getter @Setter double ratio3D = 1.5;

	/**
	 * The error ratio can get massive and this number prevents large values for being weighted too much in the score
	 */
	public @Getter @Setter double maxRatioScore = 5.0;

	// if true then it decided there was a 3D relationship
	private boolean is3D;

	/** Number of inliers for fundamental matrix */
	@Getter int countF;

	/** Number of inliers for essential matrix */
	@Getter int countH;

	// If not null then verbose debugging information should be printed here
	private PrintStream verbose;

	public ScoreRatioFundamentalHomography( ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D,
											ModelMatcher<Homography2D_F64, AssociatedPair> ransacH ) {
		this.ransac3D = ransac3D;
		this.ransacH = ransacH;
	}

	protected ScoreRatioFundamentalHomography() {}

	@Override public boolean process( List<AssociatedPair> pairs, DMatrixRMaj fundamental, DogArray_I32 inliersIdx ) {
		// Reset output
		inliersIdx.reset();
		fundamental.fill(0);

		// Fitting Essential/Fundamental works when the scene is not planar and not pure rotation
		countF = 0;
		if (ransac3D.process(pairs)) {
			countF = ransac3D.getMatchSet().size();
		}

		// Fitting homography will work when all or part of the scene is planar or motion is pure rotation
		countH = 0;
		if (ransacH.process(pairs)) {
			countH = ransacH.getMatchSet().size();
		}

		is3D = countF > countH*ratio3D;

		if (verbose != null)
			verbose.println("ransac F=" + countF + " H=" + countH + " pairs.size=" + pairs.size() + " 3d=" + is3D);

		// Always use fundamental if it's available
		if (countF >= minimumInliers) {
			saveInlierMatches(ransac3D, inliersIdx);
			fundamental.setTo(ransac3D.getModelParameters());
		} else if (countH >= minimumInliers) {
			saveInlierMatches(ransacH, inliersIdx);
			Homography2D_F64 H = ransacH.getModelParameters();
			DConvertMatrixStruct.convert(H, fundamental);
			// returning H instead of F could cause some confusion here. Will deal with that when it becomes an issue
		} else {
			return false;
		}

		return true;
	}

	@Override public double getScore() {
		// countF and countF will be <= totalFeatures

		// Prefer a scene more features from a fundamental matrix than a homography.
		// This can be sign that the scene has a rich 3D structure and is poorly represented by
		// a plane or rotational motion
		double score = Math.min(maxRatioScore, countF/(double)(countH + 1));
		// Also prefer more features from the original image to be matched
		score *= countF;

		return score;
	}

	@Override public boolean is3D() {
		return is3D;
	}

	@Override public void setVerbose( @Nullable PrintStream verbose, @Nullable Set<String> options ) {
		this.verbose = verbose;
	}

	/**
	 * Saves inlier indexes
	 */
	private void saveInlierMatches( ModelMatcher<?, ?> ransac, DogArray_I32 inliers ) {
		int N = ransac.getMatchSet().size();
		inliers.resize(N);
		for (int i = 0; i < N; i++) {
			inliers.set(i, ransac.getInputIndex(i));
		}
	}
}
