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

package boofcv.factory.feature.detect.extract;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.override.BOverrideClass;
import boofcv.override.BOverrideManager;
import org.jetbrains.annotations.Nullable;

/**
 * Location of override functions related to {@link FactoryFeatureExtractor}.
 *
 * @author Peter Abeles
 */
public class BOverrideFactoryFeatureExtractor extends BOverrideClass {

	public static @Nullable NonMax nonmax;
	public static @Nullable NonMaxCandidate nonmaxCandidate;

	static {
		BOverrideManager.register(BOverrideFactoryFeatureExtractor.class);
	}

	public interface NonMax {
		NonMaxSuppression process( ConfigExtract config );
	}

	public interface NonMaxCandidate {
		NonMaxSuppression process( ConfigExtract config );
	}
}
