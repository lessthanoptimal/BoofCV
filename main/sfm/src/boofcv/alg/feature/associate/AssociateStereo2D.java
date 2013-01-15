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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.struct.FastQueue;
import boofcv.struct.GrowQueue_I32;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import boofcv.struct.feature.TupleDesc;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

/**
 * Association for a stereo pair where the source is the left camera and the destination is the right camera. Pixel
 * coordinates are rectified and associations are only considered if the two observations are within tolerance
 * of each other along the y-axis and that the left observation's x-coordinate is greater than the right.
 *
 * @author Peter Abeles
 */
public class AssociateStereo2D<Desc extends TupleDesc> implements AssociateDescription2D<Desc>
{
	// computes match score between two descriptions
	private ScoreAssociation<Desc> scorer;
	// storage for associated features
	private FastQueue<AssociatedIndex> matches = new FastQueue<AssociatedIndex>(AssociatedIndex.class,true);
	// stores indexes of unassociated source features
	private GrowQueue_I32 unassociated = new GrowQueue_I32();

	// tolerance used for epipolar checks
	private double tolerance;

	// maximum allowed score when matching descriptors
	private double scoreThreshold = Double.MAX_VALUE;

	// transformations used to rectify image coordinates
	private PointTransform_F64 leftImageToRect;
	private PointTransform_F64 rightImageToRect;

	// stores rectified coordinates of observations in left and right images
	private FastQueue<Point2D_F64> locationLeft = new FastQueue<Point2D_F64>(Point2D_F64.class,true);
	private FastQueue<Point2D_F64> locationRight = new FastQueue<Point2D_F64>(Point2D_F64.class,true);
	// stores reference to descriptions in left and right iamges
	private FastQueue<Desc> descriptionsLeft;
	private FastQueue<Desc> descriptionsRight;

	public AssociateStereo2D( ScoreAssociation<Desc> scorer , double locationTolerance , Class<Desc> descType ) {
		this.scorer = scorer;
		this.tolerance = locationTolerance;
		descriptionsLeft = new FastQueue<Desc>(descType,false);
		descriptionsRight = new FastQueue<Desc>(descType,false);
	}


	public void setCalibration(StereoParameters param) {
		IntrinsicParameters left = param.getLeft();
		IntrinsicParameters right = param.getRight();


		// compute rectification
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();
		Se3_F64 leftToRight = param.getRightToLeft().invert(null);

		// original camera calibration matrices
		DenseMatrix64F K1 = PerspectiveOps.calibrationMatrix(left, null);
		DenseMatrix64F K2 = PerspectiveOps.calibrationMatrix(right, null);

		rectifyAlg.process(K1,new Se3_F64(),K2,leftToRight);

		// rectification matrix for each image
		DenseMatrix64F rect1 = rectifyAlg.getRect1();
		DenseMatrix64F rect2 = rectifyAlg.getRect2();

		leftImageToRect = RectifyImageOps.transformPixelToRect_F64(param.left, rect1);
		rightImageToRect = RectifyImageOps.transformPixelToRect_F64(param.right, rect2);
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
		unassociated.reset();

		for( int i = 0; i < locationLeft.size; i++ ) {
			Point2D_F64 left = locationLeft.get(i);
			Desc descLeft = descriptionsLeft.get(i);

			int bestIndex = -1;
			double bestScore = scoreThreshold;

			for( int j = 0; j < locationRight.size; j++ ) {
				Point2D_F64 right = locationRight.get(j);

				if( Math.abs(left.y-right.y) < tolerance && left.x-tolerance > right.x ) {
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
				unassociated.push(i);
			}
		}
	}

	@Override
	public FastQueue<AssociatedIndex> getMatches() {
		return matches;
	}

	@Override
	public GrowQueue_I32 getUnassociatedSource() {
		return unassociated;
	}

	@Override
	public void setThreshold(double score) {
		this.scoreThreshold = score;
	}

	@Override
	public MatchScoreType getScoreType() {
		return scorer.getScoreType();
	}
}
