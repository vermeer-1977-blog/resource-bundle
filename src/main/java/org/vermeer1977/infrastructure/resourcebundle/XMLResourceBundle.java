/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Copyright © 2017 Yamashita,Takahiro
 */
package org.vermeer1977.infrastructure.resourcebundle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * XML形式のResourceBundleクラス.<br>
 *
 * XMLのフォーマット形式（http://java.sun.com/dtd/properties.dtd）
 *
 * <pre>
 * {@code
 * <!--
 * Copyright 2006 Sun Microsystems, Inc.  All rights reserved.
 * -->
 *
 * <!-- DTD for properties -->
 *
 * <!ELEMENT properties ( comment?, entry* ) >
 *
 * <!ATTLIST properties version CDATA #FIXED "1.0">
 *
 * <!ELEMENT comment (#PCDATA) >
 *
 * <!ELEMENT entry (#PCDATA) >
 *
 * <!ATTLIST entry key CDATA #REQUIRED>
 *
 * }
 * </pre>
 *
 * @author Yamashita,Takahiro
 */
public class XMLResourceBundle extends ResourceBundle {

    private final Properties properties;

    /**
     * XML形式のResourceBundleのコンストラクタ<br>
     * Propertiesクラスを使用してXMLファイルを読み込む
     *
     * @param stream プロパティファイルのInputStream
     * @throws IOException InputStreamの入出力時に発生した例外
     */
    public XMLResourceBundle(InputStream stream) throws IOException {
        properties = new Properties();
        properties.loadFromXML(stream);
    }

    @Override
    public Object handleGetObject(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        return properties.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Enumeration<String> getKeys() {
        return (Enumeration<String>) properties.propertyNames();
    }
}
