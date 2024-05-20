package com.corner.util

fun catch(body: () -> Unit) = runCatching { body() }.onFailure { it.printStackTrace() }.getOrNull() ?: Unit