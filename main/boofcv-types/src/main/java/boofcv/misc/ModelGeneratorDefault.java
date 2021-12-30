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

import org.ddogleg.fitting.modelset.ModelGenerator;

import java.util.List;

/** Provides default implementations of {@link ModelGenerator}. Primarily used for testing. */
public class ModelGeneratorDefault<Model, Point> implements ModelGenerator<Model, Point> {
	@Override public boolean generate( List<Point> dataSet, Model output ) {return true;}

	@Override public int getMinimumPoints() {return 0;}
}
