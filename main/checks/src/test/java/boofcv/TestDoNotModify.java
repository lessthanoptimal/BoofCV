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

import boofcv.io.UtilIO;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests which make sure no one commits code which modifies critical lines in a configuration needed for
 * production.
 *
 * @author Peter Abeles
 */
public class TestDoNotModify {
	@Test void buildJava11() throws IOException {
		String pathBuild = UtilIO.path("build.gradle");
		String[] lines  = IOUtils.toString(new FileInputStream(pathBuild), StandardCharsets.UTF_8).split("\\R+");
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].equals("    ext.build_java11 = true // IntelliJ has issues when this is set to true"))
				return;
		}
		fail("build java 11 line not found");
	}
}
