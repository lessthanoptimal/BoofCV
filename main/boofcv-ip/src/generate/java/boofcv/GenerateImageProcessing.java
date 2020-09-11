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

package boofcv;

import boofcv.alg.misc.impl.GenerateImplImageBandMath;
import boofcv.alg.misc.impl.GenerateImplImageStatistics;
import boofcv.alg.misc.impl.GenerateImplPixelMath;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class GenerateImageProcessing {
	public static void main( String[] args )
			throws NoSuchMethodException, IllegalAccessException,
			InvocationTargetException, InstantiationException, FileNotFoundException {
		Class<CodeGeneratorBase>[] generators = new Class[]{
				GenerateImplImageBandMath.class,
				GenerateImplPixelMath.class,
				GenerateImplImageStatistics.class,
				GenerateImplPixelMath.class,
		};

		for (Class c : generators) {
			System.out.println("Generating: " + c.getSimpleName());
			CodeGeneratorBase g = (CodeGeneratorBase)c.getConstructor().newInstance();
			g.setModuleName("boofcv-ip");
			g.setOverwrite(true);
			g.generate();
		}
	}
}
