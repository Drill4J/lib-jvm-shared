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
package com.epam.dsm.test

import com.epam.dsm.*
import com.epam.dsm.test.TestDatabaseContainer.Companion.clearData
import com.zaxxer.hikari.*
import org.jetbrains.exposed.sql.transactions.*
import org.testcontainers.containers.*
import org.testcontainers.containers.wait.strategy.*

/**
 * This class helps to write tests with DB
 * in init is created docker container before run tests
 * @see clearData to clear all data from DB after a test
 */
class TestDatabaseContainer {
    companion object {
        private val postgresContainer: PostgreSQLContainer<Nothing> =
            PostgreSQLContainer<Nothing>("postgres:12").apply {
                withExposedPorts(PostgreSQLContainer.POSTGRESQL_PORT)
                waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2))
                withUrlParam("reWriteBatchedInserts", "true")
                start()
            }

        fun startOnce() = println("init container")

        fun createConfig(
            maximumPoolSize: Int = 10,
            isAutoCommit: Boolean = true,
            schema: String = "public",
        ): HikariConfig {
            val hikariConfig = HikariConfig().apply {
                this.driverClassName = postgresContainer.driverClassName
                this.jdbcUrl = postgresContainer.jdbcUrl
                this.username = postgresContainer.username
                this.password = postgresContainer.password
                this.maximumPoolSize = maximumPoolSize
                this.isAutoCommit = isAutoCommit
                this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                this.schema = schema
                this.validate()
            }
            return hikariConfig
        }

        private val pools = mutableSetOf<HikariDataSource>()

        fun createDataSource(
            maximumPoolSize: Int = 10,
            isAutoCommit: Boolean = true,
            schema: String = "public",
        ): HikariDataSource {
            val hikariConfig = createConfig(maximumPoolSize, isAutoCommit, schema)
            val hikariDataSource = HikariDataSource(hikariConfig)
            pools.add(hikariDataSource)
            return hikariDataSource
        }

        fun clearData() {
            println("clear database...")
            transaction {
                createdTables.forEach {
                    exec("DROP TABLE IF EXISTS $it CASCADE")
                }
            }
            createdTables.clear()

            pools.forEach { it.close() }
            pools.clear()
        }
    }
}
