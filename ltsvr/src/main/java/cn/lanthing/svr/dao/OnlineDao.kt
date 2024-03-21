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

import cn.lanthing.svr.model.Online
import cn.lanthing.svr.model.Onlines
import jakarta.annotation.PostConstruct
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class OnlineDao {

    @Autowired
    private lateinit var database: Database

    @PostConstruct
    fun init() {
        database.useConnection { conn ->
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS "onlines" (
                	"id"				INTEGER NOT NULL UNIQUE,
                	"createdAt"			DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                	"controlling"		INTEGER NOT NULL,
                	"controlled"		INTEGER NOT NULL,
                	PRIMARY KEY("id" AUTOINCREMENT)
                );
            """.trimIndent())
        }
    }

    fun queryOnlineHistory(index: Int, limit: Int) : List<Online> {
        return database
            .from(Onlines)
            .select()
            .orderBy(Onlines.id.desc())
            .limit(limit)
            .offset(index)
            .map { row -> Onlines.createEntity(row) }
    }

    fun insertRecord(controlling: Int, controlled: Int) {
        database.insert(Onlines) {
            set(it.controlling, controlling)
            set(it.controlled, controlled)
        }
    }
}