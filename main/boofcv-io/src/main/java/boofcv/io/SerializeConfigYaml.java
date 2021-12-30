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

import boofcv.BoofVersion;
import boofcv.io.serialize.SerializeFieldsYamlBase;
import boofcv.struct.Configuration;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Serializes any BoofCV Config* into a yaml file
 *
 * @author Peter Abeles
 */
public class SerializeConfigYaml extends SerializeFieldsYamlBase {
	public void serialize( Configuration config, @Nullable Object canonical, Writer writer ) {
		Map<String, Object> state = new HashMap<>();

		// Add some version info to make debugging in the future easier
		state.put("BoofCV.Version", BoofVersion.VERSION);
		state.put("BoofCV.GIT_SHA", BoofVersion.GIT_SHA);
		state.put(config.getClass().getName(), serialize(config, canonical));

		// Print what this is at the top of the file
		try {
			writer.write("# Serialized " + config.getClass().getName() + "\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		Yaml yaml = createYmlObject();
		yaml.dump(state, writer);
	}

	public <T extends Configuration> T deserialize( Reader reader ) {
		Yaml yaml = createYmlObject();
		Map<String, Object> state = yaml.load(reader);
		try {
			for (String key : state.keySet()) {
				if (key.startsWith("BoofCV"))
					continue;
				T config = (T)Class.forName(key).getConstructor().newInstance();
				deserialize(config, (Map<String, Object>)Objects.requireNonNull(state.get(key)));
				config.serializeInitialize();
				return config;
			}
		} catch (InstantiationException | IllegalAccessException |
				InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		throw new RuntimeException("Couldn't find object to deserialize");
	}
}
