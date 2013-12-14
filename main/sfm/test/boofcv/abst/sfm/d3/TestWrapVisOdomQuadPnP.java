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

package boofcv.abst.sfm.d3;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detdesc.DetectDescribeMulti;
import boofcv.abst.feature.detdesc.DetectDescribeMultiFusion;
import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.abst.feature.detect.interest.DetectorInterestPointMulti;
import boofcv.abst.feature.detect.interest.GeneralToInterestMulti;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.struct.image.ImageFloat32;

/**
 * @author Peter Abeles
 */
public class TestWrapVisOdomQuadPnP extends CheckVisualOdometryStereoSim<ImageFloat32> {

	public TestWrapVisOdomQuadPnP() {
		super(ImageFloat32.class,0.3);
	}

	@Override
	public StereoVisualOdometry<ImageFloat32> createAlgorithm() {
		GeneralFeatureIntensity intensity =
				FactoryIntensityPoint.shiTomasi(1, false, ImageFloat32.class);
		NonMaxSuppression nonmax = FactoryFeatureExtractor.nonmax(new ConfigExtract(2, 1, 0, true, false, true));
		GeneralFeatureDetector<ImageFloat32,ImageFloat32> general =
				new GeneralFeatureDetector<ImageFloat32,ImageFloat32>(intensity,nonmax);
		general.setMaxFeatures(600);
		DetectorInterestPointMulti detector = new GeneralToInterestMulti(general,2,ImageFloat32.class,ImageFloat32.class);
		DescribeRegionPoint describe = FactoryDescribeRegionPoint.surfFast(null, ImageFloat32.class);
		DetectDescribeMulti detDescMulti =  new DetectDescribeMultiFusion(detector,null,describe);

		return FactoryVisualOdometry.stereoQuadPnP(1.5, 0.5, 200, Double.MAX_VALUE, 300, 50, detDescMulti, ImageFloat32.class);
	}
}
