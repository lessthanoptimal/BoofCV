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

package boofcv.io.gstwebcamcapture;

/**
 * Provides information on current OS
 * @author Devin Willis
 */
public class OSHelper {

    private static String OS = System.getProperty("os.name").toLowerCase();

    public static void main(String[] args) {

        System.out.println(OS);

        if (isWindows()) {
            System.out.println("This is Windows");
        } else if (isMac()) {
            System.out.println("This is Mac");
        } else if (isUnix()) {
            System.out.println("This is Unix or Linux");
        } else if (isSolaris()) {
            System.out.println("This is Solaris");
        } else {
            System.out.println("Your OS is not support!!");
        }
    }

    /**
     * Returns True if Windows
     * @return 
     */
    public static boolean isWindows() {
        return OS.contains("win");
    }

    /**
     * Returns True if Max
     * @return 
     */
    public static boolean isMac() {
        return OS.contains("mac");
    }

    /**
     * Returns True if Unix
     * @return 
     */
    public static boolean isUnix() {
        return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));
    }
    /**
     * Returns True if Solaris
     * @return 
     */
    public static boolean isSolaris() {
        return OS.contains("sunos");
    }
    
    /**
     * Returns OS as a string
     * @return 
     */
    public static String getOS(){
        if (isWindows()) {
            return "win";
        } else if (isMac()) {
            return "osx";
        } else if (isUnix()) {
            return "uni";
        } else if (isSolaris()) {
            return "sol";
        } else {
            return "err";
        }
    }

}
