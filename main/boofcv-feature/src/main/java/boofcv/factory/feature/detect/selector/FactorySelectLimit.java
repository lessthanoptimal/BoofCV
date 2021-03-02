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

package boofcv.factory.feature.detect.selector;

import boofcv.alg.feature.detect.selector.*;
import georegression.struct.GeoTuple;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import org.jetbrains.annotations.Nullable;

/**
 * Factory that creates {@link FeatureSelectLimitIntensity}
 *
 * @author Peter Abeles
 */
@SuppressWarnings("rawtypes")
public class FactorySelectLimit {
	/**
	 * Creates and returns {@link FeatureSelectLimitIntensity} using a {@link ConfigSelectLimit configuration}.
	 *
	 * @param config Creates the specified select limit. if null it defaults to {@link ConfigSelectLimit}.
	 */
	public static <Point extends GeoTuple<Point>>
	FeatureSelectLimitIntensity<Point> intensity( @Nullable ConfigSelectLimit config, Class<Point> type ) {
		FeatureSelectLimitIntensity<Point> ret = intensity(config);
		ret.setSampler(imageSampler(type));
		return ret;
	}

	/**
	 * Creates and returns {@link FeatureSelectLimitIntensity} using a {@link ConfigSelectLimit configuration}.
	 *
	 * @param config Creates the specified select limit. if null it defaults to {@link ConfigSelectLimit}.
	 */
	public static <Point>
	FeatureSelectLimitIntensity<Point> intensity( @Nullable ConfigSelectLimit config ) {
		if (config == null)
			config = new ConfigSelectLimit();

		return switch (config.type) {
			case SELECT_N -> new FeatureSelectNBest<>();
			case RANDOM -> new ConvertLimitToIntensity<>(new FeatureSelectRandom<Point>(config.randomSeed));
			case UNIFORM -> new FeatureSelectUniformBest<>();
		};
	}

	/**
	 * Creates the correct {@link SampleIntensityImage} for the given point type
	 */
	public static <Point extends GeoTuple<Point>>
	SampleIntensity<Point> imageSampler( Class<Point> type ) {
		if (type == Point2D_I16.class) {
			return (SampleIntensity)new SampleIntensityImage.I16();
		} else if (type == Point2D_F32.class) {
			return (SampleIntensity)new SampleIntensityImage.F32();
		} else if (type == Point2D_F64.class) {
			return (SampleIntensity)new SampleIntensityImage.F64();
		} else {
			throw new IllegalArgumentException("Unknown point type " + type.getSimpleName());
		}
	}

	/**
	 * Creates and returns {@link FeatureSelectLimit} using a {@link ConfigSelectLimit configuration}.
	 *
	 * @param config Creates the specified select limit. if null it defaults to {@link ConfigSelectLimit}.
	 */
	public static <Point extends GeoTuple<Point>>
	FeatureSelectLimit<Point> spatial( @Nullable ConfigSelectLimit config, Class<Point> type ) {
		if (config == null)
			config = new ConfigSelectLimit();

		return switch (config.type) {
			case SELECT_N -> new FeatureSelectN<>();
			case RANDOM -> new FeatureSelectRandom<>(config.randomSeed);
			case UNIFORM -> {
				FeatureSelectLimit ret;
				if (type == Point2D_I16.class) {
					ret = new FeatureSelectUniform.I16();
				} else if (type == Point2D_F32.class) {
					ret = new FeatureSelectUniform.F32();
				} else if (type == Point2D_F64.class) {
					ret = new FeatureSelectUniform.F64();
				} else {
					throw new IllegalArgumentException("Unknown point type " + type.getSimpleName());
				}
				yield ret;
			}
		};
	}
}
