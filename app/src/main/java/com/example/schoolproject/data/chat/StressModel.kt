package com.example.schoolproject.data.chat


import kotlin.math.exp

/**
 * Parameters of the logistic-regression stress classifier trained in the CAT_2 notebook
 * (Peer pressure, Academic pressure from home, Academic competition + Study environment,
 * Coping strategy, Bad habits -> Stress level Low / Medium / High).
 *
 * Regenerate StressModelData.kt from the notebook with the export cell in
 * stress_model_export.py whenever the model is retrained.
 */
data class StressModelParams(
    val classes: List<String>,
    val scalerMean: List<Double>,
    val scalerScale: List<Double>,
    /** One list of category strings per categorical feature, in the order the model was trained. */
    val categories: List<List<String>>,
    /** Numeric feature names first, then one-hot names "Feature_Category". */
    val featureNames: List<String>,
    val coef: List<List<Double>>,
    val intercept: List<Double>,
    /** True while the values are not yet exported from a trained notebook run. */
    val isPlaceholder: Boolean = false,
)

data class StressResult(
    val level: String,
    val probabilities: Map<String, Double>,
    /** Features pushing towards the High class, strongest first (positive contributions only). */
    val drivers: List<String>,
)

/** On-device inference: a weighted sum per class followed by a softmax, exactly like the notebook. */
class StressModel(private val p: StressModelParams = StressModelData.params) {

    fun predict(numeric: List<Double>, categorical: List<String>): StressResult {
        require(numeric.size == p.scalerMean.size) { "Expected ${p.scalerMean.size} numeric answers" }
        require(categorical.size == p.categories.size) { "Expected ${p.categories.size} categorical answers" }

        val x = ArrayList<Double>(p.featureNames.size)
        numeric.forEachIndexed { i, v ->
            val scale = if (p.scalerScale[i] == 0.0) 1.0 else p.scalerScale[i]
            x += (v - p.scalerMean[i]) / scale
        }
        // Unknown categories become all zeros, like OneHotEncoder(handle_unknown="ignore").
        categorical.forEachIndexed { i, v ->
            p.categories[i].forEach { c -> x += if (c.equals(v, ignoreCase = true)) 1.0 else 0.0 }
        }
        check(x.size == p.featureNames.size && p.coef.all { it.size == x.size }) {
            "Model parameters do not match the feature layout"
        }

        val logits = DoubleArray(p.classes.size) { k ->
            p.intercept[k] + x.indices.sumOf { p.coef[k][it] * x[it] }
        }
        val maxLogit = logits.max()
        val exps = logits.map { exp(it - maxLogit) }
        val total = exps.sum()
        val probs = exps.map { it / total }
        val best = probs.indices.maxBy { probs[it] }

        val high = p.classes.indexOf("High").takeIf { it >= 0 } ?: best
        val drivers = x.indices
            .map { it to p.coef[high][it] * x[it] }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .map { p.featureNames[it.first] }

        return StressResult(p.classes[best], p.classes.zip(probs).toMap(), drivers)
    }
}
