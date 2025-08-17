package com.deadarchive.v2.app.model

/**
 * V2 database initialization phases for progress tracking
 */
enum class PhaseV2 {
    IDLE,
    CHECKING,
    USING_LOCAL,
    DOWNLOADING,
    EXTRACTING,
    IMPORTING_SHOWS,
    COMPUTING_VENUES,
    IMPORTING_RECORDINGS,
    COMPLETED,
    ERROR
}