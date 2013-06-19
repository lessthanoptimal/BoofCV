/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package com.thoughtworks.boofcv.core.util;

/**
 * Wrapper around {@link ClassLoader} which allows it to be used as a {@link XStreamClassLoader}.
 *
 * @author Peter Abeles
 */
public class ConvertClassLoader implements XStreamClassLoader {

   ClassLoader loader;

   public ConvertClassLoader(ClassLoader loader) {
      this.loader = loader;
   }

   public Class<?> loadClass(String name) throws ClassNotFoundException {
      return loader.loadClass(name);
   }

   public ClassLoader getClassLoader() {
      return loader;
   }
}
