package com.pinterest.ktlint.environment

import java.lang.String.CASE_INSENSITIVE_ORDER
import java.util.TreeMap

internal class CaseInsensitiveMap<V : Any> :
    TreeMap<String, V>(CASE_INSENSITIVE_ORDER), MutableMap<String, V>
