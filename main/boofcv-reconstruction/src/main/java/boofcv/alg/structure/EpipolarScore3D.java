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

import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.geo.AssociatedPair;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Evaluates how 3D a pair of views are from their associated points
 *
 * @author Peter Abeles
 */
public interface EpipolarScore3D extends VerbosePrint {
	/**
	 * Determines if there's a 3D relationship between the views and scores how strong it is
	 *
	 * @param cameraA (Input) Prior information on the camera intrinsics. View A.
	 * @param cameraB (Input) Prior information on the camera intrinsics. View B.
	 * @param pairs (Input) Set of point feature pairs between the two images
	 * @param fundamental (Output) Fundamental matrix describing the geometric relationship between the two views
	 * @param inliersIdx (Output) Which features inside of pairs are in the inlier sets
	 * @return true if it was successful or false if something went very wrong and nothing could be determined
	 */
	boolean process( CameraPinholeBrown cameraA, CameraPinholeBrown cameraB,
					 List<AssociatedPair> pairs, DMatrixRMaj fundamental, DogArray_I32 inliersIdx );

	/**
	 * Returns a score for how much 3D information there is. Higher the value more 3D information. Zero
	 * means that's no 3D information.
	 *
	 * @return score
	 */
	double getScore();

	/**
	 * Decides if the two views have a 3D relationship.
	 *
	 * @return true if 3D or false if not
	 */
	boolean is3D();
}
