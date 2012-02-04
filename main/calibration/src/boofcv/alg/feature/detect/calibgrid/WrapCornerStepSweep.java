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

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class WrapCornerStepSweep<T extends ImageSingleBand> implements RefineCalibrationGridCorner<T> {

	CornerSubpixelStepSweep<T> alg;
	int radius = 12;

	public WrapCornerStepSweep(CornerSubpixelStepSweep<T> alg) {
		this.alg = alg;
	}

	@Override
	public void refine(List<Point2D_I32> crudeCorners, int gridWidth, int gridHeight, 
					   T image , 
					   List<Point2D_F32> refinedCorners) {

		InterpolatePixel<T> interp = FactoryInterpolation.bilinearPixel((Class)image.getClass());
		
		for( int i = 0; i < crudeCorners.size(); i++ ) {
			Point2D_I32 cp = crudeCorners.get(i);

			ImageRectangle r = new ImageRectangle(cp.x-radius,cp.y-radius,cp.x+radius+1,cp.y+radius+1);

			BoofMiscOps.boundRectangleInside(image,r);
			
			T subimage = (T)image.subimage(r.x0,r.y0,r.x1,r.y1);
			interp.setImage(subimage);
			
			alg.process(interp);
			
			Point2D_F32 rp = refinedCorners.get(i);
			rp.x = r.x0 + (float)alg.getCornerPoint().x;
			rp.y = r.y0 + (float)alg.getCornerPoint().y;
		}
	}
}
