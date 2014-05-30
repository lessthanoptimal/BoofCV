/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.tracker;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.shapes.Quadrilateral_F64;

/**
 * <p>
 * High level interface for an object tracker where the object being tracked is specified using a quadrilateral. The
 * input is assumed to be a sequence of consecutive video images. When initialize is called the tracker is put
 * into its initial state ago and its past history is discarded.   The vertices in the quadrilateral are specified
 * in a clock-wise direction (a,b,c,d).
 * </p>
 * <p>
 * The motion/distortion model used by the trackers will vary.  Typical models are (scale,translation),
 * (scale,translation,rotation), and affine.  For maximum abstraction, access to the underlying model is not provided
 * through this interface.  To access that model invoke the lower level algorithm directly.
 * </p>
 *
 * @author Peter Abeles
 */
// TODO add binary image for background model?
public interface TrackerObjectQuad<T extends ImageBase> {

	/**
	 * Initializes tracking by specifying the object's location using a quadrilateral. Some implementations can
	 * fail if there is insufficient visual information for it to track.  All previous tracking information
	 * is discarded when this function is called.
	 *
	 * @param image Initial image in the sequence
	 * @param location Initial location of the object being tracked
	 * @return true if successful and false if not.
	 */
	public boolean initialize( T image , Quadrilateral_F64 location );

	/**
	 * Updates the tracks location using the latest video frame.  {@link #initialize(boofcv.struct.image.ImageBase, Quadrilateral_F64)}
	 * must be called once before this function can be called.
	 *
	 * @param image The next image in the video sequence.
	 * @param location The new location of the object being tracked.
	 * @return true if the target was found and 'location' updated.
	 */
	public boolean process( T image , Quadrilateral_F64 location );

	/**
	 * Returns information on the type of image that it can process.
	 *
	 * @return Image type
	 */
	public ImageType<T> getImageType();
}
