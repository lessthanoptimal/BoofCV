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

package boofcv;

import com.peterebeles.autocode.AutocodeConcurrent;

import java.io.File;
import java.io.IOException;

/**
 * Generates concurrent implementations of classes using comment based hints
 *
 * @author Peter Abeles
 */
@SuppressWarnings("NullAway")
public class AutocodeConcurrentApp {
	public static void main( String[] args ) throws IOException {
		String[] directories = new String[]{
				"main/boofcv-ip/src/main/java/boofcv/alg/filter/derivative/impl/",
				"main/boofcv-ip/src/main/java/boofcv/alg/filter/blur/impl/",
				"main/boofcv-ip/src/main/java/boofcv/alg/filter/convolve/noborder/",
				"main/boofcv-ip/src/main/java/boofcv/alg/filter/binary/impl",
				"main/boofcv-ip/src/main/java/boofcv/alg/filter/binary",
				"main/boofcv-ip/src/main/java/boofcv/alg/filter/misc/impl/",
				"main/boofcv-ip/src/main/java/boofcv/alg/misc/impl/",
				"main/boofcv-ip/src/main/java/boofcv/alg/color/impl",
				"main/boofcv-ip/src/main/java/boofcv/alg/enhance/impl/",
				"main/boofcv-ip/src/main/java/boofcv/core/image/impl/",
				"main/boofcv-ip/src/main/java/boofcv/core/encoding/impl",
				"main/boofcv-ip/src/main/java/boofcv/alg/transform/ii/impl/",
				"main/boofcv-ip/src/main/java/boofcv/alg/transform/pyramid/impl/",
				"main/boofcv-ip/src/main/java/boofcv/alg/transform/census/impl/",
				"main/boofcv-feature/src/main/java/boofcv/alg/background/moving",
				"main/boofcv-feature/src/main/java/boofcv/alg/background/stationary",
				"main/boofcv-feature/src/main/java/boofcv/alg/feature/detect/edge/impl",
				"main/boofcv-feature/src/main/java/boofcv/alg/feature/detect/intensity/impl",
				"main/boofcv-feature/src/main/java/boofcv/alg/feature/associate",
				"main/boofcv-io/src/main/java/boofcv/io/image/impl",
		};

		String[] files = new String[]{
//				"main/boofcv-ip/src/main/java/boofcv/alg/enhance/impl/ImplEnhanceHistogram.java"
		};

		File rootDir = AutocodeMasterApp.findPathToProjectRoot();
		System.out.println("Autocode Concurrent: current=" + new File(".").getAbsolutePath());
		System.out.println("                     root=" + rootDir.getAbsolutePath());

		for (String f : directories) {
			System.out.println("directory " + f);
			AutocodeConcurrent.convertDir(new File(rootDir, f), "\\S+\\.java", "\\S+MT\\S+");
		}

		for (String f : files) {
			System.out.println("File " + f);
			AutocodeConcurrent.convertFile(new File(rootDir, f));
		}
	}
}
