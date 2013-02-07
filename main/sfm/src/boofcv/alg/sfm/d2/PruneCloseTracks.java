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

package boofcv.alg.sfm.d2;

import boofcv.abst.feature.tracker.PointTrack;

import java.util.List;

/**
 * Detects if tracks are too close together and discards some of the close ones.  Tracks are projected into
 * a smaller grid (specified by scale) and if more than one lands on the same grid element it is pruned.
 * 
 * @author Peter Abeles
 */
public class PruneCloseTracks {
	// how close the tracks can be before they are pruned.
	int scale;
	int imgWidth;
	int imgHeight;
	PointTrack[] pairImage;

	public PruneCloseTracks(int scale, int imgWidth, int imgHeight) {
		this.scale = scale;
		resize(imgWidth,imgHeight);
	}
	
	public void resize( int imgWidth , int imgHeight ) {
		this.imgWidth = imgWidth;
		this.imgHeight = imgHeight;

		// +1 to avoid out of bounds error along image border
		int w = imgWidth/scale+1;
		int h = imgHeight/scale+1;
		
		if( pairImage == null || pairImage.length < w*h ) {
			pairImage = new PointTrack[ w*h ];
		}
	}
	
	public void process( List<PointTrack> tracks , List<PointTrack> dropTracks ) {

		int w = imgWidth/scale;
		int h = imgHeight/scale;
		
		int l=w*h;
		for( int i = 0; i < l; i++ )
			pairImage[i] = null;

		for( int i = 0; i < tracks.size(); i++ ) {
			PointTrack p = tracks.get(i);
			
			int x = (int)(p.x/scale);
			int y = (int)(p.y/scale);
			
			int index = y*w+x;
			
			if( pairImage[index] == null ) {
				pairImage[index] = p;
			} else {
				dropTracks.add(p);
			}
		}
	}
}
