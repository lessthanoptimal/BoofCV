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

import boofcv.core.image.GeneralizedImageOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class WrapRefineLineFit<T extends ImageSingleBand> 
	implements RefineCalibrationGridCorner<T>
{
	ImageFloat32 floatImage = new ImageFloat32(1,1);
	
	int searchRadius = 12;

	RefineLineFit alg = new RefineLineFit();
	
	@Override
	public void refine(List<Point2D_I32> crudeCorners, 
					   int gridWidth, int gridHeight,
					   T image, 
					   List<Point2D_F32> refinedCorners) 
	{
		floatImage.reshape(image.width,image.height);
		GeneralizedImageOps.convert(image,floatImage);

		for( int i = 0; i < crudeCorners.size(); i++ ) {
			Point2D_I32 cp = crudeCorners.get(i);

			ImageRectangle r = new ImageRectangle(cp.x- searchRadius,cp.y - searchRadius,cp.x + searchRadius +1,cp.y+ searchRadius +1);
			BoofMiscOps.boundRectangleInside(image, r);
			
			ImageFloat32 sub = floatImage.subimage(r.x0,r.y0,r.x1,r.y1);
			
			Point2D_F32 cr = refinedCorners.get(i);


			alg.process(sub);
			cr.x = r.x0 + alg.getCorner().x;
			cr.y = r.y0 + alg.getCorner().y;
		}
	}
}
