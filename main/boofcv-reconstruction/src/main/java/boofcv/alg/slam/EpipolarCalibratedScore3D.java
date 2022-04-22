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

package boofcv.alg.slam;

import boofcv.struct.distort.Point3Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Evaluates how much 3D information can be derived from two views given that the calibration is known.
 *
 * @author Peter Abeles
 */
public interface EpipolarCalibratedScore3D extends VerbosePrint {
	/**
	 * Estimates the amount of 3D information contained between the two observations
	 *
	 * @param imageShapeA (Input) Camera A: Image size
 	 * @param imageShapeB (Input) Camera B: Image size
	 * @param pointToPixelA (Input) Camera A: Projector from pointing to image pixel
	 * @param pointToPixelB (Input) Camera B: Projector from pointing to image pixel
	 * @param obsA (Input) Camera A: Pointing vector of feature observations
	 * @param obsB (Input) Camera B: Pointing vector of feature observations. Matches obsA
	 * @param pairs (Input) Which features have been paired up in the two views.
	 * @param a_to_b (Input) If not null, then it specifies the extrinsic relationship between the views
	 * @param inliersIdx (Output) Which features inside of pairs are in the inlier sets
	 */
	void process( ImageDimension imageShapeA, ImageDimension imageShapeB,
				  Point3Transform2_F64 pointToPixelA, Point3Transform2_F64 pointToPixelB,
				  List<Point3D_F64> obsA, List<Point3D_F64> obsB,
				  List<AssociatedIndex> pairs, @Nullable Se3_F64 a_to_b, DogArray_I32 inliersIdx );

	/**
	 * Returns a score for how much 3D information there is. 0 = no 3D information. 1 = very strong 3D information.
	 * score is 0 to 1.
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
