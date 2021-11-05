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

package boofcv.factory.feature.associate;

import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;

/**
 * Configuration for associating using descriptors only
 *
 * @author Peter Abeles
 * @see boofcv.abst.feature.associate.AssociateDescription
 */
public class ConfigAssociate implements Configuration {

	/** The association algorithm used. Not always used. */
	public AssociationType type = AssociationType.GREEDY;

	public ConfigAssociateGreedy greedy = new ConfigAssociateGreedy();
	public ConfigAssociateNearestNeighbor nearestNeighbor = new ConfigAssociateNearestNeighbor();

	/**
	 * Specifies the maximum distance allowed between associated pixels. This is only used when creating
	 * an association algorithm that supports 2D information.
	 *
	 * If an absolute value is specified then it's in units of pixels. If relative then it is a fraction of
	 * max(imageWidth, imageHeight)
	 */
	public ConfigLength maximumDistancePixels = ConfigLength.relative(1.0, 0.0);

	@Override
	public void checkValidity() {
		greedy.checkValidity();
		nearestNeighbor.checkValidity();
	}

	public enum AssociationType {
		GREEDY, KD_TREE, RANDOM_FOREST,
	}

	public ConfigAssociate setTo( ConfigAssociate src ) {
		this.type = src.type;
		this.greedy.setTo(src.greedy);
		this.nearestNeighbor.setTo(src.nearestNeighbor);
		return this;
	}

	public ConfigAssociate copy() {
		return new ConfigAssociate().setTo(this);
	}
}
