/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.filter.binary;

import boofcv.abst.filter.binary.BinaryLabelContourFinder;
import boofcv.override.BOverrideClass;
import boofcv.override.BOverrideManager;

/**
 * Override for {@link FactoryBinaryContourFinder}.+
 *
 *
 * @author Peter Abeles
 */
public class BOverrideFactoryBinaryContourFinder extends BOverrideClass {

	public static Chang2004 chang2004;

	static {
		BOverrideManager.register(BOverrideFactoryBinaryContourFinder.class);
	}

	public interface Chang2004 {
		BinaryLabelContourFinder createChang2004();
	}

}
