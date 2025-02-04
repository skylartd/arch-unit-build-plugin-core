package com.societegenerale.commons.plugin.utils;

import static java.util.Collections.emptyList;

import com.societegenerale.commons.plugin.Log;
import com.societegenerale.commons.plugin.model.RootClassFolder;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by agarg020917 on 11/17/2017.
 */
public class ArchUtils {

    private static Log log;

    public ArchUtils(Log log) {
        this.log = log;
    }

    public static JavaClasses importAllClassesInPackage(RootClassFolder rootClassFolder, String packagePath) {
        return importAllClassesInPackages(rootClassFolder, List.of(packagePath), emptyList());
    }

    public static JavaClasses importAllClassesInPackage(RootClassFolder rootClassFolder, String packagePaths, Collection<String> excludedPaths) {
        return importAllClassesInPackages(rootClassFolder, List.of(packagePaths), excludedPaths);
    }

    public static JavaClasses importAllClassesInPackages(RootClassFolder rootClassFolder, List<String> packagePaths, Collection<String> excludedPaths) {

        if (log == null) {
            throw new IllegalStateException("please make sure you instantiate " + ArchUtils.class + " with a proper " + Log.class + " before calling this static method");
        }

        List<Path> classesPaths = new ArrayList<>();

        for (String packagePath : packagePaths) {
            Path classesPath = Paths.get(rootClassFolder.getValue() + packagePath);

            if (!classesPath.toFile().exists()) {
                StringBuilder warnMessage = new StringBuilder("Classpath ").append(classesPath.toFile())
                        .append(" doesn't exist : loading all classes from root, ie ")
                        .append(rootClassFolder.getValue())
                        .append(" even though it's probably not what you want to achieve.")
                        .append(" Enable debug logs in your build to see the list of actual resources being loaded and analyzed by the plugin.");
                log.warn(warnMessage.toString());

                //logging content of directory, to help with debugging..
                log.debug("existing folders and files under root project : ");
                try {
                    Files.walk(Paths.get(rootClassFolder.getValue()))
                            .forEach(f -> log.debug(f.toFile().getName()));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                classesPath = Paths.get(rootClassFolder.getValue());
            } else {
                if (log.isDebugEnabled()) {
                    try {
                        log.debug("loading classes from a location that exists : " + classesPath.toFile().getCanonicalPath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            classesPaths.add(classesPath);
        }


        if (log.isDebugEnabled()) {
            ClassFileImporter classFileImporterForDebug = new ClassFileImporter();
            JavaClasses classesImportedBeforeExclusion = classFileImporterForDebug.importPaths(classesPaths);

            log.debug("nb classes imported before exclusion : " + classesImportedBeforeExclusion.size());
        }

        ClassFileImporter classFileImporter = new ClassFileImporter();

        for (String excludedPath : excludedPaths) {
            ExclusionImportOption exclusionImportOption = new ExclusionImportOption(log, excludedPath);
            classFileImporter = classFileImporter.withImportOption(exclusionImportOption);
        }

        JavaClasses classesImportedAfterExclusionProcessing = classFileImporter.importPaths(classesPaths);

        if (log.isDebugEnabled()) {
            log.debug("nb classes imported after exclusion : " + classesImportedAfterExclusionProcessing.size());
        }

        return classesImportedAfterExclusionProcessing;
    }

}
