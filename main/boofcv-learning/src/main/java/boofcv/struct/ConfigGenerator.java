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

import boofcv.misc.BoofMiscOps;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Generates configuration. Intended for use in parameter tuning when you wish to sample a grid of values or randomly
 * sample possible settings.
 *
 * Usage:
 * <ol>
 *     <li>Specify which parameters are to be sampled</li>
 *     <li>Call {@link #initialize()}</li>
 *     <li>Check {@link #hasNext()} to see if there are more configs to generate</li>
 *     <li>Call {@link #next()} for a new instance of a config to test</li>
 * </ol>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked", "NullAway.Init"})
public abstract class ConfigGenerator<Config extends Configuration> {

	/** Total number of different configurations it will generate */
	@Getter int numTrials;
	/** Initial seed for the random number generator */
	@Getter long seed;
	/** What type of {@link Configuration} it will generate */
	@Getter Class<Config> type;

	/** How many configurations have already been generated. */
	@Getter int trial;

	// random number generator used internally
	protected Random rand;

	// All the parameters that will have their states sampled
	protected List<Parameter> parameters = new ArrayList<>();

	/** Base config that's modified when generating new configures */
	@Getter protected Config configurationBase;
	/** The most recently generated configuration */
	protected Config configCurrent;

	protected ConfigGenerator( long seed, Class<Config> type ) {
		this.seed = seed;
		this.type = type;
		try {
			configurationBase = type.getConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * After all the parameters have been set, call this function to finalize internal variables and begin
	 * the generator. Up until this function is called, any changes in the baseline will be reflected in
	 * later calls
	 */
	public void initialize() {
		trial = 0;
		rand = new Random(seed);
	}

	/**
	 * Returns the number of possible states a parameter has
	 */
	protected int getNumberOfStates( Parameter p ) {
		if (p.getStateSize() == 0)
			throw new RuntimeException("There are an infinite number of states");
		return p.getStateSize();
	}

	/**
	 * Assignment state is the set of enum values passed in
	 */
	public <E extends Enum<E>> void setOfEnums( String parameter, Enum<E>... values ) {
		checkPath(parameter, values[0].getDeclaringClass());
		parameters.add(new SetOfObjects(parameter, BoofMiscOps.asList((Object[])values)));
	}

	/**
	 * Assignment state is the finite set of passed in integers
	 */
	public void setOfIntegers( String parameter, int... values ) {
		checkPath(parameter, int.class);

		List<Object> states = new ArrayList<>();
		for (int i = 0; i < values.length; i++) {
			states.add(values[i]);
		}
		parameters.add(new SetOfObjects(parameter, states));
	}

	/**
	 * Assignment state is the finite set of passed in floating point numbers. This works with 'float' and 'double'
	 */
	public void setOfFloats( String parameter, double... values ) {
		checkPath(parameter, double.class);

		List<Object> states = new ArrayList<>();
		for (int i = 0; i < values.length; i++) {
			states.add(values[i]);
		}
		parameters.add(new SetOfObjects(parameter, states));
	}

	/**
	 * Assignment state is a finite set of integers with in the specified range of values.
	 *
	 * @param parameter String path to the config parameter
	 * @param min Lower extent. Inclusive.
	 * @param max Upper extent. Inclusive.
	 */
	public void rangeOfIntegers( String parameter, int min, int max ) {
		checkPath(parameter, int.class);
		parameters.add(new RangeOfIntegers(parameter, min, max));
	}

	/**
	 * Assignment state is a finite set of floats with in the specified range of values. Works with float and
	 * double valued parameters.
	 *
	 * @param parameter String path to the config parameter
	 * @param min Lower extent. Inclusive.
	 * @param max Upper extent. Inclusive..
	 */
	public void rangeOfFloats( String parameter, double min, double max ) {
		checkPath(parameter, double.class);
		parameters.add(new RangeOfFloats(parameter, min, max));
	}

	/**
	 * True if there are remaining trails
	 */
	public boolean hasNext() {
		return trial < numTrials;
	}

	/**
	 * Generates the next config and returns it. Each call will return a new instance of Config
	 *
	 * @return New instance of the next Config variant.
	 */
	public abstract Config next();

	/**
	 * Creates a string which summarizes what settings are being configured
	 */
	public String toStringSettings() {
		String ret = "type," + type.getCanonicalName() + "\n";
		ret += "random_seed," + seed + "\n";
		ret += "parameters:\n";
		for (int i = 0; i < parameters.size(); i++) {
			Parameter p = parameters.get(i);
			ret += p.getPath() + "," + p.getDescription() + "\n";
		}
		return ret;
	}

	/**
	 * Creates a config which summarizes the current selected state
	 */
	public String toStringState() {
		String ret = "";
		for (int i = 0; i < parameters.size(); i++) {
			Parameter p = parameters.get(i);
			ret += p.getPath() + "," + getValue(configCurrent, p.path) + "\n";
		}
		return ret;
	}

	/**
	 * Returns the most recently generated configuration. This is the same instance as what {@link #next()} returns.
	 */
	public Config getConfiguration() {
		return configCurrent;
	}

	/**
	 * Assigns a field in the config to the specified value.
	 */
	@SuppressWarnings("SelfAssignment")
	public static void assignValue( Object config, String path, Object value ) {
		try {
			String[] names = path.split("\\.");
			// Traverse through the config until it gets to the variable
			Field field = null;
			Class<?> fieldType = null;
			for (int i = 0; i < names.length; i++) {
				String fieldName = names[i];
				field = config.getClass().getField(fieldName);
				fieldType = field.getType();

				if (i + 1 < names.length)
					config = field.get(config);
			}
			Objects.requireNonNull(field);
			Objects.requireNonNull(fieldType);

			// Double are used as a common data type with floats
			if (fieldType == float.class && value.getClass() == Double.class)
				value = (float)((double)value);
			else if (fieldType != value.getClass() && fieldType != value.getClass().getField("TYPE").get(null))
				throw new RuntimeException("Unexpected type. path=" + path + " expected=" +
						value.getClass().getSimpleName() + " found=" + fieldType.getSimpleName());

			// assign the new value
			Objects.requireNonNull(field).set(config, value);
		} catch (Exception e) {
			// Keep the path clean and don't wrap the exception
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;

			throw new RuntimeException(e);
		}
	}

	public static Object getValue( Object config, String path ) {
		try {
			String[] names = path.split("\\.");
			// Traverse through the config until it gets to the variable
			Field field = null;
			for (int i = 0; i < names.length; i++) {
				String fieldName = names[i];
				field = config.getClass().getField(fieldName);

				if (i + 1 < names.length)
					config = field.get(config);
			}

			return Objects.requireNonNull(field).get(config);
		} catch (Exception e) {
			// Keep the path clean and don't wrap the exception
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;

			throw new RuntimeException(e);
		}
	}

	/**
	 * Ensures that the parameter path is valid and assignable to the specified parameter
	 */
	protected void checkPath( String parameter, Class<?> variableType ) {
		String[] names = parameter.split("\\.");
		if (names.length==0)
			throw new IllegalArgumentException("path of length 0. path="+parameter);

		Class<?> pathType = type;
		try {
			Field field;
			for (int i = 0; i < names.length; i++) {
				String fieldName = names[i];
				field = pathType.getField(fieldName);
				pathType = field.getType();
			}
		} catch (Exception e) {
			// Keep the path clean and don't wrap the exception
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;

			throw new RuntimeException(e);
		}

		if (pathType == float.class || pathType == double.class) {
			if (variableType != double.class)
				throw new IllegalArgumentException("Expected floating point but found " + variableType.getSimpleName());
		} else {
			if (variableType != pathType)
				throw new RuntimeException("Unexpected type. path=" + parameter + " expected=" +
						variableType.getSimpleName() + " found=" + pathType.getSimpleName());
		}
	}

	/**
	 * This describes a single parameter which is to be modified and it's allowed states
	 */
	static abstract class Parameter {
		String path;

		protected Parameter( String path ) {
			this.path = path;
		}

		String getPath() {return path;}

		/**
		 * Selects a random value
		 */
		abstract Object selectValue( Random rand );

		/**
		 * Selects a value from its allowed range.
		 *
		 * @param fraction a number from 0 to 1, inclusive
		 */
		abstract Object selectValue( double fraction );

		/**
		 * Describes what this parameter is modifying
		 */
		abstract String getDescription();

		/**
		 * Number of possible allowed states or 0 if its specified over a range of values
		 */
		abstract int getStateSize();
	}

	/**
	 * Tunable parameter for a set of objects
	 */
	static class SetOfObjects extends Parameter {
		List<Object> states;

		public SetOfObjects( String path, List<Object> states ) {
			super(path);
			this.states = states;
		}

		@Override Object selectValue( Random rand ) {
			return states.get(rand.nextInt(states.size()));
		}

		@Override Object selectValue( double fraction ) {
			return states.get(Math.min(states.size() - 1, (int)(states.size()*fraction)));
		}

		@Override String getDescription() {
			String typeName = states.get(0).getClass().getName();

			String ret = typeName + ",";
			for (int i = 0; i < states.size(); i++) {
				ret += states.get(i).toString();
				if (i + 1 != states.size()) {
					ret += ",";
				}
			}

			return ret;
		}

		@Override int getStateSize() {
			return states.size();
		}
	}

	/**
	 * Tunable parameter for a range of integers
	 */
	static class RangeOfIntegers extends Parameter {
		int idx0, idx1;

		public RangeOfIntegers( String path, int idx0, int idx1 ) {
			super(path);
			this.idx0 = idx0;
			this.idx1 = idx1;
		}

		@Override Object selectValue( Random rand ) {
			return idx0 + rand.nextInt(1 + idx1 - idx0);
		}

		@Override Object selectValue( double fraction ) {
			if (fraction >= 1.0)
				return idx1;
			return idx0 + (int)((1 + idx1 - idx0)*fraction);
		}

		@Override String getDescription() {
			return "range-integers," + idx0 + "," + idx1;
		}

		// Even though the number of possible states isn't infinite, 0 indicates that it's a range and should be
		// handled as special since it could be very very large
		@Override int getStateSize() {return 0;}
	}

	/**
	 * Tunable parameter for a range of floats
	 */
	static class RangeOfFloats extends Parameter {
		double idx0, idx1;

		public RangeOfFloats( String path, double idx0, double idx1 ) {
			super(path);
			this.idx0 = idx0;
			this.idx1 = idx1;
		}

		@Override Object selectValue( Random rand ) {
			return idx0 + rand.nextDouble()*(idx1 - idx0);
		}

		@Override Object selectValue( double fraction ) {
			return idx0 + (idx1 - idx0)*fraction;
		}

		@Override String getDescription() {
			return "range-float," + idx0 + "," + idx1;
		}

		@Override int getStateSize() {return 0;}
	}
}
