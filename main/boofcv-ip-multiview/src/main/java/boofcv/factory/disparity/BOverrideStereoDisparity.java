/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.disparity;

import boofcv.abst.disparity.StereoDisparityBlockMatch;
import boofcv.abst.disparity.StereoDisparitySgm;
import boofcv.override.BOverrideClass;
import boofcv.override.BOverrideManager;
import boofcv.struct.image.ImageGray;
import org.jetbrains.annotations.Nullable;

/// Override for [StereoDisparity]. Allows you to specify comparable native or other implementations.
public class BOverrideStereoDisparity extends BOverrideClass {
	static {
		BOverrideManager.register(BOverrideStereoDisparity.class);
	}

	public static @Nullable CreateBlockMatch opCreateBlockMatch = null;
	public static @Nullable CreateBlockMatch5 opCreateBlockMatch5 = null;
	public static @Nullable CreateSgm opCreateSgm = null;

	public static <T extends ImageGray<T>, DI extends ImageGray<DI>>
	@Nullable StereoDisparityBlockMatch<T, DI> createBlockMatch( ConfigDisparityBM config, Class<T> imageType, Class<DI> dispType ) {
		if (opCreateBlockMatch != null) {
			try {
				return opCreateBlockMatch.create(config, imageType, dispType);
			} catch (RuntimeException ignore) {
			}
		}
		return null;
	}

	public static <T extends ImageGray<T>, DI extends ImageGray<DI>>
	@Nullable StereoDisparityBlockMatch<T, DI> createBlockMatch5( ConfigDisparityBMBest5 config, Class<T> imageType, Class<DI> dispType ) {
		if (opCreateBlockMatch5 != null) {
			try {
				return opCreateBlockMatch5.create(config, imageType, dispType);
			} catch (RuntimeException ignore) {
			}
		}
		return null;
	}

	public static <T extends ImageGray<T>, DI extends ImageGray<DI>>
	@Nullable StereoDisparitySgm<T, DI> createSgm( ConfigDisparitySGM config, Class<T> imageType, Class<DI> dispType ) {
		if (opCreateSgm != null) {
			try {
				return opCreateSgm.create(config, imageType, dispType);
			} catch (RuntimeException ignore) {
			}
		}
		return null;
	}

	public interface CreateBlockMatch {
		<T extends ImageGray<T>, DI extends ImageGray<DI>>
		@Nullable StereoDisparityBlockMatch<T, DI> create( ConfigDisparityBM config, Class<T> imageType, Class<DI> dispType );
	}

	public interface CreateBlockMatch5 {
		<T extends ImageGray<T>, DI extends ImageGray<DI>>
		@Nullable StereoDisparityBlockMatch<T, DI> create( ConfigDisparityBMBest5 config, Class<T> imageType, Class<DI> dispType );
	}

	public interface CreateSgm {
		<T extends ImageGray<T>, DI extends ImageGray<DI>>
		@Nullable StereoDisparitySgm<T, DI> create( ConfigDisparitySGM config, Class<T> imageType, Class<DI> dispType );
	}
}
