package com.narendar.hub.ai

/**
 * First offline StudyPilot tutor layer: typo-tolerant topic matching and curated explanations.
 * This is NOT the Python Transformer checkpoint and is not a general-purpose generative model.
 */
object StudyPilotAIEngine {
    data class Answer(val topic: String, val text: String, val confidence: Double, val needsClarification: Boolean = false)
    private data class Concept(val title: String, val keywords: List<String>, val answer: String)

    private val concepts = listOf(
        Concept("Photosynthesis", listOf("photosynthesis","photosynthsis","photosinthesis","fotosynthesis","fotosinthesis","photsynthesis","plant food","plants make food","plants food kaise banate hain"), "Photosynthesis is the process by which green plants use light energy to make sugars from carbon dioxide and water. Oxygen is released during this process. In simple words, plants use sunlight, water, and carbon dioxide to make food."),
        Concept("Water cycle", listOf("water cycle","evaporation","condensation","precipitation"), "The water cycle is the continuous movement of water between Earth's surface and atmosphere. Its main stages are evaporation, condensation, precipitation, and collection."),
        Concept("Gravity", listOf("gravity","gravty","gravitiy","force pulls down"), "Gravity is the attractive force between objects that have mass. Earth's gravity pulls objects toward the ground and keeps the Moon in orbit around Earth."),
        Concept("Fractions", listOf("fraction","fractions","fracion","frakshan","numerator","denominator"), "A fraction represents part of a whole. The numerator is the top number and the denominator is the bottom number. For example, 3/4 means three of four equal parts."),
        Concept("Percentage", listOf("percentage","percent","persent","%"), "Percent means out of one hundred. For example, 25% means 25 out of 100, equal to 1/4 or 0.25."),
        Concept("Computer algorithm", listOf("algorithm","algoritm","algorythm","steps to solve problem"), "An algorithm is a clear sequence of steps used to solve a problem or complete a task. A recipe is a simple everyday example."),
        Concept("Artificial intelligence", listOf("artificial intelligence","ai","artifical intelligence","artificial inteligence"), "Artificial intelligence (AI) is a field of computing that builds systems able to perform tasks such as recognizing patterns, understanding language, or making predictions. AI systems can still make mistakes."),
        Concept("Renewable energy", listOf("renewable energy","renewble energy","solar energy","wind energy"), "Renewable energy comes from sources naturally replenished, such as sunlight, wind, and flowing water."),
        Concept("Cell", listOf("cell","sel","living unit"), "A cell is the basic structural and functional unit of living organisms. Some organisms consist of one cell, while plants and animals consist of many cells."),
        Concept("Water states", listOf("states of matter","solid liquid gas","water freezes","boiling point"), "The three common states of matter are solid, liquid, and gas. Water freezes at about 0°C and boils at about 100°C at standard atmospheric pressure."),
        Concept("Speed", listOf("speed","speeed","distance divided by time"), "Average speed equals total distance divided by total time. For example, travelling 100 kilometres in 2 hours gives an average speed of 50 kilometres per hour."),
        Concept("Education", listOf("importance of education","education","role of education"), "Education builds knowledge, practical skills, critical thinking, and opportunities for individuals and communities.")
    )

    fun ask(message: String, subject: String? = null, classLevel: Int? = null): Answer {
        val normalized = normalize(message)
        if (normalized.isBlank()) return Answer("Clarification", "Type a question or topic and I'll try to help.", 1.0, true)
        val ranked = concepts.map { concept ->
            val score = concept.keywords.maxOf { keyword -> similarity(normalized, normalizeBasic(keyword)) }
            concept to score
        }.sortedByDescending { it.second }
        val best = ranked.firstOrNull()
        if (best == null || best.second < 0.48) return Answer("Not matched yet", "I couldn't confidently match that to my current offline study topics. Try adding the subject or rephrasing the question. I won't pretend I know an answer when I can't support it.", best?.second ?: 0.0, true)
        val prefix = when {
            normalized.contains("why") || normalized.contains("kyun") -> "Why it matters: "
            normalized.contains("example") || normalized.contains("misal") -> "Example and explanation: "
            normalized.contains("simple") || normalized.contains("easy") || normalized.contains("asaan") -> "In simple words: "
            else -> best.first.title + ": "
        }
        val subjectNote = if (!subject.isNullOrBlank()) "Subject: $subject. " else ""
        val gradeNote = if (classLevel != null && classLevel in 1..12) " (aimed around Class $classLevel)" else ""
        return Answer(best.first.title, subjectNote + prefix + best.first.answer + gradeNote, best.second.coerceIn(0.0,1.0))
    }

    private fun normalizeBasic(input: String) = input.lowercase().replace(Regex("[^a-z0-9%\\s]"), " ").replace(Regex("\\s+"), " ").trim()
    private fun normalize(input: String): String {
        var s = normalizeBasic(input)
        val replacements = listOf("plants food kaise banate hain" to "plants make food", "plants khana kaise banate hain" to "plants make food", "kya hota hai" to "what is", "kya hota" to "what is", "kya hai" to "what is", "kya h" to "what is", "samjhao" to "explain", "batao" to "explain", "kyun" to "why", "kaise" to "how", "kese" to "how")
        replacements.forEach { (from,to) -> s = s.replace(from,to) }
        return s.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            if (word.length < 4 || word.all(Char::isDigit)) word
            else {
                val all = concepts.flatMap { it.keywords }.distinct()
                val candidate = all.filter { it.length in (word.length-2)..(word.length+2) && !it.contains(" ") }
                    .minByOrNull { editDistance(word,it) }
                    ?.takeIf { editDistance(word,it) <= maxOf(1,word.length/4) }
                candidate ?: word
            }
        }
    }
    private fun similarity(a: String,b: String): Double {
        if (a == b) return 1.0
        if (a.contains(b) || b.contains(a)) return 0.88
        val aw=a.split(" ").filter { it.isNotBlank() }.toSet(); val bw=b.split(" ").filter { it.isNotBlank() }.toSet()
        val overlap=aw.intersect(bw).size.toDouble()/(aw union bw).size.coerceAtLeast(1)
        val edit=editDistance(a,b).toDouble()/maxOf(a.length,b.length,1)
        return maxOf(overlap,1.0-edit)
    }
    private fun editDistance(a:String,b:String):Int {
        var prev=IntArray(b.length+1){it}
        for(i in a.indices){val cur=IntArray(b.length+1);cur[0]=i+1
            for(j in b.indices) cur[j+1]=minOf(cur[j]+1,prev[j+1]+1,prev[j]+if(a[i]==b[j])0 else 1)
            prev=cur
        }
        return prev[b.length]
    }
}
