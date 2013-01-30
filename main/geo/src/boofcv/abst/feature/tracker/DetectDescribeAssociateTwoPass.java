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

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;

/**
 * @author Peter Abeles
 */
public class DetectDescribeAssociateTwoPass<I extends ImageSingleBand, Desc extends TupleDesc>
	extends DetectDescribeAssociate<I,Desc> implements PointTrackerTwoPass<I>
{
	public DetectDescribeAssociateTwoPass(DdaFeatureManager<I, Desc> manager,
										  AssociateDescription2D<Desc> associate,
										  boolean updateDescription)
	{
		super(manager, associate, updateDescription);
	}

	@Override
	public void process( I input ) {
		tracksActive.clear();
		tracksInactive.clear();
		tracksDropped.clear();
		tracksNew.clear();

		featDst.reset();
		locDst.reset();

		manager.detectFeatures(input, locDst, featDst);

		// skip if there are no features
		if( !tracksAll.isEmpty() ) {
			putIntoSrcList();

			associate.setSource(locSrc, featSrc);
			associate.setDestination(locDst, featDst);
			associate.associate();

			updateTrackLocation(associate.getMatches());
		}
	}

	@Override
	public void performSecondPass() {
		if( tracksAll.isEmpty() )
			return;

		associate.setSource(locSrc, featSrc);
		associate.setDestination(locDst, featDst);
		associate.associate();

		updateTrackLocation(associate.getMatches());
	}

	@Override
	public void finishTracking() {
		if( tracksAll.isEmpty() )
			return;

		// Update the track state using association information
		updateTrackState(matches);

		// add unassociated to the list
		for( int i = 0; i < tracksAll.size(); i++ ) {
			if( !isAssociated[i] )
				tracksInactive.add(tracksAll.get(i));
		}
	}

	/**
	 * Update each track's location only and not its description.  Update the active list too
	 */
	protected void updateTrackLocation( FastQueue<AssociatedIndex> matches ) {
		tracksActive.clear();
		for( int i = 0; i < matches.size; i++ ) {
			AssociatedIndex indexes = matches.data[i];
			PointTrack track = tracksAll.get(indexes.src);
			Point2D_F64 loc = locDst.data[indexes.dst];
			track.set(loc.x, loc.y);
			tracksActive.add(track);
		}
	}

	@Override
	public void setHint( double pixelX , double pixelY , PointTrack track ) {
		track.x = pixelX;
		track.y = pixelY;
	}
}
