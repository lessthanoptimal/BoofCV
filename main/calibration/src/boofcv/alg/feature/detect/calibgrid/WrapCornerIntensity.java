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

import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.detect.intensity.FactoryIntensityGeneral;
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
public class WrapCornerIntensity<T extends ImageSingleBand, D extends ImageSingleBand>
		implements RefineCalibrationGridCorner<T> {

	Class<D> derivType;
	
	GeneralFeatureIntensity<T,D> featDet;

	D derivX,derivY;
	
	int searchRadius = 12;
	
	public WrapCornerIntensity( int radius , Class<T> imageType ) {
		derivType = GImageDerivativeOps.getDerivativeType(imageType);
//		featDet = FactoryIntensityGeneral.median(radius, imageType);
		featDet = FactoryIntensityGeneral.klt(radius, derivType);

		if( featDet.getRequiresGradient() ) {
			derivX = GeneralizedImageOps.createSingleBand(derivType,1,1);
			derivY = GeneralizedImageOps.createSingleBand(derivType,1,1);
		}
	}

	@Override
	public void refine(List<Point2D_I32> crudeCorners, int gridWidth, int gridHeight, T image,
					   List<Point2D_F32> refinedCorners) {
		if( featDet.getRequiresGradient() ) {
			derivX.reshape(image.width,image.height);
			derivY.reshape(image.width,image.height);

			GImageDerivativeOps.sobel(image,derivX,derivY, BorderType.EXTENDED);
		}

		featDet.process(image,derivX,derivY,null,null,null);
		
		ImageFloat32 intensity = featDet.getIntensity();
		
		for( int i = 0; i < crudeCorners.size(); i++ ) {
			Point2D_I32 cp = crudeCorners.get(i);

			ImageRectangle r = new ImageRectangle(cp.x- searchRadius,cp.y - searchRadius,cp.x + searchRadius +1,cp.y+ searchRadius +1);
			BoofMiscOps.boundRectangleInside(image,r);
			
			float maxValue=-1;
			int maxX=-1;
			int maxY=-1;
			
			for( int y = r.y0; y < r.y1; y++ ) {
				for( int x = r.x0; x < r.x1; x++ ) {
					float v = intensity.get(x,y);
					
					if( v > maxValue ) {
						maxValue = v;
						maxX = x;
						maxY = y;
					}
				}
			}
			
			Point2D_F32 rp = refinedCorners.get(i);
			rp.x = maxX;
			rp.y = maxY;
		}
	}
}
