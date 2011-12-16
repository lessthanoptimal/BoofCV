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

package boofcv.alg.geo.d2.stabilization;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.alg.geo.AssociatedPair;

import java.util.List;

/**
 * Detects if tracks are too close together and discards some of the close ones
 * 
 * @author Peter Abeles
 */
public class PruneCloseTracks {
	int scale;
	int imgWidth;
	int imgHeight;
	AssociatedPair[] pairImage;

	public PruneCloseTracks(int scale, int imgWidth, int imgHeight) {
		this.scale = scale;
		resize(imgWidth,imgHeight);
	}
	
	public void resize( int imgWidth , int imgHeight ) {
		this.imgWidth = imgWidth;
		this.imgHeight = imgHeight;

		int w = imgWidth/scale;
		int h = imgHeight/scale;
		
		if( pairImage == null || pairImage.length < w*h ) {
			pairImage = new AssociatedPair[ w*h ];
		}
	}
	
	public void process( ImagePointTracker<?> tracker ) {
		List<AssociatedPair> tracks = tracker.getActiveTracks();

		int w = imgWidth/scale;
		int h = imgHeight/scale;
		
		int l=w*h;
		for( int i = 0; i < l; i++ )
			pairImage[i] = null;

		for( int i = 0; i < tracks.size(); ) {
			AssociatedPair p = tracks.get(i);
			
			int x = (int)(p.currLoc.x/scale);
			int y = (int)(p.currLoc.y/scale);
			
			int index = y*w+x;
			
			if( pairImage[index] == null ) {
				pairImage[index] = p;
				i++;
			} else {
				tracker.dropTrack(p);
			}
		}
	}
}
