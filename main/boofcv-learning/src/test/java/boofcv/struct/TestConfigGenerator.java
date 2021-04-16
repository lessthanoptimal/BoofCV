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

package boofcv.struct;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestConfigGenerator extends BoofStandardJUnit {
	@Test void setOfEnums() {
		var alg = new DummyGenerator();

		alg.setOfEnums("valueEnum", EnumType.SECOND, EnumType.THIRD);
		ConfigGenerator.Parameter p = alg.parameters.get(0);
		assertEquals("valueEnum", p.getPath());
		assertEquals(2, p.getStateSize());
		assertEquals(EnumType.SECOND, p.selectValue(0.1));
		assertTrue(((Enum<?>)p.selectValue(rand)).ordinal() > 0);
	}

	@Test void setOfIntegers() {
		var alg = new DummyGenerator();

		alg.setOfIntegers("valueInt", 2, 3, 5, 6);
		ConfigGenerator.Parameter p = alg.parameters.get(0);
		assertEquals("valueInt", p.getPath());
		assertEquals(4, p.getStateSize());
		assertEquals(3, p.selectValue(0.26));
		assertTrue((int)p.selectValue(rand) >= 2);
	}

	@Test void setOfFloats() {
		var alg = new DummyGenerator();

		alg.setOfFloats("valueFloat", 2, 3, 5, 6);
		ConfigGenerator.Parameter p = alg.parameters.get(0);
		assertEquals("valueFloat", p.getPath());
		assertEquals(4, p.getStateSize());
		assertEquals(3.0, p.selectValue(0.26));
		assertTrue((double)p.selectValue(rand) >= 2);
	}

	@Test void rangeOfIntegers() {
		var alg = new DummyGenerator();

		alg.rangeOfIntegers("valueInt", 5, 10);
		ConfigGenerator.Parameter p = alg.parameters.get(0);
		assertEquals("valueInt", p.getPath());
		assertEquals(0, p.getStateSize());

		int[] count = new int[6];
		for (int i = 0; i < 36; i++) {
			int value = (int)p.selectValue(i/35.0);
			assertTrue(value >= 5 && value <= 10);
			count[value - 5]++;

			value = (int)p.selectValue(rand);
			assertTrue(value >= 5 && value <= 10);
		}

		// make sure each one was selected the same number of times
		assertTrue(count[0] > 0);
		for (int i = 1; i < count.length; i++) {
			assertEquals(count[0], count[i]);
		}
	}

	@Test void rangeOfFloats() {
		var alg = new DummyGenerator();

		alg.rangeOfFloats("valueFloat", 5, 10);
		ConfigGenerator.Parameter p = alg.parameters.get(0);
		assertEquals("valueFloat", p.getPath());
		assertEquals(0, p.getStateSize());
		for (int i = 0; i < 20; i++) {
			double value = (double)p.selectValue(i/19.0);
			assertTrue(value >= 5 && value <= 10);

			value = (double)p.selectValue(rand);
			assertTrue(value >= 5 && value <= 10);
		}
	}

	@Test void checkPath() {
		var alg = new DummyGenerator();

		// check positive examples
		alg.checkPath("valueChar", char.class);
		alg.checkPath("valueByte", byte.class);
		alg.checkPath("valueShort", short.class);
		alg.checkPath("valueInt", int.class);
		alg.checkPath("valueFloat", double.class);
		alg.checkPath("valueDouble", double.class);
		alg.checkPath("valueEnum", EnumType.class);
		alg.checkPath("next.valueInt", int.class);
		alg.checkPath("next.valueFloat", double.class);
		alg.checkPath("next.valueDouble", double.class);
		alg.checkPath("next.valueEnum", EnumType.class);

		// check negative examples
		checkPathException(alg, "valueChar", byte.class);
		checkPathException(alg, "valueByte", int.class);
		checkPathException(alg, "valueShort", byte.class);
		checkPathException(alg, "valueInt", float.class);
		checkPathException(alg, "valueFloat", int.class);
		checkPathException(alg, "valueDouble", int.class);
		checkPathException(alg, "valueEnum", int.class);
		checkPathException(alg, "valueA", byte.class);
		checkPathException(alg, "next.valueA", int.class);
		checkPathException(alg, "next.valueFloat", byte.class);
	}

	private void checkPathException( ConfigGenerator<?> alg, String path, Class<?> type ) {
		try {
			alg.checkPath(path, type);
			fail("Should have thrown an exception");
		} catch (RuntimeException ignore) {
		}
	}

	/**
	 * Positive examples of where assignValue should work
	 */
	@Test void assignValue() {
		var config = new ConfigDummyA();

		ConfigGenerator.assignValue(config, "valueChar", 'b');
		assertEquals('b', config.valueChar);
		ConfigGenerator.assignValue(config, "valueByte", (byte)1);
		assertEquals(1, config.valueByte);
		ConfigGenerator.assignValue(config, "valueShort", (short)1);
		assertEquals(1, config.valueShort);
		ConfigGenerator.assignValue(config, "valueInt", 1);
		assertEquals(1, config.valueInt);
		ConfigGenerator.assignValue(config, "valueFloat", 1.0);
		assertEquals(1.0f, config.valueFloat);
		ConfigGenerator.assignValue(config, "valueDouble", 1.0);
		assertEquals(1.0, config.valueDouble);
		ConfigGenerator.assignValue(config, "valueEnum", EnumType.SECOND);
		assertEquals(EnumType.SECOND, config.valueEnum);
		ConfigGenerator.assignValue(config, "next.valueInt", 1);
		assertEquals(1, config.next.valueInt);
	}

	/**
	 * Negative examples of where assignValue should throw an exception
	 */
	@Test void assignValue_negative() {
		var config = new ConfigDummyA();

		assignValueFail(config, "valueChar", 1);
		assignValueFail(config, "valueByte", (short)1);
		assignValueFail(config, "valueShort", 1);
		assignValueFail(config, "valueInt", "asd");
		assignValueFail(config, "valueEnum", 1);
		assignValueFail(config, "next.valueInt", "moo");
	}

	private void assignValueFail( ConfigDummyA config, String path, Object value ) {
		try {
			ConfigGenerator.assignValue(config, path, value);
			fail("Should have thrown an exception");
		} catch (RuntimeException ignore) {
		}
	}

	/**
	 * A config references a object which doesn't implement Configuration but is very similar
	 */
	@Test void assignValue_NotConfig() {
		var config = new ConfigParent();

		ConfigGenerator.assignValue(config, "next.a", 5);
		assertEquals(5, config.next.a);
	}

	/**
	 * A config references a object which doesn't implement Configuration but is very similar
	 */
	@Test void getValue_NotConfig() {
		var config = new ConfigParent();
		config.next.a = 5;
		int found = (Integer)ConfigGenerator.getValue(config, "next.a");
		assertEquals(5, found);
	}

	/**
	 * A config with different internal data types to test
	 */
	public static class ConfigDummyA implements Configuration {
		public char valueChar = 'a';
		public byte valueByte = 0;
		public short valueShort = 0;
		public int valueInt = 0;
		public float valueFloat = 0;
		public double valueDouble = 0;
		public EnumType valueEnum = EnumType.FIRST;

		public ConfigDummyB next = new ConfigDummyB();

		public ConfigDummyA() {}

		@Override public void checkValidity() {}

		public void setTo( ConfigDummyA src ) {
			this.valueChar = src.valueChar;
			this.valueByte = src.valueByte;
			this.valueShort = src.valueShort;
			this.valueInt = src.valueInt;
			this.valueFloat = src.valueFloat;
			this.valueDouble = src.valueDouble;
			this.valueEnum = src.valueEnum;
			this.next.setTo(src.next);
		}
	}

	/**
	 * Another configuration. Used to test chained configs
	 */
	public static class ConfigDummyB implements Configuration {
		public int valueInt = 0;
		public float valueFloat = 0;
		public double valueDouble = 0;
		public EnumType valueEnum = EnumType.FIRST;

		public ConfigDummyB() {}

		@Override public void checkValidity() {}

		public void setTo( ConfigDummyB src ) {
			this.valueInt = src.valueInt;
			this.valueFloat = src.valueFloat;
			this.valueDouble = src.valueDouble;
			this.valueEnum = src.valueEnum;
		}
	}

	public static class ConfigParent implements Configuration {
		public int a = 0;
		public final ConfigLike next = new ConfigLike();

		@Override public void checkValidity() {}
	}

	public static class ConfigLike {
		public int a = 0;
	}

	enum EnumType {
		FIRST,
		SECOND,
		THIRD
	}

	static class DummyGenerator extends ConfigGenerator<ConfigDummyA> {
		public DummyGenerator() {
			super(2342342, ConfigDummyA.class);
		}

		@Override public ConfigDummyA next() {return null;}
	}
}
