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

package boofcv.alg.tracker.tld;

import org.ddogleg.struct.FastQueue;

/**
 * Performs non-maximum suppression on high confidence detected regions.  A graph of connected regions is constructed.
 * Two regions are considered connected if their overlap is above a threshold.  A region is considered a local maximum
 * if it has a score higher than all its neighbors.  A weighted average is computed using all regions connected to
 * the local maximum.
 *
 * NOTE: This is a completely different non-maximum algorithm from what was described in the paper.  The algorithm
 * described in the paper only approximates non-maximum suppression.
 *
 * @author Peter Abeles
 */
public class TldNonMaximalSuppression {

	// cut off for connecting two nodes
	private double connectionThreshold;

	// connection graph
	private FastQueue<Connections> conn = new FastQueue<>(Connections.class, true);

	// used for computing the overlap between two regions
	private TldHelperFunctions helper = new TldHelperFunctions();

	/**
	 * Configures non-maximum suppression
	 *
	 * @param connectionThreshold Two regions are considered connected of their overlap is &ge; to this value.
	 *                               0 to 1.0. A value of 0.5 is recommended
	 */
	public TldNonMaximalSuppression(double connectionThreshold) {
		this.connectionThreshold = connectionThreshold;
	}

	/**
	 * Finds local maximums from the set of provided regions
	 *
	 * @param regions Set of high confidence regions for target
	 * @param output Output after non-maximum suppression
	 */
	public void process( FastQueue<TldRegion> regions , FastQueue<TldRegion> output ) {

		final int N = regions.size;

		// set all connections to be a local maximum initially
		conn.growArray(N);
		for( int i = 0; i < N; i++ ) {
			conn.data[i].reset();
		}

		// Create the graph of connected regions and mark which regions are local maximums
		for( int i = 0; i < N; i++ ) {
			TldRegion ra = regions.get(i);
			Connections ca = conn.data[i];

			for( int j = i+1; j < N; j++ ) {
				TldRegion rb = regions.get(j);
				Connections cb = conn.data[j];

				// see if they are connected
				double overlap = helper.computeOverlap(ra.rect,rb.rect);
				if( overlap < connectionThreshold ) {
					continue;
				}

				// connect the two and check for strict maximums
				ca.maximum &= ra.confidence > rb.confidence;
				cb.maximum &= rb.confidence > ra.confidence;
				ra.connections++;
				rb.connections++;
			}
		}

		// Compute the output from local maximums.
		for( int i = 0; i < N; i++ ) {
			TldRegion ra = regions.get(i);
			Connections ca = conn.data[i];

			if( ca.maximum ) {
				TldRegion o = output.grow();
				o.connections = ra.connections;
				o.confidence = ra.confidence;
				o.rect.set(ra.rect);
			} else if( ra.connections == 0 ) {
				System.out.println("Not a maximum but has zero connections?");
			}
		}
	}

	public FastQueue<Connections> getConnections() {
		return conn;
	}

	/**
	 * Contains connection information for a specific rectangular region
	 */
	public static class Connections
	{
		// if true then it's a local maximum
		boolean maximum;

		public void reset() {
			maximum = true;
		}
	}
}
