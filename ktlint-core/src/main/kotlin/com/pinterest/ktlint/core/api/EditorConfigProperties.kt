package com.pinterest.ktlint.core.api

import org.ec4j.core.model.Property

/**
 * Loaded [Property]s from `.editorconfig` files.
 */
@Deprecated("Deprecated since ktlint 0.49.0. See changelog 0.49.")
public typealias EditorConfigProperties = Map<String, Property>
