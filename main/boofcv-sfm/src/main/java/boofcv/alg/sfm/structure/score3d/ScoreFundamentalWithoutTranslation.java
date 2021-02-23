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

package boofcv.alg.sfm.structure.score3d;

import boofcv.alg.geo.robust.DistanceFundamentalGeometric;
import boofcv.alg.geo.robust.DistanceHomographySq;
import boofcv.alg.geo.robust.GenerateHomographyLinear;
import boofcv.alg.sfm.structure.EpipolarScore3D;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.homography.UtilHomography_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Peter Abeles
 */
public class ScoreFundamentalWithoutTranslation implements EpipolarScore3D {
	@Getter ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D;

	/**
	 * If number of matches from fundamental divided by homography is more than this then it is considered a 3D scene
	 */
	public @Getter @Setter double ratio3D = 4.0;

	List<AssociatedPair> inliers = new ArrayList<>();

	DogArray_F64 errors = new DogArray_F64();

	GenerateHomographyLinear estimateH = new GenerateHomographyLinear(true);
	DistanceHomographySq distanceH = new DistanceHomographySq();
	DistanceFundamentalGeometric distanceF = new DistanceFundamentalGeometric();

	double score;
	boolean is3D;
	double eps = 0.5;

	PrintStream verbose;

	public ScoreFundamentalWithoutTranslation( ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D ) {
		this.ransac3D = ransac3D;
	}

	@Override public boolean process( List<AssociatedPair> pairs, DMatrixRMaj fundamental, DogArray_I32 inliersIdx ) {
		if (!ransac3D.process(pairs))
			return false;

		fundamental.setTo(ransac3D.getModelParameters());

		distanceF.setModel(ransac3D.getModelParameters());
		inliersIdx.resize(ransac3D.getMatchSet().size());
		inliers.clear();
		errors.resize(inliersIdx.size);
		for (int i = 0; i < inliersIdx.size; i++) {
			inliersIdx.set(i, ransac3D.getInputIndex(i));
			inliers.add(pairs.get(inliersIdx.get(i)));
			errors.set(i, distanceF.distance(pairs.get(inliersIdx.get(i))));
		}

		errors.sort();
		double errorF = errors.getFraction(0.5);

		var H = new DMatrixRMaj(3, 3);
		var H2 = new Homography2D_F64();
		estimateH.generate(inliers, H2);
		UtilHomography_F64.convert(H2, H);
		distanceH.setModel(H);
		errors.resize(inliersIdx.size);
		for (int i = 0; i < inliersIdx.size; i++) {
			errors.set(i, distanceH.distance(pairs.get(inliersIdx.get(i))));
		}
		errors.sort();
		double errorH = Math.sqrt(errors.getFraction(0.5));

		score = (errorH + eps)/(errorF + eps);
		is3D = score > ratio3D;

		if (verbose != null)
			verbose.println("score=" + score + " errorH=" + errorH + " errorF=" + errorF + " 3d=" + is3D);

		// TODO if not 3D identify planar scene case

		return true;
	}

	@Override public double getScore() {
		return score;
	}

	@Override public boolean is3D() {
		return is3D;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> param ) {
		this.verbose = out;
	}
}
