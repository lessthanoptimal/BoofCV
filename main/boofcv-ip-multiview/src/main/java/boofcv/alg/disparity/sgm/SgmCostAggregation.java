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

package boofcv.alg.disparity.sgm;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.Planar;
import org.ddogleg.struct.DogArray;
import pabeles.concurrency.GrowArray;
import pabeles.concurrency.IntRangeObjectConsumer;

/**
 * <p>
 * Aggregates the cost along different paths to compute the final cost. Cost is summed up inside of a tensor
 * of size H*W*D with 16-bit unsigned elements. At most 16 directions can be considered. In general
 * more paths that are considered the smoother the disparity will be. The cost of a single path is done using
 * a dynamic programming approach. This step is the major step that makes SGM what it is.
 * </p>
 *
 * <p>
 * See [1] for details, but the cost for each element along a path is specified as follows:<br>
 * a = Lr(p-r,d  )<br>
 * b = Lr(p-r,d-1) + penalty1<br>
 * c = Lr(p-r,d+1) + penalty1<br>
 * <br>
 * cost = min(penalty2,b,c) + a<br>
 * </p>
 * <p></p>Lr(p,d) is the cost along the path at 'p' and disparity 'd'. penalty1 in the penalty associated with
 * a small change in disparity and penalty2 is a large change in disparity</p>
 *
 * <p>The equation above has been modified from what is stated in [1]. One of the equations is likely to have a
 * type-o in it because their formula doesn't have the stated properties. A simple modification prevents the cost
 * variables from overflowing.</p>
 *
 * <p>[1] Hirschmuller, Heiko. "Stereo processing by semiglobal matching and mutual information."
 * IEEE Transactions on pattern analysis and machine intelligence 30.2 (2007): 328-341.</p>
 *
 * @author Peter Abeles
 * @see SgmDisparityCost
 * @see SgmDisparitySelector
 * @see SgmStereoDisparity
 */
@SuppressWarnings({"NullAway.Init"})
public class SgmCostAggregation {

	// NOTATION
	// Images are being used to store tensors. This results in some weirdness between comment and code
	// Lr(x,d) = Lr.get(d,x)
	// Lr(y,x,d) = Lr.getBand(y).get(d,x)

	// TODO compute forward then reverse. In forward save read cost. In reverse add results to local cache before
	//      adding to aggregated. This could reduce cache misses
	protected SgmHelper helper = new SgmHelper();

	// Contains aggregated cost. The image is being used to store a tensor.
	// band = y-axis, x=x-axis, y=depth
	Planar<GrayU16> aggregated = new Planar<>(GrayU16.class, 1, 1, 2);

	Planar<GrayU16> costYXD;
	// Length of original image. x = col, y = rows, d = disparity range
	int lengthX, lengthY, lengthD;
	// If disparityMin > 0 then the first disparityMin x elements have no score and are skipped
	int effectiveLengthX;
	// The minimum disparity that will be considered.
	int disparityMin;

	/**
	 * Number of paths to consider. 1 to 16 is valid
	 */
	int pathsConsidered = 8;

	// Cost applied to small and large changes in the neighborhood
	int penalty1 = 200, penalty2 = 2000;

	// Book keeping for concurrency
	DogArray<Trajectory> trajectories = new DogArray<>(Trajectory.class, Trajectory::new);
	GrowArray<WorkSpace> workspace = new GrowArray<>(WorkSpace::new);
	ComputeBlock computeBlock = new ComputeBlock();

	/**
	 * Configures the minimum disparity. The range is specified implicitly by the cost tensor.
	 *
	 * @param disparityMin The minimum disparity that will be considered
	 */
	public void configure( int disparityMin ) {
		this.disparityMin = disparityMin;
	}

	/**
	 * Aggregates the cost in the tensor `costYXD`. From the aggregated cost the disparity can be computed.
	 * The input is a tensor 3D stored in a planar image. The name refers to the order data is stored. (Y,X,D) =
	 * (band,row,col). D = disparity and is relative to some minimum disparity.
	 *
	 * @param costYXD Cost for all possible combinations of x,y,d in input image.
	 */
	public void process( Planar<GrayU16> costYXD ) {
		init(costYXD);

		if (pathsConsidered >= 1) {
			scoreDirection(1, 0);
		}
		if (pathsConsidered >= 2) {
			scoreDirection(-1, 0);
		}
		if (pathsConsidered >= 4) {
			scoreDirection(0, 1);
			scoreDirection(0, -1);
		}
		if (pathsConsidered >= 8) {
			scoreDirection(1, 1);
			scoreDirection(-1, -1);
			scoreDirection(-1, 1);
			scoreDirection(1, -1);
		}
		if (pathsConsidered >= 16) {
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
	void init( Planar<GrayU16> costYXD ) {
		if (pathsConsidered < 1 || pathsConsidered > 16)
			throw new IllegalArgumentException("Number of paths must be 1 to 16, inclusive. Not " + pathsConsidered);
		this.costYXD = costYXD;
		aggregated.reshape(costYXD);
		GImageMiscOps.fill(aggregated, 0);

		this.lengthX = costYXD.getHeight();
		this.lengthD = costYXD.getWidth();
		this.lengthY = costYXD.getNumBands();
		this.effectiveLengthX = this.lengthX - disparityMin;

		helper.configure(lengthX, disparityMin, lengthD);
		workspace.resize(1);
	}

	/**
	 * Scores all possible paths for this given direction and add it to the aggregated cost.
	 *
	 * Concurrency note: It's safe to write to the aggregated score without synchronization since only one
	 * path in the block below will touch a pixel.
	 */
	void scoreDirection( int dx, int dy ) {

		// Create a list of paths it will score
		trajectories.reset();
		if (dx > 0) {
			for (int y = 0; y < lengthY; y++) {
				trajectories.grow().set(0, y, dx, dy);
			}
		} else if (dx < 0) {
			for (int y = 0; y < lengthY; y++) {
				trajectories.grow().set(effectiveLengthX - 1, y, dx, dy);
			}
		}
		if (dy > 0) {
			int x0 = 0, x1 = effectiveLengthX;
			if (dx > 0) x0 += 1;
			if (dx < 0) x1 -= 1;
			for (int x = x0; x < x1; x++) {
				trajectories.grow().set(x, 0, dx, dy);
			}
		} else if (dy < 0) {
			int x0 = 0, x1 = effectiveLengthX;
			if (dx > 0) x0 += 1;
			if (dx < 0) x1 -= 1;
			for (int x = x0; x < x1; x++) {
				trajectories.grow().set(x, lengthY - 1, dx, dy);
			}
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			BoofConcurrency.loopBlocks(0, trajectories.size, 1, workspace, computeBlock);
		} else {
			WorkSpace w = workspace.get(0);
			w.checkSize();
			for (int i = 0; i < trajectories.size; i++) {
				Trajectory t = trajectories.get(i);
				scorePath(t.x0, t.y0, t.dx, t.dy, w.workCostLr);
			}
		}
	}

	private class ComputeBlock implements IntRangeObjectConsumer<WorkSpace> {
		@Override
		public void accept( WorkSpace workspace, int minInclusive, int maxExclusive ) {
			workspace.checkSize();

			for (int i = minInclusive; i < maxExclusive; i++) {
				Trajectory t = trajectories.get(i);
				scorePath(t.x0, t.y0, t.dx, t.dy, workspace.workCostLr);
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
	void scorePath( int x0, int y0, int dx, int dy, short[] workCostLr ) {

		// there is no previous disparity score so simply fill the cost for d=0
		{
			int minCost = Integer.MAX_VALUE;
			final GrayU16 costXD = costYXD.getBand(y0);
			final int idxCost = costXD.getIndex(0, x0);   // C(0,0)
			final int localRangeD = helper.localDisparityRangeLeft(x0 + disparityMin);
			for (int d = 0; d < localRangeD; d++) {
				int v = costXD.data[idxCost + d] & 0xFFFF; // Lr(0,d) = C(0,d)
				workCostLr[d] = (short)v;
				minCost = Math.min(minCost, v);
			}
			// The modified equation 13. Cost Equation 12 - min[k] Lr(p,k)
			for (int d = 0; d < localRangeD; d++) {
				workCostLr[d] = (short)((workCostLr[d] & 0xFFFF) - minCost);
			}
			// In the for loop below it needs the previous cost at index localRangeD[i]-1.
			// If we are on the left side of the image then localRangeD[i] < localRangeD[i+1]!
			// That's a problem because if we read localRangeD[i+1]-1 it will be unassigned and have
			// an unknown value! We get around that by just copying the cost at the end.
			if (localRangeD != helper.disparityRange) {
				workCostLr[localRangeD] = workCostLr[localRangeD - 1];
			}
		}

		// Compute the cost of rest of the path recursively
		int lengthPath = computePathLength(x0, y0, dx, dy);
		for (int i = 1, x = x0 + dx, y = y0 + dy; i < lengthPath; i++, x += dx, y += dy) {
			// Index of cost for C(y,p0+i,0)
			final GrayU16 costXD = costYXD.getBand(y);
			final int idxCost = costXD.getIndex(0, x);
			// remember x=0 is really x+disparityMin because the first elements are skipped
			final int localRangeD = helper.localDisparityRangeLeft(x + disparityMin);

			// Index for the previous cost in this path
			int idxLrPrev = (i - 1)*lengthD;

			// Score the inner portion of disparity first to avoid bounds checks
			computeCostInnerD(costXD.data, idxCost, idxLrPrev, localRangeD, workCostLr);

			// Now handle the borders at d=0 and d=N-1
			computeCostBorderD(idxCost, idxLrPrev, 0, costXD, localRangeD, workCostLr);
			computeCostBorderD(idxCost, idxLrPrev, localRangeD - 1, costXD, localRangeD, workCostLr);

			// see comments above for what's going on here
			if (localRangeD != helper.disparityRange) {
				workCostLr[idxLrPrev + lengthD + localRangeD] = workCostLr[idxLrPrev + lengthD + localRangeD - 1];
			}

			// The modified equation 13. Cost Equation 12 - min[k] Lr(p,k)
			int minCost = Integer.MAX_VALUE;
			int idxLr = i*lengthD;
			for (int d = 0; d < localRangeD; d++) {
				minCost = Math.min(minCost, workCostLr[idxLr + d] & 0xFFFF);
			}
			for (int d = 0; d < localRangeD; d++) {
				workCostLr[idxLr + d] = (short)((workCostLr[idxLr + d] & 0xFFFF) - minCost);
			}
		}

		saveWorkToAggregated(x0, y0, dx, dy, lengthPath, workCostLr);
	}

	/**
	 * Adds the work LR onto the aggregated cost Tensor, which is the sum of all paths
	 */
	void saveWorkToAggregated( int x0, int y0, int dx, int dy, int length, short[] workCostLr ) {
		for (int i = 0, x = x0, y = y0; i < length; i++, x += dx, y += dy) {
			final int localLengthD = helper.localDisparityRangeLeft(x + disparityMin);
			GrayU16 aggrXD = aggregated.getBand(y);

			int idxWork = i*lengthD;                // Lr(i,0)
			int idxAggr = aggrXD.getIndex(0, x);  // A(d=0,x)
			for (int d = 0; d < localLengthD; d++, idxAggr++, idxWork++) {
//				if( (aggrXD.data[idxAggr]&0xFFFF) + (workCostLr[idxWork]&0xFFFF) > 2*Short.MAX_VALUE )
//					throw new RuntimeException("Overflowed!");
				aggrXD.data[idxAggr] = (short)((aggrXD.data[idxAggr] & 0xFFFF) + (workCostLr[idxWork] & 0xFFFF));
			}
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
	void computeCostInnerD( final short[] costXD, final int idxCost, int idxLrPrev, final int lengthLocalD, final short[] workCostLr ) {
		final int nextRow = this.lengthD - 1; // idxLrPrev is +1
		final int penalty1 = this.penalty1;
		final int penalty2 = this.penalty2;

		idxLrPrev += 1; // start at d=1

		// initialize the sampling at d=1. elements will be exchanged inside the loop
		int c1 = workCostLr[idxLrPrev - 1] & 0xFFFF;  // Lr(p-r,d-1)
		int c2 = workCostLr[idxLrPrev] & 0xFFFF;  // Lr(p-r,d  )
		idxLrPrev += 1; // avoid extra addition later on

		for (int d = 1; d < lengthLocalD - 1; d++, idxLrPrev++) {
			int cost = costXD[idxCost + d] & 0xFFFF; // C(p,d)

			int c0 = c1; // workCostLr[idxLrPrev-1]&0xFFFF; // Lr(p-r,d-1)
			c1 = c2;     // workCostLr[idxLrPrev  ]&0xFFFF; // Lr(p-r,d  )
			// workCostLr[idxLrPrev+1]&0xFFFF; // Lr(p-r,d+1)
			c2 = workCostLr[idxLrPrev] & 0xFFFF;            // Lr(p-r,d+1)

			// Add penalty terms
			int a = c1;
			int b = c0 + penalty1;
			int c = c2 + penalty1;

			// Find the minimum of the three scores
			if (b < a)
				a = b;
			if (c < a)
				a = c;
			if (penalty2 < a)
				a = penalty2;

//			if( cost + a > Short.MAX_VALUE )
//				throw new RuntimeException("Overflowed!");

			// minCostPrev is done to reduce the rate at which the cost increases
			workCostLr[idxLrPrev + nextRow] = (short)(cost + a);
			// Lr(p,d) = above
		}
	}

	/**
	 * Computes the aggregate cost but with bounds checks to ensure it doesn't sample outside of the
	 * disparity change.
	 *
	 * @param idxCost Index of value in costXD
	 * @param idxLrPrev Index of value in workCostLr
	 * @param d disparity value being considered
	 * @param costXD cost in X-D plane
	 */
	void computeCostBorderD( int idxCost, int idxLrPrev, int d, GrayU16 costXD, int localRangeD, short[] workCostLr ) {
		int cost = costXD.data[idxCost + d] & 0xFFFF;  // C(p,d)

		// Sample previously computed aggregate costs with bounds checking
		int a = workCostLr[idxLrPrev + d] & 0xFFFF; // Lr(p-r,d)
		int b = d > 0 ? workCostLr[idxLrPrev + d - 1] & 0xFFFF : SgmDisparityCost.MAX_COST; // Lr(p-r,d-1)
		int c = d < localRangeD - 1 ? workCostLr[idxLrPrev + d + 1] & 0xFFFF : SgmDisparityCost.MAX_COST; // Lr(p-r,d+1)

		// NOTE: See comments in scorePath() for why 'a' doesn't have problems with 'idxLrPrev+d' going outside
		// the disparity range of the previous step with d = localRangeD-1. In the previous step localRangeD
		// could have had a smaller value

		// Add penalty terms
		b += penalty1;
		c += penalty1;

		// Find the minimum of the three scores
		if (b < a)
			a = b;
		if (c < a)
			a = c;
		if (penalty2 < a)
			a = penalty2;

//		if( cost + a > Short.MAX_VALUE )
//			throw new RuntimeException("Overflowed!");

		// minCostPrev is done to reduce the rate at which the cost increases. It has potential for overflow otherwise
		workCostLr[idxLrPrev + this.lengthD + d] = (short)(cost + a);
	}

	/**
	 * Computes the number of image pixel are in the path. The path is defined with an initial
	 * pixel coordinate (always on the border) and the step in (x,y).
	 *
	 * If (x0,y0) is at the right or bottom border then it should be x0=width and/or y0=height,
	 */
	int computePathLength( int x0, int y0, int dx, int dy ) {
		int pathX = pathLength(x0, dx, effectiveLengthX);
		int pathY = pathLength(y0, dy, lengthY);
		return Math.min(pathX, pathY);
	}

	/**
	 * Returns number of steps it takes to reach the end of the path
	 */
	private int pathLength( int t0, int step, int length ) {
		if (step > 0)
			return (length - t0 + step/2)/step;
		else if (step < 0)
			return (t0 + 1 - step/2)/-step;
		else
			return Integer.MAX_VALUE;
	}

	class WorkSpace {
		// Stores aggregated cost along a single path. Row major (path element 'i', depth 'd').
		// Thus the index of (row=i,col=d) = i*lengthD+d and the total array size is = (max path length) * lengthD.
		// After computed it is then added to 'aggregated' once done.
		// This is actually why a work space is required and aggregated isn't used directly
		short[] workCostLr = new short[0];

		public void checkSize() {
			int N = Math.max(lengthX, lengthY)*lengthD;
			if (workCostLr.length != N)
				this.workCostLr = new short[N];
			// Uncomment the line below to make sure no invalid values are being written over. When the unit tests
			// are run they will fail if everything isn't problem handled correctly
//			Arrays.fill(workCostLr,(short)5000);
		}
	}

	/**
	 * Defines the starting coordinate and direction a trajectory takes.
	 * (x0,y0) is the initial coordinate
	 * (dx,dy) is the direction
	 */
	private static class Trajectory {
		public int x0, y0, dx, dy;

		public void set( int x0, int y0, int dx, int dy ) {
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

	public void setPenalty1( int penalty1 ) {
		this.penalty1 = penalty1;
	}

	public int getPenalty2() {
		return penalty2;
	}

	public void setPenalty2( int penalty2 ) {
		this.penalty2 = penalty2;
	}

	public int getPathsConsidered() {
		return pathsConsidered;
	}

	public void setPathsConsidered( int pathsConsidered ) {
		this.pathsConsidered = pathsConsidered;
	}
}
