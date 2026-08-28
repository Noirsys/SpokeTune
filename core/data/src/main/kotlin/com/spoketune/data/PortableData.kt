package com.spoketune.data

import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class SpokeTuneExport(val schemaVersion: Int = 1, val wheels: List<WheelRecord>, val sessions: List<SessionRecord>, val measurements: List<MeasurementRecord>)
@Serializable
data class WheelRecord(val id: String, val name: String, val spokeCount: Int, val archived: Boolean = false, val notes: String? = null, val createdAt: String, val updatedAt: String)
@Serializable
data class SessionRecord(val id: String, val wheelId: String, val state: SessionState = SessionState.DRAFT, val startedAt: String, val completedAt: String? = null)
@Serializable enum class SessionState { DRAFT, COMPLETE, ABANDONED }
@Serializable
data class MeasurementRecord(val id: String, val sessionId: String, val spokeNumber: Int, val side: WheelSide, val frequencyHz: Double, val confidence: Double, val capturedAt: String, val supersededBy: String? = null)
@Serializable enum class WheelSide { LEFT, RIGHT }

class DataValidationException(message: String) : IllegalArgumentException(message)

object PortableData {
    const val CURRENT_SCHEMA = 1
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false; explicitNulls = true; allowSpecialFloatingPointValues = false }
    fun encode(data: SpokeTuneExport): String { validate(data); return json.encodeToString(SpokeTuneExport.serializer(), data) }
    fun decode(payload: String): SpokeTuneExport = try {
        require(payload.length <= 50_000_000) { "Import exceeds 50 MB" }
        json.decodeFromString(SpokeTuneExport.serializer(), payload).also(::validate)
    } catch (e: SerializationException) { throw DataValidationException("Invalid SpokeTune export: ${e.message}") }
    fun validate(data: SpokeTuneExport) {
        require(data.schemaVersion == CURRENT_SCHEMA) { "Unsupported schema version ${data.schemaVersion}" }
        require(data.wheels.size <= 10_000 && data.sessions.size <= 50_000 && data.measurements.size <= 2_500_000) { "Import exceeds resource limits" }
        val wheelIds = data.wheels.map { it.id }; require(wheelIds.size == wheelIds.toSet().size) { "Duplicate wheel ID" }
        data.wheels.forEach { require(it.id.isNotBlank() && it.name.isNotBlank() && it.name.length <= 200) { "Invalid wheel" }; require(it.spokeCount in 12..48 && it.spokeCount % 2 == 0) { "Spoke count must be even and 12..48" }; require(it.notes == null || it.notes.length <= 4_000) { "Notes too long" }; Instant.parse(it.createdAt); Instant.parse(it.updatedAt) }
        val sessionIds = data.sessions.map { it.id }; require(sessionIds.all(String::isNotBlank) && sessionIds.size == sessionIds.toSet().size) { "Invalid or duplicate session ID" }; data.sessions.forEach { require(it.wheelId in wheelIds) { "Session references missing wheel" }; Instant.parse(it.startedAt); it.completedAt?.let(Instant::parse) }
        val measurementIds = data.measurements.map { it.id }; require(measurementIds.size == measurementIds.toSet().size) { "Duplicate measurement ID" }; val sessions = data.sessions.associateBy { it.id }; val wheels = data.wheels.associateBy { it.id }
        require(measurementIds.all(String::isNotBlank)) { "Blank measurement ID" }
        data.measurements.forEach { m -> val wheel = wheels[sessions[m.sessionId]?.wheelId] ?: throw DataValidationException("Measurement references missing session"); require(m.spokeNumber in 1..wheel.spokeCount) { "Spoke number out of range" }; require(m.side == if (m.spokeNumber % 2 == 1) WheelSide.LEFT else WheelSide.RIGHT) { "Measurement side does not match spoke assignment" }; require(m.frequencyHz.isFinite() && m.frequencyHz in 1.0..20_000.0) { "Invalid frequency" }; require(m.confidence.isFinite() && m.confidence in 0.0..1.0) { "Invalid confidence" }; Instant.parse(m.capturedAt); require(m.supersededBy == null || m.supersededBy in measurementIds && m.supersededBy != m.id) { "Broken supersession link" } }
    }
}

class InMemorySpokeTuneRepository(initial: SpokeTuneExport = SpokeTuneExport(1, emptyList(), emptyList(), emptyList())) {
    private val measurements = LinkedHashMap<String, MeasurementRecord>(); private var base = initial.also { PortableData.validate(it) }; init { initial.measurements.forEach { measurements[it.id] = it } }
    fun export() = base.copy(measurements = measurements.values.toList())
    fun import(data: SpokeTuneExport, replace: Boolean = false) { PortableData.validate(data); val next = if (replace) data else { if (data.wheels.any { w -> base.wheels.any { it.id == w.id } }) throw DataValidationException("Duplicate wheel ID"); if (data.sessions.any { s -> base.sessions.any { it.id == s.id } }) throw DataValidationException("Duplicate session ID"); if (data.measurements.any { measurements.containsKey(it.id) }) throw DataValidationException("Duplicate measurement ID"); export().copy(wheels = base.wheels + data.wheels, sessions = base.sessions + data.sessions, measurements = measurements.values + data.measurements) }; PortableData.validate(next); base = next.copy(measurements = emptyList()); measurements.clear(); next.measurements.forEach { measurements[it.id] = it } }
    fun appendMeasurement(measurement: MeasurementRecord) { require(!measurements.containsKey(measurement.id)) { "Measurement IDs are immutable" }; PortableData.validate(export().copy(measurements = export().measurements + measurement)); measurements[measurement.id] = measurement }
}
