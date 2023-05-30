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
package com.epam.drill.plugins.test2code.api

import kotlinx.serialization.*
import kotlin.jvm.*

const val DEFAULT_TEST_NAME = "unspecified"

@Serializable
data class StartPayload(
    val testType: String = "MANUAL",
    val sessionId: String = "",
    val testName: String? = null,
    val isRealtime: Boolean = false,
    val isGlobal: Boolean = false,
    val labels: Set<Label> = emptySet(),
)

@Serializable
data class SessionPayload(val sessionId: String)

@Serializable
data class SessionDataPayload(val sessionId: String, val data: String)

@Serializable
data class CoverPayload(
    val sessionId: String,
    val data: List<EntityProbes>,
)

@Serializable
class EntityProbes(
    val name: String,
    val test: String = "",
    val testId: String = "", //TODO mb make it as required field
    val probes: List<Boolean>,
)

@Serializable
data class StopSessionPayload(
    val sessionId: String,
    val tests: List<TestInfo> = emptyList(),
)

@Serializable
data class AddTestsPayload(
    val sessionId: String,
    val tests: List<TestInfo> = emptyList(),
)


@Serializable
data class TestInfo(
    val id: String,
    val result: TestResult,
    val startedAt: Long,
    val finishedAt: Long,
    val details: TestDetails,
)

@Serializable
data class Label(
    val name: String,
    val value: String,
)

@Serializable
data class TestDetails @JvmOverloads constructor(
    val engine: String = "",
    val path: String = "",
    val testName: String,
    val params: Map<String, String> = emptyMap(),
    val metadata: Map<String, String> = emptyMap(),
    val labels: Set<Label> = emptySet(),
) : Comparable<TestDetails> {
    companion object {
        val emptyDetails = TestDetails(testName = DEFAULT_TEST_NAME)
    }

    override fun compareTo(other: TestDetails): Int {
        return toString().compareTo(other.toString())
    }

    override fun toString(): String {
        return "engine='$engine', path='$path', testName='$testName', params=$params"
    }
}

enum class TestResult {
    PASSED,
    FAILED,
    ERROR,
    SKIPPED,
    UNKNOWN
}

@Serializable
data class ActiveScopeChangePayload(
    val scopeName: String,
    val savePrevScope: Boolean = false,
    val prevScopeEnabled: Boolean = true,
    val forceFinish: Boolean = false,
)

@Serializable
data class RenameScopePayload(
    val scopeId: String,
    val scopeName: String,
)

@Serializable
data class ScopePayload(val scopeId: String = "")

@Serializable
data class FilterPayload(
    val name: String,
    val id: String = "",
    val attributes: List<TestOverviewFilter>,
    //now use only AND here:
    val attributesOp: BetweenOp = BetweenOp.AND,
    val buildVersion: String = "",//todo EPMDJ-8975 use the filter by buildVersion
)

@Serializable
data class ApplyPayload(
    val id: String,
    val buildVersion: String = "",//todo EPMDJ-8975 use the filter by buildVersion
)

@Serializable
data class DuplicatePayload(
    val id: String,
)

enum class BetweenOp { AND, OR }

const val PATH_DELIMITER = "->"

/**
 * @see TestOverview filter for this class.
 * @param fieldPath - name of the field. If it nested object then use the delimiterForWayToObject
 * Example: details->testName
 */
@Serializable
data class TestOverviewFilter(
    val fieldPath: String,
    val isLabel: Boolean = true,
    val values: List<FilterValue>,
    val valuesOp: BetweenOp = BetweenOp.OR,
)

@Serializable
data class FilterValue(
    val value: String,
    val op: FieldOp = FieldOp.EQ,
)

enum class FieldOp { EQ, }

@Serializable
data class DeleteFilterPayload(
    val id: String,
)

@Serializable
data class BuildPayload(val version: String)

@Serializable
data class BuildVersionDto(
    val version: String,
)

@Serializable
data class CoverMethod(
    val ownerClass: String,
    val name: String,
    val desc: String,
    val hash: String = "",
    val count: CountDto,
    val coverage: Double,
    val type: MethodType,
    val coverageRate: CoverageRate = CoverageRate.MISSED,
)

enum class CoverageRate {
    MISSED,
    PARTLY,
    FULL
}

enum class MethodType {
    NEW,
    MODIFIED,
    UNAFFECTED,
    DELETED,
}

@Serializable
data class RiskCounts(
    val new: Int = 0,
    val modified: Int = 0,
    val total: Int = 0,
)

@Serializable
data class TestTypeSummary(
    val type: String,
    val summary: TestSummary,
)

interface Coverage {
    val percentage: Double
    val count: Count
    val methodCount: Count
    val classCount: Count
    val packageCount: Count
    val riskCount: Count
    val testTypeOverlap: CoverDto
    val byTestType: List<TestTypeSummary>
}

@Serializable
data class ScopeCoverage(
    override val percentage: Double = 0.0,
    override val count: Count = zeroCount,
    val overlap: CoverDto = CoverDto.empty,
    override val methodCount: Count = zeroCount,
    override val classCount: Count = zeroCount,
    override val packageCount: Count = zeroCount,
    override val riskCount: Count = zeroCount,
    override val testTypeOverlap: CoverDto = CoverDto.empty,
    override val byTestType: List<TestTypeSummary> = emptyList(),
) : Coverage


@Serializable
data class BuildCoverage(
    override val percentage: Double,
    override val count: Count,
    override val methodCount: Count,
    override val classCount: Count,
    override val packageCount: Count,
    override val riskCount: Count = zeroCount,
    override val testTypeOverlap: CoverDto = CoverDto.empty,
    override val byTestType: List<TestTypeSummary> = emptyList(),
    val finishedScopesCount: Int = 0,
) : Coverage

enum class ArrowType {
    INCREASE,
    DECREASE,
    UNCHANGED
}

@Serializable
data class MethodsSummaryDto(
    val all: CountDto,
    val new: CountDto,
    val modified: CountDto,
    val unaffected: CountDto,
    val deleted: CountDto,
    val risks: RiskCounts = RiskCounts(),
)

@Serializable
data class MethodsInfo(
    val totalCount: Int = 0,
    val coveredCount: Int = 0,
    val methods: List<CoverMethod> = emptyList(),
)

@Serializable
data class PackageTree(
    val totalCount: Int = 0,
    val totalMethodCount: Int = 0,
    val totalClassCount: Int = 0,
    val packages: List<JavaPackageCoverage> = emptyList(),
)

@Serializable
data class JavaPackageCoverage(
    val id: String,
    val name: String,
    val totalClassesCount: Int = 0,
    val totalMethodsCount: Int = 0,
    val totalCount: Int = 0,
    val coverage: Double = 0.0,
    val coveredClassesCount: Int = 0,
    val coveredMethodsCount: Int = 0,
    val assocTestsCount: Int = 0,
    val classes: List<JavaClassCoverage> = emptyList(),
)

@Serializable
data class JavaClassCoverage(
    val id: String,
    val name: String,
    val path: String,
    val totalMethodsCount: Int = 0,
    val totalCount: Int = 0,
    val coverage: Double = 0.0,
    val coveredMethodsCount: Int = 0,
    val assocTestsCount: Int = 0,
    val methods: List<JavaMethodCoverage> = emptyList(),
    val probes: List<Boolean> = emptyList(),
)

@Serializable
data class JavaMethodCoverage(
    val id: String,
    val name: String,
    val desc: String,
    val decl: String,
    val probesCount: Int,
    val coverage: Double = 0.0,
    val assocTestsCount: Int = 0,
    val probeRange: ProbeRange = ProbeRange.EMPTY,
)

@Serializable
data class ProbeRange(val first: Int, val last: Int) {
    companion object {
        val EMPTY = ProbeRange(1, 0)
    }
}

@Serializable
data class AssociatedTests(
    val id: String,
    val packageName: String = "",
    val className: String = "",
    val methodName: String = "",
    val tests: List<TypedTest> = emptyList(),
) {
    override fun equals(other: Any?) = other is AssociatedTests && id == other.id && tests.containsAll(other.tests)
    override fun hashCode() = id.hashCode()
}

@Serializable
data class TypedTest(
    val type: String,
    val details: TestDetails = TestDetails.emptyDetails,
)

@Serializable
data class CoveredMethodCounts(
    val all: Int,
    val new: Int,
    val modified: Int,
    val unaffected: Int,
)

@Serializable
data class CoverageByTests(
    val all: TestSummary,
    val byType: List<TestTypeSummary>,
)

@Serializable
data class TestedMethodsSummary(
    val id: String,
    val details: TestDetails,
    val testType: String,
    val methodCounts: CoveredMethodCounts,
)

@Serializable
data class TestCoverageDto(
    val id: String,
    val type: String,
    val toRun: Boolean = false,
    val coverage: CoverDto = CoverDto.empty,
    val overview: TestOverview = TestOverview.empty,
)

@Serializable
data class TestsSummaryDto(
    val agentId: String,
    val tests: List<TestCoverageDto> = emptyList(),
    val totalCount: Int = 0,
)

@Serializable
data class TestOverview(
    val testId: String,
    val duration: Long = 0,
    val result: TestResult = TestResult.PASSED,
    val details: TestDetails = TestDetails.emptyDetails,
) {
    companion object {
        val empty = TestOverview("")
    }
}

@Serializable
data class TestData(
    val id: String,
    val details: TestDetails = TestDetails.emptyDetails,
)

@Serializable
data class ActiveSessions(
    val count: Int,
    val testTypes: Set<String> = emptySet(),
)

@Serializable
data class ActiveSessionDto(
    val id: String,
    val agentId: String,
    val testType: String,
    val isGlobal: Boolean,
    val isRealtime: Boolean,
)

@Serializable
data class ScopeSummary(
    val name: String,
    val id: String,
    val started: Long,
    val finished: Long = 0L,
    var enabled: Boolean = true,
    val active: Boolean = true,
    val sessionsFinished: Int = 0,
    val coverage: ScopeCoverage = ScopeCoverage(),
)

val zeroCount = Count(covered = 0, total = 0)

val zeroCountDto = Count(covered = 0, total = 0).toDto()

@Serializable
data class TestCountDto(
    val count: Int = 0,
    val byType: Map<String, Int> = emptyMap(),
)

@Serializable
data class TestSummary(
    val coverage: CoverDto,
    val testCount: Int = 0,
    val duration: Long = 0L,
)

@Serializable
data class TestsToRunSummaryDto(
    val buildVersion: String,
    val stats: TestsToRunStatsDto,
    val statsByType: Map<String, TestsToRunStatsDto> = emptyMap(),
)

@Serializable
data class TestsToRunStatsDto(
    val total: Int,
    val completed: Int,
    val duration: Long,
    val parentDuration: Long,
)

@Serializable
data class RiskDto(
    val id: String,
    val ownerClass: String,
    val name: String,
    val desc: String,
    val type: RiskType,
    val count: CountDto,
    val coverage: Double = 0.0,
    val previousCovered: RiskStatDto?,
    val coverageRate: CoverageRate = CoverageRate.MISSED,
    val assocTestsCount: Int = 0,
)

@Serializable
data class RiskStatDto(
    val buildVersion: String,
    val coverage: Double = 0.0,
)

enum class RiskStatus {
    COVERED,
    NOT_COVERED
}

enum class RiskType {
    NEW, MODIFIED
}

typealias GroupedTests = Map<String, List<TestData>>

@Serializable
data class GroupedTestsDto(
    val byType: GroupedTests = emptyMap(),
    val totalCount: Int = 0,
)

@Serializable
data class BuildStatsDto(
    val parentVersion: String = "",
    val total: Int = 0,
    val new: Int = 0,
    val modified: Int = 0,
    val unaffected: Int = 0,
    val deleted: Int = 0,
)

@Serializable
data class SummaryDto(
    val coverage: Double = 0.0,
    val coverageCount: Count = zeroCount,
    val methodCount: Count = zeroCount,
    val scopeCount: Int = 0,
    val arrow: ArrowType = ArrowType.UNCHANGED,
    val risks: Int = 0, //TODO remove after changes on frontend
    val riskCounts: RiskCounts = RiskCounts(),
    val testDuration: Long,
    val tests: List<TestTypeSummary> = emptyList(),
    val testsToRun: TestCountDto = TestCountDto(),
    val recommendations: Set<String> = emptySet(),
)

@Serializable
data class AgentSummaryDto(
    val id: String,
    val name: String,
    val buildVersion: String,
    val summary: SummaryDto,
)

@Serializable
data class ServiceGroupSummaryDto(
    val name: String,
    val aggregated: SummaryDto,
    val summaries: List<AgentSummaryDto> = emptyList(),
)

@Serializable
data class CoverDto(
    val percentage: Double = 0.0,
    val methodCount: CountDto = zeroCountDto,
    val count: CountDto = zeroCountDto,
) {
    companion object {
        val empty = CoverDto()
    }
}

@Serializable
data class CountDto(
    val covered: Int,
    val total: Int,
)

fun Count.toDto() = CountDto(covered, total)

typealias Count = Long

val Count.covered
    get() = (this shr 32).toInt()

val Count.total
    get() = this.toInt()


fun Count.copy(covered: Int = (this shr 32).toInt(), total: Int = this.toInt()): Count {
    return Count(covered, total)
}

fun Count(covered: Int, total: Int): Count {
    return (covered.toLong() shl 32) or (total.toLong() and 0xFFFFFFFFL)
}

