package com.pinterest.ktlint.rule.engine.core.api

import org.ec4j.core.model.Property

/**
 * Loaded [Property]s from `.editorconfig` files.
 */
public typealias EditorConfigProperties = Map<String, Property>
