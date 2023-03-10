package com.flyprosper.models

import kotlinx.serialization.Serializable

@Serializable
data class MessageData(
    val channel: String,
    val user: User? = null,
    val roomCode: String? = null,
    val message: String,
    val err: Boolean? = false,
    val appVersion: String? = null,
    val info: String? = null,
    var nUsers: Int? = null,
    val isVideoPlaying: Boolean? = null,
    val currentTime: Long? = null
)
