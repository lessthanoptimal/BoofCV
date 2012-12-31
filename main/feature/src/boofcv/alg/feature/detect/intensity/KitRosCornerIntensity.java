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

package boofcv.alg.feature.detect.intensity;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.feature.detect.intensity.impl.ImplKitRosCornerIntensity;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;

/**
 * <p>
 * Implementation of the Kitchen and Rosenfeld corner detector as described in [1].  Unlike the KLT or Harris corner
 * detectors this corner detector is designed to detect corners on the actual corner.  Because it uses requires the
 * image's local curvature it uses the second derivative, also known as the Hessian.
 * </p>
 *
 * <p>
 * [1] Page 393 of E.R. Davies, "Machine Vision Theory Algorithms Practicalities," 3rd ed. 2005
 * </p>
 *
 * @author Peter Abeles
 */
public class KitRosCornerIntensity {

	public static void process( ImageFloat32 featureIntensity,
								ImageFloat32 derivX, ImageFloat32 derivY,
								ImageFloat32 hessianXX, ImageFloat32 hessianYY , ImageFloat32 hessianXY )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,hessianXX,hessianYY,hessianXY);
		InputSanityCheck.checkSameShape(derivX,featureIntensity);

		ImplKitRosCornerIntensity.process(featureIntensity,derivX,derivY,hessianXX,hessianYY,hessianXY);
	}

	public static void process( ImageFloat32 featureIntensity,
								ImageSInt16 derivX, ImageSInt16 derivY,
								ImageSInt16 hessianXX, ImageSInt16 hessianYY , ImageSInt16 hessianXY )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,hessianXX,hessianYY,hessianXY);
		InputSanityCheck.checkSameShape(derivX,featureIntensity);

		ImplKitRosCornerIntensity.process(featureIntensity,derivX,derivY,hessianXX,hessianYY,hessianXY);
	}
}
