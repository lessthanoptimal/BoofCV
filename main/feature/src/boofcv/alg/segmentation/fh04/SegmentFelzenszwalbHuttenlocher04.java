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

package boofcv.alg.segmentation.fh04;

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.sorting.ApproximateSort_F32;
import org.ddogleg.sorting.QuickSortObj_F32;
import org.ddogleg.sorting.SortableParameter_F32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * <p>
 * Implementation of Felzenszwalb-Huttenlocher [1] image segmentation algorithm. It is fast and uses a graph based
 * heuristic with a tuning parameter that can be used to adjust region size.  Regions are irregularly shaped.
 * </p>
 *
 * <p>
 * It works by constructing a graph in which pixels are the nodes and edges describe the relationship between
 * adjacent pixels.  Each pixel has a weight that is determined from the difference in pixel values using the F-norm.
 * The weights for all the edges are computed first and sorted from smallest to largest.  In the next step
 * the first edge in the list is selected and is tested to see if the nodes should be connected or not.  This
 * process is repeated for all edges.  Small regions are then merged into large ones. For more details
 * see [1].
 * </p>
 *
 * <p>
 * NOTE:
 * <ul>
 * <li>Region ID's in output image will NOT be sequential.  You need to call {@link #getRegionId()} to find
 * out what the ID's are.</li>
 * <li>The output image can't be a sub-image because it is used internally and needs to be a continuous
 * block of memory.</li>
 * <li>Pixel connectivity rule and weight metric is by the {@link FhEdgeWeights}
 * class passed in to the constructor.</li>
 * <li>To emulate the reference implementation use a </li>
 * </ul>
 * <pP>
 *
 * <p>
 * Algorithmic Changes:<br>
 * This implementation is a faithful of the original and has been compared against the authors
 * reference source code.  It does produce different results from the reference, some times significant, due to the
 * sensitivity of the algorithm to minor differences.  The sensitivity arises from it being a greedy algorithm.</p>
 *
 * <p>Here is a list of minor differences that cause different regions due to its sensitivity.  The order in which
 * edges with identical weights are sorted is arbitrary.  The order that edges are computed is arbitrary.  Floating
 * point error in weight calculation gradually causes segmentation to diverge to a different solution even
 * when given the same input.</p>
 *
 * <p>One difference from the original is that Gaussian blur is not applied to the input image by default.  That
 * should be done prior to the image being passed in.</p>
 *
 * <p>
 * [1] Felzenszwalb, Pedro F., and Daniel P. Huttenlocher.
 * "Efficient graph-based image segmentation." International Journal of Computer Vision 59.2 (2004): 167-181.
 * </p>
 *
 * @author Peter Abeles
 */
public class SegmentFelzenszwalbHuttenlocher04<T extends ImageBase> {

	// tuning parameter.  Determines the number of segments.  Larger number means larger regions
	private float K;

	// the minimum region size.  Regions smaller than this are merged into larger ones
	private int minimumSize;

	// Storage for the disjoint-set forest.  Same data structure as 'output', but renamed for convenience.
	// Value stored in each pixel refers to the parent vertex.  A root vertex contains a reference to itself
	protected GrayS32 graph;

	// Function that computes the weight for each edge
	private FhEdgeWeights<T> computeWeights;

	private QuickSortObj_F32 sorter = new QuickSortObj_F32();
	private ApproximateSort_F32 sorterApprox = null;
	// storage for edges so that they can be recycled on the next call
	protected FastQueue<Edge> edges = new FastQueue<>(Edge.class, true);
	// list of edges which were not matched to anything.  used to merge small regions
	protected FastQueue<Edge> edgesNotMatched = new FastQueue<>(Edge.class, false);
	// Size of each region
	protected GrowQueue_I32 regionSize = new GrowQueue_I32();
	// This is equivalent to Int(C) + tau(C) in Equation 4.
	// NOTE: Is the maximum weight in the MST really weight of the edge causing the merge?  Maybe I'm missing
	// something, but it seems trivial to find counter examples.
	protected GrowQueue_F32 threshold = new GrowQueue_F32();

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
	public SegmentFelzenszwalbHuttenlocher04(float k, int minimumSize, FhEdgeWeights<T> computeWeights) {
		K = k;
		this.minimumSize = minimumSize;
		this.computeWeights = computeWeights;
	}

	/**
	 * If this function is called the exact sort routine will not be used and instead an approximate routine will
	 * be used.
	 * @param numBins Number of bins.  Try 2000.  More bins the more accurate it will be
	 */
	public void configureApproximateSort( int numBins ) {
		sorterApprox = new ApproximateSort_F32(numBins);
	}

	/**
	 * Segments the image.  Each region in the output image is given a unique ID.  To find out what the ID
	 * of each region is call {@link #getRegionId()}.  To get a list of number of pixels in each region call
	 * {@link #getRegionSizes()}.
	 *
	 * @param input Input image.  Not modified.
	 * @param output Output segmented image.  Modified.
	 */
	public void process( T input , GrayS32 output ) {
		if( output.isSubimage() )
			throw new IllegalArgumentException("Output can't be a sub-image");
		InputSanityCheck.checkSameShape(input, output);

		initialize(input,output);

		// compute edges weights
//		long time0 = System.currentTimeMillis();
		computeWeights.process(input, edges);
//		long time1 = System.currentTimeMillis();

//		System.out.println("Edge weights time " + (time1 - time0));

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
	protected void initialize(T input , GrayS32 output ) {
		this.graph = output;
		final int N = input.width*input.height;

		regionSize.resize(N);
		threshold.resize(N);
		for( int i = 0; i < N; i++ ) {
			regionSize.data[i] = 1;
			threshold.data[i] = K;
			graph.data[i] = i; // assign a unique label to each pixel since they are all their own region initially
		}

		edges.reset();
		edgesNotMatched.reset();
	}

	/**
	 * Follows the merge procedure output in [1].  Two regions are merged together if the edge linking them
	 * has a weight which is &le; the minimum of the heaviest edges in the two regions.
	 */
	protected void mergeRegions() {

		// sort edges
//		long time0 = System.currentTimeMillis();
		if( sorterApprox != null ) {
			sorterApprox.computeRange(edges.data,0,edges.size);
			sorterApprox.sortObject(edges.data,0,edges.size);
		} else {
			sorter.sort(edges.data,edges.size);
		}
//		long time1 = System.currentTimeMillis();

//		System.out.println("Sort time " + (time1 - time0));

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

//		long time2 = System.currentTimeMillis();
//		System.out.println("Edge merge time " + (time2 - time1));
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

	public ImageType<T> getInputType() {
		return computeWeights.getInputType();
	}

	/**
	 * Describes the relationship between to adjacent pixels in the image.
	 *
	 * The weight is saved in 'sortValue'
	 */
	public static class Edge extends SortableParameter_F32 {
		// indexes of connected pixels in output image.  The index for pixel (x,y) is: index = y*width + x
		public int indexA;
		public int indexB;

		public Edge(int indexA, int indexB) {
			this.indexA = indexA;
			this.indexB = indexB;
		}

		public Edge() {
		}

		public final float weight() {
			return sortValue;
		}
	}
}
