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

import cn.lanthing.svr.model.UsedID
import cn.lanthing.svr.model.UsedIDs
import jakarta.annotation.PostConstruct
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class UsedIDDao {

    @Autowired
    lateinit var database: Database

    @PostConstruct
    fun init() {
        val c = database.useConnection { conn ->
            conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS used_device_ids2 (
                	"id"	        INTEGER NOT NULL UNIQUE,
                	"createdAt"	    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                	"updatedAt"	    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                	"deviceID"      INTEGER,
                	"cookie"        TEXT,
                	PRIMARY KEY("id" AUTOINCREMENT)
                );
                CREATE TRIGGER IF NOT EXISTS UpdateTimestamp
                	AFTER UPDATE
                	ON used_device_ids2
                BEGIN
                	UPDATE used_device_ids2 SET updatedAt = CURRENT_TIMESTAMP WHERE id=OLD.id;
                END;
            """.trimIndent())
        }
        c.execute()
    }

    fun queryByDeviceID(deviceID: Long): UsedID? {
        return try {
            database
                .from(UsedIDs)
                .select()
                .where { UsedIDs.deviceID eq deviceID.toInt() }
                .limit(1)
                .map { row -> UsedIDs.createEntity(row) }
                .first()
        } catch (e: NoSuchElementException) {
            null
        }
    }

    fun addDeviceID(deviceID: Long, cookie: String) {
        database.insert(UsedIDs) {
            set(it.deviceID, deviceID.toInt())
            set(it.cookie, cookie)
        }
    }

    fun updateCookie(deviceID: Long, cookie: String) {
        database.update(UsedIDs) {
            set(it.cookie, cookie)
            where { it.deviceID eq deviceID.toInt() }
        }
    }

    fun countID() : Int {
        val result = database
            .from(UsedIDs)
            .select(count())
            .map { it.getInt(0) }
        return if (result.isEmpty()) {
            0
        } else {
            result[0]
        }
    }

    fun clear() {
        database.deleteAll(UsedIDs)
    }
}