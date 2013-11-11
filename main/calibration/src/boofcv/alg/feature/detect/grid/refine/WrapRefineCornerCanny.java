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

package boofcv.alg.feature.detect.grid.refine;

import boofcv.alg.feature.detect.grid.RefineCalibrationGridCorner;
import boofcv.alg.feature.detect.quadblob.QuadBlob;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class WrapRefineCornerCanny
	implements RefineCalibrationGridCorner
{

	RefineCornerCanny alg = new RefineCornerCanny();
	
	@Override
	public void refine(List<QuadBlob> squares,
					   ImageFloat32 image ) 
	{
		for( QuadBlob s : squares) {

			// search the smallest side size to avoid accidentally including
			// another corner in the region being considered.
			// hmm if the square is at a 45 degree angle this might not work.... Oh well don't use small squares
			// Also need to be careful of perspective distortion, it can cause the smallest side to be
			// too short too
			int searchRadius = (int)s.smallestSide-2;
			if( searchRadius > 15 )
				searchRadius = 15;
			if( searchRadius < 3 )
				searchRadius = 3;
			
			for( int i = 0; i < 4; i++ ) {
				Point2D_I32 cp = s.corners.get(i);
				Point2D_F64 rp = s.subpixel.get(i);
				
				ImageRectangle r = new ImageRectangle(cp.x- searchRadius,cp.y - searchRadius,
						cp.x + searchRadius +1,cp.y+ searchRadius +1);
				BoofMiscOps.boundRectangleInside(image, r);

				ImageFloat32 sub = image.subimage(r.x0,r.y0,r.x1,r.y1, null);

				alg.process(sub);
				rp.x = r.x0 + (float)alg.getCorner().x;
				rp.y = r.y0 + (float)alg.getCorner().y;
			}
		}
	}
}
