/* Retrieved from https://github.com/twall/jna/blob/master/src/com/sun/jna/Native.java
 * June, 2012 by Trent Jarvi.
 */

/* Copyright (c) 2007, 2008, 2009 Timothy Wall, All Rights Reserved
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.  
 */
package gnu.io;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.StringTokenizer;

/** Provides generation of invocation plumbing for a defined native
 * library interface.  Also provides various utilities for native operations.
 * <p>
 * {@link #getTypeMapper} and {@link #getStructureAlignment} are provided
 * to avoid having to explicitly pass these parameters to {@link Structure}s, 
 * which would require every {@link Structure} which requires custom mapping
 * or alignment to define a constructor and pass parameters to the superclass.
 * To avoid lots of boilerplate, the base {@link Structure} constructor
 * figures out these properties based on its enclosing interface.<p>
 * <a name=library_loading></a>
 * <h2>Library Loading</h2>
 * When JNA classes are loaded, the native shared library (jnidispatch) is
 * loaded as well.  An attempt is made to load it from the any paths defined
 * in <code>jna.boot.library.path</code> (if defined), then the system library
 * path using {@link System#loadLibrary}, unless <code>jna.nosys=true</code>.
 * If not found, the appropriate library will be extracted from the class path
 * into a temporary directory and loaded from there.  If your system has
 * additional security constraints regarding execution or load of files
 * (SELinux, for example), you should  probably install the native library in
 * an accessible location and configure  your system accordingly, rather than
 * relying on JNA to extract the library  from its own jar file.<p/>
 * To avoid the automatic unpacking (in situations where you want to force a
 * failure if the JNA native library is not properly installed on the system),
 * set the system property <code>jna.nounpack=true</code>.
 * NOTE: all native functions are provided within this class to ensure that
 * all other JNA-provided classes and objects are GC'd and/or
 * finalized/disposed before this class is disposed and/or removed from
 * memory (most notably Memory and any other class which by default frees its
 * resources in a finalizer).<p/>
 * @see Library
 * @author Todd Fast, todd.fast@sun.com
 * @author twall@users.sf.net
 */
public final class Native {

    private static final String VERSION = "3.4.1";
    private static final String VERSION_NATIVE = "3.4.0";
    private static String nativeLibraryPath = null;

    static {
        loadNativeLibrary();

        String version = RXTXVersion.nativeGetVersion();
        if (!VERSION_NATIVE.equals(version)) {
            String LS = System.getProperty("line.separator");
            throw new Error(LS + LS
                            + "There is an incompatible JNA native library installed on this system." + LS
                            + "To resolve this issue you may do one of the following:" + LS
                            + " - remove or uninstall the offending library" + LS
                            + " - set the system property jna.nosys=true" + LS
                            + " - set jna.boot.library.path to include the path to the version of the " + LS + "   jnidispatch library included with the JNA jar file you are using" + LS);
        }

    }
    
    /** Force a dispose when this class is GC'd. */
    private static final Object finalizer = new Object() {
        protected void finalize() {
            dispose();
        }
    };

    /** Properly dispose of JNA functionality. */
    private static void dispose() {
        nativeLibraryPath = null;
    }

    /** Remove any automatically unpacked native library.

        This will fail on windows, which disallows removal of any file that is
        still in use, so an alternative is required in that case.  Mark
        the file that could not be deleted, and attempt to delete any
        temporaries on next startup.

        Do NOT force the class loader to unload the native library, since
        that introduces issues with cleaning up any extant JNA bits
        (e.g. Memory) which may still need use of the library before shutdown.
     */
    private static boolean deleteNativeLibrary(String path) {
        File flib = new File(path);
        if (flib.delete()) {
            return true;
        }

        // Couldn't delete it, mark for later deletion
        markTemporaryFile(flib);

        return false;
    }

    private Native() { }


    /** Generate a canonical String prefix based on the given OS
        type/arch/name.
    */
    static String getNativeLibraryResourcePath(int osType, String arch, String name) {
        String osPrefix;
        arch = arch.toLowerCase();
        if ("powerpc".equals(arch)) {
            arch = "ppc";
        }
        else if ("powerpc64".equals(arch)) {
            arch = "ppc64";
        }
        switch(osType) {
        case Platform.WINDOWS:
            if ("i386".equals(arch))
                arch = "x86";
            osPrefix = "win32-" + arch;
            break;
        case Platform.WINDOWSCE:
            osPrefix = "w32ce-" + arch;
            break;
        case Platform.MAC:
            osPrefix = "darwin";
            break;
        case Platform.LINUX:
            if ("x86".equals(arch)) {
                arch = "i386";
            }
            else if ("x86_64".equals(arch)) {
                arch = "amd64";
            }
            osPrefix = "linux-" + arch;
            break;
        case Platform.SOLARIS:
            osPrefix = "sunos-" + arch;
            break;
        default:
            osPrefix = name.toLowerCase();
            if ("x86".equals(arch)) {
                arch = "i386";
            }
            if ("x86_64".equals(arch)) {
                arch = "amd64";
            }
            int space = osPrefix.indexOf(" ");
            if (space != -1) {
                osPrefix = osPrefix.substring(0, space);
            }
            osPrefix += "-" + arch;
            break;
        }
        return "/com/sun/jna/" + osPrefix;
    }

    /**
     * Loads the JNA stub library.
     * First tries jna.boot.library.path, then the system path, then from the
     * jar file.
     */
    private static void loadNativeLibrary() {
        removeTemporaryFiles();

        String libName = System.getProperty("jna.boot.library.name", "jnidispatch");
        String bootPath = System.getProperty("jna.boot.library.path");
        if (bootPath != null) {
            // String.split not available in 1.4
            StringTokenizer dirs = new StringTokenizer(bootPath, File.pathSeparator);
            while (dirs.hasMoreTokens()) {
                String dir = dirs.nextToken();
                File file = new File(new File(dir), System.mapLibraryName(libName));
                String path = file.getAbsolutePath();
                if (file.exists()) {
                    try {
                        System.load(path);
                        nativeLibraryPath = path;
                        return;
                    } catch (UnsatisfiedLinkError ex) {
                        // Not a problem if already loaded in another class loader
                        // Unfortunately we can't distinguish the difference...
                        //System.out.println("File found at " + file + " but not loadable: " + ex.getMessage());
                    }
                }
                if (Platform.isMac()) {
                    String orig, ext;
                    if (path.endsWith("dylib")) {
                        orig = "dylib";
                        ext = "jnilib";
                    } else {
                        orig = "jnilib";
                        ext = "dylib";
                    }
                    path = path.substring(0, path.lastIndexOf(orig)) + ext;
                    if (new File(path).exists()) {
                        try {
                            System.load(path);
                            nativeLibraryPath = path;
                            return;
                        } catch (UnsatisfiedLinkError ex) {
                            System.err.println("File found at " + path + " but not loadable: " + ex.getMessage());
                        }
                    }
                }
            }
        }
        try {
            if (!Boolean.getBoolean("jna.nosys")) {
                System.loadLibrary(libName);
                return;
            }
        }
        catch(UnsatisfiedLinkError e) {
            if (Boolean.getBoolean("jna.nounpack")) {
                throw e;
            }
        }
        if (!Boolean.getBoolean("jna.nounpack")) {
            loadNativeLibraryFromJar();
            return;
        }
        throw new UnsatisfiedLinkError("Native jnidispatch library not found");
    }

    /**
     * Attempts to load the native library resource from the filesystem,
     * extracting the JNA stub library from jna.jar if not already available.
     */
    private static void loadNativeLibraryFromJar() {
        String libname = System.mapLibraryName("jnidispatch");
        String arch = System.getProperty("os.arch");
        String name = System.getProperty("os.name");
        String resourceName = getNativeLibraryResourcePath(Platform.getOSType(), arch, name) + "/" + libname;
        URL url = Native.class.getResource(resourceName);
        boolean unpacked = false;
                
        // Add an ugly hack for OpenJDK (soylatte) - JNI libs use the usual
        // .dylib extension 
        if (url == null && Platform.isMac()
            && resourceName.endsWith(".dylib")) {
            resourceName = resourceName.substring(0, resourceName.lastIndexOf(".dylib")) + ".jnilib";
            url = Native.class.getResource(resourceName);
        }
        if (url == null) {
            throw new UnsatisfiedLinkError("jnidispatch (" + resourceName 
                                           + ") not found in resource path");
        }
    
        File lib = null;
        if (url.getProtocol().toLowerCase().equals("file")) {
            try {
                lib = new File(new URI(url.toString()));
            }
            catch(URISyntaxException e) {
                lib = new File(url.getPath());
            }
            if (!lib.exists()) {
                throw new Error("File URL " + url + " could not be properly decoded");
            }
        }
        else {
            InputStream is = Native.class.getResourceAsStream(resourceName);
            if (is == null) {
                throw new Error("Can't obtain jnidispatch InputStream");
            }
            
            FileOutputStream fos = null;
            try {
                // Suffix is required on windows, or library fails to load
                // Let Java pick the suffix, except on windows, to avoid
                // problems with Web Start.
                File dir = getTempDir();
                lib = File.createTempFile("jna", Platform.isWindows()?".dll":null, dir);
                lib.deleteOnExit();
                fos = new FileOutputStream(lib);
                int count;
                byte[] buf = new byte[1024];
                while ((count = is.read(buf, 0, buf.length)) > 0) {
                    fos.write(buf, 0, count);
                }
                unpacked = true;
            }
            catch(IOException e) {
                throw new Error("Failed to create temporary file for jnidispatch library: " + e);
            }
            finally {
                try { is.close(); } catch(IOException e) { }
                if (fos != null) {
                    try { fos.close(); } catch(IOException e) { }
                }
            }
        }
        System.load(lib.getAbsolutePath());
        nativeLibraryPath = lib.getAbsolutePath();
        // Attempt to delete immediately once jnidispatch is successfully
        // loaded.  This avoids the complexity of trying to do so on "exit",
        // which point can vary under different circumstances (native
        // compilation, dynamically loaded modules, normal application, etc).
        if (unpacked) {
            deleteNativeLibrary(lib.getAbsolutePath());
        }
    }

    /** If running web start, determine the location of a given native 
     * library.  This value may be used to properly set 
     * <code>jna.library.path</code> so that JNA can load libraries identified
     * by the &lt;nativelib&gt; tag in the JNLP configuration file.  Returns 
     * <code>null</code> if the Web Start native library cache location can not 
     * be determined.  Note that the path returned may be different for any
     * given library name.
     * <p>
     * Use <code>System.getProperty("javawebstart.version")</code> to detect
     * whether your code is running under Web Start.
     * @throws UnsatisfiedLinkError if the library can't be found by the
     * Web Start class loader, which usually means it wasn't included as 
     * a <code>&lt;nativelib&gt;</code> resource in the JNLP file.
     * @return null if unable to query the web start loader.
     */
    @SuppressWarnings("unchecked")
	public static String getWebStartLibraryPath(final String libName) {
        if (System.getProperty("javawebstart.version") == null)
            return null;
        try {

            final ClassLoader cl = Native.class.getClassLoader();
            @SuppressWarnings("rawtypes")
			Method m = (Method)AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    try {
                        Method m = ClassLoader.class.getDeclaredMethod("findLibrary", new Class[] { String.class });
                        m.setAccessible(true);
                        return m;
                    }
                    catch(Exception e) {
                        return null;
                    }
                }
            });
            String libpath = (String)m.invoke(cl, new Object[] { libName });
            if (libpath != null) {
                return new File(libpath).getParent();
            }
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }

    /** Perform cleanup of automatically unpacked native shared library.
     */
    static void markTemporaryFile(File file) {
        // If we can't force an unload/delete, flag the file for later 
        // deletion
        try {
            File marker = new File(file.getParentFile(), file.getName() + ".x");
            marker.createNewFile();
        }
        catch(IOException e) { e.printStackTrace(); }
    }

    static File getTempDir() {
        File tmp = new File(System.getProperty("java.io.tmpdir"));
        File jnatmp = new File(tmp, "jna-" + System.getProperty("user.name"));
        jnatmp.mkdirs();
        return jnatmp.exists() ? jnatmp : tmp;
    }

    /** Remove all marked temporary files in the given directory. */
    static void removeTemporaryFiles() {
        File dir = getTempDir();
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".x") && name.indexOf("jna") != -1;
            }
        };
        File[] files = dir.listFiles(filter);
        for (int i=0;files != null && i < files.length;i++) {
            File marker = files[i];
            String name = marker.getName();
            name = name.substring(0, name.length()-2);
            File target = new File(marker.getParentFile(), name);
            if (!target.exists() || target.delete()) {
                marker.delete();
            }
        }
    }
    /** Prints JNA library details to the console. */
    public static void main(String[] args) {
        Package pkg = Native.class.getPackage();
        String title = "RXTX";
        title += " API Version " + RXTXVersion.getVersion();
        System.out.println(title);
        System.out.println("Version: " + RXTXVersion.getVersion());
        System.out.println(" Native: " + RXTXVersion.nativeGetVersion());
        System.exit(0);
    }
}