/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.slam.score3d;

import boofcv.alg.slam.EpipolarCalibratedScore3D;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.AssociatedIndex;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;

/**
 * Scores the geometric information between two views when the extrinsics and intrinsics are known.
 *
 * @author Peter Abeles
 */
public class EpipolarScoreKnownExtrinsics implements EpipolarCalibratedScore3D {

	PrintStream verbose;

	@Override
	public void process( List<Point3D_F64> obsA,
						 List<Point3D_F64> obsB, List<AssociatedIndex> pairs, @Nullable Se3_F64 b_to_a,
						 DogArray_I32 inliersIdx ) {

	}

	@Override public double getScore() {
		return 0;
	}

	@Override public boolean is3D() {
		return false;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
