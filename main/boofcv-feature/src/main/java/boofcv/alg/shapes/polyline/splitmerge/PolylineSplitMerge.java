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

package boofcv.alg.shapes.polyline.splitmerge;

import boofcv.misc.CircularIndex;
import boofcv.struct.ConfigLength;
import georegression.geometry.UtilPolygons2D_I32;
import georegression.metric.Distance2D_F64;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;
import org.ddogleg.struct.LinkedList;
import org.ddogleg.struct.LinkedList.Element;

import java.util.List;

/**
 * <p>
 * Fits a polyline to a contour by fitting the simplest model (3 sides) and adding more sides to it. The number of sides
 * is increased until the number of sides reaches maxSides  + extraConsider or it already has fit the contour
 * to within the specified precision. It then merges lines together until no more can be merged.
 * </p>
 *
 * <p>
 * When a side is added to the polygon it selects the side in which the score will be improved the most by splitting.
 * The score is computed by computing the euclidean distance a point on the contour is from the line segment it
 * belongs to. Note that distance from a line segment and not a line is found, thus if the closest point is past
 * an end point the end point is used. The final score is the average distance.
 * </p>
 *
 * <p>
 * A set of polylines is found and scored. The best polyline is the one with the best overall score. The overall score is
 * found by summing up the average error across all line segments (sum of segment scores dividing by the number of
 * segments) [1] and adding a fixed penalty for each line segment.
 * Without a penalty the polyline with the largest number of sides will almost always be selected.
 * </p>
 *
 * <p>
 * For a complete description of all parameters see the source code.
 * </p>
 *
 * <p>[1] Note that the score is NOT weighted based on the number of points in a line segment. This was done at one
 * point at produced much worse results.</p>
 *
 * The polyline will always be in counter-clockwise ordering.
 *
 * @author Peter Abeles
 */
public class PolylineSplitMerge {

	// Does the polyline form a loop or are the end points disconnected
	private boolean loops = true;

	// Can it assume the shape is convex? If so it can reject shapes earlier
	private boolean convex = false;

	// maximum number of sides it will consider
	private int maxSides = Integer.MAX_VALUE;
	// minimum number of sides that will be considered for the best polyline
	private int minSides = 3;

	// The minimum length of a side
	private int minimumSideLength = 10;

	// how many corners past the max it will fit a polygon to
	private ConfigLength extraConsider = ConfigLength.relative(1.0,0);

	// When selecting the best model how much is a split penalized
	private double cornerScorePenalty = 0.25;

	// If the score of a side is less than this it is considered a perfect fit and won't be split any more
	private double thresholdSideSplitScore = 0;

	// maximum number of points along a side it will sample when computing a score
	// used to limit computational cost of large contours
	int maxNumberOfSideSamples = 50;

	// If the contour between two corners is longer than this multiple of the distance
	// between the two corners then it will be rejected as not convex
	double convexTest = 2.5;

	// maximum error along any side
	ConfigLength maxSideError = ConfigLength.relative(0.1,3);

	// work space for side score calculation
	private LineSegment2D_F64 line = new LineSegment2D_F64();

	// the corner list that's being built
	LinkedList<Corner> list = new LinkedList<>();
	FastQueue<Corner> corners = new FastQueue<>(Corner.class,true);

	private SplitSelector splitter = new MaximumLineDistance();
	private SplitResults resultsA = new SplitResults();
	private SplitResults resultsB = new SplitResults();

	// List of all the found polylines and their score
	private FastQueue<CandidatePolyline> polylines = new FastQueue<>(CandidatePolyline.class,true);
	private CandidatePolyline bestPolyline;

	// if true that means a fatal error and no polygon can be fit
	private boolean fatalError;

	// storage for results
	ErrorValue sideError = new ErrorValue();

	/**
	 * Process the contour and returns true if a polyline could be found.
	 * @param contour Contour. Must be a ordered in CW or CCW
	 * @return true for success or false if one could not be fit
	 */
	public boolean process(List<Point2D_I32> contour ) {
		// Reset internal book keeping variables
		reset();

		if( loops ) {
			// Reject pathological case
			if (contour.size() < 3)
				return false;

			if (!findInitialTriangle(contour))
				return false;
		} else {
			// Reject pathological case
			if( contour.size() < 2 )
				return false;

			// two end points are the seeds. Plus they can't change
			addCorner(0);
			addCorner(contour.size()-1);
			initializeScore(contour,false);
		}
		savePolyline();

		sequentialSideFit(contour,loops);

		if( fatalError )
			return false;

		int MIN_SIZE = loops ? 3 : 2;

		double bestScore = Double.MAX_VALUE;
		int bestSize = -1;
		for (int i = 0; i < Math.min(maxSides-(MIN_SIZE-1),polylines.size); i++) {
			if( polylines.get(i).score < bestScore ) {
				bestPolyline = polylines.get(i);
				bestScore = bestPolyline.score;
				bestSize = i + MIN_SIZE;
			}
		}

		// There was no good match within the min/max size requirement
		if( bestSize < minSides) {
			return false;
		}

		// make sure all the sides are within error tolerance
		for (int i = 0,j=bestSize-1; i < bestSize; j=i,i++) {
			Point2D_I32 a = contour.get(bestPolyline.splits.get(i));
			Point2D_I32 b = contour.get(bestPolyline.splits.get(j));

			double length = a.distance(b);
			double thresholdSideError = this.maxSideError.compute(length);
			if( bestPolyline.sideErrors.get(i) >= thresholdSideError*thresholdSideError) {
				bestPolyline = null;
				return false;
			}
		}

		return true;
	}

	private void sequentialSideFit(List<Point2D_I32> contour, boolean loops ) {
		// by finding more corners than necessary it can recover from mistakes previously
		int limit = maxSides+extraConsider.computeI(maxSides);
		if( limit <= 0 )limit = contour.size(); // handle the situation where it overflows
		while( list.size() < limit && !fatalError ) {
			if( !increaseNumberOfSidesByOne(contour,loops) ) {
				break;
			}
		}
		// remove corners and recompute scores. If the result is better it will be saved
		while( !fatalError  ) {
			Element<Corner> c = selectCornerToRemove(contour, sideError, loops);
			if( c != null ) {
				removeCornerAndSavePolyline(c, sideError.value);
			} else {
				break;
			}
		}
	}

	private void reset() {
		list.reset();
		corners.reset();
		polylines.reset();
		bestPolyline = null;
		fatalError = false;
	}

	private void printCurrent( List<Point2D_I32> contour ) {
		System.out.print(list.size()+"  Indexes[");
		Element<Corner> e = list.getHead();
		while( e != null ) {
			System.out.print(" "+e.object.index);
			e = e.next;
		}
		System.out.println(" ]");
		System.out.print("   Errors[");
		e = list.getHead();
		while( e != null ) {
			String split = e.object.splitable ? "T" : "F";
			System.out.print(String.format(" %6.1f %1s",e.object.sideError,split));
			e = e.next;
		}
		System.out.println(" ]");
		System.out.print("      Pos[");
		e = list.getHead();
		while( e != null ) {
			Point2D_I32 p = contour.get(e.object.index);
			System.out.print(String.format(" %3d %3d,",p.x,p.y));
			e = e.next;
		}
		System.out.println(" ]");
	}

	/**
	 * Saves the current polyline
	 *
	 * @return true if the polyline is better than any previously saved result false if not and it wasn't saved
	 */
	boolean savePolyline() {
		int N = loops ? 3 : 2;

		// if a polyline of this size has already been saved then over write it
		CandidatePolyline c;
		if( list.size() <= polylines.size+N-1 ) {
			c = polylines.get( list.size()-N );
			// sanity check
			if( c.splits.size != list.size() )
				throw new RuntimeException("Egads saved polylines aren't in the expected order");
		} else {
			c = polylines.grow();
			c.reset();
			c.score = Double.MAX_VALUE;
		}

		double foundScore = computeScore(list,cornerScorePenalty, loops);

		// only save the results if it's an improvement
		if( c.score > foundScore ) {
			c.score = foundScore;
			c.splits.reset();
			c.sideErrors.reset();
			Element<Corner> e = list.getHead();
			double maxSideError = 0;
			while (e != null) {
				maxSideError = Math.max(maxSideError,e.object.sideError);
				c.splits.add(e.object.index);
				c.sideErrors.add(e.object.sideError);
				e = e.next;
			}
			c.maxSideError = maxSideError;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Computes the score for a list
	 */
	static double computeScore( LinkedList<Corner> list , double cornerPenalty , boolean loops ) {
		double sumSides = 0;
		Element<Corner> e = list.getHead();
		Element<Corner> end = loops ? null : list.getTail();
		while( e != end ) {
			sumSides += e.object.sideError;
			e = e.next;
		}

		int numSides = loops ? list.size() : list.size() - 1;

		return sumSides/numSides + cornerPenalty*numSides;
	}

	/**
	 * Select an initial triangle. A good initial triangle is needed. By good it
	 * should minimize the error of the contour from each side
	 */
	boolean findInitialTriangle(List<Point2D_I32> contour) {
		// find the first estimate for a corner
		int cornerSeed = findCornerSeed(contour);

		// see if it can reject the contour immediately
		if( convex ) {
			if( !isConvexUsingMaxDistantPoints(contour,0,cornerSeed))
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
			addCorner(resultsB.index);
		}

		// Select the third corner. Initial triangle will be complete now
		// the third corner will be the one which maximizes the distance from the first two
		int index0 = list.getHead().object.index;
		int index1 = list.getHead().next.object.index;
		int index2 = maximumDistance(contour,index0,index1);
		addCorner(index2);

		// enforce CCW requirement
		ensureTriangleOrder(contour);

		return initializeScore(contour, true);
	}

	/**
	 * Computes the score and potential split for each side
	 * @param contour
	 * @return
	 */
	private boolean initializeScore(List<Point2D_I32> contour , boolean loops ) {
		// Score each side
		Element<Corner> e = list.getHead();
		Element<Corner> end = loops ? null : list.getTail();
		while( e != end ) {
			if (convex && !isSideConvex(contour, e))
				return false;

			Element<Corner> n = e.next;

			double error;
			if( n == null ) {
				error = computeSideError(contour,e.object.index, list.getHead().object.index);
			} else {
				error = computeSideError(contour,e.object.index, n.object.index);
			}
			e.object.sideError = error;
			e = n;
		}

		// Compute what would happen if a side was split
		e = list.getHead();
		while( e != end ) {
			computePotentialSplitScore(contour,e,list.size() < minSides);
			e = e.next;
		}

		return true;
	}

	/**
	 * Make sure the next corner after the head is the closest one to the head
	 */
	void ensureTriangleOrder(List<Point2D_I32> contour ) {
		Element<Corner> e = list.getHead();
		Corner a = e.object;e=e.next;
		Corner b = e.object;e=e.next;
		Corner c = e.object;

		int distB = CircularIndex.distanceP(a.index,b.index,contour.size());
		int distC = CircularIndex.distanceP(a.index,c.index,contour.size());

		if( distB > distC ) {
			list.reset();
			list.pushTail(a);
			list.pushTail(c);
			list.pushTail(b);
		}
	}

	Element<Corner> addCorner( int where ) {
		Corner c = corners.grow();
		c.reset();
		c.index = where;
		list.pushTail(c);
		return list.getTail();
	}

	/**
	 * Increase the number of sides in the polyline. This is done greedily selecting the side which would improve the
	 * score by the most of it was split.
	 * @param contour Contour
	 * @return true if a split was selected and false if not
	 */
	boolean increaseNumberOfSidesByOne(List<Point2D_I32> contour, boolean loops ) {
//		System.out.println("increase number of sides by one. list = "+list.size());
		Element<Corner> selected = selectCornerToSplit(loops);

		// No side can be split
		if( selected == null )
			return false;

		// Update the corner who's side was just split
		selected.object.sideError = selected.object.splitError0;
		// split the selected side and add a new corner
		Corner c = corners.grow();
		c.reset();
		c.index = selected.object.splitLocation;
		c.sideError = selected.object.splitError1;
		Element<Corner> cornerE = list.insertAfter(selected,c);

		// see if the new side could be convex
		if (convex && !isSideConvex(contour, selected))
			return false;
		else {
			// compute the score for sides which just changed
			computePotentialSplitScore(contour, cornerE, list.size() < minSides);
			computePotentialSplitScore(contour, selected, list.size() < minSides);

			// Save the results
//		printCurrent(contour);
			savePolyline();

			return true;
		}
	}

	/**
	 * Checks to see if the side could belong to a convex shape
	 */
	boolean isSideConvex(List<Point2D_I32> contour, Element<Corner> e1) {
		// a conservative estimate for concavity. Assumes a triangle and that the farthest
		// point is equal to the distance between the two corners

		Element<Corner> e2 = next(e1);

		int length = CircularIndex.distanceP(e1.object.index,e2.object.index,contour.size());

		Point2D_I32 p0 = contour.get(e1.object.index);
		Point2D_I32 p1 = contour.get(e2.object.index);

		double d = p0.distance(p1);

		if (length >= d*convexTest) {
			return false;
		}
		return true;
	}

	/**
	 * Selects the best side to split the polyline at.
	 * @return the selected side or null if the score will not be improved if any of the sides are split
	 */
	Element<Corner> selectCornerToSplit( boolean loops ) {
		Element<Corner> selected = null;
		double bestChange = convex ? 0 : -Double.MAX_VALUE;

		// Pick the side that if split would improve the overall score the most
		Element<Corner> e=list.getHead();
		Element<Corner> end = loops ? null : list.getTail();

		while( e != end ) {
			Corner c = e.object;
			if( !c.splitable) {
				e = e.next;
				continue;
			}

			// compute how much better the score will improve because of the split
			double change = c.sideError*2 - c.splitError0 - c.splitError1;
			// it was found that selecting for the biggest change tends to produce better results
			if( change < 0 ) {
				change = -change;
			}
			if( change > bestChange ) {
				bestChange = change;
				selected = e;
			}
			e = e.next;
		}

		return selected;
	}

	/**
	 * Selects the best corner to remove. If no corner was found that can be removed then null is returned
	 * @return The corner to remove. Should only return null if there are 3 sides or less
	 */
	Element<Corner> selectCornerToRemove(List<Point2D_I32> contour , ErrorValue sideError , boolean loops ) {
		if( list.size() <= 3 )
			return null;

		// Pick the side that if split would improve the overall score the most
		Element<Corner> target,end;

		// if it loops any corner can be split. If it doesn't look the end points can't be removed
		if( loops ) {
			target = list.getHead();
			end = null;
		} else {
			target = list.getHead().next;
			end = list.getTail();
		}

		Element<Corner> best = null;
		double bestScore = -Double.MAX_VALUE;

		while( target != end ) {
			Element<Corner> p = previous(target);
			Element<Corner> n = next(target);

			// just contributions of the corners in question
			double before = (p.object.sideError + target.object.sideError)/2.0 + cornerScorePenalty;
			double after = computeSideError(contour, p.object.index, n.object.index);

			if( before-after > bestScore ) {
				bestScore = before-after;
				best = target;
				sideError.value = after;
			}
			target = target.next;
		}

		return best;
	}

	/**
	 * Remove the corner from the current polyline. If the new polyline has a better score than the currently
	 * saved one with the same number of corners save it
	 * @param corner The corner to removed
	 */
	boolean removeCornerAndSavePolyline( Element<Corner> corner, double sideErrorAfterRemoved ) {
		//			System.out.println("removing a corner idx="+target.object.index);
		// Note: the corner is "lost" until the next contour is fit. Not worth the effort to recycle
		Element<Corner> p = previous(corner);

		// go through the hassle of passing in this value instead of recomputing it
		// since recomputing it isn't trivial
		p.object.sideError = sideErrorAfterRemoved;
		list.remove(corner);
		// the line below is commented out because right now the current algorithm will
		// never grow after removing a corner. If this changes in the future uncomment it
//			computePotentialSplitScore(contour,p);
		return savePolyline();
	}

	/**
	 * The seed corner is the point farther away from the first point. In a perfect polygon with no noise this should
	 * be a corner.
	 */
	static int findCornerSeed(List<Point2D_I32> contour ) {
		Point2D_I32 a = contour.get(0);

		int best = -1;
		double bestDistance = -Double.MAX_VALUE;

		for (int i = 1; i < contour.size(); i++) {
			Point2D_I32 b = contour.get(i);

			double d = distanceSq(a,b);
			if( d > bestDistance ) {
				bestDistance = d;
				best = i;
			}
		}

		return best;
	}

	/**
	 * Finds the point in the contour which maximizes the distance between points A
	 * and B.
	 *
	 * @param contour List of all pointsi n the contour
	 * @param indexA Index of point A
	 * @param indexB Index of point B
	 * @return Index of maximal distant point
	 */
	static int maximumDistance(List<Point2D_I32> contour , int indexA , int indexB ) {
		Point2D_I32 a = contour.get(indexA);
		Point2D_I32 b = contour.get(indexB);

		int best = -1;
		double bestDistance = -Double.MAX_VALUE;

		for (int i = 0; i < contour.size(); i++) {
			Point2D_I32 c = contour.get(i);
			// can't sum sq distance because some skinny shapes it maximizes one and not the other
//			double d = Math.sqrt(distanceSq(a,c)) + Math.sqrt(distanceSq(b,c));
			double d = distanceAbs(a,c) + distanceAbs(b,c);
			if( d > bestDistance ) {
				bestDistance = d;
				best = i;
			}
		}

		return best;
	}

	/**
	 * Scores a side based on the sum of Euclidean distance squared of each point along the line. Euclidean squared
	 * is used because its fast to compute
	 *
	 * @param indexA first index. Inclusive
	 * @param indexB last index. Exclusive
	 */
	double computeSideError(List<Point2D_I32> contour , int indexA , int indexB ) {
		assignLine(contour, indexA, indexB, line);

		// don't sample the end points because the error will be zero by definition
		int numSamples;
		double sumOfDistances = 0;
		int length;
		if( indexB >= indexA ) {
			length = indexB-indexA-1;
			numSamples = Math.min(length,maxNumberOfSideSamples);
			for (int i = 0; i < numSamples; i++) {
				int index = indexA+1+length*i/numSamples;
				Point2D_I32 p = contour.get(index);
				sumOfDistances += Distance2D_F64.distanceSq(line,p.x,p.y);
			}
			sumOfDistances /= numSamples;
		} else {
			length = contour.size()-indexA-1 + indexB;
			numSamples = Math.min(length,maxNumberOfSideSamples);
			for (int i = 0; i < numSamples; i++) {
				int where = length*i/numSamples;
				int index = (indexA+1+where)%contour.size();
				Point2D_I32 p = contour.get(index);
				sumOfDistances += Distance2D_F64.distanceSq(line,p.x,p.y);
			}
			sumOfDistances /= numSamples;
		}

		// handle divide by zero error
		if( numSamples > 0 )
			return sumOfDistances;
		else
			return 0;
	}

	/**
	 * Computes the split location and the score of the two new sides if it's split there
	 */
	void computePotentialSplitScore( List<Point2D_I32> contour , Element<Corner> e0 , boolean mustSplit )
	{
		Element<Corner> e1 = next(e0);

		e0.object.splitable = canBeSplit(contour,e0,mustSplit);

		if( e0.object.splitable ) {
			setSplitVariables(contour, e0, e1);
		}
	}

	/**
	 * Selects and splits the side defined by the e0 corner. If convex a check is performed to
	 * ensure that the polyline will be convex still.
	 */
	void setSplitVariables(List<Point2D_I32> contour, Element<Corner> e0, Element<Corner> e1) {

		int distance0 = CircularIndex.distanceP(e0.object.index, e1.object.index, contour.size());

		int index0 = CircularIndex.plusPOffset(e0.object.index,minimumSideLength,contour.size());
		int index1 = CircularIndex.minusPOffset(e1.object.index,minimumSideLength,contour.size());

		splitter.selectSplitPoint(contour, index0, index1, resultsA);

		// if convex only perform the split if it would result in a convex polygon
		if( convex ) {
			Point2D_I32 a = contour.get(e0.object.index);
			Point2D_I32 b = contour.get(resultsA.index);
			Point2D_I32 c = contour.get(next(e0).object.index);

			if (UtilPolygons2D_I32.isPositiveZ(a, b, c)) {
				e0.object.splitable = false;
				return;
			}
		}

		// see if this would result in a side that's too small
		int dist0 = CircularIndex.distanceP(e0.object.index,resultsA.index, contour.size());
		if( dist0 < minimumSideLength || (contour.size()-dist0) < minimumSideLength ) {
			throw new RuntimeException("Should be impossible");
		}

		// this function is only called if splitable is set to true so no need to set it again
		e0.object.splitLocation = resultsA.index;
		e0.object.splitError0 = computeSideError(contour, e0.object.index, resultsA.index);
		e0.object.splitError1 = computeSideError(contour, resultsA.index, e1.object.index);

		if( e0.object.splitLocation >= contour.size() )
			throw new RuntimeException("Egads");
	}

	/**
	 * Determines if the side can be split again. A side can always be split as long as
	 * it's &ge; the minimum length or that the side score is larger the the split threshold
	 *
	 * @param e0 The side which is to be tested to see if it can be split
	 * @param mustSplit if true this will force it to split even if the error would prevent it from splitting
	 * @return true if it can be split or false if not
	 */
	boolean canBeSplit( List<Point2D_I32> contour, Element<Corner> e0 , boolean mustSplit ) {
		Element<Corner> e1 = next(e0);

		// NOTE: The contour is passed in but only the size of the contour matters. This was done to prevent
		//       changing the signature if the algorithm was changed later on.
		int length = CircularIndex.distanceP(e0.object.index, e1.object.index, contour.size());

		// needs to be <= to prevent it from trying to split a side less than 1
		// times two because the two new sides would have to have a length of at least min
		if (length <= 2*minimumSideLength) {
			return false;
		}

		// threshold is greater than zero ti prevent it from saying it can split a perfect side
		return mustSplit || e0.object.sideError > thresholdSideSplitScore;
	}

	/**
	 * Returns the next corner in the list
	 */
	Element<Corner> next( Element<Corner> e ) {
		if( e.next == null ) {
			return list.getHead();
		} else {
			return e.next;
		}
	}

	/**
	 * Returns the previous corner in the list
	 */
	Element<Corner> previous( Element<Corner> e ) {
		if( e.previous == null ) {
			return list.getTail();
		} else {
			return e.previous;
		}
	}

	/**
	 * If point B is the point farthest away from A then by definition this means no other point can be farther
	 * away.If the shape is convex then the line integration from A to B or B to A cannot be greater than 1/2
	 * a circle. This test makes sure that the line integral meets the just described constraint and is thus
	 * convex.
	 *
	 * NOTE: indexA is probably the top left point in the contour, since that's how most contour algorithm scan
	 * but this isn't known for sure. If it was known you could make this requirement tighter.
	 *
	 * @param contour Contour points
	 * @param indexA index of first point
	 * @param indexB index of second point, which is the farthest away from A.
	 * @return if it passes the sanity check
	 */
	static boolean isConvexUsingMaxDistantPoints(List<Point2D_I32> contour , int indexA , int indexB )
	{
		double d = Math.sqrt(distanceSq(contour.get(indexA),contour.get(indexB)));

		// conservative upper bounds would be 1/2 a circle, including interior side.
		int maxAllowed = (int)((Math.PI+1)*d+0.5);

		int length0 = CircularIndex.distanceP(indexA,indexB,contour.size());
		int length1 = CircularIndex.distanceP(indexB,indexA,contour.size());

		return length0 <= maxAllowed && length1 <= maxAllowed;
	}

	/**
	 * Using double prevision here instead of int due to fear of overflow in very large images
	 */
	static double distanceSq( Point2D_I32 a , Point2D_I32 b ) {
		double dx = b.x-a.x;
		double dy = b.y-a.y;

		return dx*dx + dy*dy;
	}

	static double distanceAbs( Point2D_I32 a , Point2D_I32 b ) {
		double dx = b.x-a.x;
		double dy = b.y-a.y;

		return Math.abs(dx) + Math.abs(dy);
	}

	/**
	 * Assigns the line so that it passes through points A and B.
	 */
	public static void assignLine(List<Point2D_I32> contour, int indexA, int indexB, LineParametric2D_F64 line) {
		Point2D_I32 endA = contour.get(indexA);
		Point2D_I32 endB = contour.get(indexB);

		line.p.x = endA.x;
		line.p.y = endA.y;
		line.slope.x = endB.x-endA.x;
		line.slope.y = endB.y-endA.y;
	}

	public static void assignLine(List<Point2D_I32> contour, int indexA, int indexB, LineSegment2D_F64 line) {
		Point2D_I32 endA = contour.get(indexA);
		Point2D_I32 endB = contour.get(indexB);

		line.a.set(endA.x,endA.y);
		line.b.set(endB.x,endB.y);
	}

	public FastQueue<CandidatePolyline> getPolylines() {
		return polylines;
	}

	/**
	 * Returns the polyline with the best score or null if it failed to fit a polyline
	 */
	public CandidatePolyline getBestPolyline() {
		return bestPolyline;
	}

	public void setLoops(boolean loops) {
		this.loops = loops;
	}

	public boolean isLoops() {
		return loops;
	}

	/**
	 * Storage for results from selecting where to split a line
	 */
	static class SplitResults
	{
		public int index;
		public double score;
	}

	/**
	 * Corner in the polyline. The side that this represents is this corner and the next in the list
	 */
	public static class Corner
	{
		public int index;
		public double sideError;
		// if this side was to be split this is where it would be split and what the scores
		// for the new sides would be
		public int splitLocation;
		public double splitError0, splitError1;

		// if a side can't be split (e.g. too small or already perfect)
		public boolean splitable;

		public void reset() {
			index = -1;
			sideError = -1;
			splitLocation = -1;
			splitError0 = splitError1 = -1;
			splitable = true;
		}
	}

	public static class CandidatePolyline
	{
		public GrowQueue_I32 splits = new GrowQueue_I32();
		public double score;
		public double maxSideError;
		public GrowQueue_F64 sideErrors = new GrowQueue_F64();

		public void reset() {
			splits.reset();
			sideErrors.reset();
			score = Double.NaN;
			maxSideError = Double.NaN;
		}
	}

	static class ErrorValue {
		public double value;
	}

	public boolean isConvex() {
		return convex;
	}

	public void setConvex(boolean convex) {
		this.convex = convex;
	}

	public int getMaxSides() {
		return maxSides;
	}

	public void setMaxSides(int maxSides) {
		this.maxSides = maxSides;
	}

	public int getMinimumSideLength() {
		return minimumSideLength;
	}

	public void setMinimumSideLength(int minimumSideLength) {
		if( minimumSideLength <= 0 )
			throw new IllegalArgumentException("Minimum length must be at least 1");
		this.minimumSideLength = minimumSideLength;
	}

	public double getCornerScorePenalty() {
		return cornerScorePenalty;
	}

	public void setCornerScorePenalty(double cornerScorePenalty) {
		this.cornerScorePenalty = cornerScorePenalty;
	}

	public double getThresholdSideSplitScore() {
		return thresholdSideSplitScore;
	}

	public void setThresholdSideSplitScore(double thresholdSideSplitScore) {
		this.thresholdSideSplitScore = thresholdSideSplitScore;
	}

	public int getMaxNumberOfSideSamples() {
		return maxNumberOfSideSamples;
	}

	public void setMaxNumberOfSideSamples(int maxNumberOfSideSamples) {
		this.maxNumberOfSideSamples = maxNumberOfSideSamples;
	}

	public void setSplitter(SplitSelector splitter) {
		this.splitter = splitter;
	}

	public int getMinSides() {
		return minSides;
	}

	public void setMinSides(int minSides) {
		this.minSides = minSides;
	}

	public ConfigLength getExtraConsider() {
		return extraConsider;
	}

	public void setExtraConsider( ConfigLength extraConsider) {
		this.extraConsider = extraConsider;
	}

	public double getConvexTest() {
		return convexTest;
	}

	public void setConvexTest(double convexTest) {
		this.convexTest = convexTest;
	}

	public ConfigLength getMaxSideError() {
		return maxSideError;
	}

	public void setMaxSideError(ConfigLength maxSideError) {
		this.maxSideError = maxSideError;
	}
}

