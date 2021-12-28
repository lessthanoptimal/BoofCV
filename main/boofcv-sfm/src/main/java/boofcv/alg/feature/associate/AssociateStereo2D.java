/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;

/**
 * Association for a stereo pair where the source is the left camera and the destination is the right camera. Pixel
 * coordinates are rectified and associations are only considered if the two observations are within tolerance
 * of each other along the y-axis and that the left observation's x-coordinate is greater than the right.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class AssociateStereo2D<Desc extends TupleDesc<Desc>>
		extends StereoConsistencyCheck
		implements AssociateDescription2D<Desc> {
	// computes match score between two descriptions
	private final ScoreAssociation<Desc> scorer;
	// storage for associated features
	private final DogArray<AssociatedIndex> matches = new DogArray<>(AssociatedIndex::new);
	// stores indexes of unassociated source features
	private final DogArray_I32 unassociatedSrc = new DogArray_I32();
	// creates a list of unassociated features from the list of matches
	private final FindUnassociated unassociated = new FindUnassociated();

	// maximum allowed score when matching descriptors
	private double scoreThreshold = Double.MAX_VALUE;

	// stores rectified coordinates of observations in left and right images
	private final DogArray<Point2D_F64> locationLeft = new DogArray<>(Point2D_F64::new);
	private final DogArray<Point2D_F64> locationRight = new DogArray<>(Point2D_F64::new);
	// stores reference to descriptions in left and right images
	private FastAccess<Desc> descriptionsLeft;
	private FastAccess<Desc> descriptionsRight;

	public AssociateStereo2D( ScoreAssociation<Desc> scorer, double locationTolerance ) {
		super(locationTolerance, locationTolerance);
		this.scorer = scorer;
	}

	@Override
	public void initialize( int imageWidth, int imageHeight ) {}

	/**
	 * Converts location into rectified coordinates and saved a reference to the description.
	 */
	@Override
	public void setSource( FastAccess<Point2D_F64> location, FastAccess<Desc> descriptions ) {
		locationLeft.reset();
		for (int i = 0; i < location.size; i++) {
			Point2D_F64 orig = location.get(i);
			Point2D_F64 rectified = locationLeft.grow();
			leftImageToRect.compute(orig.x, orig.y, rectified);
		}
		this.descriptionsLeft = descriptions;
	}

	/**
	 * Converts location into rectified coordinates and saved a reference to the description.
	 */
	@Override
	public void setDestination( FastAccess<Point2D_F64> location, FastAccess<Desc> descriptions ) {
		locationRight.reset();
		for (int i = 0; i < location.size; i++) {
			Point2D_F64 orig = location.get(i);
			Point2D_F64 rectified = locationRight.grow();
			rightImageToRect.compute(orig.x, orig.y, rectified);
		}
		this.descriptionsRight = descriptions;
	}

	@Override public Class<Desc> getDescriptionType() {
		return scorer.getDescriptorType();
	}

	@Override
	public void associate() {

		matches.reset();
		unassociatedSrc.reset();

		for (int i = 0; i < locationLeft.size; i++) {
			Point2D_F64 left = locationLeft.get(i);
			Desc descLeft = descriptionsLeft.get(i);

			int bestIndex = -1;
			double bestScore = scoreThreshold;

			for (int j = 0; j < locationRight.size; j++) {
				Point2D_F64 right = locationRight.get(j);

				if (checkRectified(left, right)) {
					double dist = scorer.score(descLeft, descriptionsRight.get(j));
					if (dist < bestScore) {
						bestScore = dist;
						bestIndex = j;
					}
				}
			}

			if (bestIndex >= 0) {
				matches.grow().setTo(i, bestIndex, bestScore);
			} else {
				unassociatedSrc.push(i);
			}
		}
	}

	@Override
	public DogArray<AssociatedIndex> getMatches() {
		return matches;
	}

	@Override
	public DogArray_I32 getUnassociatedSource() {
		return unassociatedSrc;
	}

	@Override
	public DogArray_I32 getUnassociatedDestination() {
		return unassociated.checkDestination(matches, locationRight.size);
	}

	@Override
	public void setMaxScoreThreshold( double score ) {
		this.scoreThreshold = score < 0.0 ? Double.MAX_VALUE : score;
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
