/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.template;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestTemplateNCC {

	@Test
	public void instanceF32() {
		TemplateNCC.F32 alg = new TemplateNCC.F32();

		new GeneralTemplateMatchTests<GrayF32>(alg, GrayF32.class) {
		}.allTests();
	}

	@Test
	public void instanceU8() {
		TemplateNCC.U8 alg = new TemplateNCC.U8();

		new GeneralTemplateMatchTests<GrayU8>(alg, GrayU8.class) {
		}.allTests();
	}
}
