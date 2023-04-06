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

import com.epam.dsm.common.*
import com.epam.dsm.test.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import org.flywaydb.core.*
import org.jetbrains.exposed.sql.transactions.*
import java.io.*
import kotlin.test.*
import kotlin.test.Test

/**
 * This test works with files in resources/db/migration
 */
class MigrationTest : PostgresBased("migration") {

    @BeforeTest
    fun setUp() = runBlocking {
        storeClient.createProcedureIfTableExist()
    }

    lateinit var flyway: Flyway

    @AfterTest
    fun clean() {
        flyway.clean()
    }

    private fun StoreClient.migrate(folder: String): Flyway {
        val hikariDataSource = hikariConfig
        @Suppress("DEPRECATION")
        flyway = Flyway.configure()
            .dataSource(hikariDataSource.jdbcUrl, hikariDataSource.username, hikariDataSource.password)
            .schemas(hikariDataSource.schema)
            .locations("filesystem:${File("src/test/resources/db/migration/$folder").absolutePath}")
            .baselineOnMigrate(true)
            .outOfOrder(true)//for testing
            .ignoreMissingMigrations(true)//for testing
            .load()
        flyway.migrate()
        return flyway
    }

    @Test
    fun `should skip migration when table does not exist`() = runBlocking {
        storeClient.migrate("remove")
        assertEquals(0, storeClient.getAll<RemoveField>().size)
    }

    @Test
    fun `should skip migration when table exist in another schema`() = runBlocking {
        val another = StoreClient(hikariConfig = TestDatabaseContainer.createConfig(schema = "another"))
        another.store(RemoveField("dd"))
        storeClient.migrate("remove")
        assertEquals(0, storeClient.getAll<RemoveField>().size)
    }

    @Test
    fun `should remove field when has migration script`() = runBlocking {
        val tableName = createTableIfNotExists<RemoveField>(storeClient.hikariConfig.schema)
        val id = "id"
        transaction {
            storeAsString(RemoveFieldOld(id, "this field will be removed in migration"), tableName)
        }

        storeClient.migrate("remove")

        assertEquals(RemoveField(id), storeClient.getAll<RemoveField>().first())
    }

    @Serializable
    data class RemoveField(
        @Id val id: String,
    )

    @Serializable
    data class RemoveFieldOld(
        @Id val id: String,
        val removeField: String,
    )

    @Test
    fun `should rename field when has migration script`() = runBlocking {
        val tableName = createTableIfNotExists<RenameField>(storeClient.hikariConfig.schema)
        val id = "id"
        val value = "this field will be renamed"
        transaction {
            storeAsString(RenameFieldOld(id, value), tableName)
        }

        storeClient.migrate("rename")

        assertEquals(RenameField(id, value), storeClient.getAll<RenameField>().first())
    }

    @Serializable
    data class RenameFieldOld(
        @Id val id: String,
        val oldName: String,
    )

    @Serializable
    data class RenameField(
        @Id val id: String,
        val newName: String,
    )


}

