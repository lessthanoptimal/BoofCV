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

package boofcv.alg.segmentation;

import boofcv.struct.image.ImageSInt32;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * Merges together regions which have modes close to each other and have a similar color.
 *
 * @author Peter Abeles
 */
public class MergeRegionMeanShift extends RegionMergeTree {

	// maximum distance in pixels that two nodes can be apart to be merged
	private int maxSpacialDistance;
	// Maximum Euclidean distance squared two colors can be for them to be considered similar
	private float maxColorDistanceSq;

	// ------ Data structures related to nearest-neighbor search
	private NearestNeighbor<Info> nn = FactoryNearestNeighbor.kdtree();
	private FastQueue<Info> storageInfo = new FastQueue<Info>(Info.class,true);
	private List<double[]> nnPoints = new ArrayList<double[]>();
	private List<Info> nnData = new ArrayList<Info>();
	private FastQueue<NnData<Info>> nnResult = new FastQueue<NnData<Info>>((Class)NnData.class,true);
	private double[] point = new double[2];
	// maximum number of results returned by NN search.  Just to be safe, set it to the upper limit
	private int nnMaxNumber;

	FastQueue<float[]> tmpColor;

	/**
	 * Configures MergeRegionMeanShift
	 *
	 * @param maxSpacialDistance The maximum spacial distance (pixels) at which two modes can be for their
	 *                           regions to be merged together.
	 * @param maxColorDistance The maximum Euclidean distance two colors can be from each other for them to be merged.
	 * @param numBands Number of bands in the input image.
	 */
	public MergeRegionMeanShift(int maxSpacialDistance, float maxColorDistance, final int numBands) {
		nn.init(2);

		this.maxSpacialDistance = maxSpacialDistance;
		this.maxColorDistanceSq = maxColorDistance*maxColorDistance;
		int w = maxSpacialDistance*2+1;
		this.nnMaxNumber = w*w;

		tmpColor = new FastQueue<float[]>(float[].class,true) {
			@Override
			protected float[] createInstance() {
				return new float[ numBands ];
			}
		};
	}

	/**
	 * Merges together similar regions which are in close proximity to each other.  After merging
	 * most of the input data structures are modified to take in account the  changes.
	 *
	 * @param pixelToRegion (Input/output) Image that specifies the segmentation.  Modified.
	 * @param regionMemberCount (Input/output) Number of pixels in each region. Modified.
	 * @param regionColor (Input/output) Color of each region. Modified.
	 * @param modeLocation (Input) Location of each region's mode. Not modified.
	 */
	public void process( ImageSInt32 pixelToRegion ,
						 GrowQueue_I32 regionMemberCount,
						 FastQueue<float[]> regionColor ,
						 FastQueue<Point2D_I32> modeLocation ) {
		initializeMerge(regionMemberCount.size);

		// Merge regions
		setupSearchNN(modeLocation);
		markMergeRegions(regionColor,modeLocation);

		performMerge(pixelToRegion,regionMemberCount);
	}

	/**
	 * Takes the mode of a region and searches the local area around it for other regions.  If the region's mode
	 * is also within the local area its color is checked to see if it's similar enough.  If the color is similar
	 * enough then the two regions are marked for merger.
	 */
	// TODO implement the above procedure

	/**
	 * For each region, it searches for modes which are close to it.  If those close by modes have a similar
	 * color they are then marked for merging.
	 */
	protected void markMergeRegions(FastQueue<float[]> regionColor,
									FastQueue<Point2D_I32> modeLocation) {

		for( int i = 0; i < modeLocation.size; i++ ) {

			float[] color = regionColor.get(i);
			Point2D_I32 location = modeLocation.get(i);
			point[0] = location.x;
			point[1] = location.y;

			// find the mode locations which are close to each other and consider those regions for merging
			nnResult.reset();
			nn.findNearest(point,maxSpacialDistance,nnMaxNumber,nnResult);

			for( int j = 0; j < nnResult.size;j++ ) {
				Info info = nnResult.get(j).data;

				// make sure it isn't the region being searched around
				if( info.index == i )
					continue;

				// see if their colors are close enough
				float[] candidateColor = regionColor.get(info.index);
				float colorDistance = distanceSq(color,candidateColor);

				if( colorDistance > maxColorDistanceSq )
					continue;

				// mark the two regions as merged
				markMerge(i, info.index);
			}

		}
	}

	private void setupSearchNN( FastQueue<Point2D_I32> modeLocation) {
		storageInfo.reset();

		for( int i = 0; i < modeLocation.size; i++ ) {
			Point2D_I32 location = modeLocation.get(i);

			Info info = storageInfo.grow();
			info.index = i;
			info.p[0] = location.x;
			info.p[1] = location.y;

			nnPoints.add(info.p);
			nnData.add(info);
		}

		nn.setPoints(nnPoints,nnData);
	}

	protected static float distanceSq( float[] a , float []b ) {
		float distanceSq = 0;
		for( int i = 0; i < a.length; i++ )  {
			float d = a[i]-b[i];
			distanceSq += d*d;
		}
		return distanceSq;
	}

	public static class Info {
		public int index;
		public double[] p = new double[2];
	}
}
