/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.line;


import boofcv.alg.InputSanityCheck;
import boofcv.alg.feature.detect.line.gridline.Edgel;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.line.LinePolar2D_F32;

/**
 * TODO comment up
 *
 * @author Peter Abeles
 */
public class GridRansacLineDetector {

	float edgeThreshold;

	// size of a region's width/height in pixels
	int regionSize;

	FastQueue<Edgel> edgels = new FastQueue<Edgel>(30,Edgel.class,true);

	ModelMatcher<LinePolar2D_F32,Edgel> robustMatcher;

	public void process( ImageFloat32 derivX , ImageFloat32 derivY , ImageFloat32 intensity )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,intensity);

		int regionRadius = regionSize/2;
		int w = derivX.width-regionRadius;
		int h = derivY.height-regionRadius;

		for( int y = regionRadius; y < h; y += regionSize) {
			// index of the top left pixel in the region being considered
			// possible over optimization
			int index = intensity.startIndex + y*intensity.stride + regionRadius;
			for( int x = regionRadius; x < w; x+= regionSize , index += regionSize) {
				// detects edgels inside the region
				detectEdgels(index,x,y,derivX,derivY,intensity);

				// find lines inside the region using RANSAC

			}
		}
	}

	private void detectEdgels( int index0 , int x0 , int y0 ,
							   ImageFloat32 derivX , ImageFloat32 derivY ,
							   ImageFloat32 intensity) {

		edgels.reset();
		for( int y = 0; y < regionSize; y++ ) {
			int index = index0 + y*intensity.stride;

			for( int x = 0; x < regionSize; x++ ) {
				if( intensity.data[index++] > edgeThreshold ) {
					Edgel e = edgels.pop();
					int xx = x0+x;
					int yy = y0+y;

					e.set(xx,yy);

					float dx = derivX.get(xx,yy);
					float dy = derivY.get(xx,yy);

					if( dx != 0 )
						e.theta = (float)Math.atan(dy/dx);
					else
						e.theta = (float)(Math.PI/2.0);
				}
			}
		}
	}

	private void findLinesInRegion() {
		// exit if not enough points or max iterations exceeded

		// use model matcher to detect lines inside the region

		// if a good match is found, remove matching points and run again


	}
}
