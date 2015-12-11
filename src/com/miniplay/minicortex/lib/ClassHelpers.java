package com.miniplay.minicortex.lib;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by vxc on 10/12/15.
 */
public class ClassHelpers {

    public static ArrayList<String> getClassNamesFromPackage(String packageName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;
        ArrayList<String> names = new ArrayList<String>();

        try {
            packageName = packageName.replace(".", "/");
            packageURL = classLoader.getResource(packageName);

            URI uri = new URI(packageURL.toString());
            File folder = new File(uri.getPath());
            // won't work with path which contains blank (%20)
            // File folder = new File(packageURL.getFile());
            File[] folderFiles = folder.listFiles();
            String entryName;
            for(File actual: folderFiles){
                if (actual.isDirectory()) { continue; } // Exclude directories
                entryName = actual.getName();
                entryName = entryName.substring(0, entryName.lastIndexOf('.'));
                names.add(entryName);
            }
        }catch (Exception e) {}

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
