package com.narendar.hub.ai

import kotlin.math.max

/** Offline StudyPilot educational brain: fuzzy matching + multilingual knowledge + math solving. */
object StudyPilotAIEngine {
    data class Answer(val topic:String,val text:String,val confidence:Double,val needsClarification:Boolean=false)
    private data class Concept(val title:String,val aliases:List<String>,val en:String,val ur:String,val sd:String,val example:String="")

    private val concepts=listOf(
        Concept("Photosynthesis",listOf("photosynthesis","photosynthsis","photosinthesis","plant food","plants make food"),
            "Photosynthesis is how green plants use light energy, water and carbon dioxide to make food. Oxygen is released.",
            "ضیائی تالیف وہ عمل ہے جس میں سبز پودے سورج کی روشنی، پانی اور کاربن ڈائی آکسائیڈ کی مدد سے اپنی غذا بناتے ہیں۔ اس عمل میں آکسیجن خارج ہوتی ہے۔",
            "ڦوٽو سنتھیسس اهو عمل آهي جنهن ۾ ساوا ٻوٽا سج جي روشني، پاڻي ۽ ڪاربان ڊاءِ آڪسائيڊ جي مدد سان پنهنجي خوراڪ ٺاهين ٿا. هن عمل ۾ آڪسيجن خارج ٿئي ٿي.","Example: a green leaf uses sunlight to make sugar."),
        Concept("Gravity",listOf("gravity","gravty","gravitiy","کشش ثقل","ڪشش ثقل"),
            "Gravity is the attractive force between masses. Earth's gravity pulls objects toward Earth and helps keep the Moon in orbit.",
            "کششِ ثقل وہ قوت ہے جو کمیت رکھنے والی چیزوں کو ایک دوسرے کی طرف کھینچتی ہے۔ زمین کی کشش چیزوں کو اپنے مرکز کی طرف کھینچتی ہے۔",
            "ڪشش ثقل اها قوت آهي جيڪا ماس رکندڙ شين کي هڪ ٻئي ڏانهن ڇڪي ٿي. زمين جي ڪشش شين کي پنهنجي مرڪز ڏانهن ڇڪي ٿي.","Example: a dropped ball falls because Earth pulls it downward."),
        Concept("Water cycle",listOf("water cycle","evaporation","condensation","precipitation","آبی چکر","آبي چڪر","پاڻي جو چڪر"),
            "The water cycle is the continuous movement of water through evaporation, condensation, precipitation and collection.",
            "آبی چکر میں پانی بخارات بن کر اوپر جاتا ہے، ٹھنڈا ہو کر بادل بناتا ہے، پھر بارش یا برف کی صورت میں زمین پر واپس آتا ہے۔",
            "پاڻي جي چڪر ۾ پاڻي ٻاڦ بڻجي مٿي وڃي ٿو، ٿڌو ٿي ڪڪر ٺاهي ٿو، پوءِ مينهن يا برف جي صورت ۾ زمين تي موٽي اچي ٿو.","Example: wet water evaporates, clouds form, then rain returns water."),
        Concept("Fractions",listOf("fraction","fractions","fracion","frakshan","numerator","denominator","کسر","ڀاڱو"),
            "A fraction represents equal parts of a whole. The numerator is the top number and the denominator is the bottom number.",
            "کسر کسی مکمل چیز کے برابر حصوں میں سے ایک یا زیادہ حصوں کو ظاہر کرتی ہے۔ اوپر والا عدد شمار کنندہ اور نیچے والا مخرج ہے۔",
            "ڀاڱو ڪنهن مڪمل شيءِ جي برابر حصن مان هڪ يا وڌيڪ حصا ڏيکاري ٿو. مٿي وارو عدد شمار ڪندڙ ۽ هيٺيون عدد مخرج آهي.","Example: 3/4 means three of four equal parts."),
        Concept("Percentage",listOf("percentage","percent","persent","%","فیصد","سيڪڙو"),
            "Percent means per hundred. 25% means 25 out of 100, equal to 0.25 or 1/4.",
            "فیصد کا مطلب ہر سو میں سے ہے۔ 25 فیصد کا مطلب 100 میں سے 25 ہے، یعنی 0.25 یا 1/4۔",
            "سيڪڙو جو مطلب هر سئو مان آهي. 25 سيڪڙو جو مطلب 100 مان 25 آهي، يعني 0.25 يا 1/4.","Example: 20% of 50 is 10."),
        Concept("Force",listOf("force","push","pull","قوت","ڌڪو","ڇڪ"),
            "A force is a push or pull that can change motion, direction or shape. Its SI unit is the newton.",
            "قوت دھکا یا کھینچاؤ ہے جو کسی چیز کی حرکت، سمت یا شکل بدل سکتی ہے۔ قوت کی اکائی نیوٹن ہے۔",
            "قوت ڌڪو يا ڇڪ آهي جيڪا ڪنهن شيءِ جي حرڪت، رخ يا شڪل بدلائي سگهي ٿي. قوت جي ايڪائي نيوٽن آهي.","Example: pushing a box can make it move."),
        Concept("Energy",listOf("energy","توانائی","توانائي"),
            "Energy is the ability to do work or cause change. Common forms include kinetic, potential, thermal, chemical and electrical energy.",
            "توانائی کام کرنے یا تبدیلی پیدا کرنے کی صلاحیت ہے۔ اس کی صورتوں میں حرکی، ممکنہ، حرارتی، کیمیائی اور برقی توانائی شامل ہیں۔",
            "توانائي ڪم ڪرڻ يا تبديلي پيدا ڪرڻ جي صلاحيت آهي. ان جي صورتن ۾ حرڪتي، امڪاني، گرمي، ڪيميائي ۽ برقي توانائي شامل آهن.","Example: a moving bicycle has kinetic energy."),
        Concept("Atom",listOf("atom","atoms","ایٹم","ايٽم"),
            "An atom is the basic unit of an element. Its nucleus contains protons and usually neutrons, while electrons are around it.",
            "ایٹم کسی عنصر کی بنیادی اکائی ہے۔ اس کے مرکزے میں پروٹون اور عموماً نیوٹران ہوتے ہیں جبکہ الیکٹران مرکزے کے اردگرد ہوتے ہیں۔",
            "ايٽم ڪنهن عنصر جي بنيادي ايڪائي آهي. ان جي مرڪز ۾ پروٽان ۽ عام طور نيوٽران هوندا آهن، جڏهن ته اليڪٽران مرڪز جي چوڌاري هوندا آهن.","Example: a hydrogen atom has one proton."),
        Concept("Molecule",listOf("molecule","molecules","مالیکیول","ماليڪيول"),
            "A molecule is made when two or more atoms are chemically bonded together.",
            "مالیکیول دو یا زیادہ ایٹموں کے کیمیائی طور پر جڑے ہوئے مجموعے کو کہتے ہیں۔",
            "ماليڪيول ٻن يا وڌيڪ ايٽمن جي ڪيميائي طور ڳنڍيل مجموعي کي چئبو آهي.","Example: H₂O contains two hydrogen atoms and one oxygen atom."),
        Concept("Cell",listOf("cell","cells","sel","living unit","خلیہ","سيل","خلية"),
            "A cell is the basic structural and functional unit of life. Some organisms have one cell and others have many.",
            "خلیہ جانداروں کی بنیادی ساختی اور فعلی اکائی ہے۔ کچھ جاندار ایک خلیے پر مشتمل ہوتے ہیں جبکہ پودے اور جانور بہت سے خلیوں سے بنتے ہیں۔",
            "سيل جاندارن جي بنيادي بناوتي ۽ فعلي ايڪائي آهي. ڪجهه جاندار هڪ سيل جا هوندا آهن، جڏهن ته ٻوٽا ۽ جانور ڪيترن سيلن مان ٺهن ٿا.","Example: plant cells have structures such as chloroplasts."),
        Concept("Ecosystem",listOf("ecosystem","ecosystems","ماحولیاتی نظام","ماحولياتي نظام"),
            "An ecosystem includes living organisms interacting with each other and with air, water, soil and other non-living parts.",
            "ماحولیاتی نظام میں جاندار ایک دوسرے اور ہوا، پانی اور مٹی جیسے غیر جاندار ماحول کے ساتھ تعامل کرتے ہیں۔",
            "ماحولياتي نظام ۾ جاندار هڪ ٻئي ۽ هوا، پاڻي ۽ مٽيءَ جهڙن غير جاندار حصن سان لاڳاپو رکن ٿا.","Example: a pond ecosystem includes water, plants, animals and microbes."),
        Concept("Renewable energy",listOf("renewable energy","renewble energy","solar energy","wind energy","قابل تجدید توانائی","قابلِ تجديد توانائي","ٻيهر پيدا ٿيندڙ توانائي"),
            "Renewable energy comes from sources naturally replenished over time, such as sunlight, wind and flowing water.",
            "قابلِ تجدید توانائی ایسے ذرائع سے حاصل ہوتی ہے جو قدرتی طور پر دوبارہ دستیاب ہوتے رہتے ہیں، جیسے سورج کی روشنی، ہوا اور بہتا پانی۔",
            "قابلِ تجديد توانائي اهڙن ذريعن مان ملي ٿي جيڪي قدرتي طور ٻيهر موجود ٿيندا رهن ٿا، جيئن سج جي روشني، هوا ۽ وهندڙ پاڻي.","Example: solar panels convert sunlight into electricity."),
        Concept("Algorithm",listOf("algorithm","algoritm","algorythm","steps to solve problem","الگورتھم","الگورٿم"),
            "An algorithm is a clear sequence of steps used to solve a problem or complete a task.",
            "الگورتھم کسی مسئلے کو حل کرنے یا کام مکمل کرنے کے لیے واضح اور ترتیب وار ہدایات کا مجموعہ ہے۔",
            "الگورٿم ڪنهن مسئلي کي حل ڪرڻ يا ڪم مڪمل ڪرڻ لاءِ واضح ۽ ترتيبوار هدايتن جو مجموعو آهي.","Example: a recipe is an everyday example of ordered steps."),
        Concept("Artificial intelligence",listOf("artificial intelligence","artifical intelligence","artificial inteligence","ai","مصنوعی ذہانت","مصنوعي ذهانت","اي آءِ"),
            "Artificial intelligence is computing that lets systems recognize patterns, process language, make predictions and perform tasks. AI can make mistakes.",
            "مصنوعی ذہانت کمپیوٹر سائنس کا شعبہ ہے جس میں ایسے نظام بنائے جاتے ہیں جو زبان سمجھنے، نمونے پہچاننے اور پیش گوئی جیسے کام کرتے ہیں۔ AI غلطی بھی کر سکتی ہے۔",
            "مصنوعي ذهانت ڪمپيوٽر سائنس جو شعبو آهي، جنهن ۾ اهڙا نظام ٺاهيا وڃن ٿا جيڪي ٻولي سمجهڻ، نمونا سڃاڻڻ ۽ اڳڪٿي ڪرڻ جهڙا ڪم ڪن ٿا. AI کان غلطي به ٿي سگهي ٿي.","Example: StudyPilot can classify a study question and produce a supported explanation."),
        Concept("Speed",listOf("speed","speeed","distance divided by time","رفتار"),
            "Average speed equals total distance divided by total time. Formula: speed = distance ÷ time.",
            "اوسط رفتار معلوم کرنے کے لیے کل فاصلہ کو کل وقت پر تقسیم کریں۔ فارمولا: رفتار = فاصلہ ÷ وقت۔",
            "سراسري رفتار معلوم ڪرڻ لاءِ ڪل فاصلو ڪل وقت سان ورهايو. فارمولو: رفتار = فاصلو ÷ وقت.","Example: 100 km in 2 hours gives 50 km/h."),
        Concept("Climate",listOf("climate","weather climate","آب و ہوا","آبهوا"),
            "Climate describes the long-term patterns and averages of weather in a region. Weather can change daily.",
            "آب و ہوا کسی علاقے کے موسم کے طویل مدتی اوسط اور معمولات کو کہتے ہیں۔ موسم روز بدل سکتا ہے۔",
            "آبهوا ڪنهن علائقي جي موسم جي ڊگهي مدي وارن اوسط ۽ نمونن کي چئبو آهي. موسم روز بدلجي سگهي ٿي.","Example: a region can have a hot, dry climate while individual days vary."),
        Concept("Democracy",listOf("democracy","democrasy","جمہوریت","جمهوريت"),
            "Democracy is a system in which people participate in choosing leaders and influencing government through lawful processes.",
            "جمہوریت ایسا نظامِ حکومت ہے جس میں عوام انتخابات اور دیگر قانونی طریقوں سے حکمرانی کے فیصلوں میں حصہ لیتے ہیں۔",
            "جمهوريت حڪومت جو اهڙو نظام آهي جنهن ۾ ماڻهو چونڊن ۽ ٻين قانوني طريقن سان حڪومتي فيصلن ۾ حصو وٺن ٿا.","Example: citizens voting in an election is a democratic process."),
        Concept("Computer",listOf("computer","computr","کمپیوٹر","ڪمپيوٽر"),
            "A computer receives data, processes it according to instructions and produces results.",
            "کمپیوٹر ایک الیکٹرانک مشین ہے جو ہدایات کے مطابق ڈیٹا وصول کرتی، اس پر عمل کرتی اور نتائج دیتی ہے۔",
            "ڪمپيوٽر هڪ اليڪٽرانڪ مشين آهي جيڪا هدايتن موجب ڊيٽا وٺي ٿي، ان تي عمل ڪري ٿي ۽ نتيجا ڏئي ٿي.","Example: a phone contains computing hardware that runs apps."),
        Concept("Internet",listOf("internet","internat","web network","انٹرنیٹ","انٽرنيٽ"),
            "The Internet is a worldwide system of interconnected computer networks used for communication and information exchange.",
            "انٹرنیٹ دنیا بھر کے کمپیوٹر نیٹ ورکس کا باہم جڑا ہوا نظام ہے۔ اس پر معلومات، پیغام رسانی اور آن لائن سیکھنے جیسے کام ہوتے ہیں۔",
            "انٽرنيٽ سڄي دنيا جي ڪمپيوٽر نيٽ ورڪن جو پاڻ ۾ ڳنڍيل نظام آهي. ان تي ڄاڻ، پيغام رساني ۽ آن لائن سکڻ جهڙا ڪم ٿين ٿا.","Example: a browser requests web pages through the Internet."),
        Concept("Electricity",listOf("electricity","electric current","بجلی","بجلي"),
            "Electricity involves electric charge and its effects. A simple circuit needs a complete conducting path for current.",
            "بجلی برقی بار اور اس کی حرکت سے متعلق مظاہر کا نام ہے۔ سادہ سرکٹ میں کرنٹ کے بہنے کے لیے مکمل راستہ درکار ہوتا ہے۔",
            "بجلي برقي چارج ۽ ان جي حرڪت سان لاڳاپيل عملن کي چئبو آهي. سادي سرڪٽ ۾ ڪرنٽ وهڻ لاءِ مڪمل رستو ضروري آهي.","Example: opening a circuit can stop current."),
        Concept("Evaporation",listOf("evaporation","evaporate","تبخیر","بخارات","ٻاڦ"),
            "Evaporation is the change of a liquid into gas from its surface. Heating often makes it faster.",
            "تبخیر وہ عمل ہے جس میں مائع سطح سے توانائی لے کر گیس یا بخارات میں تبدیل ہوتا ہے۔ گرمی اس عمل کو تیز کر سکتی ہے۔",
            "بخارات ٿيڻ اهو عمل آهي جنهن ۾ مائع توانائي وٺي گئس يا ٻاڦ ۾ بدلجي ٿو. گرمي هن عمل کي تيز ڪري سگهي ٿي.","Example: wet clothes dry because water evaporates."),
        Concept("Condensation",listOf("condensation","condense","تکثیف","گاڙها ٿيڻ"),
            "Condensation happens when a gas cools and changes into liquid. Water vapour can form droplets.",
            "تکثیف میں گیس ٹھنڈی ہو کر مائع بنتی ہے۔ پانی کے بخارات ٹھنڈے ہو کر قطرے بنا سکتے ہیں۔",
            "گاڙها ٿيڻ ۾ گئس ٿڌي ٿي مائع بڻجي ٿي. پاڻيءَ جي ٻاڦ ٿڌي ٿي ڦڙا ٺاهي سگهي ٿي.","Example: droplets on a cold glass come from condensation."),
        Concept("Education",listOf("education","importance of education","role of education"),
            "Education builds knowledge, practical skills, critical thinking and opportunities for individuals and communities.",
            "تعلیم علم، عملی مہارت، تنقیدی سوچ اور بہتر مواقع پیدا کرتی ہے۔",
            "تعليم ڄاڻ، عملي مهارت، تنقيدي سوچ ۽ بهتر موقعا پيدا ڪري ٿي.","Example: learning to test evidence improves critical thinking.")
    )

    fun ask(message:String,subject:String?=null,classLevel:Int?=null):Answer {
        val raw=message.trim()
        if(raw.isBlank()) return Answer("Clarification","Type a study question or topic and I will work through it with you.",1.0,true)
        val lang=language(raw)
        solveMath(raw,lang)?.let{return it}
        val normalized=normalize(raw)
        val best=concepts.map{c->c to c.aliases.maxOf{a->similarity(normalized,normalize(a))}}
            .maxByOrNull{it.second}
        if(best!=null && best.second>=0.43){
            val c=best.first
            val askExample=contains(normalized,"example","misal","مثال")
            val askSimple=contains(normalized,"simple","easy","asaan","آسان","samjhao","سمجھاؤ")
            val prefix=when(lang){Lang.URDU->if(askSimple)"آسان الفاظ میں: "else "وضاحت: ";Lang.SINDHI->if(askSimple)"سادن لفظن ۾: "else "وضاحت: ";Lang.ENGLISH->if(askSimple)"In simple words: "else "Explanation: "}
            val core=when(lang){Lang.URDU->c.ur;Lang.SINDHI->c.sd;Lang.ENGLISH->c.en}
            val ex=if(askExample||askSimple)" ${c.example}"else ""
            val grade=if(classLevel!=null&&classLevel in 1..12) when(lang){Lang.URDU->" جماعت $classLevel کے مطابق۔";Lang.SINDHI->" ڪلاس $classLevel مطابق.";Lang.ENGLISH->" Aimed around Class $classLevel."}else ""
            return Answer(c.title,prefix+core+ex+grade,best.second.coerceIn(0.0,0.99))
        }
        val fallback=when(lang){
            Lang.URDU->"میں نے سوال سمجھنے کی کوشش کی، مگر اس موضوع کے لیے میرے موجودہ آف لائن علم میں قابلِ اعتماد جواب نہیں ہے۔ موضوع یا اہم لفظ دوبارہ لکھیں؛ معمولی spelling mistake سے مسئلہ نہیں ہوگا۔"
            Lang.SINDHI->"مان سوال سمجهڻ جي ڪوشش ڪئي، پر هن موضوع لاءِ منهنجي موجوده آف لائن ڄاڻ ۾ پڪ وارو جواب ناهي. موضوع يا اهم لفظ ٻيهر لکو؛ معمولي spelling mistake مسئلو نه هوندي."
            Lang.ENGLISH->"I understood the question style, but this exact topic is not in my supported offline knowledge yet. Rephrase it with the main topic; small spelling mistakes are okay."
        }
        return Answer("Offline knowledge","$fallback${if(subject!=null)" Subject: $subject."else ""}",0.22,true)
    }

    private enum class Lang{ENGLISH,URDU,SINDHI}
    private fun language(s:String):Lang{
        val x=s.lowercase();val sd="ٻڄڙڏڳڱڻٽٿڀڦڪ"
        if(x.contains("sindhi")||x.contains("سنڌي")||x.any{it in sd})return Lang.SINDHI
        if(x.contains("urdu")||x.contains("اردو")||x.any{it in '\u0600'..'\u06ff'})return Lang.URDU
        return Lang.ENGLISH
    }
    private fun solveMath(s:String,lang:Lang):Answer?{
        val x=s.lowercase().replace("what is","").replace("calculate","").replace("solve","").replace("kitna","").replace("kya hoga","").replace("=","").trim()
        val m=Regex("^(-?\\d+(?:\\.\\d+)?)\\s*([+\\-x×*÷/])\\s*(-?\\d+(?:\\.\\d+)?)$").find(x)?:return null
        val a=m.groupValues[1].toDoubleOrNull()?:return null;val op=m.groupValues[2];val b=m.groupValues[3].toDoubleOrNull()?:return null
        val r=when(op){"+"->a+b,"-"->a-b,"x","×","*"->a*b,"÷","/"->if(b==0.0)return Answer("Division",if(lang==Lang.URDU)"صفر سے تقسیم نہیں کی جا سکتی۔"else if(lang==Lang.SINDHI)"صفر سان ورهائڻ ممڪن ناهي."else"Division by zero is not defined.",0.99)else a/b else return null}
        val shown=if(r%1.0==0.0)r.toLong().toString() else "%.4f".format(r)
        val text=when(lang){Lang.URDU->"حل: $a $op $b = $shown";Lang.SINDHI->"حل: $a $op $b = $shown";Lang.ENGLISH->"Solved: $a $op $b = $shown"}
        return Answer("Mathematics",text,0.99)
    }
    private fun normalize(s:String):String{
        var x=s.lowercase().replace('ي','ی').replace('ك','ک').replace(Regex("[^a-z0-9%\\u0600-\\u06ff+\\-×*/÷\\s]")," ").replace(Regex("\\s+")," ").trim()
        listOf("kya hai" to "what is","kya h" to "what is","kya hota hai" to "what is","samjhao" to "explain","batao" to "explain","kyun" to "why","kaise" to "how","kese" to "how","plants food kaise banate hain" to "plants make food").forEach{(a,b)->x=x.replace(a,b)}
        val words=concepts.flatMap{it.aliases}.filter{!it.contains(' ')}.distinct()
        return x.split(' ').joinToString(" "){w->if(w.length<4||w.any{it in '\u0600'..'\u06ff'})w else {val c=words.filter{it.length in w.length-2..w.length+2}.minByOrNull{edit(w,it.lowercase())};if(c!=null&&edit(w,c.lowercase())<=max(1,w.length/4))c.lowercase()else w}}
    }
    private fun contains(s:String,vararg q:String)=q.any{s.contains(it.lowercase())}
    private fun similarity(a:String,b:String):Double{if(a==b)return 1.0;if(a.contains(b)||b.contains(a))return .9;val aw=a.split(' ').toSet();val bw=b.split(' ').toSet();val overlap=aw.intersect(bw).size.toDouble()/(aw union bw).size.coerceAtLeast(1);val ed=edit(a,b).toDouble()/max(a.length,b.length).coerceAtLeast(1);return max(overlap,1-ed)}
    private fun edit(a:String,b:String):Int{var p=IntArray(b.length+1){it};for(i in a.indices){val c=IntArray(b.length+1);c[0]=i+1;for(j in b.indices)c[j+1]=minOf(c[j]+1,p[j+1]+1,p[j]+if(a[i]==b[j])0 else 1);p=c};return p[b.length]}
}
