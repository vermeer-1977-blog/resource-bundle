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

import java.util.List;
import java.util.Locale;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * CustomControlで使用するCandidateLocaleのペア.<br>
 * candidateLocaleにnullを指定した場合、{@link org.vermeer1977.infrastructure.resourcebundle.CustomControl#getCandidateLocales(java.lang.String, java.util.Locale)
 * }getCandidateLocalesで候補となるLocaleを取得する際、引数として指定したlocaleを処理対象として置き換えて使用する.
 *
 * @author Yamashita,Takahiro
 */
@Builder @Getter
public class TargetCandidateLocalePair {

    private final Locale targetLocale;

    @Singular
    private final List<Locale> candidateLocales;

}
