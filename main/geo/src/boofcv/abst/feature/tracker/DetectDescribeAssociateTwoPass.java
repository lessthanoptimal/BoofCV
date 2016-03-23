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

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

/**
 * Changes behavior of {@link DetectDescribeAssociate} so that it conforms to the {@link PointTrackerTwoPass} interface.
 * It can now take hints for where tracks might appear in the image.   If possible
 * {@link AssociateDescription2D#setSource(org.ddogleg.struct.FastQueue, org.ddogleg.struct.FastQueue)} will only be called once
 * on the second pass.
 *
 * @author Peter Abeles
 */
public class DetectDescribeAssociateTwoPass<I extends ImageGray, Desc extends TupleDesc>
	extends DetectDescribeAssociate<I,Desc> implements PointTrackerTwoPass<I>
{
	// associate used in the second pass
	AssociateDescription2D<Desc> associate2;
	// has source been set in associate for the second pass
	boolean sourceSet2;

	/**
	 * Configure the tracker.  The parameters associate and associate2 can be the same instance.
	 *
	 * @param manager Feature manager
	 * @param associate Association algorithm for the first pass
	 * @param associate2 Association algorithm for the second pass
	 * @param updateDescription Should descriptions be updated? Typically false
	 */
	public DetectDescribeAssociateTwoPass(DdaFeatureManager<I, Desc> manager,
										  AssociateDescription2D<Desc> associate,
										  AssociateDescription2D<Desc> associate2,
										  boolean updateDescription)
	{
		super(manager, associate, updateDescription);
		this.associate2 = associate2;
	}

	@Override
	public void process( I input ) {
		sourceSet2 = false;
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

		// minimize the number of times set source is called.  In some implementations of associate this is an
		// expensive operation
		if( associate2 != associate && !sourceSet2 ) {
			sourceSet2 = true;
			associate.setSource(locSrc, featSrc);
		}
		associate2.setDestination(locDst, featDst);
		associate2.associate();

		updateTrackLocation(associate2.getMatches());
	}

	@Override
	public void finishTracking() {
		if( tracksAll.isEmpty() )
			return;

		// Update the track state using association information
		tracksActive.clear();
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
		this.matches = matches;
	}

	@Override
	public void setHint( double pixelX , double pixelY , PointTrack track ) {
		track.x = pixelX;
		track.y = pixelY;
	}
}
