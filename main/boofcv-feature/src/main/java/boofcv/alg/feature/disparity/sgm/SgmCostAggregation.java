/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.sgm;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.concurrency.IntRangeObjectConsumer;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.Planar;
import org.ddogleg.struct.FastQueue;

/**
 * TODO fill in
 *
 * <p>[1] Hirschmuller, Heiko. "Stereo processing by semiglobal matching and mutual information."
 * IEEE Transactions on pattern analysis and machine intelligence 30.2 (2007): 328-341.</p>
 *
 * @author Peter Abeles
 */
public class SgmCostAggregation {

	// NOTATION
	// Images are being used to store tensors. This results in some weirdness between comment and code
	// Lr(x,d) = Lr.get(d,x)
	// Lr(y,x,d) = Lr.getBand(y).get(d,x)

	// TODO compute forward then reverse. In forward save read cost. In reverse add results to local cache before
	//      adding to aggregated

	protected SgmHelper helper = new SgmHelper();

	// Contains aggregated cost. The image is being used to store a tensor.
	// band = y-axis, x=x-axis, y=depth
	Planar<GrayU16> aggregated = new Planar<>(GrayU16.class,1,1,2);

	Planar<GrayU16> costYXD;
	// Length of original image. x = col, y = rows, d = disparity range
	int lengthX,lengthY,lengthD;

	/**
	 * Number of paths to consider. 1 to 16 is valid
	 */
	int pathsConsidered = 8;

	// Cost applied to small and large changes in the neighborhood
	int penalty1 =200, penalty2 =2000;

	// The minimum disparity that will be considered.
	int minDisparity;

	// Book keeping for concurrency
	FastQueue<Trajectory> trajectories = new FastQueue<>(Trajectory.class,Trajectory::new);
	FastQueue<WorkSpace> workspace = new FastQueue<>(WorkSpace.class, WorkSpace::new);
	ComputeBlock computeBlock = new ComputeBlock();

	/**
	 * Aggregates the cost in the tensor `costYXD`. From the aggregated cost the disparity can be computed.
	 * The input is a tensor 3D stored in a planar image. The name refers to the order data is stored. (Y,X,D) =
	 * (band,row,col). D = disparity and is relative to some minimum disparity.
	 *
	 * @param costYXD Cost for all possible combinations of x,y,d in input image.
	 */
	public void process( Planar<GrayU16> costYXD , int disparityMin ) {
		init(costYXD);
		this.minDisparity = disparityMin;

		if( pathsConsidered >= 1 ) {
			scoreDirection(1, 0);
		}
		if( pathsConsidered >= 2 ) {
			scoreDirection(-1, 0);
		}
		if( pathsConsidered >= 4 ) {
			scoreDirection(0, 1);
			scoreDirection(0, -1);
		}
		if( pathsConsidered >= 8 ) {
			scoreDirection(1, 1);
			scoreDirection(-1, -1);
			scoreDirection(-1, 1);
			scoreDirection(1, -1);
		}
		if( pathsConsidered >= 16 ) {
			scoreDirection(1, 2);
			scoreDirection(2, 1);
			scoreDirection(2, -1);
			scoreDirection(1, -2);
			scoreDirection(-1, -2);
			scoreDirection(-2, -1);
			scoreDirection(-2, 1);
			scoreDirection(-1, 2);
		}
	}

	/**
	 * Initializes data structures
	 */
	void init(Planar<GrayU16> costYXD) {
		if( pathsConsidered < 1 || pathsConsidered > 16 )
			throw new IllegalArgumentException("Number of paths must be 1 to 16, inclusive. Not "+ pathsConsidered);
		this.costYXD = costYXD;
		aggregated.reshape(costYXD);
		GImageMiscOps.fill(aggregated,0);

		this.lengthX = costYXD.getHeight();
		this.lengthD = costYXD.getWidth();
		this.lengthY = costYXD.getNumBands();

		helper.configure(lengthX, minDisparity,lengthD);
		workspace.resize(1);
	}

	/**
	 * Scores all possible paths for this given direction and add it to the aggregated cost.
	 *
	 * Concurrency note: It's safe to write to the aggregated score without synchronization since only one
	 * path in the block below will touch a pixel.
	 */
	void scoreDirection(int dx , int dy ) {

		// Create a list of paths it will score
		trajectories.reset();
		if( dx > 0 ) {
			for (int y = 0; y < lengthY; y++) {
				trajectories.grow().set(0,y,dx,dy);
			}
		} else if( dx < 0 ) {
			for (int y = 0; y < lengthY; y++) {
				trajectories.grow().set(lengthX-1,y,dx,dy);
			}
		}
		if( dy > 0 ) {
			int x0 = dx > 0 ? 1 : 0;
			int x1 = dx < 0 ? lengthX-1 : lengthX;
			for (int x = x0; x < x1; x++) {
				trajectories.grow().set(x,0,dx,dy);
			}
		} else if( dy < 0 ) {
			int x0 = dx > 0 ? 1 : 0;
			int x1 = dx < 0 ? lengthX-1 : lengthX;
			for (int x = x0; x < x1; x++) {
				trajectories.grow().set(x,lengthY-1,dx,dy);
			}
		}

		if( BoofConcurrency.USE_CONCURRENT ) {
			BoofConcurrency.loopBlocks(0,trajectories.size,1,workspace,computeBlock);
		} else {
			WorkSpace w= workspace.get(0);
			w.checkSize();
			for (int i = 0; i < trajectories.size; i++) {
				Trajectory t = trajectories.get(i);
				scorePath(t.x0, t.y0, t.dx, t.dy, w.workCostLr);
			}
		}
	}

	private class ComputeBlock implements IntRangeObjectConsumer<WorkSpace> {
		@Override
		public void accept(WorkSpace workspace, int minInclusive, int maxExclusive) {
			workspace.checkSize();

			for (int i = minInclusive; i < maxExclusive; i++) {
				Trajectory t = trajectories.get(i);
				scorePath(t.x0,t.y0,t.dx,t.dy,workspace.workCostLr);
			}
		}
	}

	/**
	 * Computes the score for all points along the path specified by (x0,y0,dx,dy).
	 *
	 * There's a change from the paper. In equation 13 it says to subtract min[k] Lr(p-r,k)
	 * I believe that's a mistake. The upper limit for the cost in the paper
	 * is only true if you change it to  min[k] Lr(p,k). This is only a serious problem with 16 paths.
	 *
	 * It also improve performance when max disparity is less than lengthD. That's because the cost
	 * grows, giving the cost near the localLengthD an advantage since it hasn't had time to
	 * grow all the way.
	 *
	 * @param x0 start x-axis
	 * @param y0 start y-axis
	 * @param dx step x-axis
	 * @param dy step y-axis
	 */
	void scorePath(int x0 , int y0 , int dx , int dy , short[] workCostLr) {
		// there is no previous disparity score so simply fill the cost for d=0

		{
			int minCost = Integer.MAX_VALUE;
			final GrayU16 costXD = costYXD.getBand(y0);
			final int idxCost = costXD.getIndex(0,x0);   // C(0,0)
			final int localLengthD = helper.localDisparityRangeLeft(x0);
			for (int d = 0; d < localLengthD; d++) {
				int v = costXD.data[idxCost + d]&0xFFFF; // Lr(0,d) = C(0,d)
				workCostLr[d] = (short)v;
				minCost = Math.min(minCost,v);
			}
			// The modified equation 13. Cost Equation 12 - min[k] Lr(p,k)
			for (int d = 0; d < localLengthD; d++) {
				workCostLr[d] = (short)((workCostLr[d]&0xFFFF)-minCost);
			}
		}

		// Compute the cost of rest of the path recursively
		int lengthPath = computePathLength(x0, y0, dx, dy);
		int x = x0 + dx;
		int y = y0 + dy;

		for (int i = 1; i < lengthPath; i++, x += dx, y += dy) {

			// Index of cost for C(y,p0+i,0)
			final GrayU16 costXD = costYXD.getBand(y);
			final int idxCost = costXD.getIndex(0,x);
			final int lengthLocalD = helper.localDisparityRangeLeft(x);

			// Index for the previous cost in this path
			int idxLrPrev = (i-1)*lengthD;

			// Score the inner portion of disparity first to avoid bounds checks
			computeCostInnerD(costXD, idxCost, idxLrPrev, lengthLocalD, workCostLr);

			// Now handle the borders at d=0 and d=N-1
			computeCostBorderD(idxCost,idxLrPrev,0,costXD, lengthLocalD, workCostLr);
			computeCostBorderD(idxCost,idxLrPrev,lengthLocalD-1,costXD, lengthLocalD, workCostLr);

			// The modified equation 13. Cost Equation 12 - min[k] Lr(p,k)
			int minCost = Integer.MAX_VALUE;
			int idxLr = i*lengthD;
			for (int d = 0; d < lengthLocalD; d++) {
				minCost = Math.min(minCost,workCostLr[idxLr+d]&0xFFFF);
			}
			for (int d = 0; d < lengthLocalD; d++) {
				workCostLr[idxLr+d] = (short)((workCostLr[idxLr+d]&0xFFFF)- minCost);
			}

		}

		saveWorkToAggregated(x0,y0,dx,dy,lengthPath, workCostLr);
	}

	/**
	 * Adds the work LR onto the aggregated cost Tensor, which is the sum of all paths
	 */
	void saveWorkToAggregated( int x0 , int y0 , int dx , int dy , int length , short[] workCostLr) {
		int x = x0;
		int y = y0;
		for (int i = 0; i < length; i++, x += dx, y += dy) {
			final int localLengthD = helper.localDisparityRangeLeft(x);
			GrayU16 aggrXD = aggregated.getBand(y);

			int idxWork = i*lengthD;
			int idxAggr = aggrXD.getIndex(0,x);   // Lr(i,0)
			for (int d = 0; d < localLengthD; d++, idxAggr++, idxWork++) {
				if( (aggrXD.data[idxAggr]&0xFFFF) + (workCostLr[idxWork]&0xFFFF) > 2*Short.MAX_VALUE )
					throw new RuntimeException("EGads");
				aggrXD.data[idxAggr] = (short)((aggrXD.data[idxAggr]&0xFFFF) + (workCostLr[idxWork]&0xFFFF));
			}
//			for (int d = localLengthD; d < lengthD; d++, idxAggr++, idxWork++) {
//				aggrXD.data[idxAggr] = (short)(aggrXD.data[idxAggr] + SgmDisparityCost.MAX_COST);
//			}
		}
	}

	/**
	 * Computes the cost according to equation (12) in the paper in the inner portion where border checks are not
	 * needed.
	 *
	 * Note: With the modified equation 13 the minimum previous cost is always zero.
	 *
	 * @param idxLrPrev index of work at the previous location in the path, i.e. Lr(p-r,0)
	 */
	void computeCostInnerD(GrayU16 costXD, int idxCost, int idxLrPrev, int lengthLocalD , short[] workCostLr) {
		idxLrPrev += 1; // start at d=1
		for (int d = 1; d < lengthLocalD-1; d++, idxLrPrev++) {
			int cost = costXD.data[idxCost+d] & 0xFFFF; // C(p,d)

			int b = workCostLr[idxLrPrev-1]&0xFFFF; // Lr(p-r,d-1)
			int a = workCostLr[idxLrPrev  ]&0xFFFF; // Lr(p-r,d  )
			int c = workCostLr[idxLrPrev+1]&0xFFFF; // Lr(p-r,d+1)

			// Add penalty terms
			b += penalty1;
			c += penalty1;

			// Find the minimum of the three scores
			if( b < a )
				a = b;
			if( c < a )
				a = c;
			if( penalty2 < a )
				a = penalty2;

			// minCostPrev is done to reduce the rate at which the cost increases
			if( cost + a > Short.MAX_VALUE )
				throw new RuntimeException("Egads");
			workCostLr[idxLrPrev+this.lengthD] = (short)(cost + a);
			// Lr(p,d) = above
		}
	}

	/**
	 * Computes the aggregate cost but with bounds checks to ensure it doesn't sample outside of the
	 * disparity change.
	 * @param idxCost Index of value in costXD
	 * @param idxLrPrev Index of value in workCostLr
	 * @param d disparity value being considered
	 * @param costXD cost in X-D plane
	 */
	void computeCostBorderD(int idxCost , int idxLrPrev , int d , GrayU16 costXD , int lengthLocalD , short[] workCostLr) {
		int cost = costXD.data[idxCost+d] & 0xFFFF;  // C(p,d)

		// Sample previously computed aggregate costs with bounds checking
		int a = workCostLr[idxLrPrev+d]&0xFFFF; // Lr(p-r,d)
		int b = d > 0 ? workCostLr[idxLrPrev+d-1]&0xFFFF : SgmDisparityCost.MAX_COST; // Lr(p-r,d-1)
		int c = d < lengthLocalD-1 ? workCostLr[idxLrPrev+d+1]&0xFFFF : SgmDisparityCost.MAX_COST; // Lr(p-r,d+1)

		// Add penalty terms
		b += penalty1;
		c += penalty1;

		// Find the minimum of the three scores
		if( b < a )
			a = b;
		if( c < a )
			a = c;
		if( penalty2 < a )
			a = penalty2;

		// minCostPrev is done to reduce the rate at which the cost increases. It has potential for overflow otherwise
		if( cost + a > Short.MAX_VALUE )
			throw new RuntimeException("Egads");
		workCostLr[idxLrPrev+this.lengthD+d] = (short)(cost + a);
	}

	/**
	 * Computes the number of image pixel are in the path. The path is defined with an initial
	 * pixel coordinate (always on the border) and the step in (x,y).
	 *
	 * If (x0,y0) is at the right or bottom border then it should be x0=width and/or y0=height,
	 */
	int computePathLength(int x0, int y0, int dx, int dy) {
		int pathX = pathLength(x0,dx,lengthX);
		int pathY = pathLength(y0,dy,lengthY);
		return Math.min(pathX,pathY);
	}

	/**
	 * Returns number of steps it takes to reach the end of the path
	 */
	private int pathLength( int t0 , int step , int length ) {
		if( step > 0 )
			return (length-t0+step/2)/step;
		else if( step < 0 )
			return (t0+1-step/2)/(-step);
		else
			return Integer.MAX_VALUE;
	}

	class WorkSpace {
		// Stores aggregated cost along a single path. Row major (path[i], depth).
		// Size = (max path length) * lengthD.
		// After computed it is then added to 'aggregated' once done.
		// This is actually why a work space is required and aggregated isn't used directly
		short[] workCostLr = new short[0];

		public void checkSize() {
			int N = Math.max(lengthX,lengthY)*lengthD;
			if( workCostLr.length != N )
				this.workCostLr = new short[ N ];
		}
	}

	private static class Trajectory {
		public int x0,y0,dx,dy;

		public void set(int x0, int y0, int dx , int dy ) {
			this.x0 = x0;
			this.y0 = y0;
			this.dx = dx;
			this.dy = dy;
		}
	}


	public Planar<GrayU16> getAggregated() {
		return aggregated;
	}

	public int getPenalty1() {
		return penalty1;
	}

	public void setPenalty1(int penalty1) {
		this.penalty1 = penalty1;
	}

	public int getPenalty2() {
		return penalty2;
	}

	public void setPenalty2(int penalty2) {
		this.penalty2 = penalty2;
	}

	public int getPathsConsidered() {
		return pathsConsidered;
	}

	public void setPathsConsidered(int pathsConsidered) {
		this.pathsConsidered = pathsConsidered;
	}
}
