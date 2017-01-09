package org.vermeer1977.infrastructure.resourcebundle;

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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import lombok.Builder;
import lombok.Singular;

/**
 * 通常のCotrolに加えて、文字コードを指定してResourseを取得できるように拡張をしたResourceBundle.Controlクラス.<br>
 * 冗長的ではあるが拡張メソッドを把握するため、およびその用途メモを記すために、拡張メソッドを全てOverrideする.<br>
 * <br>
 * 実装例
 * <ul>
 *
 * <li>
 * デフォルト指定.<br>
 * 文字コード ASCII、取得対象検索順：class・properties・xml、Localeはデフォルトロケール（リソースではない）<br>
 * デフォルトロケールが存在しない場合は、デフォルトリソース（ロケール記載の無いリソースファイル）を検索する.<br>
 * <pre>
 * {@code
 * CustomControl control = CustomControl.builder().build();
 * }
 * </pre>
 * </li>
 *
 * <li>
 * 文字コードを指定する<br>
 * <pre>
 * {@code
 * CustomControl control = CustomControl.builder().charCode(StandardCharsets.UTF_8.toString()).build();
 * CustomControl control = CustomControl.builder().charCode("UTF-8").build();
 * CustomControl control = CustomControl.builder().charCode("SJIS").build();
 * }
 * </pre>
 * </li>
 *
 * <li>
 * 参照するリソースの順番を変える<br>
 * デフォルトはclass・properties・xmlからproperties・xml・classに変更する.<br>
 *
 * <pre>
 * {@code
 * CustomControl control = CustomControl.builder()
 *  .formats(CustomControl.FORMAT_DEFAULT)
 *  .formats(CustomControl.FORMAT_XML)
 *  .build();
 * }
 * </pre>
 * </li>
 *
 * <li>
 * 参照するリソースを固定する<br>
 *
 * XMLのみの場合
 * <pre>
 * {@code
 * CustomControl control = CustomControl.builder()
 *  .formats(CustomControl.FORMAT_XML)
 *  .build();
 * }
 * </pre>
 * </li>
 *
 * <li>
 * 検索ロケール順を指定する<br>
 * 必ず、デフォルト{@code Locale.ROOT}を入れるようにしておくと、リソース検索エラーになりにくいと思われます.<br>
 *
 * 事例は検索を適用するロケールが日本語（JAPANESE）で、検索順をUS・JAPAN・デフォルトリソースの順とした記述（普通はこういうことはしたいと思われるが実装例として記載）<br>
 * <pre>
 * {@code
 * CustomControl control = CustomControl.builder()
 * .targetCandidateLocalePair(
 *  TargetCandidateLocalePair.builder()
 *      .targetLocale(Locale.JAPANESE)
 *      .candidateLocale(Locale.US)
 *      .candidateLocale(Locale.JAPAN)
 *      .candidateLocale(Locale.ROOT)
 *      .build()
 *  .build();
 * }
 * </pre>
 * <br>
 * 検索を適用するロケールを複数指定した場合.<br>
 * <pre>
 * {@code
 * CustomControl control = CustomControl.builder()
 * .charCode("UTF-8")
 * .targetCandidateLocalePair(
 *  TargetCandidateLocalePair.builder()
 *      .targetLocale(Locale.US)
 *      .candidateLocale(Locale.Locale.ENGLISH)
 *      .candidateLocale(Locale.ROOT)
 *      .build()
 * .targetCandidateLocalePair(
 *  TargetCandidateLocalePair.builder()
 *      .targetLocale(Locale.JAPANESE)
 *      .candidateLocale(Locale.JAPAN)
 *      .candidateLocale(Locale.ROOT)
 *      .build()
 *  .build();
 * }
 * </pre>
 *
 * nullの場合は、ResourceBundleで指定したロケールと同じロケールに置き換える.<br>
 * <pre>
 * {@code
 * CustomControl control = CustomControl.builder()
 * .targetCandidateLocalePair(
 *  TargetCandidateLocalePair.builder()
 *      .targetLocale(Locale.JAPANESE)
 *      .candidateLocale(Locale.US)
 *      .candidateLocale(null)
 *      .candidateLocale(Locale.ROOT)
 *      .build()
 *  .build();
 * }
 * </pre>
 * <br>
 *
 * </li>
 *
 * <li>
 * データをキャッシュの指定<br>
 *
 * キャッシュする：CustomControl.TTL_NO_EXPIRATION_CONTROL（デフォルト）<br>
 * <pre>
 * {@code
 * CustomControl control = CustomControl.builder().timeToLive(CustomControl.TTL_NO_EXPIRATION_CONTROL).build();
 * }
 * </pre>
 *
 * キャッシュしない・都度リソースを読み込む：CustomControl.TTL_DONT_CACHE.<br>
 * <pre>
 * {@code
 * CustomControl control = CustomControl.builder().timeToLive(CustomControl.TTL_DONT_CACHE).build();
 * }
 * </pre>
 *
 * ResourceBundleの有効期限(0またはキャッシュ格納時刻からの正のミリ秒オフセット)を指定する.<br>
 * <pre>
 * {@code
 * CustomControl control = CustomControl.builder().timeToLive(1000L).build();
 * }
 * </pre>
 *
 * </li>
 *
 * </ul>
 *
 * 全ての設定は組み合わせて使用することが出来る。
 *
 *
 * @author Yamashita,Takahiro
 */
@Builder
public class CustomControl extends Control {

    /**
     * formatに使用するXML用の定数
     */
    public static final List<String> FORMAT_XML = Collections.unmodifiableList(Arrays.asList("xml"));

    /**
     * formatに指定可能な全定数
     */
    public static final List<String> FORMAT_ALL = Collections.unmodifiableList(Arrays.asList("java.class", "java.properties", "xml"));

    private final String charCode;

    private final Long timeToLive;

    @Singular
    private final List<String> formats;

    @Singular
    private final List<TargetCandidateLocalePair> targetCandidateLocalePairs;

    /* fallbackが再帰的に呼び込まれている状態 = 無限ループ */
    private boolean isFallBackInfiniteLoop = false;

    /**
     * 新しいResourceBundleを生成する.<br>
     * propertiesは、charCodeで指定した文字コードでエンコードしながら読み込む.<br>
     *
     * @see java.util.ResourceBundle.Control#newBundle(java.lang.String, java.util.Locale,
     * java.lang.String,java.lang.ClassLoader, boolean)
     *
     * @return 生成したResourceBundle フォーマットが
     * @throws java.lang.IllegalAccessException 配列以外のインスタンス作成、フィールドの設定または取得、メソッドの呼び出しを試みた場合の例外
     * @throws java.lang.InstantiationException 指定されたクラスオブジェクトのインスタンスを生成できない場合の例外
     * @throws java.io.IOException resourceファイルの取得時に発生時の例外
     */
    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
            throws IllegalAccessException, InstantiationException, IOException {

        this.isFallBackInfiniteLoop = false;
        if (CustomControl.FORMAT_CLASS.contains(format)) {
            return super.newBundle(baseName, locale, format, loader, reload);
        }

        if (CustomControl.FORMAT_PROPERTIES.contains(format)) {
            return this.newBundleProperties(baseName, locale, loader, reload);
        }

        if (CustomControl.FORMAT_XML.contains(format)) {
            return this.newBundleXML(baseName, locale, format, loader, reload);
        }

        throw new IllegalArgumentException("unknown format: " + format);
    }

    /**
     * propertiesファイルの読み込みResourceBundleを生成する.
     *
     * @see java.util.ResourceBundle.Control#newBundle(java.lang.String, java.util.Locale,
     * java.lang.String,java.lang.ClassLoader, boolean)
     *
     * @param baseName
     * @param locale
     * @param format
     * @param loader
     * @param reload
     * @return 生成したResourceBundle
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IOException
     */
    private ResourceBundle newBundleProperties(String baseName, Locale locale, ClassLoader loader, boolean reload)
            throws IllegalAccessException, InstantiationException, IOException {

        String bundleName = toBundleName(baseName, locale);
        ResourceBundle bundle = null;
        final String resourceName = (bundleName.contains("://"))
                ? null
                : toResourceName(bundleName, "properties");
        if (resourceName == null) {
            return bundle;
        }
        final ClassLoader classLoader = loader;
        final boolean reloadFlag = reload;
        InputStream stream = null;
        try {
            stream = AccessController.doPrivileged((PrivilegedExceptionAction<InputStream>) () -> {
                InputStream is = null;
                if (reloadFlag) {
                    URL url = classLoader.getResource(resourceName);
                    if (url != null) {
                        URLConnection connection = url.openConnection();
                        if (connection != null) {
                            // Disable caches to get fresh data for reloading.
                            connection.setUseCaches(false);
                            is = connection.getInputStream();
                        }
                    }
                } else {
                    is = classLoader.getResourceAsStream(resourceName);
                }
                return is;
            });
        }
        catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
        if (stream != null) {
            try {
                if (this.charCode != null) {
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, this.charCode));
                } else {
                    bundle = new PropertyResourceBundle(stream);
                }
            }
            finally {
                stream.close();
            }
        }
        return bundle;
    }

    /**
     * XMLファイルの読み込みResourceBundleを生成する.
     *
     * @see java.util.ResourceBundle.Control#newBundle(java.lang.String, java.util.Locale,
     * java.lang.String,java.lang.ClassLoader, boolean)
     *
     * @param baseName
     * @param locale
     * @param format
     * @param loader
     * @param reload
     * @return 生成したResourceBundle
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IOException
     */
    private ResourceBundle newBundleXML(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
            throws IllegalAccessException, InstantiationException, IOException {

        ResourceBundle bundle = null;

        String bundleName = toBundleName(baseName, locale);
        final String resourceName = (bundleName.contains("://"))
                ? null
                : toResourceName(bundleName, "xml");

        InputStream stream = null;
        if (reload) {
            URL url = loader.getResource(resourceName);
            if (url != null) {
                URLConnection connection = url.openConnection();
                if (connection != null) {
                    // Disable caches to get fresh data for reloading.
                    connection.setUseCaches(false);
                    stream = connection.getInputStream();
                }
            }
        } else {
            stream = loader.getResourceAsStream(resourceName);
        }
        if (stream != null) {
            try (BufferedInputStream bis = new BufferedInputStream(stream)) {
                bundle = new XMLResourceBundle(bis);
            }
        }
        return bundle;
    }

    /**
     * リソースを取得する優先度を指定する.<br>
     *
     * @see java.util.ResourceBundle.Control#getCandidateLocales(java.lang.String, java.util.Locale)
     * @return 優先度順のロケールリスト
     */
    @Override
    public List<Locale> getCandidateLocales(String baseName, Locale locale) {

        Optional<List<Locale>> candidateLocales = targetCandidateLocalePairs.stream()
                .filter(pair -> pair.getTargetLocale().equals(locale))
                .map(TargetCandidateLocalePair::getCandidateLocales)
                .findAny();

        if (candidateLocales.isPresent() == false) {
            return super.getCandidateLocales(baseName, locale);
        }

        List<Locale> localeSetedCandidateLocales = new ArrayList<>();
        candidateLocales.get().stream()
                .forEachOrdered(candidateLocale -> {
                    localeSetedCandidateLocales.add(candidateLocale == null ? locale : candidateLocale);
                });
        return localeSetedCandidateLocales;
    }

    /**
     * デフォルトリソースを取得する.<br>
     * リソースバンドルの検索時、指定したロケールに対応したリソースバンドルが存在しない場合、デフォルトリソースではなく、デフォルトロケールに対応したリソースバンドルを検索してしまう.<br>
     * 本対応をしないと意図したリソースではないデフォルトロケールを取得してしまい国際化対応が正しく行われない.<br>
     *
     * @see java.util.ResourceBundle.Control#getFallbackLocale(java.lang.String, java.util.Locale)
     * @return デフォルトリソースのロケール
     */
    @Override
    public Locale getFallbackLocale(String baseName, Locale locale) {
        if (this.isFallBackInfiniteLoop) {
            throw new MissingResourceException("you set baseName is  [" + baseName + "]. fallback locale, but does not exist baseName resource file. check ResourceBundle.getBundle param 'baseName' and resource file name.", baseName, "");
        }
        this.isFallBackInfiniteLoop = true;
        return Locale.ROOT;
    }

    /**
     * ResourceBundleとして読み込み対象とする分類を返却する.<br>
     * デフォルトの優先度は高い順に、{@literal class > properties > xml}.<br>
     * 特定のフォーマットを指定した場合は、そちらを採用する.<br>
     * baseNameが同じで拡張子が異なるファイルが存在する場合、フォーマットを指定することで正しく処理できるようになる.<br>
     *
     * @see java.util.ResourceBundle.Control#getFormats(java.lang.String)
     * @return ResourceBundleとして読み込み対象とする分類リスト
     */
    @Override
    public List<String> getFormats(String baseName) {
        if (this.formats.isEmpty()) {
            return Collections.unmodifiableList(FORMAT_ALL);
        }

        List<String> setFormats = (new ArrayList<>(this.formats));
        setFormats.retainAll(FORMAT_ALL);
        if (setFormats.isEmpty()) {
            throw new IllegalArgumentException("unknown format: " + this.formats.toString());
        }

        return Collections.unmodifiableList(this.formats);
    }

    /**
     * キャッシュ内のロード済みバンドルの有効期限を取得する.<br>
     * キャッシュ内のロード済みバンドルに有効期限を設ける場合はその時間(0またはキャッシュ格納時刻からの正のミリ秒オフセット)、有効期限制御を無効にする場合はTTL_NO_EXPIRATION_CONTROL、キャッシュを無効にする場合はTTL_DONT_CACHE。
     * <br>
     *
     * @see java.util.ResourceBundle.Control#getTimeToLive(java.lang.String, java.util.Locale)
     * @return バンドルの有効期限
     */
    @Override
    public long getTimeToLive(String baseName, Locale locale) {
        return this.timeToLive == null
                ? super.getTimeToLive(baseName, locale)
                : this.timeToLive;
    }

    /**
     * キャッシュ再ロード判定.<br>
     * キャッシュ内で有効期限の切れたbundleを再ロードする必要があるかどうかを、loadTimeに指定されたロード時刻やその他のいくつかの条件に基づいて判定する（継承元クラスのコメント抜粋）.<br>
     * （拡張仕様が無いので継承元の操作をそのまま行う）<br>
     *
     * @see java.util.ResourceBundle.Control#needsReload(java.lang.String, java.util.Locale,
     * java.lang.String, java.lang.ClassLoader, java.util.ResourceBundle, long)
     * @return キャッシュ内で有効期限の切れたbundleを再ロード要否
     */
    @Override
    public boolean needsReload(String baseName, Locale locale, String format, ClassLoader loader, ResourceBundle bundle, long loadTime) {
        // ここを実装
        return super.needsReload(baseName, locale, format, loader, bundle, loadTime);
    }

    /**
     * BaseNameとlocaleの組み合わせで取得対象となるプロパティファイル名を編集する.<br>
     * （拡張仕様が無いので継承元の操作をそのまま行う）<br>
     *
     * @see java.util.ResourceBundle.Control#toBundleName(java.lang.String, java.util.Locale)
     * @return 取得対象のプロパティファイル名
     */
    @Override
    public String toBundleName(String baseName, Locale locale) {
        // ここを実装
        return super.toBundleName(baseName, locale);
    }
}
