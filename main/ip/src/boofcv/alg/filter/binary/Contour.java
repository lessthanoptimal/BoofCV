/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal and externals contours for a binary blob.  The set of points in each contour list are ordered in
 * CW or CCW directions.
 *
 * @author Peter Abeles
 */
public class Contour {
	/**
	 * ID of blob in the image.  Pixels belonging to this blob in the labeled image will have this pixel value.
	 */
	public int id;
	/**
	 * External contour around the blob.
	 */
	public List<Point2D_I32> external = new ArrayList<>();
	/**
	 * Internal contours that are inside the blob.
	 */
	public List<List<Point2D_I32>> internal = new ArrayList<>();

	public void reset() {
		id = -1;
		external.clear();
		internal.clear();
	}

   public Contour copy() {
      Contour ret = new Contour();
      ret.id = id;
      for( Point2D_I32 p : external ) {
         ret.external.add( p.copy() );
      }

      for( List<Point2D_I32> l : ret.internal ) {
         List<Point2D_I32> a = new ArrayList<>();

         for( Point2D_I32 p : l ) {
            a.add( p.copy() );
         }

         internal.add(a);
      }

      return ret;
   }
}
