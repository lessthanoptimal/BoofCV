/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.edge;

import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt8;
import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class HysteresisEdgeTrace4 {

	ImageFloat32 intensity = new ImageFloat32(1,1);
	ImageSInt8 direction;

	List<Point2D_I32> open = new ArrayList<Point2D_I32>();

	FastQueue<EdgeContour> queueContour = new FastQueue<EdgeContour>(EdgeContour.class,true);
	FastQueue<Point2D_I32> queuePoints = new FastQueue<Point2D_I32>(Point2D_I32.class,true);

	public void setImages( ImageFloat32 intensity , ImageSInt8 direction ) {
		this.intensity = intensity;
		this.direction = direction;
	}

	public void process( float lower , float upper ) {
		queueContour.reset();

		for( int y = 0; y < intensity.height; y++ ) {
			int indexInten = intensity.startIndex + y*intensity.stride;

			for( int x = 0; x < intensity.width; x++ , indexInten++ ) {
				float v = intensity.data[indexInten];
				if( v > upper ) {
					trace( x,y,indexInten,lower);
				}
			}
		}
	}

	protected void trace( int x , int y , int indexInten , float threshold ) {

		EdgeContour e = queueContour.grow();
		e.reset();

		int dx,dy;

		Point2D_I32 p = queuePoints.grow();
		p.set(x,y);
		intensity.data[ indexInten ] = 0;
		open.add(p);

		while( open.size() > 0 ) {
			p = open.remove( open.size()-1 );
			List<Point2D_I32> edge = new ArrayList<Point2D_I32>();
			e.edges.add(edge);
			edge.add(p);
			indexInten = intensity.getIndex(p.x,p.y);
			int indexDir = direction.getIndex(p.x,p.y);

			while( true ) {
				switch( direction.data[ indexDir ] ) {
					case  0: dx =  0;dy=  1; break;
					case  1: dx =  1;dy= -1; break;
					case  2: dx =  1;dy=  0; break;
					case -1: dx =  1;dy=  1; break;
					default: throw new RuntimeException("Unknown direction: "+direction.data[ indexDir ]);
				}

				int indexForward = indexInten + dy*intensity.stride + dx;
				int indexBackward = indexInten - dy*intensity.stride - dx;

				int prevIndexDir = indexDir;

				boolean match = false;

				// pixel coordinate of forward and backward point
				int fx = p.x+dx, fy = p.y+dy;
				int bx = p.x-dx, by = p.y-dy;

				if( intensity.isInBounds(fx,fy) && intensity.data[ indexForward ] >= threshold ) {
					intensity.data[ indexForward ] = 0;
					p = queuePoints.grow();
					p.set(fx,fy);
					edge.add(p);
					match = true;
					indexInten = indexForward;
					indexDir = prevIndexDir  + dy*intensity.stride + dx;
				}
				if( intensity.isInBounds(bx,by) && intensity.data[ indexBackward ] >= threshold ) {
					intensity.data[ indexBackward ] = 0;
					Point2D_I32 b = queuePoints.grow();
					b.set(bx, by);
					if( match ) {
						open.add(b);
					} else {
						p = b;
						edge.add(p);
						match = true;
						indexInten = indexBackward;
						indexDir = prevIndexDir  - dy*intensity.stride - dx;
					}
				}

				if( !match ) {
					if( edge.size() == 1 ) {
						e.edges.remove(e.edges.size()-1);
					}
					break;
				}
			}
		}
	}

	public FastQueue<EdgeContour> getQueueContour() {
		return queueContour;
	}
}
