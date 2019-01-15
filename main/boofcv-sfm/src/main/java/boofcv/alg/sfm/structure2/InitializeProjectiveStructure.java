/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure2;

import georegression.struct.point.Point2D_F64;

import java.util.List;

/**
 * Given a set of views all of which view all the same features, estimate their structure up to a
 * projective transform
 *
 * @author Peter Abeles
 */
public class InitializeProjectiveStructure {

	public void reset( int totalViews , int totalFeatures ) {

	}

	public void setView( List<Point2D_F64> observations , int []identifiers ) {

	}

	public boolean process() {
		// TODO find the 3 views with the most tracks
		// TODO use trifocal tensor to prune tracks from that set
		// TODO compute projective transform
		// TODO add new views using trifocal and find common projective

		return true;
	}
}
