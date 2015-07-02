/*******************************************************************************
 * Copyright 2015 eTilbudsavis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.eTilbudsavis.etasdk;

public class Constants {

    public static final String META_API_KEY = "com.eTilbudsavis.etasdk.api_key";
    public static final String META_API_SECRET = "com.eTilbudsavis.etasdk.api_secret";

    public static final String META_DEVELOP_API_KEY = "com.eTilbudsavis.etasdk.develop.api_key";
    public static final String META_DEVELOP_API_SECRET = "com.eTilbudsavis.etasdk.develop.api_secret";

    public static final String TAG_PREFIX = "Etasdk-";
    public static final String ARG_PREFIX = "com.eTilbudsavis.etasdk.";

    public static String getTag(Class<?> clazz) {
        return getTag(clazz.getSimpleName());
    }

    public static String getTag(String tag) {
        return TAG_PREFIX + tag;
    }

    public static String getArg(String arg) {
        return ARG_PREFIX + arg;
    }

}
