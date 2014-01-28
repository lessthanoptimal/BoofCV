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
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
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
 *
 * <p>
 * One difference from the original is that Gaussian blur is not applied to the input image by default.  That
 * should be done prior to the image being passed in.
 * </p>
 *
 * <p>
 * [1] Felzenszwalb, Pedro F., and Daniel P. Huttenlocher.
 * "Efficient graph-based image segmentation." International Journal of Computer Vision 59.2 (2004): 167-181.
 * </p>
 *
 * @author Peter Abeles
 */
public class SegmentFelzenszwalb04<T extends ImageSingleBand> {

	// tuning parameter.  Determines the number of segments
	private int K;

	// Storage for the disjoint-set forest.  This is the same image as the output segmented image.
	// Value stored in each pixel refers to the parent vertex.  A root vertex contains a reference for itself
	private ImageSInt32 graph;

	// Function that computes the weight for each edge
	private ComputeEdgeWeights<T> computeWeights;

	// storage for edges so that they can be recycled on the next call
	private List<Edge> edgesStorage = new ArrayList<Edge>();
	// List of edges currently being examined
	private List<Edge> activeEdges = new ArrayList<Edge>();
	// The edges which are to be examined in the next iteration
	private List<Edge> nextActiveEdges = new ArrayList<Edge>();

	// Size of each region
	private GrowQueue_I32 regionSize = new GrowQueue_I32();
	// Internal difference of a component, Int(C) Equation 1.  Max edge in the minimum-spanning-tree.
	// Comment: The only reason this is the a MST is because it is the only tree.  If all adjacent pixels
	//          were considered for connectivity (as is reasonable/common) then their technique would be incorrect.
	private GrowQueue_F32 segmentIntDiff = new GrowQueue_F32();

	// Conversion from new label to original label of root nodes
	private GrowQueue_I32 outputLabels = new GrowQueue_I32();

	/**
	 * Specifies tuning parameter
	 *
	 * @param k Tuning parameter.  Larger regions are preferred for larger values of K.  Try 300
	 * @param computeWeights Function used to compute the weight for all the edges.
	 */
	public SegmentFelzenszwalb04(int k, ComputeEdgeWeights<T> computeWeights) {
		K = k;
		this.computeWeights = computeWeights;
	}

	/**
	 * Segments the image.  Each region is given a unique ID from 0 to N-1.  The number of regions and the number
	 * of pixels in each region is provided in outputRegionSize.
	 * @param input Input image.  Not modified.
	 * @param output Output segmented image.  Modified.
	 * @param outputRegionSize Output list containing the number of pixels in each region.
	 */
	public void process( T input , ImageSInt32 output , GrowQueue_I32 outputRegionSize ) {
		InputSanityCheck.checkSameShape(input,output);

		initialize(input, output);

		// compute edges weights
		computeWeights.process(input, output.startIndex, output.stride, activeEdges);

		// Merge regions together
		mergeRegions();

		// Update regions in the output image such that they are consecutive
		formatForOutput(output, outputRegionSize);
	}

	/**
	 * Predeclares all memory required and sets data structures to their initial values
	 */
	private void initialize(T input, ImageSInt32 output) {
		this.graph = output;
		final int N = input.width*input.height;

		edgesStorage.clear();
		activeEdges.clear();
		nextActiveEdges.clear();

		regionSize.resize(N);
		segmentIntDiff.resize(N);
		for( int i = 0; i < N; i++ ) {
			output.data[i] = i;
			regionSize.data[i] = 1;
			segmentIntDiff.data[i] = 0;
		}
		for( int i = edgesStorage.size(); i < N; i++ ) {
			edgesStorage.add(new Edge());
		}
		for( int i = edgesStorage.size(); i < N; i++ ) {
			activeEdges.add( edgesStorage.get(i) );
		}
	}

	/**
	 * Follows the merge procedure output in [1].  Two regions are merged together if the edge linking them
	 * has a weight which is <= the minimum of the heaviest edges in the two regions.
	 */
	private void mergeRegions() {

		// sort edges
		Collections.sort(activeEdges);

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

				if( e.weight <= MInt )  {
					// ----- Merge the two regions/components

					// recompute the internal difference
					float internalDiff = intA > intB ? intA : intB;
					if( internalDiff < e.weight )
						internalDiff = e.weight;

					// Everything is merged into region A, so update its internal difference
					segmentIntDiff.data[rootA] = internalDiff;

					// Point everything towards rootA
					graph.data[e.indexB] = rootA;
					graph.data[rootB] = rootA;

					// Update the size of regionA
					regionSize.data[rootA] += graph.data[rootB];
				} else {
					nextActiveEdges.add(e);
				}

				// see if the graph has changed
				if( totalActive == nextActiveEdges.size() ) {
					break;
				}

				// swap the lists
			}

			// swap active lists
			List<Edge> tmp = activeEdges;
			activeEdges = nextActiveEdges;
			nextActiveEdges = tmp;
		}
	}

	/**
	 * Finds the root given child.  If the child does not point directly to the parent find the parent and make
	 * the child point directly towards it.
	 */
	private int find( int child ) {
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
	 * Compacts the region labels such that they are consecutive numbers starting from 0.  Creates the final list
	 * of region size.
	 */
	private void formatForOutput(ImageSInt32 output, GrowQueue_I32 outputRegionSize) {
		outputRegionSize.reset();
		outputLabels.reset();
		// in the first pass find all the root nodes and make sure all the children directly point to the root
		// Also add the size of each region to the output region size
		for( int i = 0; i < output.data.length; i++ ) {
			int parent = output.data[i];
			if( parent == output.data[parent] ) {
				outputLabels.add( parent );
				outputRegionSize.add( regionSize.get(parent) );
			} else {
				// find the parent and set the child to it
				int child = i;
				while( parent != child ) {
					child = parent;
					parent = graph.data[child];
				}
				output.data[i] = parent;
			}
		}
		// Change the label of root nodes to be the new compacted labels
		for( int i = 0; i < outputLabels.size; i++ ) {
			output.data[outputLabels.data[i]] = i;
		}

		// In the second pass assign all the children to the new compacted labels
		for( int i = 0; i < output.data.length; i++ ) {
			int parent = output.data[i];
			output.data[i] = output.data[parent];
		}
	}

	/**
	 * Describes the relationship between to adjacent pixels in the image
	 */
	public static class Edge implements Comparable<Edge> {
		float weight; // could reduce
		// indexes of connected pixels in output image.
		// Note: output image could be a sub-image so these might not be simply "y*width + x"
		int indexA;
		int indexB;

		@Override
		public int compareTo(Edge o) {
			if( weight < o.weight )
				return -1;
			else if( weight > o.weight )
				return 1;
			return 0;
		}
	}
}
