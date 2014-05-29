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

import boofcv.alg.feature.detect.edge.GGradientToEdgeFeatures;
import boofcv.struct.image.*;
import processing.core.PImage;

/**
 * Storage for image gradients
 *
 * @author Peter Abeles
 */
public class SimpleGradient<T extends ImageBase> {
	public T dx;
	public T dy;

	public SimpleGradient(ImageType<T> imageType, int width, int height) {
		dx = imageType.createImage(width,height);
		dy = imageType.createImage(width,height);
	}

	public SimpleGradient(T dx, T dy) {
		this.dx = dx;
		this.dy = dy;
	}

	/**
	 * @see GGradientToEdgeFeatures#intensityAbs
	 */
	public SimpleGray intensityAbs() {
		ImageFloat32 intensity = new ImageFloat32(dx.width,dx.height);
		if( dx instanceof ImageSingleBand ) {
			GGradientToEdgeFeatures.intensityAbs((ImageSingleBand)dx, (ImageSingleBand)dy, intensity);
		} else {
			throw new RuntimeException("Unknown image type");
		}
		return new SimpleGray(intensity);
	}

	/**
	 * @see GGradientToEdgeFeatures#intensityE
	 */
	public SimpleGray intensityE() {
		ImageFloat32 intensity = new ImageFloat32(dx.width,dx.height);
		if( dx instanceof ImageSingleBand ) {
			GGradientToEdgeFeatures.intensityE((ImageSingleBand) dx, (ImageSingleBand) dy, intensity);
		} else {
			throw new RuntimeException("Unknown image type");
		}
		return new SimpleGray(intensity);
	}

	/**
	 * @see GGradientToEdgeFeatures#direction
	 */
	public SimpleGray direction() {
		ImageFloat32 intensity = new ImageFloat32(dx.width,dx.height);
		if( dx instanceof ImageSingleBand ) {
			GGradientToEdgeFeatures.direction((ImageSingleBand) dx, (ImageSingleBand) dy, intensity);
		} else {
			throw new RuntimeException("Unknown image type");
		}
		return new SimpleGray(intensity);
	}

	/**
	 * @see GGradientToEdgeFeatures#direction2
	 */
	public SimpleGray direction2() {
		ImageFloat32 intensity = new ImageFloat32(dx.width,dx.height);
		if( dx instanceof ImageSingleBand ) {
			GGradientToEdgeFeatures.direction2((ImageSingleBand)dx, (ImageSingleBand)dy, intensity);
		} else {
			throw new RuntimeException("Unknown image type");
		}
		return new SimpleGray(intensity);
	}

	public PImage visualize() {
		if( dx instanceof ImageSInt16 ) {
			return VisualizeProcessing.gradient((ImageSInt16) dx, (ImageSInt16) dy);
		} else if( dx instanceof ImageFloat32 ) {
			return VisualizeProcessing.gradient((ImageFloat32) dx, (ImageFloat32) dy);
		} else {
			throw new RuntimeException("Unknown image type");
		}
	}

	public SimpleGray dx() {

		if( dx instanceof ImageSingleBand )
			return new SimpleGray((ImageSingleBand)dx);
		throw new RuntimeException("Unknown image type");
	}

	public SimpleGray dy() {

		if( dy instanceof ImageSingleBand )
			return new SimpleGray((ImageSingleBand)dy);
		throw new RuntimeException("Unknown image type");
	}

	public T getRawDx() {
		return dx;
	}

	public T getRawDy() {
		return dy;
	}
}
