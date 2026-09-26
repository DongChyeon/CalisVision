package com.calisvision.domain.knowledge

import com.calisvision.domain.model.BodySide
import com.calisvision.domain.model.FramePose
import com.calisvision.domain.model.Joint
import com.calisvision.domain.rules.AngleThreshold
import com.calisvision.domain.rules.Exercise
import com.calisvision.domain.rules.PoseFault
import com.calisvision.domain.rules.PoseRule
import com.calisvision.domain.rules.RuleId
import com.calisvision.domain.rules.ShootingGuide
import com.calisvision.domain.rules.Violation

object HandstandKnowledge {
    val ALIGNMENT = RuleId("handstand.alignment")
    val HIP = RuleId("handstand.hip")
    val SHOULDER_OPEN = RuleId("handstand.shoulder_open")
    val ELBOW_LOCK = RuleId("handstand.elbow_lock")

    val BANANA = PoseFault(
        id = "banana_back",
        name = "바나나 등",
        description = "허리가 과하게 꺾여 몸이 활처럼 휘어 있습니다. 허리에 부담이 가고 균형이 불안정해집니다.",
        correctionHint = "갈비뼈를 닫고 엉덩이를 조여 골반을 후방경사시키세요. 복부에 힘을 주어 몸을 일직선으로 만드세요.",
    )
    val PIKE = PoseFault(
        id = "pike",
        name = "파이크",
        description = "엉덩이가 접혀 상체와 하체가 일직선에서 벗어나 있습니다. 무게중심이 흔들려 오래 버티기 어렵습니다.",
        correctionHint = "엉덩이를 앞으로 밀어 펴고 다리를 천장 방향으로 곧게 뻗으세요. 둔근을 조여 고관절을 완전히 펴세요.",
    )
    val ANTERIOR_TILT = PoseFault(
        id = "anterior_pelvic_tilt",
        name = "골반 전방경사",
        description = "골반이 앞으로 기울어 허리가 젖혀져 있습니다. 요추에 압박이 집중됩니다.",
        correctionHint = "꼬리뼈를 말아 넣듯 골반을 후방경사시키고, 복부와 엉덩이를 동시에 조이세요.",
    )
    val CLOSED_SHOULDER = PoseFault(
        id = "closed_shoulder",
        name = "어깨 닫힘",
        description = "팔이 귀 옆까지 올라가지 않아 어깨가 닫혀 있습니다. 몸이 비스듬해지고 손목 부담이 커집니다.",
        correctionHint = "바닥을 밀어내며 어깨를 귀 쪽으로 끌어올리세요(견갑 거상). 팔과 몸통이 일직선이 되도록 가슴을 손 쪽으로 보내세요.",
    )
    val BENT_ELBOW = PoseFault(
        id = "bent_elbow",
        name = "팔꿈치 굽힘",
        description = "팔꿈치가 굽어 있어 팔로 체중을 지지하는 힘이 새고 버티기 어렵습니다.",
        correctionHint = "팔꿈치를 완전히 잠그고, 삼두근에 힘을 주어 바닥을 강하게 밀어내세요.",
    )

    val rules = listOf(
        PoseRule(
            id = ALIGNMENT,
            name = "전신 정렬",
            joints = listOf(Joint.WRIST, Joint.HIP, Joint.ANKLE),
            threshold = AngleThreshold.Deviation(maxExtensionDeg = 10f, maxFlexionDeg = 10f),
            faults = mapOf(Violation.EXTENSION to BANANA, Violation.FLEXION to PIKE),
        ),
        PoseRule(
            id = HIP,
            name = "골반/허리",
            joints = listOf(Joint.SHOULDER, Joint.HIP, Joint.KNEE),
            threshold = AngleThreshold.Deviation(maxExtensionDeg = 15f, maxFlexionDeg = 15f),
            faults = mapOf(Violation.EXTENSION to ANTERIOR_TILT, Violation.FLEXION to PIKE),
        ),
        PoseRule(
            id = SHOULDER_OPEN,
            name = "어깨 열림",
            joints = listOf(Joint.ELBOW, Joint.SHOULDER, Joint.HIP),
            threshold = AngleThreshold.Range(min = 165f, max = 180f),
            faults = mapOf(Violation.BELOW to CLOSED_SHOULDER),
        ),
        PoseRule(
            id = ELBOW_LOCK,
            name = "팔꿈치 펴짐",
            joints = listOf(Joint.WRIST, Joint.ELBOW, Joint.SHOULDER),
            threshold = AngleThreshold.Range(min = 170f, max = 180f),
            faults = mapOf(Violation.BELOW to BENT_ELBOW),
        ),
    )

    /** Hands below feet in original image coordinates (y down), using visible landmarks of both sides. */
    val orientationPrior: (FramePose) -> Boolean = { pose ->
        val wristY = meanY(pose, Joint.WRIST)
        val ankleY = meanY(pose, Joint.ANKLE)
        wristY != null && ankleY != null && wristY > ankleY
    }

    val exercise = Exercise(
        id = "handstand",
        name = "물구나무",
        rules = rules,
        keyJoints = listOf(Joint.WRIST, Joint.ELBOW, Joint.SHOULDER, Joint.HIP, Joint.KNEE, Joint.ANKLE),
        orientationPrior = orientationPrior,
        shootingGuide = ShootingGuide(
            view = "측면",
            faultsCovered = listOf(BANANA.name, PIKE.name, ANTERIOR_TILT.name, CLOSED_SHOULDER.name, BENT_ELBOW.name),
            instruction = "몸의 옆면이 보이도록 카메라를 두세요. 삼각대를 엉덩이 높이에 놓고 2–3m 떨어져, 손끝부터 발끝까지 전신이 프레임 안에 들어오게 촬영하세요.",
        ),
    )

    private fun meanY(pose: FramePose, joint: Joint): Float? {
        val points = BodySide.entries.mapNotNull { pose[it, joint] }
        if (points.isEmpty()) return null
        val visible = points.filter { it.visibility >= 0.5f }.ifEmpty { points }
        return visible.map { it.y }.average().toFloat()
    }
}
