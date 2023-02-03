package com.flyprosper.plugins

import com.flyprosper.chat.ChatServer
import com.flyprosper.models.MessageData
import com.flyprosper.models.User
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json) // for development
//        contentConverter = KotlinxWebsocketSerializationConverter(ProtoBuf) // for build
    }
    routing {
        webSocket("/ws") {
            println("Client trying to connect")
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val frameText = frame.readText()
                try {
                    println(frameText)
//                    val data = receiveDeserialized<MessageData>()
                    val data = Json.decodeFromString<MessageData>(frameText)
                    when (data.channel) {
                        "create-room" -> {
                            val response = ChatServer.createRoom(
                                user = data.user ?: User(
                                    name = "",
                                    isAdmin = true, socket = this
                                ), data = data
                            )
                            println(response.convertToFrame().readText())
                            send(response.convertToFrame())
                        }

                        "join-room" -> {
                            val response = ChatServer.joinRoom(
                                user = User(
                                    name = data.user?.name ?: "",
                                    isAdmin = data.user?.isAdmin ?: true,
                                    socket = this
                                ), data = data
                            )
                            if (response.err == true)
                                send(response.convertToFrame())
                            else {
                                val sockets =
                                    data.roomCode?.let { data.user?.let { it1 -> ChatServer.getMembers(it, it1) } }
                                sockets?.forEach {
                                    it.socket?.send(response.convertToFrame())
                                }
                            }

                        }

                        "chat" -> {
//                            val sockets =
//                                data.roomCode?.let { data.user?.let { it1 -> ChatServer.getMembers(it, it1) } }
                            val members = ChatServer.getMembers(roomCode = data.roomCode?:"",
                                user = User(data.user?.name?:"", data.user?.isAdmin?:true, this))
                            if (members == null)
                                send(MessageData("chat", message = "User-Room mismatch", err=true).convertToFrame())
                            else
                                members.forEach {
                                    if (it.name != data.user?.name)
                                        it.socket?.send(data.convertToFrame())
                                }
                        }

                        "exit-room" -> {
                            ChatServer.disconnect(data.roomCode, data.user)
                            close(CloseReason(CloseReason.Codes.NORMAL, "A user disconnected"))
                        }

                        "media-sync" -> {
                            if (data.user?.isAdmin == true) {
                                val sockets = data.roomCode?.let { ChatServer.getMembers(it, data.user) }
                                if (sockets == null)
                                    send(MessageData("media-sync", message = "User-Room mismatch").convertToFrame())
                                else {
                                    ChatServer.mediaSync(data)
                                    sockets.forEach {
                                        if (it.name != data.user.name)
                                            it.socket?.send(data.convertToFrame())
                                    }
                                }
                            }
                        }

                        "test" -> {
                            println("Received on test channel")
                            println(data)
//                            sendSerialized(data)
                            send(data.convertToFrame())
//                            sendSerialized(
//                                MessageData(
//                                    channel = "test",
//                                    user = User(
//                                        name = "test user",
//                                        isAdmin = false,
//                                        null
//                                    ),
//                                    "test-room",
//                                    "This is a simple test",
//                                    false, "0.0.0"
//                                )
//                            )
                        }

                        else -> {
                            println("Invalid channel")
                            sendSerialized(MessageData("error", null, null, "Invalid channel"))
                        }
                    }
                } catch (e: Exception) {
                    println(e.message)
                    sendSerialized(MessageData("error", null, null, "${e.message}"))
                }
            }
        }
    }
}

private fun MessageData.convertToFrame(): Frame.Text = Frame.Text(Json.encodeToString(this))
