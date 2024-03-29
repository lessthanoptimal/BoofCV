/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.template;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

class TestTemplateSqDiffNormed extends BoofStandardJUnit {
	@Nested
	class F32 extends GeneralTemplateMatchTests<GrayF32> {
		F32() {
			super(new TemplateSqDiffNormed.F32(), GrayF32.class);
		}
	}

	@Nested
	class U8 extends GeneralTemplateMatchTests<GrayU8> {
		U8() {
			super(new TemplateSqDiffNormed.U8(), GrayU8.class);
		}
	}
}
