package com.github.shyiko.ktlint.internal

import java.security.Permission

fun disableSystemExitCall() {
    System.setSecurityManager(object : SecurityManager() {
        override fun checkPermission(perm: Permission?) {
            if (perm?.name?.contains("exitVM") == true) {
                throw SystemExitException()
            }
        }

        override fun checkRead(file: String?) = Unit
    })
}

fun enableSystemExitCall() = System.setSecurityManager(null)

internal class SystemExitException : Exception()
