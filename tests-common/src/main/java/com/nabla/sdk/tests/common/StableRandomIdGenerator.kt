package com.nabla.sdk.tests.common

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.boundary.UuidGenerator
import okreplay.TapeMode
import java.io.File

class StableRandomIdGenerator(private val idFile: File, private val tapeMode: TapeMode) : UuidGenerator {
    private var readUuids = mutableListOf<Uuid>()

    init {
        when (tapeMode) {
            TapeMode.WRITE_ONLY -> {
                // Clear saved uuids in record mode
                idFile.delete()
            }
            TapeMode.READ_ONLY -> {
                // Load saved uuids in replay mode
                readUuids = idFile.readLines().map { Uuid.fromString(it) }.toMutableList()
            }
            else -> throw IllegalStateException("Unsupported tape mode")
        }
    }

    override fun generate(): Uuid {
        return when (tapeMode) {
            TapeMode.WRITE_ONLY -> {
                val uuid = Uuid.randomUUID()
                // Save uuid in record mode
                idFile.apply {
                    appendText(uuid.toString())
                    appendText("\n")
                }
                uuid
            }
            TapeMode.READ_ONLY -> {
                // Read uuid in replay mode
                readUuids.removeAt(0)
            }
            else -> throw IllegalStateException("Unsupported tape mode")
        }
    }
}
