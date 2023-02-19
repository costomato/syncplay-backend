package com.flyprosper.models

data class Room(
    val creator: User,
    val members: MutableList<User>,
    val appVersion: String? = null,
    val videoInRoom: String? = null,
    var isVideoPlaying: Boolean? = null,
    var currentTime: Long? = null,
    val videoDuration: String? = null
)
