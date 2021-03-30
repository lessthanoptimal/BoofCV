/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
