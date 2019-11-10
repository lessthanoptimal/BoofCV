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
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.Planar;

/**
 * TODO fill in
 *
 * @author Peter Abeles
 */
public class SgmCostAggregation {

	// TODO compute forward then reverse. In forward save read cost. In reverse add results to local cache before
	//      adding to aggregated

	// Contains aggregated cost. The image is being used to store a tensor.
	// band = y-axis, x=x-axis, y=depth
	Planar<GrayU16> aggregated = new Planar<>(GrayU16.class,1,1,2);

	Planar<GrayU16> costYXD;
	int lengthX, lengthY,lengthD;

	// Work stores aggregated cost along a path. Row major (path[i], depth).
	// Size = max path length * lengthD.
	short[] pathWork = new short[0];

	int maxPathsConsidered = 8;

	// Cost applied to small and large changes in the neighborhood
	int penalty1 =1, penalty2 =2;

	public void process( Planar<GrayU16> costYXD ) {
		this.costYXD = costYXD;
		aggregated.reshape(costYXD);
		GImageMiscOps.fill(aggregated,0);

		this.lengthX = costYXD.getHeight();
		this.lengthD = costYXD.getWidth();
		this.lengthY = costYXD.getNumBands();

		this.pathWork = new short[ Math.max(lengthX,lengthY)*lengthD ];

		if( maxPathsConsidered >= 2 ) {
			score(1, 0);
			score(-1, 0);
		}
		if( maxPathsConsidered >= 4 ) {
			score(0, 1);
			score(0, -1);
		}
		if( maxPathsConsidered >= 8 ) {
			score(1, 1);
			score(-1, -1);
			score(-1, 1);
			score(1, -1);
		}
		if( maxPathsConsidered >= 16 ) {
			score(1, 2);
			score(2, 1);
			score(2, -1);
			score(1, -2);
			score(-1, -2);
			score(-2, -1);
			score(-2, 1);
			score(-1, 2);
		}
	}

	void score( int dx , int dy ) {
		if( dx > 0 ) {
			for (int y = 0; y < lengthY; y++) {
				score(0,y,dx,dy);
			}
		} else if( dx < 0 ) {
			for (int y = 0; y < lengthY; y++) {
				score(lengthX-1,y,dx,dy);
			}
		}
		// TODO avoid computing the same cost more than once
		if( dy > 0 ) {
			for (int x = 0; x < lengthX; x++) {
				score(x,0,dx,dy);
			}
		} else if( dy < 0 ) {
			for (int x = 0; x < lengthX; x++) {
				score(x,lengthY-1,dx,dy);
			}
		}
	}

	void score( int x0 , int y0 , int dx , int dy ) {
		// there is no previous score so simply fill
		GrayU16 costXD = costYXD.getBand(y0);
		int idxCost = costXD.getIndex(x0,0);

		for (int d = 0; d < lengthD; d++) {
			pathWork[d] = costXD.data[idxCost+d];
		}

		// Compute the cost of rest of the path recursively
		int lengthPath = computePathLength(x0, y0, dx, dy);
		int x = x0 + dx;
		int y = y0 + dy;

		final int lengthD = this.lengthD;

		for (int i = 1; i < lengthPath; i++) {
			costXD = costYXD.getBand(y);
			idxCost = costXD.getIndex(x,0);

			// Read results from the previous location along the path
			int idxWork = (i-1)*lengthD;

			// find the minimum cost for all D in the previous
			int minCostPrev = Integer.MAX_VALUE;
			for (int d = 0; d < lengthD; d++) {
				int cost = costXD.data[idxCost+d] & 0xFFFF;
				if( cost < minCostPrev )
					minCostPrev = cost;
			}

			// Add penalty
			int minCostPrevTotal = minCostPrev+ penalty2;

			// Score the inner portion of disparity first to avoid bounds checks
			computeCostInnerD(costXD, idxCost, idxWork, minCostPrev, minCostPrevTotal);

			// Now handle the borders
			computeCostBorderD(idxCost,idxWork,0,costXD,minCostPrev);
			computeCostBorderD(idxCost,idxWork,lengthD-1,costXD,minCostPrev);
		}

		saveWorkToAggregated(x0,y0,dx,dy,lengthPath);
	}

	private void computeCostInnerD(GrayU16 costXD, int idxCost, int idxWork, int minCostPrev, int minCostPrevTotal) {
		final int lengthD = this.lengthD;
		idxWork += 1; // start at d=1
		for (int d = 1; d < lengthD-1; d++, idxWork++) {
			int cost = costXD.data[idxCost+d] & 0xFFFF;

			int b = pathWork[idxWork-1]&0xFFFF; // Lr(p-r,d-1)
			int a = pathWork[idxWork  ]&0xFFFF; // Lr(p-r,d)
			int c = pathWork[idxWork+1]&0xFFFF; // Lr(p-r,d+1)

			// Add penalty terms
			b += penalty1;
			c += penalty1;

			// Find the minimum of the three scores
			if( b < a )
				a = b;
			if( c < a )
				a = c;
			if( minCostPrevTotal < c )
				a = minCostPrevTotal;

			// minCostPrev is done to reduce the rate at which the cost increases
			pathWork[idxWork+lengthD] = (short)(cost + a - minCostPrev);
		}
	}

	/**
	 * Copies the work array into the aggregated cost Tensor
	 */
	private void saveWorkToAggregated( int x0 , int y0 , int dx , int dy , int length ) {
		int idxWork = 0;
		int x = x0;
		int y = y0;
		for (int i = 0; i < length; i++) {
			GrayU16 aggrXD = aggregated.getBand(y);

			int idxAggr = aggrXD.getIndex(x,0);
			for (int d = 0; d < lengthD; d++) {
				aggrXD.data[idxAggr++] += pathWork[idxWork++];
			}

			x += dx;
			y += dy;
		}
	}

	private void computeCostBorderD(int idxCost , int idxWork , int d , GrayU16 costXD , int minCostPrev  ) {
		int cost = costXD.data[idxCost+d] & 0xFFFF;

		int a = pathWork[idxWork  ]&0xFFFF; // Lr(p-r,d)
		int b = d > 0 ? pathWork[idxWork-1]&0xFFFF : SgmDisparityCost.MAX_COST; // Lr(p-r,d-1)
		int c = d < lengthD-1 ? pathWork[idxWork+1]&0xFFFF : SgmDisparityCost.MAX_COST; // Lr(p-r,d+1)

		// Add penalty terms
		b += penalty1;
		c += penalty1;

		// Find the minimum of the three scores
		if( b < a )
			a = b;
		if( c < a )
			a = c;
		if( minCostPrev+ penalty2 < c )
			a = minCostPrev+ penalty2;

		// minCostPrev is done to reduce the rate at which the cost increases
		pathWork[idxWork+lengthD+d] = (short)(cost + a - minCostPrev);
	}

	private int computePathLength(int x0, int y0, int dx, int dy) {
		int length = dx != 0 ? (lengthX-x0)/Math.abs(dx) : Integer.MAX_VALUE;
		return Math.min(length, dy != 0 ? (lengthY-y0)/Math.abs(dy) : Integer.MAX_VALUE );
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

	public int getMaxPathsConsidered() {
		return maxPathsConsidered;
	}

	public void setMaxPathsConsidered(int maxPathsConsidered) {
		this.maxPathsConsidered = maxPathsConsidered;
	}
}
