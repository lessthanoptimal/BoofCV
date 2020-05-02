/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.associate;

import boofcv.struct.Configuration;

/**
 * @author Peter Abeles
 */
public class ConfigAssociate implements Configuration {

	/** The association algorithm used. Not always used. */
	public AssociationType type = AssociationType.GREEDY;

	public ConfigAssociateGreedy greedy = new ConfigAssociateGreedy();
	public ConfigAssociateNearestNeighbor nearestNeighbor = new ConfigAssociateNearestNeighbor();

	@Override
	public void checkValidity() {
		greedy.checkValidity();
		nearestNeighbor.checkValidity();
	}

	public enum AssociationType {
		GREEDY, KD_TREE, RANDOM_FOREST,
	}
}
