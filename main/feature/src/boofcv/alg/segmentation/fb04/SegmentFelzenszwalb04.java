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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

	// tuning parameter.  Determines the number of segments
	private float K;

	// the minimum region size.  Regions smaller than this are merged into larger ones
	private int minimumSize;

	// Storage for the disjoint-set forest.  Same data structure as 'output', but renamed for convenience.
	// Value stored in each pixel refers to the parent vertex.  A root vertex contains a reference for itself
	protected ImageSInt32 graph = new ImageSInt32(1,1);

	// Function that computes the weight for each edge
	private ComputeEdgeWeights<T> computeWeights;

	private QuickSortObj_F32 sorter = new QuickSortObj_F32();
	// storage for edges so that they can be recycled on the next call
	private FastQueue<Edge> edgesStorage = new FastQueue<Edge>(Edge.class,true);
	// List of edges currently being examined
	private List<Edge> activeEdges = new ArrayList<Edge>();
	// The edges which are to be examined in the next iteration
	private List<Edge> nextActiveEdges = new ArrayList<Edge>();

	// Size of each region
	protected GrowQueue_I32 regionSize = new GrowQueue_I32();
	// Internal difference of a component, Int(C) Equation 1.  Max edge in the minimum-spanning-tree.
	// Comment: The only reason this is the a MST is because it is the only tree.  If all adjacent pixels
	//          were considered for connectivity (as is reasonable/common) then their technique would be incorrect.
	private GrowQueue_F32 segmentIntDiff = new GrowQueue_F32();

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
		computeWeights.process(input, output.startIndex, output.stride, edgesStorage.toList());

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
		final int M = computeNumberOfEdges(input.width,input.height);

		activeEdges.clear();
		nextActiveEdges.clear();

		regionSize.resize(N);
		segmentIntDiff.resize(N);
		for( int i = 0; i < N; i++ ) {
			output.data[i] = i;
			regionSize.data[i] = 1;
			segmentIntDiff.data[i] = 0;
		}
		edgesStorage.resize(M);

	}

	/**
	 * Follows the merge procedure output in [1].  Two regions are merged together if the edge linking them
	 * has a weight which is <= the minimum of the heaviest edges in the two regions.
	 */
	protected void mergeRegions() {

		// sort edges
		long time0 = System.currentTimeMillis();
//		sorter.sort(edgesStorage.data,edgesStorage.size);
		Collections.sort(edgesStorage.toList());
		long time1 = System.currentTimeMillis();

		for( int i = 0; i < edgesStorage.size; i++ ) {
			activeEdges.add( edgesStorage.get(i) );
		}
		System.out.println("Sort time " + (time1 - time0));

		// iterate until convergence
		int M = activeEdges.size();
		for( int i = 0; i < M; i++ ) {
			int totalActive = activeEdges.size();
			nextActiveEdges.clear();

			// examine each edge to see if it can connect two regions
			for( int j = 0; j < activeEdges.size(); j++ ) {
				// compare the two nodes connected by the edge to see if their regions they should be merged
				Edge e = activeEdges.get(j);

				int rootA = find(e.indexA);
				int rootB = find(e.indexB);

				// see if they are already part of the same segment
				if( rootA == rootB )
					continue;

				int sizeA = regionSize.get(rootA);
				int sizeB = regionSize.get(rootB);

				// compute MInt: Equation 4
				float intA = segmentIntDiff.get(rootA);
				float intB = segmentIntDiff.get(rootB);

				float MInt = Math.min(intA + K/sizeA , intB + K/sizeB);

				if( e.weight() <= MInt )  {
					// ----- Merge the two regions/components

					// recompute the internal difference
					float internalDiff = intA > intB ? intA : intB;
					if( internalDiff < e.weight() )
						internalDiff = e.weight();

					// Everything is merged into region A, so update its internal difference
					segmentIntDiff.data[rootA] = internalDiff;

					// Point everything towards rootA
					graph.data[e.indexB] = rootA;
					graph.data[rootB] = rootA;

					// Update the size of regionA
					regionSize.data[rootA] = sizeA + sizeB;
				} else {
					nextActiveEdges.add(e);
				}
			}

			// swap active lists
			List<Edge> tmp = activeEdges;
			activeEdges = nextActiveEdges;
			nextActiveEdges = tmp;

			// see if the graph has changed
			if( totalActive == activeEdges.size() ) {
				break;
			}
		}
	}

	/**
	 * Look at the remaining regions and if there are any small ones marge them into a larger region
	 */
	protected void mergeSmallRegions() {
		for( int j = 0; j < activeEdges.size(); j++ ) {
			Edge e = activeEdges.get(j);

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
	 * other nodes in the graph point directly at the root.
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
	 * Computes the number of edge in an image using a 4-connect rule
	 */
	public static int computeNumberOfEdges( int width , int height ) {
		return width*(height-1) + (width-1)*height;
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
