/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary;

import org.ddogleg.struct.GrowQueue_I32;

/**
 * Internal and externals contours for a binary blob with the actual points stored in a
 * {@link boofcv.struct.PackedSetsPoint2D_I32}.  The set of points in each contour list are ordered in
 * CW or CCW directions.
 *
 * @author Peter Abeles
 */
public class ContourPacked {
	/**
	 * ID of blob in the image.  Pixels belonging to this blob in the labeled image will have this pixel value.
	 */
	public int id;

	/**
	 * Index in the packed list of the external contour
	 */
	public int externalIndex;
	/**
	 * Number of internal contours. Their ID = external + 1 + internal index
	 */
	public GrowQueue_I32 internalIndexes = new GrowQueue_I32();

	public void reset() {
		id = -1;
		externalIndex = -1;
		internalIndexes.reset();
	}

   public ContourPacked copy() {
      ContourPacked ret = new ContourPacked();
      ret.id = id;
      ret.externalIndex = externalIndex;
      ret.internalIndexes = internalIndexes.copy();

      return ret;
   }
}
