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

package boofcv.abst.filter.binary;

import boofcv.alg.filter.binary.ContourPacked;
import boofcv.struct.ConfigLength;
import boofcv.struct.ConnectRule;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Common interface for binary contour finders
 *
 * @author Peter Abeles
 */
public interface BinaryContourInterface {

	/**
	 * Returns a list of contours/blobs found.
	 *
	 * WARNING: List is recycled on the next call to process().
	 *
	 * @return List of contours.
	 */
	List<ContourPacked> getContours();

	/**
	 * Used to load the pixels associated with a contour.
	 *
	 * @param contourID ID of the contour you wish to load
	 * @param storage Storage for the contour points. Must be set to declare new elements.
	 */
	void loadContour( int contourID, DogArray<Point2D_I32> storage );

	/**
	 * Overwrites the coordinates of the saved contour. Useful when points have
	 * been undistorted and you're trying to minimize memory by not saving another copy
	 *
	 * @param contourID ID of the contour you wish to load
	 * @param storage Storage for the contour points. Must be set to declare new elements.
	 */
	void writeContour( int contourID, List<Point2D_I32> storage );

	/**
	 * Used to toggle on and off the saving of inner contours.
	 *
	 * @param enabled true to enable or false to disable
	 */
	void setSaveInnerContour( boolean enabled );

	boolean isSaveInternalContours();

	/**
	 * Specifies the minimum contour as either an absolute value in pixels or a value relative to the
	 * sqrt(width*height). Threshold &le; 0 will be treated as infinite.
	 */
	void setMinContour( ConfigLength length );

	/**
	 * Returns the minimum contour
	 *
	 * @param length Optional storage for the contour's length
	 * @return The contour's length
	 */
	ConfigLength getMinContour( @Nullable ConfigLength length );

	/**
	 * Specifies the maximum contour as either an absolute value in pixels or a value relative to the
	 * sqrt(width*height). Threshold &le; 0 will be treated as infinite.
	 */
	void setMaxContour( ConfigLength length );

	/**
	 * Returns the maximum contour. Threshold %le; 0 will be treated as infinite.
	 *
	 * @param length Optional storage for the contour's length
	 * @return The contour's length
	 */
	ConfigLength getMaxContour( @Nullable ConfigLength length );

	void setConnectRule( ConnectRule rule );

	ConnectRule getConnectRule();

	/**
	 * Convenience function which loads a contour and creates copy of all the points and returns
	 * a new list
	 *
	 * @return New copy of contour
	 */
	static List<Point2D_I32> copyContour( BinaryContourInterface finder, int contourID ) {
		DogArray<Point2D_I32> storage = new DogArray<>(Point2D_I32::new);
		finder.loadContour(contourID, storage);
		List<Point2D_I32> list = new ArrayList<>(storage.size);
		for (int i = 0; i < storage.size; i++) {
			list.add(storage.get(i));
		}
		return list;
	}

	/**
	 * Many contour algorithms require that the binary image has an outside border of all zeros. To avoid discarding
	 * those pixels a copy of the input image is created with a 1 pixel border added. This interface can be
	 * used to toggle that copy on and off. If turned off then the input image is modified in some implementation
	 * specific way.
	 */
	interface Padded {
		/**
		 * If this is set o true then internally it will create a copy of the input image with a 1-pixel border added.
		 *
		 * You probably want to also adjust the coordinates using a value of (1,1)
		 */
		void setCreatePaddedCopy( boolean hasPadding );

		boolean isCreatePaddedCopy();

		/**
		 * Adjustment applied to pixel coordinate of contour points. Only used if a padded copy is NOT done.
		 */
		void setCoordinateAdjustment( int x, int y );
	}
}
