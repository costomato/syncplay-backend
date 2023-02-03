package com.flyprosper.models

import io.ktor.server.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class User(
    val name: String,
    val isAdmin: Boolean,
    @Transient
    val socket: WebSocketServerSession? = null
)