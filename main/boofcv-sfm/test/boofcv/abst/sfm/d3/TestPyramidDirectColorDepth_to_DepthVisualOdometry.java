/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.sfm.d3;

import boofcv.alg.sfm.DepthSparse3D;
import boofcv.alg.sfm.d3.direct.PyramidDirectColorDepth;
import boofcv.core.image.ConvertImageFilter;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import boofcv.struct.pyramid.ImagePyramid;

/**
 * @author Peter Abeles
 */
public class TestPyramidDirectColorDepth_to_DepthVisualOdometry extends CheckVisualOdometryDepthSim<GrayU8,GrayU16> {

	public TestPyramidDirectColorDepth_to_DepthVisualOdometry() {
		super(GrayU8.class,GrayU16.class);

		setAlgorithm(createAlgorithm());
	}

	protected DepthVisualOdometry<GrayU8,GrayU16> createAlgorithm() {

		ImagePyramid<Planar<GrayU8>> pyramid = FactoryPyramid.discreteGaussian(new int[]{1,2,4},
				-1,2,false, ImageType.pl(1,GrayU8.class));

		PyramidDirectColorDepth<GrayU8> alg = new PyramidDirectColorDepth<>(pyramid);

		ConvertImageFilter<GrayU8,Planar<GrayU8>> convertInput = new ConvertImageFilter<>(
				ImageType.single(GrayU8.class), ImageType.pl(1,GrayU8.class));

		DepthSparse3D<GrayU16> sparse3D = new DepthSparse3D.I<>(depthUnits);

		return new PyramidDirectColorDepth_to_DepthVisualOdometry<>(sparse3D,convertInput,alg,GrayU16.class);
	}
}
