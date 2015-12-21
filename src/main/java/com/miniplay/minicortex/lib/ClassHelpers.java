package com.miniplay.minicortex.lib;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by vxc on 10/12/15.
 */
public class ClassHelpers {

    public static ArrayList<String> getClassNamesFromPackage(String packageName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;
        ArrayList<String> names = new ArrayList<String>();
        packageName = packageName.replace(".", "/");
        packageURL = classLoader.getResource(packageName);

        try {

            URI uri = new URI(packageURL.toString());

            if ( uri.getPath() == null) { // NOT REGULAR FILE CASE, JAR?
                // JAR CASE?
                if (packageURL.getProtocol().equals("jar")) {
                    /* A JAR path */
                    String jarPath = packageURL.getPath().substring(5, packageURL.getPath().indexOf("!")); //strip out only the JAR file
                    JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
                    Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
                    Set<String> result = new HashSet<String>(); //avoid duplicates in case it is a subdirectory
                    while(entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (name.startsWith(packageName)) { //filter according to the path
                            int indexOfDot = name.lastIndexOf('.');
                            Boolean isDir = (indexOfDot == -1);
                            // Ignore directories, just get files with "." in its names
                            if (!isDir){
                                String entry = name.substring(packageName.length(), name.lastIndexOf('.'));
                                entry = entry.replace("/", ""); // Remove possible slashes
                                names.add(entry);
                            }

                        }
                    }
                }

            } else { // REGULAR FILE CASE, EASY

                File folder = new File(uri.getPath());
                // won't work with path which contains blank (%20)
                File[] folderFiles = folder.listFiles();
                String entryName;
                for(File actual: folderFiles){
                    if (actual.isDirectory()) { continue; } // Exclude directories
                    entryName = actual.getName();
                    entryName = entryName.substring(0, entryName.lastIndexOf('.'));
                    names.add(entryName);
                }
            }

        }catch (Exception e) {
            e.printStackTrace();
        }

        return names;
    }

    public static <T> T instantiateClass(final String className, final Class<T> type){
        try{
            return type.cast(Class.forName(className).newInstance());
        } catch(final InstantiationException e){
            throw new IllegalStateException(e);
        } catch(final IllegalAccessException e){
            throw new IllegalStateException(e);
        } catch(final ClassNotFoundException e){
            throw new IllegalStateException(e);
        }
    }

}
