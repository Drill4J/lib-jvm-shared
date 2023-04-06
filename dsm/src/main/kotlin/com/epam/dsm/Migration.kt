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

import com.epam.dsm.util.*

/**
 * After invoke this function you are able to call procedure execute_if_table_exist in migration scripts.
 * It will run sql if the table exist else skip it.
 */
fun StoreClient.createProcedureIfTableExist() {
    executeInTransaction {
        val schema = hikariConfig.schema
        logger.debug { "Creating procedure execute_if_table_exist for '$schema'..." }
        execWrapper("""
            CREATE OR REPLACE PROCEDURE execute_if_table_exist(tableName text, sql text)
                            LANGUAGE plpgsql
                        AS
                        ${'$'}func${'$'}
                        BEGIN
                            IF EXISTS
                                (SELECT
                                 FROM information_schema.tables
                                 WHERE table_schema = '$schema'
                                   AND table_name = tableName
                                )
                            THEN
                                EXECUTE sql || ' ;' ;
                            ELSE
                                RAISE NOTICE 'Skip migration because table ''%'' does not exist.', tableName;
                            END IF;
                        end
                        ${'$'}func${'$'};
        """.trimIndent()
        )
    }
}
