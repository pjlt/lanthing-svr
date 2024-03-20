/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2024 Zhennan Tu <zhennan.tu@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package cn.lanthing.svr.model

import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import java.time.LocalDateTime

/*
CREATE TABLE IF NOT EXISTS "orders" (
	"id"				INTEGER NOT NULL UNIQUE,
	"createdAt"			DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	"updatedAt"			DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	"finishedAt"		DATETIME,
	"finishReason"		VARCHAR(32),
	"fromDeviceID"		INTEGER NOT NULL,
	"toDeviceID"		INTEGER NOT NULL,
	"clientRequestID"	INTEGER NOT NULL,
	"signalingHost"		VARCHAR(255) NOT NULL,
	"signalingPort"		INTEGER NOT NULL,
	"roomID"			VARCHAR(128) NOT NULL,
	"serviceID"			VARCHAR(128) NOT NULL,
	"clientID"			VARCHAR(128) NOT NULL,
	"authToken"			VARCHAR(128) NOT NULL,
	"p2pUser"			VARCHAR(16) NOT NULL,
	"p2pToken"			VARCHAR(16) NOT NULL,
	"relayServer"		VARCHAR(64) NOT NULL,
	"reflexServers"		VARCHAR(1024) NOT NULL,
	PRIMARY KEY("id" AUTOINCREMENT)
);

CREATE TRIGGER IF NOT EXISTS UpdateOrderTimestamp
	AFTER UPDATE
	ON orders
BEGIN
	UPDATE orders SET updatedAt = CURRENT_TIMESTAMP WHERE id=OLD.id;
END;
 */

interface Order : Entity<Order> {
    companion object : Entity.Factory<Order>()
    var id: Int
    var createdAt: LocalDateTime
    var updatedAt: LocalDateTime
    var finishedAt: LocalDateTime
    var finishReason: String
    var fromDeviceID: Int
    var toDeviceID: Int
    var clientRequestID: Int
    var signalingHost: String
    var signalingPort: Int
    var roomID: String
    var serviceID: String
    var clientID: String
    var authToken: String
    var p2pUser: String
    var p2pToken: String
    var relayServer: String
    var reflexServers: String
}

object Orders : Table<Order>("orders") {
    val id = int("id").primaryKey().bindTo { it.id }
    val createdAt = datetime("createdAt").bindTo { it.createdAt }
    val updatedAt = datetime("updatedAt").bindTo { it.updatedAt }
    val finishedAt = datetime("finishedAt").bindTo { it.finishedAt }
    val finishReason = varchar("finishReason").bindTo { it.finishReason }
    val fromDeviceID = int("fromDeviceID").bindTo { it.fromDeviceID }
    val toDeviceID = int("toDeviceID").bindTo { it.toDeviceID }
    val clientRequestID = int("clientRequestID").bindTo { it.clientRequestID }
    val signalingHost = varchar("signalingHost").bindTo { it.signalingHost }
    val signalingPort = int("signalingPort").bindTo { it.signalingPort }
    val roomID = varchar("roomID").bindTo { it.roomID }
    val serviceID = varchar("serviceID").bindTo { it.serviceID }
    val clientID = varchar("clientID").bindTo { it.clientID }
    val authToken = varchar("authToken").bindTo { it.authToken }
    val p2pUser = varchar("p2pUser").bindTo { it.p2pUser }
    val p2pToken = varchar("p2pToken").bindTo { it.p2pToken }
    val relayServer = varchar("relayServer").bindTo { it.relayServer }
    val reflexServers = varchar("reflexServers").bindTo { it.reflexServers }
}