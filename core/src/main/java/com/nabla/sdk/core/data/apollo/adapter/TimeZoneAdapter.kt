package com.nabla.sdk.core.data.apollo.adapter

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.nabla.sdk.core.annotation.NablaInternal
import kotlinx.datetime.TimeZone

@NablaInternal
public val timeZoneAdapter: Adapter<TimeZone> = object : Adapter<TimeZone> {
    override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): TimeZone {
        return TimeZone.of(reader.nextString()!!)
    }

    override fun toJson(
        writer: JsonWriter,
        customScalarAdapters: CustomScalarAdapters,
        value: TimeZone,
    ) {
        writer.value(value.id)
    }
}
