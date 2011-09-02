/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package boofcv.alg.feature.detect.interest;

import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.transform.gss.UtilScaleSpace;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.factory.feature.detect.interest.FactoryCornerDetector;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import jgrl.struct.point.Point2D_I16;
import jgrl.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Peter Abeles
 */
public class TestGeneralFeatureDetector extends GenericFeatureDetector {

	AnyImageDerivative<ImageFloat32,ImageFloat32> computeDerivative =
			UtilScaleSpace.createDerivatives(ImageFloat32.class, FactoryImageGenerator.create(ImageFloat32.class));

	@Override
	protected Object createDetector( int maxFeatures ) {
//		return FactoryBlobDetector.createLaplace(2,0,maxFeatures,ImageFloat32.class,HessianBlobIntensity.Type.DETERMINANT);
		return FactoryCornerDetector.createHarris(2,0,maxFeatures,ImageFloat32.class);
	}

	@Override
	protected List<Point2D_I32> detectFeature(ImageFloat32 input, double[] scales, Object detector) {
		GeneralFeatureDetector<ImageFloat32,ImageFloat32> d =
				(GeneralFeatureDetector<ImageFloat32,ImageFloat32>)detector;

		computeDerivative.setInput(input);

		ImageFloat32 derivX = computeDerivative.getDerivative(true);
		ImageFloat32 derivY = computeDerivative.getDerivative(false);
		ImageFloat32 derivXX = computeDerivative.getDerivative(true,true);
		ImageFloat32 derivYY = computeDerivative.getDerivative(false,false);
		ImageFloat32 derivXY = computeDerivative.getDerivative(true,false);

		d.process(input,derivX,derivY,derivXX,derivYY,derivXY);

		QueueCorner found = d.getFeatures();
		List<Point2D_I32> ret = new ArrayList<Point2D_I32>();
		for( int i = 0; i < found.num; i++ ) {
			Point2D_I16 p = found.get(i);
			ret.add( new Point2D_I32(p.x,p.y));
		}
		return ret;
	}
}
