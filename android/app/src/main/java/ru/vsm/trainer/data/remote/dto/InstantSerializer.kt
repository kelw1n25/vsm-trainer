package ru.vsm.trainer.data.remote.dto

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Даты backend: `2026-09-26T08:01:34.563204Z`. Микросекунды сохраняются и при записи в кэш. */
object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant = OffsetDateTime.parse(decoder.decodeString()).toInstant()

    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(DateTimeFormatter.ISO_INSTANT.format(value))
}
