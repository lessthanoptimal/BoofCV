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

package boofcv.gui.settings;

import com.github.weisj.darklaf.DarkLaf;
import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import com.github.weisj.darklaf.theme.IntelliJTheme;
import com.github.weisj.darklaf.theme.SolarizedLightTheme;
import lombok.Data;
import mdlaf.MaterialLookAndFeel;
import mdlaf.themes.JMarsDarkTheme;
import mdlaf.themes.MaterialLiteTheme;
import mdlaf.themes.MaterialOceanicTheme;

import javax.swing.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Used to configure Swing UI settings across all apps
 *
 * @author Peter Abeles
 */
@Data
public class GlobalDemoSettings implements Cloneable {

	public static GlobalDemoSettings SETTINGS = new GlobalDemoSettings();

	private static String KEY_THEME = "theme";
	private static String KEY_CONTROLS3D = "controls3D";
	private static String KEY_VERBOSE_RUNTIME = "verboseRuntime";
	private static String KEY_VERBOSE_TRACKING = "verboseTracking";
	private static String KEY_VERBOSE_RECURSIVE = "verboseRecursive";

	public ThemesUI theme = ThemesUI.DEFAULT;
	public Controls3D controls3D = Controls3D.WASD;
	public boolean verboseRuntime = false;
	public boolean verboseTracking = false;
	public boolean verboseRecursive = false;

	static {
		SETTINGS.load();
	}

	public void load() {
		Preferences prefs = Preferences.userRoot().node(GlobalDemoSettings.class.getCanonicalName());

		try {
			theme = ThemesUI.valueOf(prefs.get(KEY_THEME, theme.name()));
			controls3D = Controls3D.valueOf(prefs.get(KEY_CONTROLS3D, controls3D.name()));
			verboseRuntime = prefs.getBoolean(KEY_VERBOSE_RUNTIME, verboseRuntime);
			verboseTracking = prefs.getBoolean(KEY_VERBOSE_TRACKING, verboseTracking);
			verboseRecursive = prefs.getBoolean(KEY_VERBOSE_RECURSIVE, verboseRecursive);
		} catch (RuntimeException e) {
			// save the current state to fix whatever went wrong
			save();
			e.printStackTrace();
		}
	}

	public void save() {
		Preferences prefs = Preferences.userRoot().node(GlobalDemoSettings.class.getCanonicalName());

		prefs.put(KEY_THEME, theme.name());
		prefs.put(KEY_CONTROLS3D, controls3D.name());
		prefs.putBoolean(KEY_VERBOSE_RUNTIME, verboseRuntime);
		prefs.putBoolean(KEY_VERBOSE_TRACKING, verboseTracking);
		prefs.putBoolean(KEY_VERBOSE_RECURSIVE, verboseRecursive);

		try {
			prefs.sync();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	public void changeTheme() {
		LookAndFeel selectedInstance = null;
		String selectedName = null;

		LafManager.enableLogging(false);

		switch (theme) {
			case DEFAULT -> {
				if (System.getProperty("os.name").contains("Mac OS X"))
					selectedName = UIManager.getSystemLookAndFeelClassName();
				else
					selectedName = UIManager.getCrossPlatformLookAndFeelClassName();
			}
			case DARKULA -> {
				LafManager.setTheme(new DarculaTheme());
				selectedName = DarkLaf.class.getCanonicalName();
			}
			case INTELLIJ -> {
				LafManager.setTheme(new IntelliJTheme());
				selectedName = DarkLaf.class.getCanonicalName();
			}
			case SOLARIZED -> {
				LafManager.setTheme(new SolarizedLightTheme());
				selectedName = DarkLaf.class.getCanonicalName();
			}
			case MARS_DARK -> selectedInstance = new MaterialLookAndFeel(new JMarsDarkTheme());
			case MATERIAL_LITE -> selectedInstance = new MaterialLookAndFeel(new MaterialLiteTheme());
			case MATERIAL_OCEANIC -> selectedInstance = new MaterialLookAndFeel(new MaterialOceanicTheme());
			default -> {
				System.err.println("BUG! Unknown Look and Feel " + theme);
				return;
			}
		}

		try {
			if (selectedInstance == null) {
				UIManager.setLookAndFeel(selectedName);
			} else {
				UIManager.setLookAndFeel(selectedInstance);
			}
		} catch (UnsupportedLookAndFeelException | IllegalAccessException
				| InstantiationException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public GlobalDemoSettings copy() {
		GlobalDemoSettings ret = new GlobalDemoSettings();
		ret.theme = this.theme;
		ret.verboseRuntime = this.verboseRuntime;
		ret.verboseTracking = this.verboseTracking;
		return ret;
	}

	public enum ThemesUI {
		DEFAULT("Default"),
		DARKULA("Darkula"),
		INTELLIJ("IntelliJ"),
		SOLARIZED("Solarized"),
		MARS_DARK("Mars Dark"),
		MATERIAL_LITE("Material Lite"),
		MATERIAL_OCEANIC("Material Oceanic");

		ThemesUI( String name ) {
			this.name = name;
		}

		final String name;

		@Override
		public String toString() {
			return name;
		}
	}

	public enum Controls3D {
		/** Use WASD keys to translate through the 3D environment */
		WASD,
		/** Use only controls */
		MOUSE
	}
}
