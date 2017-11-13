/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.shapes.polyline;

import boofcv.misc.CircularIndex;
import georegression.metric.Distance2D_F64;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I8;
import org.ddogleg.struct.LinkedList;

import java.util.List;

/**
 * Fits a polyline to a contour by fitting the simplest model and adding more sides to it. First it always finds
 * an approximation of a triangle which minimizes the error. A side is added to the current polyline by finding
 * the side that when split will reduce the error by the largest amount. This is repeated until no sides can be
 * split or the maximum number of sides has been reached.
 *
 * TODO Sampling of a side
 *
 * TODO how sides are scored
 *
 *
 *
 * @author Peter Abeles
 */
public class PolylineSimpleToComplex {
	// Can it assume the shape is convex? If so it can reject shapes earlier
	boolean convex;

	// maximum number of sides it will consider
	int maxSides;

	// The minimum length of a side
	int minimumSideLength;

	// When selecting the best model how much is a split penalized
	double cornerScorePenalty;

	// If the score of a side is less than this it is considered a perfect fit and won't be split any more
	double perfectSideScore;

	// maximum number of points along a side it will sample when computing a score
	// used to limit computational cost of large contours
	int maxNumberOfSideSamples;
	// work space for side score calculation
	LineParametric2D_F64 line = new LineParametric2D_F64();

	// the corner list that's being built
	LinkedList<Corner> list = new LinkedList<>();
	FastQueue<Corner> corners = new FastQueue<>(Corner.class,true);

	SplitSelector splitter = new MaximumLineDistance();
	SplitResults resultsA = new SplitResults();
	SplitResults resultsB = new SplitResults();

	// List of all the found polylines and their score
	FastQueue<CandidatePolyline> polylines = new FastQueue<>(CandidatePolyline.class,true);
	CandidatePolyline bestPolyline;

	public PolylineSimpleToComplex(boolean convex,
								   int maxSides,
								   int minimumSideLength,
								   double cornerScorePenalty,
								   double perfectSideScore,
								   int maxNumberOfSideSamples) {
		this.convex = convex;
		this.maxSides = maxSides;
		this.minimumSideLength = minimumSideLength;
		this.cornerScorePenalty = cornerScorePenalty;
		this.perfectSideScore = perfectSideScore;
		this.maxNumberOfSideSamples = maxNumberOfSideSamples;
	}

	public boolean process(List<Point2D_I32> contour ) {
		list.reset();
		corners.reset();
		polylines.reset();
		bestPolyline = null;

		if( contour.size() < 3 )
			return false;

		if( !findInitialTriangle(contour) )
			return false;
		savePolyline();

		for (int numSides = 4; numSides < maxSides; numSides++) {
			if( !increaseNumberOfSidesByOne(contour) )
				break;
			savePolyline();
		}

		bestPolyline = polylines.get(0);
		for (int i = 1; i < polylines.size; i++) {
			if( polylines.get(i).score < bestPolyline.score ) {
				bestPolyline = polylines.get(i);
			}
		}

		return true;
	}

	/**
	 * Saves the current polyline
	 */
	private void savePolyline() {
		CandidatePolyline c = polylines.grow();
		c.splits.reset();

		double averageScore = 0;
		LinkedList.Element<Corner> e = list.getHead();
		while( e != null ) {
			averageScore += e.object.sideScore;
			e = e.next;

			c.splits.add( e.object.index );
		}

		c.score = averageScore/list.size() + cornerScorePenalty*list.size();
	}

	private boolean findInitialTriangle(List<Point2D_I32> contour) {
		// find the first estimate for a corner
		int cornerSeed = findCornerSeed(contour);

		// see if it can reject the contour immediately
		if( convex ) {
			if( !sanityCheckConvex(contour,0,cornerSeed))
				return false;
		}

		// Select the second corner.
		splitter.selectSplitPoint(contour,0,cornerSeed,resultsA);
		splitter.selectSplitPoint(contour,cornerSeed,0,resultsB);

		if( splitter.compareScore(resultsA.score,resultsB.score) >= 0 ) {
			addCorner(resultsA.index);
			addCorner(cornerSeed);
		} else {
			addCorner(cornerSeed);
			addCorner(resultsA.index);
		}

		// Select the third corner. Initial triangle will be complete now
		int index0 = list.getHead().object.index;
		int index1 = list.getHead().next.object.index;

		splitter.selectSplitPoint(contour,index0,index1,resultsA);
		addCorner(resultsA.index);

		// TODO recompute the seed corner? maximum distance from some arbitrary point is kinda arbitrary

		// Score each side
		LinkedList.Element<Corner> e = list.getHead();
		while( e != null ) {
			LinkedList.Element<Corner> n = e.next;
			double score;
			if( n == null ) {
				score = scoreSide(contour,e.object.index, list.getHead().object.index);
			} else {
				score = scoreSide(contour,e.object.index, n.object.index);
			}
			e.object.sideScore = score;
			e = n;
		}

		// Compute what would happen if a side was split
		e = list.getHead();
		while( e != null ) {
			computePotentialSplitScore(contour,e);
			e = e.next;
		}

		return true;
	}

	private void addCorner( int where ) {
		Corner c = corners.grow();
		c.index = where;
		list.pushTail(c);
	}

	/**
	 * Increase the number of sides in the polyline. This is done greedily selecting the side which would improve the
	 * score by the most of it was split.
	 * @param contour Contour
	 * @return true if a split was selected and false if not
	 */
	private boolean increaseNumberOfSidesByOne(List<Point2D_I32> contour) {
		LinkedList.Element<Corner> selected = null;
		double bestChange = 0;

		// Pick the side that if split would improve the overall score the most
		LinkedList.Element<Corner> e = list.getHead();
		while( e != null ) {
			Corner c = e.object;
			if( !c.splitable)
				continue;

			// compute how much better the score will improve because of the split
			double change = c.sideScore-(c.splitScore0+c.splitScore1)/2.0;
			if( change > bestChange ) {
				bestChange = change;
				selected = e;
			}
			e = e.next;
		}

		// No side can be split
		if( selected == null )
			return false;

		// split the selected side
		Corner c = corners.grow();
		c.index = selected.object.splitLocation;
		list.insertAfter(selected,c);

		// compute the score for sides which just changed
		computePotentialSplitScore(contour,selected);
		computePotentialSplitScore(contour,selected.next);

		return true;
	}

	/**
	 * The seed corner is the point farther away from the first point. In a perfect polygon with no noise this should
	 * be a corner.
	 */
	static int findCornerSeed(List<Point2D_I32> contour ) {
		Point2D_I32 a = contour.get(0);

		int best = -1;
		double bestDistance = Double.MAX_VALUE;

		for (int i = 1; i < contour.size(); i++) {
			Point2D_I32 b = contour.get(i);

			double d = distanceSq(a,b);
			if( d < bestDistance ) {
				bestDistance = d;
				best = i;
			}
		}

		return best;
	}

	/**
	 * Scores a side based on the sum of Euclidean distance squared of each point along the line. Euclidean squared
	 * is used because its fast to compute
	 */
	private double scoreSide( List<Point2D_I32> contour , int indexA , int indexB ) {
		assignLine(contour, indexA, indexB, line);

		int numSamples;
		double sumOfDistances = 0;
		if( indexB >= indexA ) {
			int length = indexB-indexA-2;
			numSamples = Math.min(length,maxNumberOfSideSamples);
			for (int i = 0; i < numSamples; i++) {
				int index = indexA+1+length*i/(numSamples-1);
				Point2D_I32 p = contour.get(index);
				sumOfDistances += Distance2D_F64.distanceSq(line,p.x,p.y);
			}
			sumOfDistances /= (indexB-indexA-1);
		} else {
			int length = contour.size()-indexB + indexA-2;
			numSamples = Math.min(length,maxNumberOfSideSamples);
			for (int i = 0; i < numSamples; i++) {
				int where = length*i/(numSamples-1);
				int index = (indexB+1+where)%contour.size();
				Point2D_I32 p = contour.get(index);
				sumOfDistances += Distance2D_F64.distanceSq(line,p.x,p.y);
			}
		}
		sumOfDistances /= numSamples;

		return sumOfDistances;
	}

	/**
	 * Computes the split location and the score of the two new sides if it's split there
	 */
	void computePotentialSplitScore( List<Point2D_I32> contour , LinkedList.Element<Corner> e0 )
	{
		LinkedList.Element<Corner> e1 = next(e0);

		e0.object.splitable = canBeSplit(contour,e0);

		if( e0.object.splitable ) {
			splitter.selectSplitPoint(contour, e0.object.index, e1.object.index, resultsA);
			e0.object.splitLocation = resultsA.index;
			e0.object.splitScore0 = scoreSide(contour, e0.object.index, resultsA.index);
			e0.object.splitScore1 = scoreSide(contour, resultsA.index, e1.object.index);
		}
	}

	/**
	 * Determines if the side can be split again
	 */
	boolean canBeSplit( List<Point2D_I32> contour , LinkedList.Element<Corner> e0 ) {
		LinkedList.Element<Corner> e1 = next(e0);

		int length = CircularIndex.distance(e0.object.index,e1.object.index,contour.size());
		if( length < minimumSideLength ) {
			return false;
		}

		return !(e0.object.sideScore <= perfectSideScore);
	}

	/**
	 * Returns the next corner in the list
	 */
	LinkedList.Element<Corner> next( LinkedList.Element<Corner> e ) {
		if( e.next == null ) {
			return list.getHead();
		} else {
			return e.next;
		}
	}

	/**
	 * For convex shapes no point along the contour can be farther away from A is from B. Thus the maximum number
	 * of points can't exceed a 1/2 circle.
	 *
	 * NOTE: indexA is probably the top left point in the contour, since that's how most contour algorithm scan
	 * but this isn't known for sure. If it was known you could make this requirement tighter.
	 * @param contour Contour points
	 * @param indexA index of first point
	 * @param indexB index of second point
	 * @return if it passes the sanity check
	 */
	static boolean sanityCheckConvex( List<Point2D_I32> contour , int indexA , int indexB )
	{
		double d = Math.sqrt(distanceSq(contour.get(indexA),contour.get(indexB)));

		int maxAllowed = (int)(2*Math.PI*d+0.5);

		if( indexB-indexA > maxAllowed )
			return false;
		if( indexA + contour.size()-indexB > maxAllowed )
			return false;

		return true;
	}

	/**
	 * Using double prevision here instead of int due to fear of overflow in very large images
	 */
	static double distanceSq( Point2D_I32 a , Point2D_I32 b ) {
		double dx = b.x-a.x;
		double dy = b.y-a.y;

		return dx*dx + dy*dy;
	}

	private static void assignLine(List<Point2D_I32> contour, int indexA, int indexB, LineParametric2D_F64 line) {
		Point2D_I32 endA = contour.get(indexA);
		Point2D_I32 endB = contour.get(indexB);

		line.p.x = endA.x;
		line.p.y = endA.y;
		line.slope.x = endB.x-endA.x;
		line.slope.y = endB.y-endA.y;
	}

	public static class MaximumLineDistance implements SplitSelector {

		LineParametric2D_F64 line = new LineParametric2D_F64();

		@Override
		public void selectSplitPoint(List<Point2D_I32> contour, int indexA, int indexB, SplitResults results) {
			assignLine(contour, indexA, indexB, line);

			if( indexB >= indexA ) {
				results.index = indexA;
				results.score = 0;
				for (int i = indexA+1; i < indexB; i++) {
					Point2D_I32 p = contour.get(i);
					double distanceSq = Distance2D_F64.distanceSq(line,p.x,p.y);

					if( distanceSq > results.score ) {
						results.score = distanceSq;
						results.index = i;
					}
				}
			} else {
				results.index = indexA;
				results.score = 0;
				int distance = contour.size()-indexB + indexA-1;
				for (int i = 1; i < distance; i++) {
					int index = (indexB+i)%contour.size();
					Point2D_I32 p = contour.get(index);
					double distanceSq = Distance2D_F64.distanceSq(line,p.x,p.y);

					if( distanceSq > results.score ) {
						results.score = distanceSq;
						results.index = index;
					}
				}
			}
		}

		@Override
		public int compareScore(double scoreA, double scoreB) {
			if( scoreA > scoreB )
				return 1;
			else if( scoreA < scoreB )
				return -1;
			else
				return 0;
		}
	}

	/**
	 * Interface for splitting a line.
	 */
	public interface SplitSelector {
		void selectSplitPoint( List<Point2D_I32> contour , int indexA , int indexB , SplitResults results );

		int compareScore( double scoreA , double scoreB );
	}

	public FastQueue<CandidatePolyline> getPolylines() {
		return polylines;
	}

	public CandidatePolyline getBestPolyline() {
		return bestPolyline;
	}

	/**
	 * Storage for results from selecting where to split a line
	 */
	private static class SplitResults
	{
		public int index;
		public double score;
	}

	/**
	 * Corner in the polyline. The side that this represents is this corner and the next in the list
	 */
	private static class Corner
	{
		public int index;
		public double sideScore;
		// if this side was to be split this is where it would be split and what the scores
		// for the new sides would be
		public int splitLocation;
		public double splitScore0,splitScore1;

		// if a side can't be split (e.g. too small or already perfect)
		public boolean splitable;
	}

	public static class CandidatePolyline
	{
		public GrowQueue_I8 splits = new GrowQueue_I8();
		public double score;
	}

}

