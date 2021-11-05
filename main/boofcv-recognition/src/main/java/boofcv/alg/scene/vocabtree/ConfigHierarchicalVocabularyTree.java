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

package boofcv.alg.scene.vocabtree;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link HierarchicalVocabularyTree}.
 *
 * @author Peter Abeles
 */
public class ConfigHierarchicalVocabularyTree implements Configuration {
	/** Number of children for each node */
	public int branchFactor = -1;

	/** Maximum number of levels in the tree */
	public int maximumLevel = -1;

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(branchFactor > 0, "Branch factor must be a positive integer");
		BoofMiscOps.checkTrue(maximumLevel > 0, "Maximum level must be a positive integer");
	}

	public ConfigHierarchicalVocabularyTree setTo( ConfigHierarchicalVocabularyTree src ) {
		this.branchFactor = src.branchFactor;
		this.maximumLevel = src.maximumLevel;
		return this;
	}
}
