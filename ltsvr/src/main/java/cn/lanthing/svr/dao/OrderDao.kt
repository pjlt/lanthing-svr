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

package cn.lanthing.svr.dao

import cn.lanthing.svr.model.Order
import cn.lanthing.svr.model.Orders
import cn.lanthing.svr.service.OrderService.OrderInfo
import jakarta.annotation.PostConstruct
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class OrderDao {
    @Autowired
    private lateinit var database: Database

    @PostConstruct
    fun init() {
        val c = database.useConnection { conn ->
            conn.prepareStatement("""
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
                
                CREATE UNIQUE INDEX IF NOT EXISTS "idx_orders_roomid" ON "orders" ("roomID");
            """.trimIndent())
        }
        c.execute()
    }

    fun queryOrderByRoomID(roomID: String) : Order? {
        return try {
            database
                .from(Orders)
                .select()
                .where { Orders.roomID eq roomID }
                .limit(1)
                .map { row -> Orders.createEntity(row) }
                .first()
        } catch (e: NoSuchElementException) {
            null
        }
    }

    fun queryHistoryOrders(index: Int, limit: Int) : List<Order> {
        return database
                .from(Orders)
                .select()
                .orderBy(Orders.id.asc())
                .limit(limit)
                .offset(index)
                .map { row -> Orders.createEntity(row) }
    }

    fun insertOrder(info: OrderInfo) : Boolean {
        val reflexServers = info.reflexServers.joinToString(",")
        val count = database.insert(Orders) {
            set(it.fromDeviceID, info.fromDeviceID.toInt())
            set(it.toDeviceID, info.toDeviceID.toInt())
            set(it.clientRequestID, info.clientRequestID.toInt())
            set(it.signalingHost, info.signalingAddress)
            set(it.signalingPort, info.signalingPort)
            set(it.roomID, info.roomID)
            set(it.serviceID, info.serviceID)
            set(it.clientID, info.clientID)
            set(it.authToken, info.authToken)
            set(it.p2pUser, info.p2pUsername)
            set(it.p2pToken, info.p2pPassword)
            set(it.relayServer, info.relayServer)
            set(it.reflexServers, reflexServers)
        }
        return count != 0
    }

    fun markOrderFinishedWithReason(roomID: String, reason: String) : Boolean {
        val count = database.update(Orders) {
            set(it.finishReason, reason)
            where {
                (it.roomID eq roomID) and (it.finishReason.isNull())
            }
        }
        return count != 0
    }

    fun countOrder() : Int {
        val result = database
            .from(Orders)
            .select(count())
            .map { it.getInt(0) }
        return if (result.isEmpty()) {
            0
        } else {
            result[0]
        }
    }

    fun clear() {
        database.deleteAll(Orders);
    }
}