/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.tracker;

import boofcv.struct.image.ImageBase;

/**
 * <p>
 * Extension of {@link PointTracker} allows for predictions of a feature's location to be incorporated into the tracker.
 * A typical usage would be to first run the tracker, estimate a motion model, then use the said motion model to
 * predict each feature's location.
 * </p>
 *
 * <p>
 * NOTE: A track hint can be set before {@link #process(boofcv.struct.image.ImageBase)} is called.
 * </p>
 *
 * @author Peter Abeles
 */
public interface PointTrackerTwoPass<T extends ImageBase> extends PointTracker<T> {

	/**
	 * Updates spacial information for each track.  Does not change the track description.  Can be called multiple
	 * times.
	 */
	public void performSecondPass();

	/**
	 *
	 */
	public void finishTracking();

	/**
	 * Provides a hint for where the
	 * @param pixelX
	 * @param pixelY
	 * @param track
	 */
	public void setHint( double pixelX , double pixelY , PointTrack track );
}
