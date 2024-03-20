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
CREATE TABLE IF NOT EXISTS "order_status" (
	"id"				INTEGER NOT NULL UNIQUE,
	"createdAt"			DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	"updatedAt"			DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	"status"			VARCHAR(32) NOT NULL,
	"fromDeviceID"		INTEGER NOT NULL,
	"toDeviceID"		INTEGER NOT NULL UNIQUE,
	"roomID"			VARCHAR(128) NOT NULL,
	PRIMARY KEY("id" AUTOINCREMENT)
);

CREATE TRIGGER IF NOT EXISTS UpdateOrderStatusTimestamp
	AFTER UPDATE
	ON order_status
BEGIN
	UPDATE orders SET updatedAt = CURRENT_TIMESTAMP WHERE id=OLD.id;
END;

//表记录那么少，需要索引吗
CREATE UNIQUE INDEX "idx_order_status_roomid" ON "order_status" ("roomID");
CREATE INDEX "idx_order_status_fromid" ON "order_status" ("fromDeviceID");
CREATE UNIQUE INDEX "idx_order_status_toid" ON "order_status" ("toDeviceID");
 */

interface OrderStatus : Entity<OrderStatus> {
    companion object : Entity.Factory<OrderStatus>()
    var id: Int
    var createdAt: LocalDateTime
    var updatedAt: LocalDateTime
    var status: String
    var fromDeviceID: Int
    var toDeviceID: Int
    var roomID: String
}


object OrderStatuses : Table<OrderStatus>("order_status") {
    val id = int("id").primaryKey().bindTo { it.id }
    val createdAt = datetime("createdAt").bindTo { it.createdAt }
    val updatedAt = datetime("updatedAt").bindTo { it.updatedAt }
    val status = varchar("status").bindTo { it.status }
    val fromDeviceID = int("fromDeviceID").bindTo { it.fromDeviceID }
    val toDeviceID = int("toDeviceID").bindTo { it.toDeviceID }
    val roomID = varchar("roomID").bindTo { it.roomID }
}