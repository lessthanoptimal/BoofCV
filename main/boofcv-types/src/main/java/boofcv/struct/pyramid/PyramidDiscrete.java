/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.pyramid;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * In this implementation the scale factor between each layer is limited to being a positive integer that is evenly
 * divisible by the previous layer. This added constraint allows further optimization to be performed.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class PyramidDiscrete<T extends ImageBase<T>> extends ImagePyramidBase<T> {

	// Configuration for the number of layers in the pyramid
	private final @Getter ConfigDiscreteLevels configLayers = new ConfigDiscreteLevels();

	// scale of each layer relative to the previous layer
	protected int[] levelScales;

	/**
	 * Specifies input image size and behavior of top most layer.
	 *
	 * @param imageType Type of image.
	 * @param saveOriginalReference If a reference to the full resolution image should be saved instead of copied.
	 * Set to false if you don't know what you are doing.
	 * @param configLayers Specifies how the levels are computed
	 */
	protected PyramidDiscrete( ImageType<T> imageType, boolean saveOriginalReference,
							   @Nullable ConfigDiscreteLevels configLayers ) {
		super(imageType, saveOriginalReference);
		if (configLayers != null)
			this.configLayers.setTo(configLayers);
	}

	protected PyramidDiscrete( PyramidDiscrete<T> orig ) {
		super(orig);
		this.configLayers.setTo(orig.configLayers);
	}

	@Override
	public void initialize( int width, int height ) {
		computeScales(width, height);
		super.initialize(width, height);
	}

	protected void computeScales( int width, int height ) {
		levelScales = new int[configLayers.computeLayers(width, height)];
		levelScales[0] = 1;
		for (int i = 1; i < levelScales.length; i++) {
			levelScales[i] = 2*levelScales[i - 1];
		}
	}

//	/**
//	 * Specifies the pyramid's structure. Scale factors are in relative to the input image.
//	 *
//	 * @param scaleFactors Change in scale factor for each layer in the pyramid.
//	 */
//	public void setScaleFactors( int ...scaleFactors ) {
//		for( int i = 1; i < scaleFactors.length; i++ ){
//			if( scaleFactors[i] % scaleFactors[i-1] != 0 ) {
//				throw new IllegalArgumentException("Layer "+i+" is not evenly divisible by its lower layer.");
//			}
//		}
//
//		// set width/height to zero to force the image to be redeclared
//		bottomWidth = bottomHeight = 0;
//		this.scale = scaleFactors.clone();
//		checkScales();
//	}

	public int[] getScales() {
		return levelScales;
	}

	@Override
	public double getScale( int layer ) {
		return levelScales[layer];
	}

	@Override
	public int getNumLayers() {
		return levelScales.length;
	}
}
