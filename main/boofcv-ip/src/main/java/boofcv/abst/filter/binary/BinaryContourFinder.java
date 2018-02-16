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

package boofcv.abst.filter.binary;

import boofcv.alg.filter.binary.ContourPacked;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface for finding contours around binary blobs and labeling the image
 * at the same time. To get the points in a contour invoke {@link #loadContour} with
 * the ID of the contour you wish to load. Adjusting the max contour size is useful in
 * situations were memory is limited. Same for turning off inner contours.
 *
 * NOTE: Contours which are too small or too large are still included in the list
 * of contours, but their contour points will not be stored. To see if it was excluded
 * you need to load the contour and see if it has zero points. This is done because
 * the blobs the contours came from will still be in the labeled image.
 *
 * Defaults:
 * <ul>
 *     <li>{@link ConnectRule#FOUR}</li>
 *     <li>Inner Contours Enabled</li>
 *     <li>Infinite Contour Size</li>
 * </ul>
 *
 * @see boofcv.alg.filter.binary.LinearContourLabelChang2004
 *
 * @author Peter Abeles
 */
public interface BinaryContourFinder {

	/**
	 * Processes the binary image to find the contour of and label blobs.
	 *
	 * @param binary Input binary image. Not modified.
	 * @param labeled Output. Labeled image.  Modified.
	 */
	void process(GrayU8 binary , GrayS32 labeled );

	/**
	 * Returns a list of contours/blobs found.
	 *
	 * WARNING: List is recycled on the next call to {@link #process}.
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
	void loadContour(int contourID , FastQueue<Point2D_I32> storage );

	/**
	 * Overwrites the coordinates of the saved contour. Useful when points have
	 * been undistorted and you're trying to minimize memory by not saving another copy
	 *
	 * @param contourID ID of the contour you wish to load
	 * @param storage Storage for the contour points. Must be set to declare new elements.
	 */
	void writeContour(int contourID , List<Point2D_I32> storage );

	/**
	 * Convenience function which loads a contour and creates copy of all the points and returns
	 * a new list
	 *
	 * @return New copy of contour
	 */
	static List<Point2D_I32> copyContour(BinaryContourFinder finder , int contourID) {
		FastQueue<Point2D_I32> storage = new FastQueue<>(Point2D_I32.class,true);
		finder.loadContour(contourID,storage);
		List<Point2D_I32> list = new ArrayList<>(storage.size);
		for (int i = 0; i < storage.size; i++) {
			list.add(storage.get(i));
		}
		return list;
	}

	/**
	 * Used to toggle on and off the saving of inner contours.
	 * @param enabled true to enable or false to disable
	 */
	void setSaveInnerContour(boolean enabled );

	boolean isSaveInternalContours();

	void setMinContour( int length );

	int getMinContour();

	void setMaxContour( int length );

	int getMaxContour();

	void setConnectRule( ConnectRule rule );

	ConnectRule getConnectRule();
}
