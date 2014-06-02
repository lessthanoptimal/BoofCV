/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.processing;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.flow.DenseOpticalFlow;
import boofcv.abst.segmentation.ImageSegmentation;
import boofcv.abst.tracker.ConfigCirculantTracker;
import boofcv.abst.tracker.ConfigComaniciu2003;
import boofcv.abst.tracker.ConfigTld;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.flow.ConfigBroxWarping;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.flow.ConfigHornSchunck;
import boofcv.factory.flow.ConfigHornSchunckPyramid;
import boofcv.factory.flow.ConfigOpticalFlowBlockPyramid;
import boofcv.factory.flow.FactoryDenseOpticalFlow;
import boofcv.factory.segmentation.*;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.struct.image.*;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

/**
 * Convince class for Processing.
 *
 * @author Peter Abeles
 */
// TODO Stitch two images together
// TODO Remove perspective
// TODO Detect lines
// TODO image enhancement

public class Boof {

	PApplet parent;

	public Boof(PApplet parent) {
		this.parent = parent;
	}

	public static SimpleGray gray(PImage image, ImageDataType type) {
		if (type == ImageDataType.F32) {
			ImageFloat32 out = new ImageFloat32(image.width, image.height);

			switch (image.format) {
				case PConstants.RGB:
				case PConstants.ARGB:
					ConvertProcessing.convert_RGB_F32(image, out);
					break;

				default:
					throw new RuntimeException("Unsupported image type");
			}

			return new SimpleGray(out);
		} else if (type == ImageDataType.U8) {
			ImageUInt8 out = new ImageUInt8(image.width, image.height);

			switch (image.format) {
				case PConstants.RGB:
				case PConstants.ARGB:
					ConvertProcessing.convert_RGB_U8(image, out);
					break;

				default:
					throw new RuntimeException("Unsupported image type");
			}

			return new SimpleGray(out);
		} else {
			throw new RuntimeException("Unsupport type: " + type);
		}
	}

	public static SimpleColor colorMS(PImage image, ImageDataType type) {
		if (type == ImageDataType.F32) {
			MultiSpectral<ImageFloat32> out =
					new MultiSpectral<ImageFloat32>(ImageFloat32.class, image.width, image.height, 3);

			switch (image.format) {
				case PConstants.RGB:
				case PConstants.ARGB:
					ConvertProcessing.convert_RGB_MSF32(image, out);
					break;

				default:
					throw new RuntimeException("Unsupported image type");
			}

			return new SimpleColor(out);
		} else if (type == ImageDataType.U8) {
			MultiSpectral<ImageUInt8> out =
					new MultiSpectral<ImageUInt8>(ImageUInt8.class, image.width, image.height, 3);

			switch (image.format) {
				case PConstants.RGB:
				case PConstants.ARGB:
					ConvertProcessing.convert_RGB_MSU8(image, out);
					break;

				default:
					throw new RuntimeException("Unsupported image type");
			}

			return new SimpleColor(out);
		} else {
			throw new RuntimeException("Unsupport type: " + type);
		}
	}

	public static SimpleTrackerPoints trackerKlt(PkltConfig config,
												ConfigGeneralDetector configExtract,
												ImageDataType imageType) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		Class derivType = GImageDerivativeOps.getDerivativeType(inputType);

		PointTracker tracker = FactoryPointTracker.klt(config, configExtract, inputType, derivType);

		return new SimpleTrackerPoints(tracker, inputType);
	}

	public static SimpleTrackerObject trackerTld(ConfigTld config, ImageDataType imageType) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		TrackerObjectQuad tracker = FactoryTrackerObjectQuad.tld(config,inputType);
		return new SimpleTrackerObject(tracker);
	}

	public static SimpleTrackerObject trackerMeanShiftComaniciu(ConfigComaniciu2003 config, ImageType imageType) {
		TrackerObjectQuad tracker = FactoryTrackerObjectQuad.meanShiftComaniciu2003(config, imageType);
		return new SimpleTrackerObject(tracker);
	}

	public static SimpleTrackerObject trackerCirculant(ConfigCirculantTracker config, ImageDataType imageType) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		TrackerObjectQuad tracker = FactoryTrackerObjectQuad.circulant(config, inputType);
		return new SimpleTrackerObject(tracker);
	}

	public static SimpleTrackerObject trackerSparseFlow(SfotConfig config, ImageDataType imageType) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		Class derivType = GImageDerivativeOps.getDerivativeType(inputType);
		TrackerObjectQuad tracker = FactoryTrackerObjectQuad.sparseFlow(config, inputType, derivType);
		return new SimpleTrackerObject(tracker);
	}

	public static SimpleImageSegmentation segmentMeanShift( ConfigSegmentMeanShift config , ImageType imageType ) {
		ImageSegmentation alg = FactoryImageSegmentation.meanShift(config,imageType);
		return new SimpleImageSegmentation(alg);
	}

	public static SimpleImageSegmentation segmentSlic( ConfigSlic config , ImageType imageType ) {
		ImageSegmentation alg = FactoryImageSegmentation.slic(config, imageType);
		return new SimpleImageSegmentation(alg);
	}

	public static SimpleImageSegmentation segmentFH04( ConfigFh04 config , ImageType imageType ) {
		ImageSegmentation alg = FactoryImageSegmentation.fh04(config, imageType);
		return new SimpleImageSegmentation(alg);
	}

	public static SimpleImageSegmentation segmentWatershed( ConfigWatershed config , ImageType imageType ) {
		ImageSegmentation alg = FactoryImageSegmentation.watershed(config, imageType);
		return new SimpleImageSegmentation(alg);
	}

	public static SimpleDenseOpticalFlow flowKlt( PkltConfig configKlt, int radius , ImageDataType imageType ) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		Class derivType = GImageDerivativeOps.getDerivativeType(inputType);
		DenseOpticalFlow flow = FactoryDenseOpticalFlow.flowKlt(configKlt,radius,inputType,derivType);
		return new SimpleDenseOpticalFlow(flow);
	}

	public static SimpleDenseOpticalFlow flowRegion( ConfigOpticalFlowBlockPyramid config, ImageDataType imageType ) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		DenseOpticalFlow flow = FactoryDenseOpticalFlow.region(config, inputType);
		return new SimpleDenseOpticalFlow(flow);
	}

	public static SimpleDenseOpticalFlow flowHornSchunck( ConfigHornSchunck config, ImageDataType imageType ) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		DenseOpticalFlow flow = FactoryDenseOpticalFlow.hornSchunck(config, inputType);
		return new SimpleDenseOpticalFlow(flow);
	}

	public static SimpleDenseOpticalFlow flowHornSchunckPyramid( ConfigHornSchunckPyramid config, ImageDataType imageType ) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		DenseOpticalFlow flow = FactoryDenseOpticalFlow.hornSchunckPyramid(config, inputType);
		return new SimpleDenseOpticalFlow(flow);
	}

	public static SimpleDenseOpticalFlow flowBroxWarping( ConfigBroxWarping configKlt, ImageDataType imageType ) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		DenseOpticalFlow flow = FactoryDenseOpticalFlow.broxWarping(configKlt,inputType);
		return new SimpleDenseOpticalFlow(flow);
	}

	public static SimpleDetectDescribePoint detectSurf( boolean stable , ImageDataType imageType ) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);

		DetectDescribePoint ddp;
		if( stable )
			ddp = FactoryDetectDescribe.surfStable(null,null,null,inputType);
		else
			ddp = FactoryDetectDescribe.surfFast(null,null,null,inputType);

		return new SimpleDetectDescribePoint(ddp, ImageType.single(inputType));
	}

	public static SimpleDetectDescribePoint detectSift( ImageDataType imageType ) {
		if( imageType != ImageDataType.F32 )
			throw new IllegalArgumentException("Only ImageFloat32 is supported, e.g. ImageDataType.F32");

		DetectDescribePoint ddp = FactoryDetectDescribe.sift(null,null,null,null);

		return new SimpleDetectDescribePoint(ddp, ImageType.single(ImageFloat32.class));
	}

	public static SimpleAssociateDescription associateGreedy( SimpleDetectDescribePoint detector ,
															  boolean backwardsValidation ) {

		ScoreAssociation score = FactoryAssociation.defaultScore(detector.detectDescribe.getDescriptionType());

		AssociateDescription assoc = FactoryAssociation.greedy(score,Double.MAX_VALUE,backwardsValidation);

		return new SimpleAssociateDescription(assoc);
	}

}
