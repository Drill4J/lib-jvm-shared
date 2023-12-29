/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.dsm

import com.zaxxer.hikari.*
import org.jetbrains.exposed.sql.*

object DatabaseFactory {
    // TODO delete unused
//    fun init() {
//        Database.connect(hikari())
//    }

    fun init(hikariDataSource: HikariDataSource): Database = Database.connect(hikariDataSource)

    // TODO delete unused
//    private fun hikari(): HikariDataSource {
//        val config = HikariConfig()
//        config.driverClassName = "org.postgresql.Driver"
//        config.jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
//        config.username = "postgres"
//        config.password = "mysecretpassword"
//        config.maximumPoolSize = 10
//        config.isAutoCommit = true
//        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
//        config.validate()
//        return HikariDataSource(config)
//    }

}
