/*
 * Copyright 2016-2022 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.javaps.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Matthias Mueller, TU Dresden; Bastian Schaeffer, IFGI, Benjamin Pross
 *
 */
public class GenericFileData {

    private static final String ZIP = "zip";

    private static final String COULD_NOT_CREATE_FILE = "Could not create file: ";

    private static final String COULD_NOT_CREATE_DIRECTORY = "Could not create directory: ";

    private static Logger LOGGER = LoggerFactory.getLogger(GenericFileData.class);

    protected final InputStream dataStream;

    protected String fileExtension;

    protected final String mimeType;

    protected File primaryFile;

    public GenericFileData(InputStream stream, String mimeType) {
        this.dataStream = stream;
        this.mimeType = mimeType;
        this.fileExtension = GenericFileDataConstants.mimeTypeFileTypeLUT().get(mimeType);
        if (fileExtension == null) {
            this.fileExtension = "dat";
        }
    }

    public GenericFileData(File primaryTempFile, String mimeType) throws IOException {
        primaryFile = primaryTempFile;
        this.mimeType = mimeType;
        this.fileExtension = GenericFileDataConstants.mimeTypeFileTypeLUT().get(mimeType);

        InputStream is = null;

        if (GenericFileDataConstants.getIncludeFilesByMimeType(mimeType) != null) {

            String baseFile = primaryFile.getName();
            baseFile = baseFile.substring(0, baseFile.lastIndexOf("."));
            File temp = new File(primaryFile.getAbsolutePath());
            File directory = new File(temp.getParent());
            String[] extensions = GenericFileDataConstants.getIncludeFilesByMimeType(mimeType);

            File[] allFiles = new File[extensions.length + 1];

            for (int i = 0; i < extensions.length; i++) {
                allFiles[i] = new File(directory, baseFile + "." + extensions[i]);
            }
            allFiles[extensions.length] = primaryFile;

            // Handling the case if the files don't exist
            // (Can occur if ArcGIS backend has an error and returns no files,
            // but only filenames).
            int numberOfFiles = allFiles.length;
            int numberOfMissing = 0;
            for (int i = 0; i < numberOfFiles; i++) {
                if (!allFiles[i].exists()) {
                    LOGGER.info("File " + (i + 1) + " of " + numberOfFiles + " missing (" + allFiles[i].getName()
                            + ").");
                    numberOfMissing++;
                }
            }
            if ((numberOfFiles - numberOfMissing) == 0) {
                String message = "There is no files to generate data from!";
                LOGGER.error(message);
                throw new FileNotFoundException(message);
            } else if (numberOfMissing > 0) {
                LOGGER.info("Not all files are available, but the available ones are zipped.");
            }

            is = new FileInputStream(org.n52.javaps.utils.IOUtils.zip(allFiles));
        } else {
            is = new FileInputStream(primaryFile);
        }

        this.dataStream = is;

    }

    public String writeData(File workspaceDir) {

        String fileName = null;
        if (GenericFileDataConstants.getIncludeFilesByMimeType(mimeType) != null) {
            try {
                fileName = unzipData(dataStream, fileExtension, workspaceDir);
            } catch (IOException e) {
                LOGGER.error("Could not unzip the archive to " + workspaceDir);
            }
        } else {
            try {
                fileName = justWriteData(dataStream, fileExtension, workspaceDir);
            } catch (IOException e) {
                LOGGER.error("Could not write the input to " + workspaceDir);
            }
        }

        return fileName;
    }

    private String unzipData(InputStream is,
            String extension,
            File writeDirectory) throws IOException {

        String baseFileName = UUID.randomUUID().toString();

        ZipInputStream zipInputStream = new ZipInputStream(is);
        ZipEntry entry;

        String returnFile = null;

        while ((entry = zipInputStream.getNextEntry()) != null) {

            String currentExtension = entry.getName();
            int beginIndex = currentExtension.lastIndexOf(".") + 1;
            currentExtension = currentExtension.substring(beginIndex);

            String fileName = baseFileName + "." + currentExtension;
            File currentFile = new File(writeDirectory, fileName);
            if (!writeDirectory.exists()) {
                if (!writeDirectory.mkdir()) {
                    String message = COULD_NOT_CREATE_DIRECTORY + writeDirectory.getAbsolutePath();
                    LOGGER.error(message);
                    throw new RuntimeException(message);
                }
            }

            if (!currentFile.createNewFile()) {
                String message = COULD_NOT_CREATE_FILE + currentFile.getAbsolutePath();
                LOGGER.error(message);
                throw new RuntimeException(message);
            }
            FileOutputStream fos = new FileOutputStream(currentFile);

            IOUtils.copy(zipInputStream, fos);

            if (currentExtension.equalsIgnoreCase(extension)) {
                returnFile = currentFile.getAbsolutePath();
            }

            fos.close();
        }
        zipInputStream.close();
        return returnFile;
    }

    private String justWriteData(InputStream is,
            String extension,
            File writeDirectory) throws IOException {

        String fileName = null;
        String baseFileName = UUID.randomUUID().toString();

        fileName = baseFileName + "." + extension;
        File currentFile = new File(writeDirectory, fileName);
        if (!writeDirectory.exists()) {
            if (!writeDirectory.mkdir()) {
                String message = COULD_NOT_CREATE_DIRECTORY + writeDirectory.getAbsolutePath();
                LOGGER.error(message);
                throw new RuntimeException(message);
            }
        }

        if (!currentFile.createNewFile()) {
            String message = COULD_NOT_CREATE_FILE + currentFile.getAbsolutePath();
            LOGGER.error(message);
            throw new RuntimeException(message);
        }

        // alter FileName for return
        fileName = currentFile.getAbsolutePath();

        FileOutputStream fos = new FileOutputStream(currentFile);

        IOUtils.copy(is, fos);

        fos.close();
        is.close();

        return fileName;
    }

    public File getBaseFile(boolean unzipIfPossible) {
        String extension = fileExtension;
        if (primaryFile == null && dataStream != null) {
            try {
                if (fileExtension.equals("shp")) {
                    extension = ZIP;
                }
                primaryFile = File.createTempFile(UUID.randomUUID().toString(), "." + extension);
                OutputStream out = new FileOutputStream(primaryFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = dataStream.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                throw new RuntimeException("Something went wrong while writing the input stream to the file system", e);
            }

        }
        if (unzipIfPossible && extension.contains(ZIP)) {
            try {
                File tempFile1 = File.createTempFile(UUID.randomUUID().toString(), "");
                File dir = new File(tempFile1.getParentFile() + "/" + UUID.randomUUID().toString());
                if (!dir.mkdir()) {
                    String message = COULD_NOT_CREATE_DIRECTORY + dir.getAbsolutePath();
                    LOGGER.error(message);
                    throw new RuntimeException(message);
                }
                FileInputStream fis = new FileInputStream(primaryFile);
                ZipInputStream zis = new ZipInputStream(fis);
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    LOGGER.debug("Extracting: " + entry);
                    // write the files to the disk
                    FileOutputStream fos = new FileOutputStream(dir.getAbsoluteFile() + "/" + entry.getName());

                    IOUtils.copy(zis, fos);

                }
                zis.close();

                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().contains(".shp") || file.getName().contains(".SHP")) {
                            primaryFile = file;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                throw new RuntimeException("Error while unzipping input data", e);
            }
        }
        return primaryFile;
    }

    @Override
    protected void finalize() {
        try {
            super.finalize();
            if (primaryFile != null) {
                if (!primaryFile.delete()) {
                    LOGGER.info("Could not delete file: " + primaryFile.getAbsolutePath());
                }
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public InputStream getDataStream() {
        return dataStream;
    }
}
