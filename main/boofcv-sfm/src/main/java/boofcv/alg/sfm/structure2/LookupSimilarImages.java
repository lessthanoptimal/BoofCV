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

package boofcv.alg.sfm.structure2;

import boofcv.struct.feature.AssociatedIndex;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * Interface for finding images with a similar apperance by some metric.
 *
 * @author Peter Abeles
 */
public interface LookupSimilarImages {

	List<String> getImageIDs();

	/**
	 *
	 * @param target ID of target image
	 * @param similar Storage for IDs of similar images. Cleared upon each call
	 */
	void findSimilar( String target , List<String> similar );

	void lookupFeatures( String target , FastQueue<Point2D_F64> features );

	void lookupMatches(String src, String dst , FastQueue<AssociatedIndex> pairs );
}
