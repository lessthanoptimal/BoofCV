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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import boofcv.struct.feature.TupleDesc;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Association for a stereo pair where the source is the left camera and the destination is the right camera. Pixel
 * coordinates are rectified and associations are only considered if the two observations are within tolerance
 * of each other along the y-axis and that the left observation's x-coordinate is greater than the right.
 *
 * @author Peter Abeles
 */
public class AssociateStereo2D<Desc extends TupleDesc>
		extends StereoConsistencyCheck
		implements AssociateDescription2D<Desc>
{
	// computes match score between two descriptions
	private ScoreAssociation<Desc> scorer;
	// storage for associated features
	private FastQueue<AssociatedIndex> matches = new FastQueue<>(AssociatedIndex.class, true);
	// stores indexes of unassociated source features
	private GrowQueue_I32 unassociatedSrc = new GrowQueue_I32();
	// creates a list of unassociated features from the list of matches
	private FindUnassociated unassociated = new FindUnassociated();

	// maximum allowed score when matching descriptors
	private double scoreThreshold = Double.MAX_VALUE;


	// stores rectified coordinates of observations in left and right images
	private FastQueue<Point2D_F64> locationLeft = new FastQueue<>(Point2D_F64.class, true);
	private FastQueue<Point2D_F64> locationRight = new FastQueue<>(Point2D_F64.class, true);
	// stores reference to descriptions in left and right iamges
	private FastQueue<Desc> descriptionsLeft;
	private FastQueue<Desc> descriptionsRight;

	public AssociateStereo2D( ScoreAssociation<Desc> scorer , double locationTolerance , Class<Desc> descType )
	{
		super(locationTolerance,locationTolerance);
		this.scorer = scorer;
		descriptionsLeft = new FastQueue<>(descType, false);
		descriptionsRight = new FastQueue<>(descType, false);
	}

	/**
	 * Converts location into rectified coordinates and saved a reference to the description.
	 */
	@Override
	public void setSource(FastQueue<Point2D_F64> location, FastQueue<Desc> descriptions) {
		locationLeft.reset();
		for( int i = 0; i < location.size; i++ ) {
			Point2D_F64 orig = location.get(i);
			Point2D_F64 rectified = locationLeft.grow();
			leftImageToRect.compute(orig.x,orig.y,rectified);
		}
		this.descriptionsLeft = descriptions;
	}


	/**
	 * Converts location into rectified coordinates and saved a reference to the description.
	 */
	@Override
	public void setDestination(FastQueue<Point2D_F64> location, FastQueue<Desc> descriptions) {
		locationRight.reset();
		for( int i = 0; i < location.size; i++ ) {
			Point2D_F64 orig = location.get(i);
			Point2D_F64 rectified = locationRight.grow();
			rightImageToRect.compute(orig.x,orig.y,rectified);
		}
		this.descriptionsRight = descriptions;
	}

	@Override
	public void associate() {

		matches.reset();
		unassociatedSrc.reset();

		for( int i = 0; i < locationLeft.size; i++ ) {
			Point2D_F64 left = locationLeft.get(i);
			Desc descLeft = descriptionsLeft.get(i);

			int bestIndex = -1;
			double bestScore = scoreThreshold;

			for( int j = 0; j < locationRight.size; j++ ) {
				Point2D_F64 right = locationRight.get(j);

				if( checkRectified(left,right) ) {
					double dist = scorer.score(descLeft, descriptionsRight.get(j));
					if( dist < bestScore ) {
						bestScore = dist;
						bestIndex = j;
					}
				}
			}

			if( bestIndex >= 0 ) {
				matches.grow().setAssociation(i,bestIndex,bestScore);
			} else {
				unassociatedSrc.push(i);
			}
		}
	}

	@Override
	public FastQueue<AssociatedIndex> getMatches() {
		return matches;
	}

	@Override
	public GrowQueue_I32 getUnassociatedSource() {
		return unassociatedSrc;
	}

	@Override
	public GrowQueue_I32 getUnassociatedDestination() {
		return unassociated.checkDestination(matches,locationRight.size);
	}

	@Override
	public void setThreshold(double score) {
		this.scoreThreshold = score;
	}

	@Override
	public MatchScoreType getScoreType() {
		return scorer.getScoreType();
	}

	@Override
	public boolean uniqueSource() {
		return true;
	}

	@Override
	public boolean uniqueDestination() {
		return false;
	}
}
