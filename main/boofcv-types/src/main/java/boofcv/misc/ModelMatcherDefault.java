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

package boofcv.misc;

import org.ddogleg.fitting.modelset.ModelMatcher;

import java.util.List;

/**
 * Provides default implementations of ModelMatcher functions.
 */
@SuppressWarnings("NullAway")
//@formatter:off
public class ModelMatcherDefault<Model,Point> implements ModelMatcher<Model,Point> {
	@Override public boolean process( List<Point> dataSet ) { return true; }
	@Override public Model getModelParameters() { return null; }
	@Override public List<Point> getMatchSet() { return null; }
	@Override public int getInputIndex( int matchIndex ) { return 0; }
	@Override public double getFitQuality() { return 0; }
	@Override public int getMinimumSize() { return 0; }
	@Override public void reset() {}
	@Override public Class<Point> getPointType() { return null; }
	@Override public Class<Model> getModelType() { return null; }
}
//@formatter:on
