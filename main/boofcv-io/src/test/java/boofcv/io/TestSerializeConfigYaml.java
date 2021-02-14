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

package boofcv.io;

import boofcv.factory.geo.EnumPNP;
import boofcv.struct.Configuration;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.io.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestSerializeConfigYaml extends BoofStandardJUnit {
	@Test void encode_decode() {
		var orig = new ConfigBoof();
		orig.childA.valA = 88;
		orig.childA.valB = -2;
		orig.childA.valC = 99.2;
		orig.childA.external.stuff = "moo";
		orig.childB.valA = 77;
		orig.childB.valB = -6;
		orig.childB.valC = 101.1;
		orig.childB.external.stuff = "foo";
		orig.valA = -123;
		orig.valB = EnumPNP.IPPE;
		orig.valC = "Test";

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		SerializeConfigYaml.serialize(orig, new OutputStreamWriter(stream));

		Reader reader = new InputStreamReader(new ByteArrayInputStream(stream.toByteArray()), UTF_8);
		ConfigBoof found = SerializeConfigYaml.deserialize(reader);

		// See if the serialization/deserialization worked
		assertEquals(orig.childA.valA, found.childA.valA);
		assertEquals(orig.childA.valB, found.childA.valB);
		assertEquals(orig.childA.valC, found.childA.valC);
		assertEquals(orig.childA.external.stuff, found.childA.external.stuff);
		assertEquals(orig.childB.valA, found.childB.valA);
		assertEquals(orig.childB.valB, found.childB.valB);
		assertEquals(orig.childB.valC, found.childB.valC);
		assertEquals(orig.childB.external.stuff, found.childB.external.stuff);
		assertEquals(orig.valA, found.valA);
		assertEquals(orig.valB, found.valB);
		assertEquals(orig.valC, found.valC);
	}

	// Create a chain of configurations, both native BoofCV and External, that will exercise the system

	public static class ConfigExternal {
		public String stuff = "default";

		public void setTo( ConfigExternal src ) {
			this.stuff = src.stuff;
		}
	}

	public static class ConfigChild implements Configuration {
		public int valA;
		public byte valB;
		public double valC;

		public final ConfigExternal external = new ConfigExternal();

		public void setTo( ConfigChild src ) {
			this.valA = src.valA;
			this.valB = src.valB;
			this.valC = src.valC;
			this.external.setTo(src.external);
		}

		@Override public void checkValidity() {}
	}

	public static class ConfigBoof implements Configuration {

		public short valA;
		public EnumPNP valB = EnumPNP.EPNP;
		public String valC = "default";

		public ConfigChild childA = new ConfigChild();
		public ConfigChild childB = new ConfigChild();

		public void setTo( ConfigBoof src ) {
			this.valA = src.valA;
			this.valB = src.valB;
			this.valC = src.valC;
			childA.setTo(src.childA);
			childB.setTo(src.childB);
		}

		@Override public void checkValidity() {}
	}
}
