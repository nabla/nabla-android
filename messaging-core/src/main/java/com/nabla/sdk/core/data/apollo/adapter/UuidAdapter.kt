package com.nabla.sdk.core.data.apollo.adapter

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.benasher44.uuid.Uuid

val uuidAdapter = object : Adapter<Uuid> {
    override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Uuid {
        return Uuid.fromString(reader.nextString())
    }

    override fun toJson(
        writer: JsonWriter,
        customScalarAdapters: CustomScalarAdapters,
        value: Uuid
    ) {
        writer.value(value.toString())
    }
}
