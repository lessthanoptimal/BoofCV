/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation.fb04;

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSInt32;
import org.ddogleg.sorting.QuickSortObj_F32;
import org.ddogleg.sorting.SortableParameter_F32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.Collections;

/**
 * <p>
 * Fast image segmentation algorithm which uses a graph based heuristic.  The weight of all the edges in the image
 * is computed first and sorted from smallest to largest.  Two pixels are connected using a 4-connect rule.  The
 * weight of an edge is the F-norm of the difference between the colors of the two pixels it connects.  It then
 * iterates until convergence where it joins two regions together if an edge has a weight &le; the maximum weight
 * in the region (paper says max weight of MST, see comments in code why this is miss leading). For more details
 * see [1].
 * </p>

 * <p>
 * One difference from the original is that Gaussian blur is not applied to the input image by default.  That
 * should be done prior to the image being passed in.
 * </p>
 *
 * <P>
 * NOTE: Region ID's in output image will NOT be sequential.  You need to call {@link #getRegionId()} to find
 * out what the ID's are.
 * </P>
 *
 * <p>
 * [1] Felzenszwalb, Pedro F., and Daniel P. Huttenlocher.
 * "Efficient graph-based image segmentation." International Journal of Computer Vision 59.2 (2004): 167-181.
 * </p>
 *
 * @author Peter Abeles
 */
// TODO optimize sort
public class SegmentFelzenszwalb04<T extends ImageBase> {

	// tuning parameter.  Determines the number of segments.  Larger number means larger
	private float K;

	// the minimum region size.  Regions smaller than this are merged into larger ones
	private int minimumSize;

	// Storage for the disjoint-set forest.  Same data structure as 'output', but renamed for convenience.
	// Value stored in each pixel refers to the parent vertex.  A root vertex contains a reference for itself
	protected ImageSInt32 graph;

	// Function that computes the weight for each edge
	private ComputeEdgeWeights<T> computeWeights;

	private QuickSortObj_F32 sorter = new QuickSortObj_F32();
	// storage for edges so that they can be recycled on the next call
	private FastQueue<Edge> edges = new FastQueue<Edge>(Edge.class,true);
	// list of edges which were not matched to anything.  used to merge small regions
	private FastQueue<Edge> edgesNotMatched = new FastQueue<Edge>(Edge.class,false);
	// Size of each region
	protected GrowQueue_I32 regionSize = new GrowQueue_I32();
	// This is equivalent to Int(C) + tau(C) in Equation 4.
	// NOTE: Is the maximum weight in the MST really weight of the edge causing the merge?  Maybe I'm missing
	// something, but it seems trivial to find counter examples.
	private GrowQueue_F32 threshold = new GrowQueue_F32();

	// List of region ID's and their size
	private GrowQueue_I32 outputRegionId = new GrowQueue_I32();
	private GrowQueue_I32 outputRegionSizes = new GrowQueue_I32();

	/**
	 * Specifies tuning parameter
	 *
	 * @param k Tuning parameter.  Larger regions are preferred for larger values of K.  Try 300
	 * @param minimumSize Regions smaller than this are merged into larger regions
	 * @param computeWeights Function used to compute the weight for all the edges.
	 */
	public SegmentFelzenszwalb04(float k, int minimumSize, ComputeEdgeWeights<T> computeWeights) {
		K = k;
		this.minimumSize = minimumSize;
		this.computeWeights = computeWeights;
	}

	/**
	 * Segments the image.  Each region in the output image is given a unique ID.  To find out what the ID
	 * of each region is call {@link #getRegionId()}.  To get a list of number of pixels in each region call
	 * {@link #getRegionSizes()}.
	 *
	 * @param input Input image.  Not modified.
	 * @param output Output segmented image.  Modified.
	 */
	public void process( T input , ImageSInt32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		initialize(input,output);

		// compute edges weights
		long time0 = System.currentTimeMillis();
		computeWeights.process(input, output.startIndex, output.stride, edges);
		long time1 = System.currentTimeMillis();

		System.out.println("Edge weights time "+(time1-time0));

		// Merge regions together
		mergeRegions();

		// Get rid of small ones
		mergeSmallRegions();

		// compute the final output
		computeOutput();
	}

	/**
	 * Predeclares all memory required and sets data structures to their initial values
	 */
	protected void initialize(T input , ImageSInt32 output ) {
		this.graph = output;
		final int N = input.width*input.height;

		regionSize.resize(N);
		threshold.resize(N);
		for( int i = 0; i < N; i++ ) {
			regionSize.data[i] = 1;
			threshold.data[i] = K;
		}
		int id = 0;
		for( int y = 0; y < output.height; y++ ) {
			int index = graph.startIndex + y*graph.stride;
			for( int x = 0; x < graph.width; x++ ) {
				graph.data[index++] = id++;
			}
		}
		edges.reset();
		edgesNotMatched.reset();
	}

	/**
	 * Follows the merge procedure output in [1].  Two regions are merged together if the edge linking them
	 * has a weight which is <= the minimum of the heaviest edges in the two regions.
	 */
	protected void mergeRegions() {

		// sort edges
		long time0 = System.currentTimeMillis();
//		sorter.sort(edges.data,edges.size);
		Collections.sort(edges.toList());
		long time1 = System.currentTimeMillis();

		System.out.println("Sort time " + (time1 - time0));

		// examine each edge to see if it can connect two regions
		for( int i = 0; i < edges.size(); i++ ) {
			// compare the two nodes connected by the edge to see if their regions they should be merged
			Edge e = edges.get(i);

			int rootA = find(e.indexA);
			int rootB = find(e.indexB);

			// see if they are already part of the same segment
			if( rootA == rootB )
				continue;

			float threshA = threshold.get(rootA);
			float threshB = threshold.get(rootB);

			if( e.weight() <= threshA && e.weight() <= threshB )  {
				// ----- Merge the two regions/components
				int sizeA = regionSize.get(rootA);
				int sizeB = regionSize.get(rootB);

				// Everything is merged into region A, so update its threshold
				threshold.data[rootA] = e.weight() + K/(sizeA + sizeB);

				// Point everything towards rootA
				graph.data[e.indexB] = rootA;
				graph.data[rootB] = rootA;

				// Update the size of regionA
				regionSize.data[rootA] = sizeA + sizeB;
			} else {
				edgesNotMatched.add(e);
			}
		}
	}

	/**
	 * Look at the remaining regions and if there are any small ones marge them into a larger region
	 */
	protected void mergeSmallRegions() {
		for( int i = 0; i < edgesNotMatched.size(); i++ ) {
			Edge e = edgesNotMatched.get(i);

			int rootA = find(e.indexA);
			int rootB = find(e.indexB);

			// see if they are already part of the same segment
			if( rootA == rootB )
				continue;

			int sizeA = regionSize.get(rootA);
			int sizeB = regionSize.get(rootB);

			// merge if one of the regions is too small
			if( sizeA < minimumSize || sizeB < minimumSize ) {
				// Point everything towards rootA
				graph.data[e.indexB] = rootA;
				graph.data[rootB] = rootA;

				// Update the size of regionA
				regionSize.data[rootA] = sizeA + sizeB;
			}
		}
	}

	/**
	 * Finds the root given child.  If the child does not point directly to the parent find the parent and make
	 * the child point directly towards it.
	 */
	protected int find( int child ) {
		int root = graph.data[child];

		if( root == graph.data[root] )
			return root;

		int inputChild = child;
		while( root != child ) {
			child = root;
			root = graph.data[child];
		}
		graph.data[inputChild] = root;
		return root;
	}

	/**
	 * Searches for root nodes in the graph and adds their size to the list of region sizes.  Makes sure all
	 * other nodes in the graph point directly at their root.
	 */
	protected void computeOutput() {
		outputRegionId.reset();
		outputRegionSizes.reset();
		for( int y = 0; y < graph.height; y++ ) {
			int indexGraph = graph.startIndex + y*graph.stride;
			for( int x = 0; x < graph.width; x++ , indexGraph++) {
				int parent = graph.data[indexGraph];
				if( parent == indexGraph ) {
					outputRegionId.add(indexGraph);
					outputRegionSizes.add(regionSize.get(indexGraph));
				} else {
					// find the parent and set the child to it
					int child = indexGraph;
					while( parent != child ) {
						child = parent;
						parent = graph.data[child];
					}
					graph.data[indexGraph] = parent;
				}
			}
		}
	}

	/**
	 * List of ID's for each region in the segmented image.
	 */
	public GrowQueue_I32 getRegionId() {
		return outputRegionId;
	}

	/**
	 * Number of pixels in each region
	 */
	public GrowQueue_I32 getRegionSizes() {
		return outputRegionSizes;
	}

	/**
	 * Describes the relationship between to adjacent pixels in the image.
	 *
	 * The weight is saved in 'sortValue'
	 */
	public static class Edge extends SortableParameter_F32 implements Comparable<Edge> {
		// indexes of connected pixels in output image.
		// Note: output image could be a sub-image so these might not be simply "y*width + x"
		int indexA;
		int indexB;

		public final float weight() {
			return sortValue;
		}

		@Override
		public int compareTo(Edge o) {
			if( sortValue < o.sortValue )
				return -1;
			else if( sortValue > o.sortValue )
				return 1;
			return 0;
		}
	}
}
