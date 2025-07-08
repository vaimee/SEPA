/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.extensions.agritwix.criteria.db;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 *
 * @author trivo
 */
public class FileUtils {
    public static void removeDirectory(String path) throws IOException {
        final Path pathToBeDeleted = Paths.get(path);

        if(!Files.exists(pathToBeDeleted)) return;

        Files.walk(pathToBeDeleted)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);

        Files.deleteIfExists(pathToBeDeleted);
    }

    public static void createDirectoyr(String path) throws IOException {
        Files.createDirectory(Path.of(path));
    }
}
