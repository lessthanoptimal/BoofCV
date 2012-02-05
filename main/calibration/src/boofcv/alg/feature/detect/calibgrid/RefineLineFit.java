/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.calibgrid;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.misc.BasicImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageFloat64;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPoint2D_I32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class RefineLineFit {

	SegmentTwoGaussianPrune segment = new SegmentTwoGaussianPrune();
	
	// binary images signifying the background and square
	ImageUInt8 regionWhite = new ImageUInt8(1,1);
	ImageUInt8 regionBlack = new ImageUInt8(1,1);

	ImageUInt8 edgeWhite = new ImageUInt8(1,1);
	ImageUInt8 edgeBlack = new ImageUInt8(1,1);
	
	// saved clustered binary image
	ImageSInt32 blobs = new ImageSInt32(1,1);

	Point2D_I32 edges[] = new Point2D_I32[]{new Point2D_I32()};
	
	int width;
	int height;
	int N;
	
	Point2D_F32 corner = new Point2D_F32();



	public void process( ImageFloat32 image ) {
		init(image);

		segment.reset(N);
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				segment.addValue(image.get(x,y));
			}
		}
		segment.process();

//		BasicImageIO.print(image);

		thresholdProb(image,regionBlack, segment.getProbLow(),segment.getMeanLow(),true);
		thresholdProb(image,regionWhite,segment.getProbHigh(),segment.getMeanHigh(),false);
		
		// find edges
		BinaryImageOps.edge4(regionBlack,edgeBlack);
		BinaryImageOps.edge4(regionWhite,edgeWhite);

		CriticalPoints critBlack = findCritical(edgeBlack,segment.getProbLow());
		CriticalPoints critWhite = findCritical(edgeWhite,segment.getProbHigh());

		corner.x = (float)(critBlack.corner.x + critWhite.corner.x)/2.0f;
		corner.y = (float)(critBlack.corner.y + critWhite.corner.y)/2.0f;
	}

	private void init(ImageFloat32 image) {
		this.width = image.width;
		this.height = image.height;
		N = width*height;

		regionWhite.reshape(width,height);
		regionBlack.reshape(width,height);
		edgeWhite.reshape(width,height);
		edgeBlack.reshape(width,height);
		blobs.reshape(width,height);

		setupEdgeArray();
	}

	public Point2D_F32 getCorner() {
		return corner;
	}

	private void setupEdgeArray() {
		if( edges.length < N ) {
			edges = new Point2D_I32[N];
			for( int i = 0; i < N; i++ ) {
				edges[i] = new Point2D_I32();
			}
		}

		int index = 0;
		for( int i = 0; i < width; i++ ) {
			edges[index++].set(i, 0);
		}
		for( int i = 1; i < height-1; i++ ) {
			edges[index++].set(width-1,i);
		}
		for( int i = width-1; i >= 0; i-- ) {
			edges[index++].set(i,height-1);
		}
		for( int i = height-2; i >= 1; i-- ) {
			edges[index++].set(0,i);
		}
	}
	
	private void thresholdProb( ImageFloat32 intensity ,
								ImageUInt8 binary , double[] prob , 
								double mean , boolean isLow ) {
		
		// do a quick threshold
		int iter = 0;
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				if( prob[iter++] >= 0.5 ) {
					binary.set(x,y,1);
				} else {
					// some times the distribution is really narrow, if its
					// not in the middle and is close to this distribution
					// turn it into a member
					float val = intensity.get(x,y);
					if( isLow ) {
						if( val < mean )
							binary.set(x,y,1);
						else
							binary.set(x,y,0);
					} else {
						if( val > mean )
							binary.set(x,y,1);
						else
							binary.set(x,y,0);
					}
				}
			}
		}
		removeBinaryNoise(binary);
	}

	/**
	 * Remove all but the largest blob.  Any sporadic pixels will be zapped this way
	 * 
	 * @param binary Initial binary image.  Modified
	 */
	private void removeBinaryNoise(ImageUInt8 binary) {
		// remove potential noise by only saving the largest cluster
		int numBlobs = BinaryImageOps.labelBlobs4(binary, blobs);

		// find the largest blob
		numBlobs++;
		int count[] = new int[numBlobs];
		for( int i = 0; i < width*height; i++ ) {
			count[blobs.data[i]]++;
		}
		int largest = -1;
		int largestIndex = -1;
		for( int i = 1; i < numBlobs; i++ ) {
			if( count[i] > largest ) {
				largestIndex = i;
				largest = count[i];
			}
		}

		for( int i = 0; i < numBlobs; i++ ) {
			count[i] = i != largestIndex ? 0 : i;
		}
		BinaryImageOps.relabel(blobs,count);
		BinaryImageOps.labelToBinary(blobs,binary);
	}
	
	private CriticalPoints findCritical( ImageUInt8 binary , double probability[] ) {
		List<Integer> onEdge = new ArrayList<Integer>();
		
		for( int i = 0; i < N; i++ ) {
			Point2D_I32 p = edges[i];
			if( binary.get(p.x,p.y) == 1 ) {
				onEdge.add(i);
			}
		}
		
		if( onEdge.size() != 2 ) {
			ImageFloat64 prob = new ImageFloat64(width,height);
			prob.data = probability;
			BasicImageIO.print(prob);
			binary.print();
			throw new RuntimeException("On Crap");
		}

		CriticalPoints ret = new CriticalPoints();
		ret.side0 = onEdge.get(0);
		ret.side1 = onEdge.get(1);
		
		Point2D_I32 p0 = edges[ret.side0];
		Point2D_I32 p1 = edges[ret.side1];

		// find the point the farthest away from the side points
		double max = 0;
		int maxX = -1;
		int maxY = -1;

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				if( binary.get(x,y) == 0 )
					continue;
				
				ret.edges.add( new Point2D_I32(x,y));
				
				double dist0 = UtilPoint2D_I32.distance(p0.x,p0.y,x,y);
				double dist1 = UtilPoint2D_I32.distance(p1.x,p1.y,x,y);

				double d = dist0+dist1;
				if( d > max ) {
					max = d;
					maxX = x;
					maxY = y;
				}
			}
		}
		
		ret.corner = new Point2D_I32(maxX,maxY);
		
		return ret;
	}
	
	private static class CriticalPoints
	{
		int side0;
		int side1;
		
		Point2D_I32 corner;
		
		List<Point2D_I32> edges = new ArrayList<Point2D_I32>();
	}
}
