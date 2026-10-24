package com.narendar.hub.ai

import kotlin.math.max

/** Offline-first educational brain for StudyPilot AI. */
object StudyPilotAIEngine {
    data class Answer(
        val topic: String,
        val text: String,
        val confidence: Double,
        val needsClarification: Boolean = false
    )

    private enum class Lang { ENGLISH, URDU, SINDHI }

    private data class Concept(
        val title: String,
        val aliases: List<String>,
        val en: String,
        val ur: String,
        val sd: String,
        val example: String
    )

    private val concepts = listOf(
        Concept("Photosynthesis", listOf("photosynthesis", "photosynthsis", "photosinthesis", "plant food", "plants make food"),
            "Photosynthesis is the process in which green plants use light energy, water and carbon dioxide to make food. Oxygen is released.",
            "ضیائی تالیف وہ عمل ہے جس میں سبز پودے سورج کی روشنی، پانی اور کاربن ڈائی آکسائیڈ کی مدد سے اپنی غذا بناتے ہیں اور آکسیجن خارج کرتے ہیں۔",
            "ڦوٽو سنتھیسس اهو عمل آهي جنهن ۾ ساوا ٻوٽا سج جي روشني، پاڻي ۽ ڪاربان ڊاءِ آڪسائيڊ سان خوراڪ ٺاهين ٿا ۽ آڪسيجن خارج ڪن ٿا.",
            "Example: a green leaf uses sunlight to make sugar."),
        Concept("Gravity", listOf("gravity", "gravty", "gravitiy", "کشش ثقل", "ڪشش ثقل"),
            "Gravity is the attractive force between masses. Earth's gravity pulls objects toward Earth.",
            "کشش ثقل وہ قوت ہے جو کمیت رکھنے والی چیزوں کو ایک دوسرے کی طرف کھینچتی ہے۔ زمین کی کشش اشیاء کو زمین کی طرف کھینچتی ہے۔",
            "ڪشش ثقل اها قوت آهي جيڪا ماس رکندڙ شين کي هڪ ٻئي ڏانهن ڇڪي ٿي. زمين جي ڪشش شين کي زمين ڏانهن ڇڪي ٿي.",
            "Example: a dropped ball falls because Earth attracts it."),
        Concept("Water cycle", listOf("water cycle", "evaporation", "condensation", "precipitation", "آبی چکر", "آبي چڪر", "پاڻي جو چڪر"),
            "The water cycle is the continuous movement of water through evaporation, condensation, precipitation and collection.",
            "آبی چکر میں پانی بخارات بنتا ہے، بادل بنتے ہیں اور بارش یا برف کی صورت میں پانی زمین پر واپس آتا ہے۔",
            "پاڻي جي چڪر ۾ پاڻي ٻاڦ بڻجي مٿي وڃي ٿو، ڪڪر ٺهن ٿا ۽ مينهن يا برف جي صورت ۾ پاڻي واپس اچي ٿو.",
            "Example: water evaporates, clouds form and rain returns water to Earth."),
        Concept("Fractions", listOf("fraction", "fractions", "fracion", "frakshan", "numerator", "denominator", "کسر", "ڀاڱو"),
            "A fraction represents equal parts of a whole. The numerator is the top number and the denominator is the bottom number.",
            "کسر کسی مکمل چیز کے برابر حصوں میں سے ایک یا زیادہ حصوں کو ظاہر کرتی ہے۔ اوپر والا عدد شمار کنندہ اور نیچے والا مخرج ہے۔",
            "ڀاڱو ڪنهن مڪمل شيءِ جي برابر حصن مان هڪ يا وڌيڪ حصا ڏيکاري ٿو. مٿي وارو عدد شمار ڪندڙ ۽ هيٺيون عدد مخرج آهي.",
            "Example: 3/4 means three of four equal parts."),
        Concept("Percentage", listOf("percentage", "percent", "persent", "%", "فیصد", "سيڪڙو"),
            "Percent means per hundred. 25% means 25 out of 100, equal to 0.25 or 1/4.",
            "فیصد کا مطلب ہر سو میں سے ہے۔ 25 فیصد کا مطلب 100 میں سے 25 ہے۔",
            "سيڪڙو جو مطلب هر سئو مان آهي. 25 سيڪڙو جو مطلب 100 مان 25 آهي.",
            "Example: 20% of 50 is 10."),
        Concept("Force", listOf("force", "push", "pull", "قوت", "ڌڪو", "ڇڪ"),
            "A force is a push or pull that can change motion, direction or shape. Its SI unit is the newton.",
            "قوت دھکا یا کھینچاؤ ہے جو حرکت، سمت یا شکل بدل سکتی ہے۔ قوت کی اکائی نیوٹن ہے۔",
            "قوت ڌڪو يا ڇڪ آهي جيڪا حرڪت، رخ يا شڪل بدلائي سگهي ٿي. قوت جي ايڪائي نيوٽن آهي.",
            "Example: pushing a box can make it move."),
        Concept("Energy", listOf("energy", "توانائی", "توانائي"),
            "Energy is the ability to do work or cause change. Forms include kinetic, potential, thermal, chemical and electrical energy.",
            "توانائی کام کرنے یا تبدیلی پیدا کرنے کی صلاحیت ہے۔ اس کی مختلف صورتیں حرکی، ممکنہ، حرارتی، کیمیائی اور برقی ہیں۔",
            "توانائي ڪم ڪرڻ يا تبديلي پيدا ڪرڻ جي صلاحيت آهي. ان جون صورتون حرڪتي، امڪاني، گرمي، ڪيميائي ۽ برقي آهن.",
            "Example: a moving bicycle has kinetic energy."),
        Concept("Atom", listOf("atom", "atoms", "ایٹم", "ايٽم"),
            "An atom is the basic unit of an element. Its nucleus contains protons and usually neutrons, while electrons are around it.",
            "ایٹم کسی عنصر کی بنیادی اکائی ہے۔ مرکزے میں پروٹون اور عموماً نیوٹران ہوتے ہیں جبکہ الیکٹران اس کے اردگرد ہوتے ہیں۔",
            "ايٽم ڪنهن عنصر جي بنيادي ايڪائي آهي. مرڪز ۾ پروٽان ۽ عام طور نيوٽران هوندا آهن، جڏهن ته اليڪٽران چوڌاري هوندا آهن.",
            "Example: a hydrogen atom has one proton."),
        Concept("Molecule", listOf("molecule", "molecules", "مالیکیول", "ماليڪيول"),
            "A molecule is made when two or more atoms are chemically bonded together.",
            "مالیکیول دو یا زیادہ ایٹموں کے کیمیائی طور پر جڑے ہوئے مجموعے کو کہتے ہیں۔",
            "ماليڪيول ٻن يا وڌيڪ ايٽمن جي ڪيميائي طور ڳنڍيل مجموعي کي چئبو آهي.",
            "Example: H2O contains two hydrogen atoms and one oxygen atom."),
        Concept("Cell", listOf("cell", "cells", "sel", "living unit", "خلیہ", "سيل", "خلية"),
            "A cell is the basic structural and functional unit of life. Some organisms have one cell and others have many.",
            "خلیہ جانداروں کی بنیادی ساختی اور فعلی اکائی ہے۔ کچھ جاندار ایک خلیے اور کچھ بہت سے خلیوں پر مشتمل ہوتے ہیں۔",
            "سيل جاندارن جي بنيادي بناوتي ۽ فعلي ايڪائي آهي. ڪجهه جاندار هڪ سيل ۽ ٻيا ڪيترن سيلن وارا آهن.",
            "Example: plant cells contain structures such as chloroplasts."),
        Concept("Ecosystem", listOf("ecosystem", "ecosystems", "ماحولیاتی نظام", "ماحولياتي نظام"),
            "An ecosystem includes living organisms interacting with each other and with non-living parts such as air, water and soil.",
            "ماحولیاتی نظام میں جاندار ایک دوسرے اور ہوا، پانی اور مٹی جیسے غیر جاندار ماحول کے ساتھ تعامل کرتے ہیں۔",
            "ماحولياتي نظام ۾ جاندار هڪ ٻئي ۽ هوا، پاڻي ۽ مٽيءَ جهڙن غير جاندار حصن سان لاڳاپو رکن ٿا.",
            "Example: a pond ecosystem includes water, plants, animals and microbes."),
        Concept("Renewable energy", listOf("renewable energy", "renewble energy", "solar energy", "wind energy", "قابل تجدید توانائی", "قابلِ تجديد توانائي", "ٻيهر پيدا ٿيندڙ توانائي"),
            "Renewable energy comes from sources naturally replenished over time, such as sunlight, wind and flowing water.",
            "قابل تجدید توانائی ایسے ذرائع سے حاصل ہوتی ہے جو قدرتی طور پر دوبارہ دستیاب ہوتے رہتے ہیں، جیسے سورج، ہوا اور بہتا پانی۔",
            "قابلِ تجديد توانائي اهڙن ذريعن مان ملي ٿي جيڪي قدرتي طور ٻيهر موجود ٿيندا رهن ٿا، جيئن سج، هوا ۽ وهندڙ پاڻي.",
            "Example: solar panels convert sunlight into electricity."),
        Concept("Algorithm", listOf("algorithm", "algoritm", "algorythm", "steps to solve problem", "الگورتھم", "الگورٿم"),
            "An algorithm is a clear sequence of steps used to solve a problem or complete a task.",
            "الگورتھم کسی مسئلے کو حل کرنے یا کام مکمل کرنے کے لیے واضح اور ترتیب وار ہدایات کا مجموعہ ہے۔",
            "الگورٿم ڪنهن مسئلي کي حل ڪرڻ يا ڪم مڪمل ڪرڻ لاءِ واضح ۽ ترتيبوار هدايتن جو مجموعو آهي.",
            "Example: a recipe is an everyday example of ordered steps."),
        Concept("Artificial intelligence", listOf("artificial intelligence", "artifical intelligence", "artificial inteligence", "ai", "مصنوعی ذہانت", "مصنوعي ذهانت", "اي آءِ"),
            "Artificial intelligence is computing that enables systems to recognize patterns, process language, make predictions and perform tasks. AI can make mistakes.",
            "مصنوعی ذہانت کمپیوٹر سائنس کا شعبہ ہے جس میں ایسے نظام بنائے جاتے ہیں جو زبان، نمونوں اور پیش گوئی جیسے کام کر سکتے ہیں۔ AI غلطی بھی کر سکتی ہے۔",
            "مصنوعي ذهانت ڪمپيوٽر سائنس جو شعبو آهي جنهن ۾ نظام ٻولي، نمونا ۽ اڳڪٿيون سمجهڻ جهڙا ڪم ڪري سگهن ٿا. AI کان غلطي به ٿي سگهي ٿي.",
            "Example: StudyPilot can recognize supported study concepts and provide explanations."),
        Concept("Speed", listOf("speed", "speeed", "distance divided by time", "رفتار"),
            "Average speed equals total distance divided by total time. Formula: speed = distance ÷ time.",
            "اوسط رفتار کے لیے کل فاصلہ کو کل وقت پر تقسیم کریں۔ فارمولا: رفتار = فاصلہ ÷ وقت۔",
            "سراسري رفتار لاءِ ڪل فاصلو ڪل وقت سان ورهايو. فارمولو: رفتار = فاصلو ÷ وقت.",
            "Example: 100 km in 2 hours gives 50 km/h."),
        Concept("Climate", listOf("climate", "weather climate", "آب و ہوا", "آبهوا"),
            "Climate describes long-term patterns and averages of weather in a region. Weather can change daily.",
            "آب و ہوا کسی علاقے کے موسم کے طویل مدتی اوسط اور معمولات کو کہتے ہیں۔",
            "آبهوا ڪنهن علائقي جي موسم جي ڊگهي مدي وارن اوسط ۽ نمونن کي چئبو آهي.",
            "Example: a region may have a hot, dry climate while daily weather varies."),
        Concept("Democracy", listOf("democracy", "democrasy", "جمہوریت", "جمهوريت"),
            "Democracy is a system in which people participate in choosing leaders and influencing government through lawful processes.",
            "جمہوریت ایسا نظام حکومت ہے جس میں عوام انتخابات اور دیگر قانونی طریقوں سے حکمرانی میں حصہ لیتے ہیں۔",
            "جمهوريت حڪومت جو اهڙو نظام آهي جنهن ۾ ماڻهو چونڊن ۽ ٻين قانوني طريقن سان حڪمراني ۾ حصو وٺن ٿا.",
            "Example: citizens voting in an election is a democratic process."),
        Concept("Computer", listOf("computer", "computr", "کمپیوٹر", "ڪمپيوٽر"),
            "A computer receives data, processes it according to instructions and produces results.",
            "کمپیوٹر ہدایات کے مطابق ڈیٹا وصول کرتا، اس پر عمل کرتا اور نتائج دیتا ہے۔",
            "ڪمپيوٽر هدايتن موجب ڊيٽا وٺي، ان تي عمل ڪري ۽ نتيجا ڏئي ٿو.",
            "Example: a phone contains computing hardware that runs apps."),
        Concept("Internet", listOf("internet", "internat", "web network", "انٹرنیٹ", "انٽرنيٽ"),
            "The Internet is a worldwide system of interconnected computer networks used for communication and information exchange.",
            "انٹرنیٹ دنیا بھر کے باہم جڑے ہوئے کمپیوٹر نیٹ ورکس کا نظام ہے۔",
            "انٽرنيٽ سڄي دنيا جي پاڻ ۾ ڳنڍيل ڪمپيوٽر نيٽ ورڪن جو نظام آهي.",
            "Example: a browser requests web pages through the Internet."),
        Concept("Electricity", listOf("electricity", "electric current", "بجلی", "بجلي"),
            "Electricity involves electric charge and its effects. A simple circuit needs a complete conducting path for current.",
            "بجلی برقی بار اور اس کے اثرات سے متعلق ہے۔ سادہ سرکٹ میں کرنٹ کے لیے مکمل راستہ ضروری ہے۔",
            "بجلي برقي چارج ۽ ان جي اثرن سان لاڳاپيل آهي. سادي سرڪٽ ۾ ڪرنٽ لاءِ مڪمل رستو ضروري آهي.",
            "Example: opening a circuit can stop current."),
        Concept("Education", listOf("education", "importance of education", "role of education", "تعلیم", "تعليم"),
            "Education builds knowledge, practical skills, critical thinking and opportunities for individuals and communities.",
            "تعلیم علم، عملی مہارت، تنقیدی سوچ اور بہتر مواقع پیدا کرتی ہے۔",
            "تعليم ڄاڻ، عملي مهارت، تنقيدي سوچ ۽ بهتر موقعا پيدا ڪري ٿي.",
            "Example: learning to test evidence improves critical thinking.")
    )

    fun ask(message: String, subject: String? = null, classLevel: Int? = null, preferredLanguage: String? = null): Answer {
        val raw = message.trim()
        if (raw.isBlank()) {
            return Answer("Clarification", "Type a study question or topic and I will help you work through it.", 1.0, true)
        }
        val lang = if (!preferredLanguage.isNullOrBlank()) language(preferredLanguage) else language(raw)
        solveMath(raw, lang)?.let { return it }
        val normalized = normalize(raw)
        val best = concepts.map { c -> c to c.aliases.maxOf { alias -> similarity(normalized, normalize(alias)) } }
            .maxByOrNull { it.second }
        if (best != null && best.second >= 0.43) {
            val c = best.first
            val simple = contains(normalized, "simple", "easy", "asaan", "آسان", "samjhao", "سمجھاؤ")
            val example = contains(normalized, "example", "misal", "مثال")
            val prefix = when (lang) {
                Lang.URDU -> if (simple) "آسان الفاظ میں: " else "وضاحت: "
                Lang.SINDHI -> if (simple) "سادن لفظن ۾: " else "وضاحت: "
                Lang.ENGLISH -> if (simple) "In simple words: " else "Explanation: "
            }
            val core = when (lang) {
                Lang.URDU -> c.ur
                Lang.SINDHI -> c.sd
                Lang.ENGLISH -> c.en
            }
            val extra = if (example || simple) " ${c.example}" else ""
            val grade = if (classLevel != null && classLevel in 1..12) {
                when (lang) {
                    Lang.URDU -> " جماعت $classLevel کے مطابق۔"
                    Lang.SINDHI -> " ڪلاس $classLevel مطابق."
                    Lang.ENGLISH -> " Aimed around Class $classLevel."
                }
            } else ""
            return Answer(c.title, prefix + core + extra + grade, best.second.coerceIn(0.0, 0.99))
        }
        val fallback = when (lang) {
            Lang.URDU -> "میں سوال سمجھنے کی کوشش کر رہا ہوں، مگر یہ موضوع میرے موجودہ آف لائن تعلیمی علم میں شامل نہیں ہے۔ مرکزی topic دوبارہ لکھیں؛ معمولی spelling mistake مسئلہ نہیں ہونی چاہیے۔"
            Lang.SINDHI -> "مان سوال سمجهڻ جي ڪوشش ڪري رهيو آهيان، پر هي موضوع منهنجي موجوده آف لائن تعليمي ڄاڻ ۾ شامل ناهي. اهم topic ٻيهر لکو؛ معمولي spelling mistake مسئلو نه هجڻ گهرجي."
            Lang.ENGLISH -> "I understood the study-question style, but this topic is not in my current offline educational knowledge. Rephrase it with the main topic; small spelling mistakes are okay."
        }
        return Answer("Offline knowledge", fallback + if (subject != null) " Subject: $subject." else "", 0.22, true)
    }

    private fun language(s: String): Lang {
        val x = s.lowercase()
        val sindhiChars = "ٻڄڙڏڳڱڻٽٿڀڦڪ"
        if (x.contains("sindhi") || x.contains("سنڌي") || x.any { it in sindhiChars }) return Lang.SINDHI
        if (x.contains("urdu") || x.contains("اردو") || x.any { it in '\u0600'..'\u06ff' }) return Lang.URDU
        return Lang.ENGLISH
    }

    private fun solveMath(s: String, lang: Lang): Answer? {
        val x = s.lowercase()
            .replace("what is", "")
            .replace("calculate", "")
            .replace("solve", "")
            .replace("kitna", "")
            .replace("kya hoga", "")
            .replace("=", "")
            .trim()
        val match = Regex("^(-?\\d+(?:\\.\\d+)?)\\s*([+\\-x×*÷/])\\s*(-?\\d+(?:\\.\\d+)?)$").find(x) ?: return null
        val a = match.groupValues[1].toDoubleOrNull() ?: return null
        val op = match.groupValues[2]
        val b = match.groupValues[3].toDoubleOrNull() ?: return null
        val result = when (op) {
            "+" -> a + b
            "-" -> a - b
            "x", "×", "*" -> a * b
            "÷", "/" -> if (b == 0.0) {
                return Answer("Division", when (lang) {
                    Lang.URDU -> "صفر سے تقسیم نہیں کی جا سکتی۔"
                    Lang.SINDHI -> "صفر سان ورهائڻ ممڪن ناهي."
                    Lang.ENGLISH -> "Division by zero is not defined."
                }, 0.99)
            } else a / b
            else -> return null
        }
        val shown = if (result == result.toLong().toDouble()) result.toLong().toString() else "%.4f".format(result)
        val text = when (lang) {
            Lang.URDU -> "حل: $a $op $b = $shown"
            Lang.SINDHI -> "حل: $a $op $b = $shown"
            Lang.ENGLISH -> "Solved: $a $op $b = $shown"
        }
        return Answer("Mathematics", text, 0.99)
    }

    private fun normalize(s: String): String {
        var x = s.lowercase()
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace(Regex("[^a-z0-9%\\u0600-\\u06ff+\\-×*/÷\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        listOf(
            "kya hai" to "what is",
            "kya h" to "what is",
            "kya hota hai" to "what is",
            "samjhao" to "explain",
            "batao" to "explain",
            "kyun" to "why",
            "kaise" to "how",
            "kese" to "how",
            "kya hota" to "what is",
            "plants food kaise banate hain" to "plants make food"
        ).forEach { (from, to) -> x = x.replace(from, to) }
        val aliases = concepts.flatMap { it.aliases }.filter { !it.contains(' ') }.distinct()
        return x.split(' ').joinToString(" ") { word ->
            if (word.length < 4 || word.any { it in '\u0600'..'\u06ff' }) word
            else {
                val candidate = aliases
                    .filter { it.length in word.length - 2..word.length + 2 }
                    .minByOrNull { edit(word, it.lowercase()) }
                if (candidate != null && edit(word, candidate.lowercase()) <= max(1, word.length / 4)) candidate.lowercase() else word
            }
        }
    }

    private fun contains(s: String, vararg terms: String): Boolean = terms.any { s.contains(it.lowercase()) }

    private fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.contains(b) || b.contains(a)) return 0.9
        val aw = a.split(' ').toSet()
        val bw = b.split(' ').toSet()
        val union = (aw union bw).size.coerceAtLeast(1)
        val overlap = aw.intersect(bw).size.toDouble() / union
        val distance = edit(a, b).toDouble() / max(a.length, b.length).coerceAtLeast(1)
        return max(overlap, 1.0 - distance)
    }

    private fun edit(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) {
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + if (a[i] == b[j]) 0 else 1
                )
            }
            previous = current
        }
        return previous[b.length]
    }
}
