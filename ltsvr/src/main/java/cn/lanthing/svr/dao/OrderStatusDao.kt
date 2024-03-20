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

import cn.lanthing.svr.model.OrderStatus
import cn.lanthing.svr.model.OrderStatuses
import cn.lanthing.svr.service.OrderService.OrderInfo
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class OrderStatusDao {
    @Autowired
    private lateinit var database: Database

    fun insertOrder(info: OrderInfo) : Boolean {
        val count = database.insert(OrderStatuses) {
            set(it.status, "NEW")
            set(it.fromDeviceID, info.fromDeviceID.toInt())
            set(it.toDeviceID, info.toDeviceID.toInt())
            set(it.roomID, info.roomID)
        }
        return count != 0
    }

    fun deleteOrder(roomID: String) {
        database.delete(OrderStatuses) { it.roomID eq roomID }
    }

    fun queryOrderByToDeviceID(toDeviceID: Int) : OrderStatus? {
        return try {
            database
                .from(OrderStatuses)
                .select()
                .where { OrderStatuses.toDeviceID eq toDeviceID }
                .map { row -> OrderStatuses.createEntity(row) }
                .first()
        } catch (e: NoSuchElementException) {
            null
        }
    }

    fun queryOrderByFromDeviceID(fromDeviceID: Int) : OrderStatus? {
        return try {
            database
                .from(OrderStatuses)
                .select()
                .where{ OrderStatuses.fromDeviceID eq fromDeviceID }
                .map { row -> OrderStatuses.createEntity(row) }
                .first()
        } catch (e: NoSuchElementException) {
            null
        }
    }
}