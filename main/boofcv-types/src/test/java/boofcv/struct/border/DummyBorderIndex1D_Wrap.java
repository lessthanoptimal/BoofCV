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
 * Copy of the original class to get around cyclical dependencies for testing.
 *
 * @author Peter Abeles
 */
public class DummyBorderIndex1D_Wrap extends BorderIndex1D {
	@Override public int getIndex(int index) {
		if( index < 0 )
			return length+index;
		else if( index >= length)
			return index-length;
		else
			return index;
	}

	@Override public DummyBorderIndex1D_Wrap copy() {return new DummyBorderIndex1D_Wrap();}
}
