package com.example.schoolproject.data.chat

/**
 * PROVISIONAL PARAMETERS - replace before presenting results.
 *
 * Coefficients are the values printed by the CAT_2 notebook (logistic regression, C = 1,
 * classes High / Low / Medium). The scaler mean/scale and the intercepts were not available,
 * so they are assumptions (Likert 1-5 centred on 3, scale 1.2, zero intercepts).
 *
 * Run the export cell in stress_model_export.py in the notebook and paste the generated
 * StressModelData.kt over this file. That sets isPlaceholder = false.
 */
object StressModelData {
    val params = StressModelParams(
        classes = listOf("High", "Low", "Medium"),
        scalerMean = listOf(3.0, 3.0, 3.0),
        scalerScale = listOf(1.2, 1.2, 1.2),
        categories = listOf(
            listOf("Noisy", "Peaceful", "disrupted"),
            listOf(
                "Analyze the situation and handle it with intellect",
                "Emotional breakdown (crying a lot)",
                "Social support (friends, family)",
            ),
            listOf("No", "Yes", "prefer not to say"),
        ),
        featureNames = listOf(
            "Peer_Pressure", "Academic_Pressure_Home", "Academic_Competition",
            "Study_Environment_Noisy", "Study_Environment_Peaceful", "Study_Environment_disrupted",
            "Coping_Strategy_Analyze the situation and handle it with intellect",
            "Coping_Strategy_Emotional breakdown (crying a lot)",
            "Coping_Strategy_Social support (friends, family)",
            "Bad_Habits_No", "Bad_Habits_Yes", "Bad_Habits_prefer not to say",
        ),
        coef = listOf(
            // High
            listOf(0.389, 0.376, 0.175, 0.286, -0.424, 0.137, -0.466, 0.050, 0.415, 0.017, 0.166, -0.185),
            // Low
            listOf(-0.861, -0.394, -0.304, -0.020, -0.323, 0.342, 0.826, -0.218, -0.609, 0.064, -0.015, -0.049),
            // Medium
            listOf(0.472, 0.017, 0.129, -0.266, 0.747, -0.478, -0.360, 0.168, 0.193, -0.081, -0.151, 0.234),
        ),
        intercept = listOf(0.0, 0.0, 0.0),
        isPlaceholder = true,
    )
}
