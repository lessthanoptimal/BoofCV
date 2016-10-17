/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.segmentation;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.segmentation.ComputeRegionMeanColor;
import boofcv.alg.segmentation.ImageSegmentationOps;
import boofcv.alg.segmentation.ms.MergeSmallRegions;
import boofcv.alg.segmentation.watershed.WatershedVincentSoille1991;
import boofcv.core.image.GConvertImage;
import boofcv.struct.ConnectRule;
import boofcv.struct.feature.ColorQueue_F32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Wrapper around {@link WatershedVincentSoille1991} for {@link ImageSuperpixels}.  Watershed regions
 * and small regions are merged together.  When merging regions a preference is given to regions which are the
 * most similar in color intensity.
 *
 * @author Peter Abeles
 */
public class Watershed_to_ImageSuperpixels<T extends ImageBase> implements ImageSuperpixels<T> {

	private WatershedVincentSoille1991 alg;
	private ConnectRule rule;

	private GrayU8 converted = new GrayU8(1,1);

	private MergeSmallRegions<GrayU8> pruneSmall;

	private GrowQueue_I32 regionMemberCount = new GrowQueue_I32();
	private FastQueue<float[]> regionColor = new ColorQueue_F32(1);

	private int numRegions;

	// this isn't really needed since image type is determined when segment is called
	// but is required by the interface
	private ImageType<T> imageType;

	public Watershed_to_ImageSuperpixels(WatershedVincentSoille1991 alg, int minimumSize, ConnectRule rule) {
		this.alg = alg;
		this.rule = rule;

		if( minimumSize > 0 )
			pruneSmall = new MergeSmallRegions<>(minimumSize,rule,new ComputeRegionMeanColor.U8());
	}

	@Override
	public void segment(T input, GrayS32 output) {
		InputSanityCheck.checkSameShape(input,output);
		converted.reshape(input.width,input.height);

		GConvertImage.convert(input,converted);

		// segment the image
		alg.process(converted);
		alg.removeWatersheds();

		numRegions = alg.getTotalRegions();
		GrayS32 pixelToRegion = alg.getOutput();

		// Merge small regions together
		if( pruneSmall != null ) {
			regionMemberCount.resize(numRegions);
			regionColor.resize(numRegions);

			ImageSegmentationOps.countRegionPixels(pixelToRegion,numRegions,regionMemberCount.data);
			pruneSmall.process(converted,pixelToRegion,regionMemberCount,regionColor);

			numRegions = regionMemberCount.size();
		}

		output.setTo(pixelToRegion);
	}

	@Override
	public int getTotalSuperpixels() {
		return numRegions;
	}

	@Override
	public ConnectRule getRule() {
		return rule;
	}

	public ImageType<T> getImageType() {
		return imageType;
	}

	public void setImageType(ImageType<T> imageType) {
		this.imageType = imageType;
	}
}
