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

package boofcv.alg.geo.robust;

import org.ddogleg.fitting.modelset.ModelMatcher;

import java.util.List;

/**
 * Base class for when you want to change the output type of a {@link ModelMatcherMultiview}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class MmModelChanger<ModelA, ModelB, Point> implements ModelMatcher<ModelB, Point> {

	protected ModelMatcher<ModelA, Point> mm;

	protected MmModelChanger( ModelMatcher<ModelA, Point> mm ) {
		this.mm = mm;
	}

	protected MmModelChanger() {}

	@Override
	public boolean process( List<Point> dataSet ) {
		return mm.process(dataSet);
	}

	@Override
	public List<Point> getMatchSet() {
		return mm.getMatchSet();
	}

	@Override
	public int getInputIndex( int matchIndex ) {
		return mm.getInputIndex(matchIndex);
	}

	@Override
	public double getFitQuality() {
		return mm.getFitQuality();
	}

	@Override
	public int getMinimumSize() {
		return mm.getMinimumSize();
	}

	@Override
	public Class<Point> getPointType() {
		return mm.getPointType();
	}
}
