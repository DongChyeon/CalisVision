package com.calisvision.domain.model

object PoseLandmark {
    const val COUNT = 33
    const val NOSE = 0
    const val LEFT_EAR = 7
    const val RIGHT_EAR = 8
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
    const val LEFT_HEEL = 29
    const val RIGHT_HEEL = 30
    const val LEFT_FOOT_INDEX = 31
    const val RIGHT_FOOT_INDEX = 32
}

enum class Joint(val leftIndex: Int, val rightIndex: Int) {
    SHOULDER(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER),
    ELBOW(PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW),
    WRIST(PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST),
    HIP(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP),
    KNEE(PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE),
    ANKLE(PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE),
    HEEL(PoseLandmark.LEFT_HEEL, PoseLandmark.RIGHT_HEEL),
    FOOT_INDEX(PoseLandmark.LEFT_FOOT_INDEX, PoseLandmark.RIGHT_FOOT_INDEX),
}

enum class BodySide {
    LEFT,
    RIGHT;

    fun index(joint: Joint): Int = if (this == LEFT) joint.leftIndex else joint.rightIndex
}
