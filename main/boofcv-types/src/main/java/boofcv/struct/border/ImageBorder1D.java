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

package boofcv.struct.border;

/**
 * Interface for classes that modify the coordinate of a pixel so that it will always reference a pixel inside
 * the image. This is done independently for x and y axes. E.g. x=-1 will become x=0.
 *
 * @author Peter Abeles
 */
public interface ImageBorder1D {
	BorderIndex1D getRowWrap();

	BorderIndex1D getColWrap();
}
