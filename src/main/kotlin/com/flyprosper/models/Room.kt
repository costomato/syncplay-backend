package com.flyprosper.models

data class Room(
    val creator: User,
    val members: MutableList<User>,
    val appVersion: String? = null,
    var isVideoPlaying: Boolean? = null,
    var currentTime: Int? = null
)
