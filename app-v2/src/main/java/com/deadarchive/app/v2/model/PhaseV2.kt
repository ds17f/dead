package com.deadarchive.app.v2.model

/**
 * V2 database initialization phases for progress tracking
 */
enum class PhaseV2 {
    IDLE,
    CHECKING,
    EXTRACTING,
    IMPORTING_SHOWS,
    COMPUTING_VENUES,
    COMPLETED,
    ERROR
}