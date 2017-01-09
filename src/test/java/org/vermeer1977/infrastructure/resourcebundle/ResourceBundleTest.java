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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Yamashita,Takahiro
 */
public class ResourceBundleTest {

    public ResourceBundleTest() {
    }

    @Test
    public void 標準のASCIIコードのpropertiesを参照する() {
        CustomControl control = CustomControl.builder().build();
        ResourceBundle bundle = ResourceBundle.getBundle("message", control);
        assertThat(bundle.getString("test"), is("ascii(default)"));
    }

    @Test
    public void 文字コードにUTF8_ロケールの指定をしない_デフォルトロケール() {
        CustomControl control = CustomControl.builder().charCode(StandardCharsets.UTF_8.toString()).build();
        ResourceBundle bundle = ResourceBundle.getBundle("utf8", control);
        assertThat(bundle.getString("test"), is("UTF8のテスト(JP)"));
    }

    @Test
    public void 文字コードにUTF8_ロケールが存在しない_デフォルトリソース() {
        CustomControl control = CustomControl.builder()
                .charCode(StandardCharsets.UTF_8.toString()).build();
        ResourceBundle bundle = ResourceBundle.getBundle("utf8", Locale.CHINESE, control);
        assertThat(bundle.getString("test"), is("UTF8のテスト(default)"));
    }

    @Test
    public void xmlのリソースを参照() {
        //XML no need charCode set (set ignore)
        CustomControl control = CustomControl.builder().build();
        ResourceBundle bundle = ResourceBundle.getBundle("SJIS", control);
        assertThat(bundle.getString("sjis.xml"), is("XML読み込み(SJIS)"));
    }

    /**
     * xmlとpropertiesに同名のbaseNameの資産が存在する場合は、getFormats()の優先度（Class-Properties-XML）に従ってリソースを参照する.<br>
     * 本ケースはpropertiesが参照されるが、文字コードの指定が無いため文字化けが発生する
     */
    @Test
    public void xmlとpropertiesに同名のbaseNameの資産が存在する_propertiesが参照される_文字コード未指定により文字化け() {
        CustomControl control = CustomControl.builder().build();
        ResourceBundle bundle = ResourceBundle.getBundle("SJIS-SAME", control);
        assertThat(bundle.getString("test"), is("properties:¯¼XML è1(SJIS)"));
    }

    @Test
    public void xmlとpropertiesに同名のbaseNameの資産が存在する_xml指定_文字コード文字化けしない() {
        CustomControl control = CustomControl.builder().formats(CustomControl.FORMAT_XML).build();
        ResourceBundle bundle = ResourceBundle.getBundle("SJIS-SAME2", control);
        assertThat(bundle.getString("sjis2.xml"), is("xml:同名propertiesあり2(SJIS)"));
    }

    /**
     * xmlとpropertiesに同名のbaseNameの資産が存在する.<br>
     * しかし、formatsの優先度に従ってpropertiesリソースを参照し、かつ文字コード指定に従って変換する
     */
    @Test
    public void xmlとpropertiesに同名のbaseNameの資産が存在する_formats優先度指定_properties参照() {
        CustomControl control = CustomControl.builder()
                .charCode("SJIS")
                .formats(CustomControl.FORMAT_DEFAULT)
                .formats(CustomControl.FORMAT_XML)
                .build();
        ResourceBundle bundle = ResourceBundle.getBundle("SJIS-SAME3", control);
        assertThat(bundle.getString("test"), is("properties:同名XMLあり3(SJIS)"));
    }

    /**
     * ロケールがJAPANESEの時の候補Localeの第一優先がUS（日本語じゃなくて、英語のメッセージが出力される
     */
    @Test
    public void ロケールがJAPANESEの時の候補Localeの第一優先がUS_日本語じゃなくて英語のメッセージが出力() {
        CustomControl control = CustomControl.builder()
                .charCode("UTF-8")
                .targetCandidateLocalePair(
                        TargetCandidateLocalePair.builder()
                        .targetLocale(Locale.JAPANESE)
                        .candidateLocale(Locale.US)
                        .candidateLocale(Locale.JAPAN)
                        .candidateLocale(Locale.ROOT)
                        .build())
                .build();

        ResourceBundle bundle = ResourceBundle.getBundle("utf8", Locale.JAPANESE, control);
        assertThat(bundle.getString("test"), is("UTF8english"));
    }

    /**
     * 優先度指定をしたロケール以外の場合は、対象ロケール本来の優先度に従って参照する.<br>
     * 本ケースの場合、USを指定しているので、優先度指定は無視される
     */
    @Test
    public void 優先度指定をしたロケール以外の場合_対象ロケール本来の優先度に従って参照_US指定で優先度無視() {
        CustomControl control = CustomControl.builder()
                .charCode("UTF-8")
                .targetCandidateLocalePair(
                        TargetCandidateLocalePair.builder()
                        .targetLocale(Locale.JAPANESE)
                        .candidateLocale(Locale.JAPAN)
                        .candidateLocale(Locale.ENGLISH)
                        .candidateLocale(Locale.US)
                        .candidateLocale(Locale.ROOT)
                        .build())
                .build();

        ResourceBundle bundle = ResourceBundle.getBundle("utf8", Locale.US, control);
        assertThat(bundle.getString("test"), is("UTF8english"));
    }

    /**
     * 優先度指定をしたロケール以外の場合は、対象ロケール本来の優先度に従って参照する.<br>
     * 本ケースの場合、CHINAを指定しているので、優先度も無視されるし、デフォルトリソースが使用される
     */
    @Test
    public void 優先度指定をしたロケール以外の場合_対象ロケール本来の優先度に従って参照_未存在ロケール指定でデフォルトリソース使用() {
        CustomControl control = CustomControl.builder()
                .charCode("UTF-8")
                .targetCandidateLocalePair(
                        TargetCandidateLocalePair.builder()
                        .targetLocale(Locale.JAPANESE)
                        .candidateLocale(Locale.JAPAN)
                        .candidateLocale(Locale.ROOT)
                        .build())
                .build();

        ResourceBundle bundle = ResourceBundle.getBundle("utf8", Locale.CHINA, control);
        assertThat(bundle.getString("test"), is("UTF8のテスト(default)"));
    }

    /**
     * 優先度指定をしたロケールにNullを指定した場合は、パラメータで指定したロケールに置き換えて参照する.<br>
     * 本ケースの場合、優先度の順で判断した場合、一番始めヒットするのはJAPANESEのリソースであり、それが参照される.
     */
    @Test
    public void 優先度指定をしたロケールにNullを指定した場合_パラメータで指定したロケールに置き換えて参照() {
        CustomControl control = CustomControl.builder()
                .charCode("UTF-8")
                .targetCandidateLocalePair(
                        TargetCandidateLocalePair.builder()
                        .targetLocale(Locale.JAPAN)
                        .candidateLocale(Locale.FRANCE)
                        .candidateLocale(null)
                        .build())
                .build();

        ResourceBundle bundle = ResourceBundle.getBundle("utf8", Locale.JAPAN, control);
        assertThat(bundle.getString("test"), is("UTF8のテスト(JP)"));
    }

    /**
     * ロケールの指定をしない場合は、デフォルトロケール（ロケールがja_JP）のpropertiesを参照して、優先度指定は無視する.<br>
     * ※デフォルトロケールで優先度を参照もしないし、デフォルトリソースを参照することもしない.
     */
    @Test
    public void ロケールの指定をしない場合_デフォルトロケール_ロケールがja_JP_のpropertiesを参照して_優先度指定は無視() {
        CustomControl control = CustomControl.builder()
                .charCode("UTF-8")
                .targetCandidateLocalePair(
                        TargetCandidateLocalePair.builder()
                        .targetLocale(Locale.JAPANESE)
                        .candidateLocale(Locale.ROOT)
                        .build())
                .targetCandidateLocalePair(
                        TargetCandidateLocalePair.builder()
                        .targetLocale(Locale.JAPAN)
                        .candidateLocale(Locale.ROOT)
                        .build())
                .build();

        ResourceBundle bundle = ResourceBundle.getBundle("utf8", control);
        assertThat(bundle.getString("test"), is("UTF8のテスト(JP)"));
    }

    /**
     * データをキャッシュしない（CustomControl.TTL_DONT_CACHE）.<br>
     * Controlの文字コードを変更すると"utf8NoCache"で同名のResourceBundleも参照文字コードも書き換わる
     */
    @Test
    public void データをキャッシュしない() {
        CustomControl control = CustomControl.builder().timeToLive(CustomControl.TTL_DONT_CACHE).build();
        ResourceBundle bundle = ResourceBundle.getBundle("utf8NoCache", control);
        assertThat(bundle.getString("test"), is("UTF8ã®ãã¹ã(NoCache)"));

        control = CustomControl.builder().charCode("UTF-8").timeToLive(CustomControl.TTL_DONT_CACHE).build();
        bundle = ResourceBundle.getBundle("utf8NoCache", control);
        assertThat(bundle.getString("test"), is("UTF8のテスト(NoCache)"));
    }

    /**
     * データをキャッシュする（CustomControl.TTL_NO_EXPIRATION_CONTROL）.<br>
     * Controlの文字コードを変更しても"utf8Cache1"で同名のキャッシュが書き換わらないので文字が化けない
     */
    @Test
    public void データをキャッシュする() {
        CustomControl control = CustomControl.builder().charCode("UTF-8").timeToLive(CustomControl.TTL_NO_EXPIRATION_CONTROL).build();
        ResourceBundle bundle = ResourceBundle.getBundle("utf8Cache1", control);
        assertThat(bundle.getString("test"), is("UTF8のテスト(cache1)"));

        control = CustomControl.builder().charCode("SJIS").timeToLive(CustomControl.TTL_NO_EXPIRATION_CONTROL).build();
        bundle = ResourceBundle.getBundle("utf8Cache1", control);
        assertThat(bundle.getString("test"), is("UTF8のテスト(cache1)"));
    }

    /**
     * データをキャッシュする（CustomControl.TTL_NO_EXPIRATION_CONTROL）.<br>
     * Controlの文字コードを変更しても"utf8Cache2"で同名のキャッシュが書き換わらないので文字が化ける
     */
    @Test
    public void データをキャッシュする_文字コード変更が適用されない() {
        CustomControl control = CustomControl.builder().timeToLive(CustomControl.TTL_NO_EXPIRATION_CONTROL).build();
        ResourceBundle bundle = ResourceBundle.getBundle("utf8Cache2", control);
        assertThat(bundle.getString("test"), is("UTF8ã®ãã¹ã(cache2)"));

        control = CustomControl.builder().charCode("UTF-8").timeToLive(CustomControl.TTL_NO_EXPIRATION_CONTROL).build();
        bundle = ResourceBundle.getBundle("utf8Cache2", control);
        assertThat(bundle.getString("test"), is("UTF8ã®ãã¹ã(cache2)"));
    }

    /**
     * timeToLiveが停止時間よりも大きいのでキャッシュされた"utf8IntervalCache1"を参照する.<br>
     *
     */
    @Test
    public void useUTF8SleeplessthanTimeLtInterval() {
        CustomControl control = CustomControl.builder().charCode("UTF-8").timeToLive(1000L).build();
        ResourceBundle bundle = ResourceBundle.getBundle("utf8IntervalCache1", control);
        assertThat(bundle.getString("test"), is("UTF8のテスト(IntervalCache1)"));

        try {
            System.err.println("sleeptime < timeToLive");
            Thread.sleep(1 * 1);
        } catch (InterruptedException e) {
            System.err.println(e);
        }

        control = CustomControl.builder().charCode("SJIS").build();
        bundle = ResourceBundle.getBundle("utf8IntervalCache1", control);
        assertThat(bundle.getString("test"), is("UTF8のテスト(IntervalCache1)"));
    }

    @Test(expected = MissingResourceException.class)
    public void リソースファイル名が存在しない() {
        CustomControl control = CustomControl.builder().build();
        ResourceBundle bundle = ResourceBundle.getBundle("msg", control);
    }

    @Test(expected = IllegalArgumentException.class)
    public void フォーマットが存在しない() {
        CustomControl control = CustomControl.builder()
                .formats(Collections.unmodifiableList(Arrays.asList("not_exist")))
                .build();
        ResourceBundle bundle = ResourceBundle.getBundle("message", control);
    }

    /**
     * 停止時間がtimeToLiveよりも大きいので改めて"utf8IntervalCache1"を参照したときに、ファイルが更新されていたらキャッシュを参照せず、再取得する 手作業でテスト。テスト後、ランナーとしては対象外にする
     */
    @Ignore
    @Test
    public void useUTF8IntervalLtSleepTime() {
        CustomControl control = CustomControl.builder().charCode("UTF-8").timeToLive(1L).build();
        ResourceBundle bundle = ResourceBundle.getBundle("utf8IntervalCache2", control);
        assertThat(bundle.getString("test"), is("UTF8のテスト(IntervalCache2edit)"));

        try {
            System.err.println("timeToLive < sleeptime");
            System.err.println("edit utf8IntervalCache2.properties while sleeping !!");
            Thread.sleep(1 * 10000L);
        } catch (InterruptedException e) {
            System.err.println(e);
        }

        control = CustomControl.builder().charCode("SJIS").build();
        bundle = ResourceBundle.getBundle("utf8IntervalCache2", control);
        assertThat(bundle.getString("test"), is(not("UTF8のテスト(IntervalCache2edit)")));
        System.err.println("utf8IntervalCache2:" + bundle.getString("test"));
    }

}
