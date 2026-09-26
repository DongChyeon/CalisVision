package com.calisvision.domain.model

data class AnalysisResult(
    val videoUri: String,
    val sampledFrames: Int,
    val detectedFrames: Int,
)
