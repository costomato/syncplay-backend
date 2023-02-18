package com.flyprosper.chat

import com.flyprosper.models.MessageData
import com.flyprosper.models.Room
import com.flyprosper.models.User
import com.github.javafaker.Faker

object ChatServer {
    private val rooms = mutableMapOf<String, Room>()

    private fun getRandomString(length: Int = 6): String {
        val allowedChars = ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun createRoom(user: User, data: MessageData): MessageData {
        var roomCode = getRandomString()
        while (rooms.containsKey(roomCode))
            roomCode = getRandomString()

        val randomName = getRandomName()
        val newUser = User(randomName, user.isAdmin, user.socket)
        rooms[roomCode] = Room(
            creator = newUser,
            members = mutableListOf(newUser),
            videoInRoom = data.message,
            isVideoPlaying = true,
            appVersion = data.appVersion,
            currentTime = 0 // would have been gone far ahead by the time next user joins. So... lets see
        )
        println("Room created with code: $roomCode")
        return MessageData(
            channel = "create-room", user = newUser, roomCode = roomCode,
            message = data.message, info = data.info, nUsers = 1, isVideoPlaying = true, currentTime = 0
        )
    }

    fun joinRoom(user: User, data: MessageData): MessageData {
        println("A user tried to join room with code: ${data.roomCode}")
        if (rooms.containsKey(data.roomCode)) {
            if (rooms[data.roomCode]?.appVersion?.equals(data.appVersion) == false)
                return MessageData(
                    channel = "join-room",
                    roomCode = data.roomCode,
                    message = "App versions do not match", err = true
                )
            if (rooms[data.roomCode]?.members?.contains(user) == true)
                return MessageData(
                    channel = "join-room",
                    roomCode = data.roomCode,
                    message = "User already in room",
                    err = true
                )
            val randomName = getRandomName()
            val newUser = User(randomName, user.isAdmin, user.socket)
            rooms[data.roomCode]?.members?.add(newUser)
            return MessageData(
                channel = "join-room",
                user = User(randomName, user.isAdmin),
                roomCode = data.roomCode,
                message = rooms[data.roomCode]?.videoInRoom ?: "",
                info = data.info,
                nUsers = rooms[data.roomCode]?.members?.size,
                isVideoPlaying = rooms[data.roomCode]?.isVideoPlaying,
                currentTime = rooms[data.roomCode]?.currentTime
            )
        } else
            return MessageData(channel = "join-room", message = "Room does not exist", err = true)
    }

    fun getMembers(roomCode: String, user: User): List<User>? {
        val room = rooms[roomCode]
        println("getMembers: $room")
        return if (room?.members?.any { it.name == user.name } == true)
            rooms[roomCode]?.members
        else
            null
    }

    fun mediaSync(data: MessageData) {
        rooms[data.roomCode]?.currentTime = data.currentTime
        rooms[data.roomCode]?.isVideoPlaying = data.isVideoPlaying
    }

    fun disconnect(roomCode: String?, user: User?): List<User>? {
        if (rooms[roomCode]?.members?.any { it.name == user?.name } == true)
            rooms[roomCode]?.members?.removeIf { it.name == user?.name }
        if (rooms[roomCode]?.members?.isEmpty() == true)
            rooms.remove(roomCode)
        return rooms[roomCode]?.members
    }

    private fun getRandomName(): String = Faker().funnyName().name()
}
