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

package boofcv.abst.feature.detect.interest;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageBase;

/**
 * General interface for point features. If multiple types of point features are detected
 * then they are segmented into different sets which can be requested independently.
 *
 * @author Peter Abeles
 */
public interface PointDetector<T extends ImageBase<T>> {

	void process( T image );

	int totalSets();

	QueueCorner getPointSet(int which );
}
