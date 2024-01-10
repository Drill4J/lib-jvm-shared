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
package com.epam.drill.agent.configuration.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.epam.drill.agent.configuration.AgentConfigurationProvider
import com.epam.drill.agent.configuration.DefaultParameterDefinitions

class PropertiesFileProviderTest {

    @Test
    fun `parse empty lines`() {
        val result = PropertiesFileProvider(emptySet()).parseLines("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse mixed lines`() {
        val result = PropertiesFileProvider(emptySet()).parseLines("""
            foo1=bar1            
            #foo2=bar2            

            foo3=bar3            
            #foo4=bar4            
        """)
        assertEquals(2, result.size)
        assertEquals("bar1", result["foo1"])
        assertEquals("bar3", result["foo3"])
    }

    @Test
    fun `config path default`() {
        val result = PropertiesFileProvider(emptySet()).configPath()
        assertEquals(".\\drill.properties", result)
    }

    @Test
    fun `config path from installation dir`() {
        val result = PropertiesFileProvider(setOf(SimpleMapProvider(mapOf(
            DefaultParameterDefinitions.INSTALLATION_DIR.name to "C:\\data\\agent"
        )))).configPath()
        assertEquals("C:\\data\\agent\\drill.properties", result)
    }

    @Test
    fun `config path from property`() {
        val result = PropertiesFileProvider(setOf(SimpleMapProvider(mapOf(
            DefaultParameterDefinitions.INSTALLATION_DIR.name to "C:\\data\\agent",
            DefaultParameterDefinitions.CONFIG_PATH.name to "C:\\data\\agent\\config.properties"
        )))).configPath()
        assertEquals("C:\\data\\agent\\config.properties", result)
    }

    @Test
    fun `from providers empty providers`() {
        val result = PropertiesFileProvider(emptySet()).fromProviders()
        assertNull(result)
    }

    @Test
    fun `from providers no entry`() {
        val provider1 = SimpleMapProvider(mapOf("foo" to "bar1"))
        val provider2 = SimpleMapProvider(mapOf("foo" to "bar2"))
        val result = PropertiesFileProvider(setOf(provider1, provider2)).fromProviders()
        assertNull(result)
    }

    @Test
    fun `from providers one entry`() {
        val provider1 = SimpleMapProvider(mapOf("foo" to "bar1"))
        val provider2 = SimpleMapProvider(mapOf(DefaultParameterDefinitions.CONFIG_PATH.name to "C:\\config1.properties"))
        val result = PropertiesFileProvider(setOf(provider1, provider2)).fromProviders()
        assertEquals("C:\\config1.properties", result)
    }

    @Test
    fun `from providers two entry prioritized`() {
        val provider1 = SimpleMapProvider(mapOf(DefaultParameterDefinitions.CONFIG_PATH.name to "C:\\config1.properties"), 100)
        val provider2 = SimpleMapProvider(mapOf(DefaultParameterDefinitions.CONFIG_PATH.name to "C:\\config2.properties"), 200)
        val result = PropertiesFileProvider(setOf(provider1, provider2)).fromProviders()
        assertEquals("C:\\config2.properties", result)
    }

    @Test
    fun `from installation dir empty providers`() {
        val result = PropertiesFileProvider(emptySet()).fromInstallationDir()
        assertNull(result)
    }

    @Test
    fun `from installation dir no entry`() {
        val provider1 = SimpleMapProvider(mapOf("foo" to "bar1"))
        val provider2 = SimpleMapProvider(mapOf("foo" to "bar2"))
        val result = PropertiesFileProvider(setOf(provider1, provider2)).fromInstallationDir()
        assertNull(result)
    }

    @Test
    fun `from installation dir one entry`() {
        val provider1 = SimpleMapProvider(mapOf("foo" to "bar1"))
        val provider2 = SimpleMapProvider(mapOf(DefaultParameterDefinitions.INSTALLATION_DIR.name to "C:\\data1"))
        val result = PropertiesFileProvider(setOf(provider1, provider2)).fromInstallationDir()
        assertEquals("C:\\data1\\drill.properties", result)
    }

    @Test
    fun `from installation dir two entry prioritized`() {
        val provider1 = SimpleMapProvider(mapOf(DefaultParameterDefinitions.INSTALLATION_DIR.name to "C:\\data1"), 100)
        val provider2 = SimpleMapProvider(mapOf(DefaultParameterDefinitions.INSTALLATION_DIR.name to "C:\\data2"), 200)
        val result = PropertiesFileProvider(setOf(provider1, provider2)).fromInstallationDir()
        assertEquals("C:\\data2\\drill.properties", result)
    }

    private class SimpleMapProvider(
        override val configuration: Map<String, String>,
        override val priority: Int = 100
    ) : AgentConfigurationProvider

}