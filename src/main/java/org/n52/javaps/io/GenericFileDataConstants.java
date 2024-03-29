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

import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import org.n52.javaps.annotation.ConfigurableClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Properties(propertyFileName = "genericfiledataconstants.properties",
//defaultPropertyFileName = "genericfiledataconstants.default.properties")
public final class GenericFileDataConstants implements ConfigurableClass {

    public static final String MIME_TYPE_ZIPPED_SHP = "application/x-zipped-shp";

    public static final String MIME_TYPE_SHP = "application/shp";

    public static final String MIME_TYPE_HDF = "application/img";

    public static final String MIME_TYPE_GEOTIFF = "application/geotiff";

    public static final String MIME_TYPE_TIFF = "image/tiff";

    public static final String MIME_TYPE_DBASE = "application/dbase";

    public static final String MIME_TYPE_REMAPFILE = "application/remap";

    public static final String MIME_TYPE_PLAIN_TEXT = "text/plain";

    public static final String MIME_TYPE_TEXT_XML = "text/xml";

    public static final String MIME_TYPE_IMAGE_GEOTIFF = "image/geotiff";

    public static final String MIME_TYPE_X_GEOTIFF = "application/x-geotiff";

    public static final String MIME_TYPE_IMAGE_PNG = "image/png";

    public static final String MIME_TYPE_IMAGE_GIF = "image/gif";

    public static final String MIME_TYPE_IMAGE_JPEG = "image/jpeg";

    public static final String MIME_TYPE_X_ERDAS_HFA = "application/x-erdas-hfa";

    public static final String MIME_TYPE_NETCDF = "application/netcdf";

    public static final String MIME_TYPE_X_NETCDF = "application/x-netcdf";

    public static final String MIME_TYPE_DGN = "application/dgn";

    public static final String MIME_TYPE_KML = "application/vnd.google-earth.kml+xml";

    public static final String MIME_TYPE_HDF4EOS = "application/hdf4-eos";

    public static final String MIME_TYPE_GML200 = "text/xml; subtype=gml/2.0.0";

    public static final String MIME_TYPE_GML211 = "text/xml; subtype=gml/2.1.1";

    public static final String MIME_TYPE_GML212 = "text/xml; subtype=gml/2.1.2";

    public static final String MIME_TYPE_GML2121 = "text/xml; subtype=gml/2.1.2.1";

    public static final String MIME_TYPE_GML300 = "text/xml; subtype=gml/3.0.0";

    public static final String MIME_TYPE_GML301 = "text/xml; subtype=gml/3.0.1";

    public static final String MIME_TYPE_GML310 = "text/xml; subtype=gml/3.1.0";

    public static final String MIME_TYPE_GML311 = "text/xml; subtype=gml/3.1.1";

    public static final String MIME_TYPE_GML321 = "text/xml; subtype=gml/3.2.1";

    private static final String[] ADDITIONAL_SHP_FILE_ITEMS = { "shx", "dbf", "prj", "sbn", "sbx", "shp.xml" };

    private static final String[] ADDITIONAL_DBF_FILE_ITEMS = { "dbf.xml" };

    private static Logger LOGGER = LoggerFactory.getLogger(GenericFileDataConstants.class);

    private static HashMap<String, String> lut;

    public static HashMap<String, String> mimeTypeFileTypeLUT() {

        synchronized (GenericFileDataConstants.class) {
            if (lut == null) {

                lut = new HashMap<String, String>();

                Properties ioProperties = new Properties();

                try (InputStream in = GenericFileDataConstants.class.getClassLoader().getResourceAsStream(
                        "io.properties")) {

                    ioProperties.load(in);

                    Enumeration<Object> en = ioProperties.keys();

                    while (en.hasMoreElements()) {
                        String type = (String) en.nextElement();
                        lut.put(type, ioProperties.getProperty(type));
                    }

                } catch (Exception e) {
                    LOGGER.error("Exception while setting up Look up table.", e);
                }
            }
        }

        return lut;
    }

    public static String[] getMimeTypes() {
        return mimeTypeFileTypeLUT().keySet().toArray(new String[0]);
    }

    public static String[] getIncludeFilesByMimeType(String mimeType) {

        String[] returnValue = null;

        if (mimeType != null && mimeType.equalsIgnoreCase(MIME_TYPE_ZIPPED_SHP)) {
            returnValue = ADDITIONAL_SHP_FILE_ITEMS;
        }
        if (mimeType != null && mimeType.equalsIgnoreCase(MIME_TYPE_DBASE)) {
            returnValue = ADDITIONAL_DBF_FILE_ITEMS;
        }

        return returnValue;

    }

}
