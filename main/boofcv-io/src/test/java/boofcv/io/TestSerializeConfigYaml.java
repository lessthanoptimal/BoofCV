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
import org.ddogleg.struct.FastArray;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.ejml.UtilEjml.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestSerializeConfigYaml extends BoofStandardJUnit {
	SerializeConfigYaml serialization = new SerializeConfigYaml();

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
		serialization.serialize(orig, null, new OutputStreamWriter(stream, UTF_8));

		Reader reader = new InputStreamReader(new ByteArrayInputStream(stream.toByteArray()), UTF_8);
		ConfigBoof found = serialization.deserialize(reader);

		// See if the serialization/deserialization worked
		checkIdentical(orig, found);
	}

	private void checkIdentical( ConfigBoof orig, ConfigBoof found ) {
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

	@Test void encode_decode_lists() {
		var orig = new ConfigLists();
		orig.valA = 88;
		orig.strings.add("asd");
		orig.strings.add("fgh");
		orig.externals.add(new ConfigExternal());
		orig.externals.get(0).stuff = "omg";

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		serialization.serialize(orig, null, new OutputStreamWriter(stream, UTF_8));

		Reader reader = new InputStreamReader(new ByteArrayInputStream(stream.toByteArray()), UTF_8);
		ConfigLists found = serialization.deserialize(reader);

		assertEquals(orig.valA, found.valA);
		assertEquals(orig.strings.size(), found.strings.size());
		for (int i = 0; i < orig.strings.size(); i++) {
			assertEquals(orig.strings.get(i), found.strings.get(i));
		}
		assertEquals(orig.externals.size(), found.externals.size());
		assertEquals(orig.externals.get(0).stuff, found.externals.get(0).stuff);
	}

	@Test void encode_decode_FastArray() {
		var orig = new ConfigLists();
		orig.enums.add(EnumPNP.EPNP);
		orig.enums.add(EnumPNP.EPNP);
		orig.enums.add(EnumPNP.IPPE);

		orig.externals.add(new ConfigExternal());
		orig.externals.add(new ConfigExternal());
		orig.externals.get(1).stuff = "Moo";

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		serialization.serialize(orig, null, new OutputStreamWriter(stream, UTF_8));
		Reader reader = new InputStreamReader(new ByteArrayInputStream(stream.toByteArray()), UTF_8);
		ConfigLists found = serialization.deserialize(reader);

		assertEquals(orig.enums.size(), found.enums.size());
		for (int i = 0; i < orig.enums.size(); i++) {
			assertEquals(orig.enums.get(i), found.enums.get(i));
		}
		assertEquals(orig.externals.size(), found.externals.size());
		for (int i = 0; i < orig.externals.size(); i++) {
			assertEquals(orig.externals.get(i).stuff, found.externals.get(i).stuff);
		}
	}

	/**
	 * Checks to see if the list of active fields is obeys by serialization
	 */
	@Test void encode_ObeysActive() {
		var orig = new ConfigBoof();
		orig.active.add("childB");
		orig.active.add("valC");
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

		var stream = new ByteArrayOutputStream();
		serialization.serialize(orig, null, new OutputStreamWriter(stream, UTF_8));

		Reader reader = new InputStreamReader(new ByteArrayInputStream(stream.toByteArray()), UTF_8);
		ConfigBoof found = serialization.deserialize(reader);

		var defaultVals = new ConfigBoof();

		assertEquals(found.childA.valA, defaultVals.childA.valA);
		assertEquals(found.childA.valB, defaultVals.childA.valB);
		assertEquals(found.childA.valC, defaultVals.childA.valC);
		assertEquals(found.childA.external.stuff, defaultVals.childA.external.stuff);
		assertEquals(found.childB.valA, orig.childB.valA);
		assertEquals(found.childB.valB, orig.childB.valB);
		assertEquals(found.childB.valC, orig.childB.valC);
		assertEquals(found.childB.external.stuff, orig.childB.external.stuff);
		assertEquals(found.valA, defaultVals.valA);
		assertEquals(found.valB, defaultVals.valB);
		assertEquals(found.valC, orig.valC);
	}

	/** Provide a canonical reference */
	@Test void encode_decode_canonical() {
		var orig = new ConfigBoof();
		orig.childA.valA = 88;
		orig.childA.valB = -2;
		orig.childA.external.stuff = "moo";
		orig.childB.valB = -6;
		orig.childB.valC = 101.1;
		orig.childB.external.stuff = "foo";
		orig.valB = EnumPNP.IPPE;

		// Nothing should be serialized if you provide the object you wish to serialize as the reference
		assertTrue(serialization.serialize(orig, orig).isEmpty());

		// Now try serializing using the default values as a reference
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		serialization.serialize(orig, new ConfigBoof(), new OutputStreamWriter(stream, UTF_8));
		Reader reader = new InputStreamReader(new ByteArrayInputStream(stream.toByteArray()), UTF_8);
		ConfigBoof found = serialization.deserialize(reader);

		// See if the serialization/deserialization worked
		checkIdentical(orig, found);
	}

	// Create a chain of configurations, both native BoofCV and External, that will exercise the system

	public static class ConfigExternal {
		public String stuff = "default";

		public ConfigExternal setTo( ConfigExternal src ) {
			this.stuff = src.stuff;
			return this;
		}
	}

	public static class ConfigChild implements Configuration {
		public int valA;
		public byte valB;
		public double valC;

		public final ConfigExternal external = new ConfigExternal();

		public ConfigChild setTo( ConfigChild src ) {
			this.valA = src.valA;
			this.valB = src.valB;
			this.valC = src.valC;
			this.external.setTo(src.external);
			return this;
		}

		@Override public void checkValidity() {}
	}

	public static class ConfigBoof implements Configuration {
		public short valA;
		public EnumPNP valB = EnumPNP.EPNP;
		public String valC = "default";

		public ConfigChild childA = new ConfigChild();
		public ConfigChild childB = new ConfigChild();

		// list of fields that are active
		private List<String> active = new ArrayList<>();

		public ConfigBoof setTo( ConfigBoof src ) {
			this.valA = src.valA;
			this.valB = src.valB;
			this.valC = src.valC;
			childA.setTo(src.childA);
			childB.setTo(src.childB);
			return this;
		}

		@Override public List<String> serializeActiveFields() {return active;}

		@Override public void checkValidity() {}
	}

	public static class ConfigLists implements Configuration {
		public short valA;
		public FastArray<String> strings = new FastArray<>(String.class);
		public FastArray<ConfigExternal> externals = new FastArray<>(ConfigExternal.class);
		public FastArray<EnumPNP> enums = new FastArray<>(EnumPNP.class);

		public void setTo( ConfigLists src ) {
			this.valA = src.valA;
			strings.reset();
			strings.addAll(src.strings);
			externals.clear();
			src.externals.forEach(v -> externals.add(new ConfigExternal().setTo(v)));
			enums.clear();
			enums.addAll(src.enums);
		}

		@Override public void checkValidity() {}
	}
}
