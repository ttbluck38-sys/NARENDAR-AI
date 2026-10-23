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


    /**
     * Arabic-script query support for Urdu and Sindhi. This is a curated offline
     * knowledge layer, not a claim that the tiny Transformer understands these scripts.
     */
    private data class LocalizedConcept(val keys: List<String>, val title: String, val urdu: String, val sindhi: String)
    private val localized = listOf(
        LocalizedConcept(listOf("photosynthesis", "ضیائی تالیف", "ضیائي عمل", "ڦوٽو سنتھیسس", "فوٹوسنتھیسس"),
            "Photosynthesis",
            "ضیائی تالیف وہ عمل ہے جس میں سبز پودے سورج کی روشنی، پانی اور کاربن ڈائی آکسائیڈ کی مدد سے اپنی غذا بناتے ہیں۔ اس عمل میں آکسیجن خارج ہوتی ہے۔",
            "ڦوٽو سنتھیسس اهو عمل آهي جنهن ۾ ساوا ٻوٽا سج جي روشني، پاڻي ۽ ڪاربان ڊاءِ آڪسائيڊ جي مدد سان پنهنجي خوراڪ ٺاهين ٿا. هن عمل ۾ آڪسيجن خارج ٿئي ٿي۔"),
        LocalizedConcept(listOf("gravity", "کشش ثقل", "ڪشش ثقل", "ڪشش"),
            "Gravity",
            "کششِ ثقل وہ قوت ہے جو کمیت رکھنے والی چیزوں کو ایک دوسرے کی طرف کھینچتی ہے۔ زمین کی کشش چیزوں کو اپنے مرکز کی طرف کھینچتی ہے۔",
            "ڪشش ثقل اها قوت آهي جيڪا ماس رکندڙ شين کي هڪ ٻئي ڏانهن ڇڪي ٿي. زمين جي ڪشش شين کي پنهنجي مرڪز ڏانهن ڇڪي ٿي."),
        LocalizedConcept(listOf("water cycle", "آبی چکر", "آبي چڪر", "پاڻي جو چڪر", "آب جو چکر"),
            "Water cycle",
            "آبی چکر میں پانی بخارات بن کر اوپر جاتا ہے، ٹھنڈا ہو کر بادل بناتا ہے، پھر بارش یا برف کی صورت میں زمین پر واپس آتا ہے۔",
            "پاڻي جي چڪر ۾ پاڻي ٻاڦ بڻجي مٿي وڃي ٿو، ٿڌو ٿي ڪڪر ٺاهي ٿو، پوءِ مينهن يا برف جي صورت ۾ زمين تي موٽي اچي ٿو."),
        LocalizedConcept(listOf("fraction", "کسر", "ڀاڱو", "ڪسر"),
            "Fractions",
            "کسر کسی مکمل چیز کے برابر حصوں میں سے ایک یا زیادہ حصوں کو ظاہر کرتی ہے۔ مثال کے طور پر 3/4 کا مطلب چار برابر حصوں میں سے تین حصے ہیں۔",
            "ڀاڱو ڪنهن مڪمل شيءِ جي برابر حصن مان هڪ يا وڌيڪ حصا ڏيکاري ٿو. مثال طور 3/4 جو مطلب چار برابر حصن مان ٽي حصا آهي."),
        LocalizedConcept(listOf("percentage", "فیصد", "سيڪڙو"),
            "Percentage",
            "فیصد کا مطلب ہے ہر سو میں سے۔ مثال کے طور پر 25 فیصد، 100 میں سے 25 ہے اور 1/4 کے برابر ہے۔",
            "سيڪڙو جو مطلب هر سئو مان آهي. مثال طور 25 سيڪڙو، 100 مان 25 آهي ۽ 1/4 جي برابر آهي."),
        LocalizedConcept(listOf("artificial intelligence", "مصنوعی ذہانت", "مصنوعي ذهانت", "اي آءِ"),
            "Artificial intelligence",
            "مصنوعی ذہانت کمپیوٹر سائنس کا شعبہ ہے جس میں ایسے نظام بنائے جاتے ہیں جو زبان سمجھنے، نمونے پہچاننے اور پیش گوئی جیسے کام کرتے ہیں۔ AI غلطی بھی کر سکتی ہے۔",
            "مصنوعي ذهانت ڪمپيوٽر سائنس جو شعبو آهي، جنهن ۾ اهڙا نظام ٺاهيا وڃن ٿا جيڪي ٻولي سمجهڻ، نمونا سڃاڻڻ ۽ اڳڪٿي ڪرڻ جهڙا ڪم ڪن ٿا. AI کان غلطي به ٿي سگهي ٿي."),
        LocalizedConcept(listOf("cell", "خلیہ", "خلية", "سيل"),
            "Cell",
            "خلیہ جانداروں کی بنیادی ساختی اور فعلی اکائی ہے۔ کچھ جاندار ایک خلیے پر مشتمل ہوتے ہیں جبکہ پودے اور جانور بہت سے خلیوں سے بنتے ہیں۔",
            "سيل جاندارن جي بنيادي بناوتي ۽ فعلي ايڪائي آهي. ڪجهه جاندار هڪ سيل جا هوندا آهن، جڏهن ته ٻوٽا ۽ جانور ڪيترن سيلن مان ٺهن ٿا."),
        LocalizedConcept(listOf("renewable energy", "قابل تجدید توانائی", "قابلِ تجديد توانائي", "ٻيهر پيدا ٿيندڙ توانائي"),
            "Renewable energy",
            "قابلِ تجدید توانائی ایسے ذرائع سے حاصل ہوتی ہے جو قدرتی طور پر دوبارہ دستیاب ہوتے رہتے ہیں، جیسے سورج کی روشنی، ہوا اور بہتا پانی۔",
            "قابلِ تجديد توانائي اهڙن ذريعن مان ملي ٿي جيڪي قدرتي طور ٻيهر موجود ٿيندا رهن ٿا، جيئن سج جي روشني، هوا ۽ وهندڙ پاڻي."),
        LocalizedConcept(listOf("algorithm", "الگورتھم", "الگورٿم"),
            "Algorithm",
            "الگورتھم کسی مسئلے کو حل کرنے یا کام مکمل کرنے کے لیے واضح اور ترتیب وار ہدایات کا مجموعہ ہے۔",
            "الگورٿم ڪنهن مسئلي کي حل ڪرڻ يا ڪم مڪمل ڪرڻ لاءِ واضح ۽ ترتيبوار هدايتن جو مجموعو آهي."),
        LocalizedConcept(listOf("speed", "رفتار", "رفتار"),
            "Speed",
            "اوسط رفتار معلوم کرنے کے لیے کل فاصلہ کو کل وقت پر تقسیم کریں۔ مثال: 100 کلومیٹر فاصلہ 2 گھنٹے میں طے ہو تو رفتار 50 کلومیٹر فی گھنٹہ ہے۔",
            "سراسري رفتار معلوم ڪرڻ لاءِ ڪل فاصلي کي ڪل وقت سان ورهايو. مثال: 100 ڪلوميٽر فاصلو 2 ڪلاڪن ۾ طئي ٿئي ته رفتار 50 ڪلوميٽر في ڪلاڪ آهي.")
    )

    private fun localizedAnswer(message: String, classLevel: Int?): Answer? {
        val raw = message.lowercase().replace('ي', 'ی').replace('ڪ', 'ک')
        val hasArabic = raw.any { it in '\u0600'..'\u06FF' }
        if (!hasArabic) return null
        val sindhiMarkers = listOf('ٻ','ڄ','ڙ','ڏ','ڳ','ڱ','ڻ','ٽ','ٿ','ڀ','ڦ','ڪ','گ')
        val sindhi = raw.any { it in sindhiMarkers } || raw.contains("سنڌي") || raw.contains("سنڌ")
        val match = localized.firstOrNull { concept -> concept.keys.any { key -> raw.contains(key.lowercase()) } }
        if (match == null) return Answer("مزید معلومات درکار / وڌيڪ ڄاڻ گهربل",
            if (sindhi) "هن وقت مون کي هن سوال لاءِ پڪ وارو سنڌي جواب موجود ناهي. مهرباني ڪري موضوع جو نالو انگريزي يا سنڌي ۾ وڌيڪ واضح لکو." else "اس سوال کے لیے میرے موجودہ آف لائن ذخیرے میں قابلِ اعتماد اردو جواب نہیں ملا۔ براہِ کرم موضوع کا نام واضح لکھیں۔",
            0.2, true)
        val gradeSuffix = if (classLevel != null && classLevel in 1..12) (if (sindhi) " (ڪلاس $classLevel لاءِ)" else " (جماعت $classLevel کے لیے)") else ""
        return Answer(match.title, (if (sindhi) match.sindhi else match.urdu) + gradeSuffix, 0.92)
    }

    fun ask(message: String, subject: String? = null, classLevel: Int? = null): Answer {\n        localizedAnswer(message, classLevel)?.let { return it }
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
