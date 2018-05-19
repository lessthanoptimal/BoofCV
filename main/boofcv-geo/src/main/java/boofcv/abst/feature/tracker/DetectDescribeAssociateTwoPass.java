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
public class DetectDescribeAssociateTwoPass<I extends ImageGray<I>, Desc extends TupleDesc>
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

		manager.detectFeatures(input);

		for (int setIndex = 0; setIndex < sets.length; setIndex++) {
			SetTrackInfo<Desc> info = sets[setIndex];

			info.featDst.reset();
			info.locDst.reset();
			manager.getFeatures(setIndex,info.locDst,info.featDst);

			// skip if there are no features
			if( !tracksAll.isEmpty() ) {
				putIntoSrcList(info);

				associate.setSource(info.locSrc, info.featSrc);
				associate.setDestination(info.locDst, info.featDst);
				associate.associate();

				updateTrackLocation(info,associate.getMatches());
			}

		}
	}

	@Override
	public void performSecondPass() {
		if( tracksAll.isEmpty() )
			return;

		boolean setSource = associate2 != associate && !sourceSet2 && sets.length==1;
		if( setSource ) {
			sourceSet2 = true;
		}
		for (int setIndex = 0; setIndex < sets.length; setIndex++) {
			SetTrackInfo<Desc> info = sets[setIndex];
			// minimize the number of times set source is called.  In some implementations of associate this is an
			// expensive operation
			if (setSource) {
				associate.setSource(info.locSrc, info.featSrc);
			}
			associate2.setDestination(info.locDst, info.featDst);
			associate2.associate();

			updateTrackLocation(info,associate2.getMatches());
		}

	}

	@Override
	public void finishTracking() {
		if( tracksAll.isEmpty() )
			return;

		// Update the track state using association information
		tracksActive.clear();

		for (int setIndex = 0; setIndex < sets.length; setIndex++) {
			SetTrackInfo<Desc> info = sets[setIndex];
			updateTrackState(info);

			// add unassociated to the list
			for (int i = 0; i < info.tracks.size(); i++) {
				if (!info.isAssociated[i])
					tracksInactive.add(info.tracks.get(i));
			}
		}
	}

	/**
	 * Update each track's location only and not its description.  Update the active list too
	 */
	protected void updateTrackLocation( SetTrackInfo<Desc> info, FastQueue<AssociatedIndex> matches) {
		info.matches.resize(matches.size);
		for (int i = 0; i < matches.size; i++) {
			info.matches.get(i).set(matches.get(i));
		}

		tracksActive.clear();
		for( int i = 0; i < info.matches.size; i++ ) {
			AssociatedIndex indexes = info.matches.data[i];
			PointTrack track = info.tracks.get(indexes.src);
			Point2D_F64 loc = info.locDst.data[indexes.dst];
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
