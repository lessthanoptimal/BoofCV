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

package boofcv.abst.feature.tracker;

import boofcv.alg.tracker.combined.CombinedTrackerScalePoint;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;

/**
 * Changes behavior of {@link PointTrackerCombined} so that it conforms to the {@link PointTrackerTwoPass} interface.
 *
 * @author Peter Abeles
 */
public class PointTrackerTwoPassCombined<I extends ImageGray, D extends ImageGray, Desc extends TupleDesc>
	extends PointTrackerCombined<I,D,Desc> implements PointTrackerTwoPass<I>
{
	public PointTrackerTwoPassCombined(CombinedTrackerScalePoint<I, D, Desc> tracker,
									   int reactivateThreshold,
									   Class<I> imageType, Class<D> derivType)
	{
		super(tracker, reactivateThreshold, imageType, derivType);
	}

	@Override
	public void process(I image) {

	}

	@Override
	public void performSecondPass() {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void finishTracking() {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void setHint(double pixelX, double pixelY, PointTrack track) {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
