package com.max.assistant.baby

import com.max.assistant.tts.VoiceLanguage

/**
 * Trilingual repository of baby entertainment content:
 * Lullabies, Nursery Rhymes, Animated Stories, and Animal Games in English, Hindi/Hinglish, and Bengali.
 */
object BabyContentRepository {

    fun getLullabies(language: VoiceLanguage): List<BabyContentItem> {
        return when (language) {
            VoiceLanguage.BENGALI -> listOf(
                BabyContentItem(
                    id = "LUL_BN_01",
                    title = "খোকা ঘুমালো পাড়া জুড়ালো",
                    subtitle = "ঘুম পাড়ানোর গান",
                    activityType = BabyActivityType.LULLABY,
                    character = BabyCharacter.STAR,
                    spokenText = "খোকা ঘুমালো পাড়া জুড়ালো বর্গি এলো দেশে। বুলবুলিতে ধান খেয়েছে খাজনা দেবো কিসে? ঘুমিয়ে পড়ো সোনা পাখি, চাঁদের আলোয় চোখ রাখো।",
                    displayText = "খোকা ঘুমালো পাড়া জুড়ালো,\nবর্গি এলো দেশে।\nবুলবুলিতে ধান খেয়েছে,\nখাজনা দেবো কিসে?\n\nঘুমিয়ে পড়ো চাঁদের আলোয়...",
                    language = VoiceLanguage.BENGALI,
                    soundTheme = BabySoundTheme.LULLABY_CHIMES
                ),
                BabyContentItem(
                    id = "LUL_BN_02",
                    title = "তাই তাই তাই মামার বাড়ি যাই",
                    subtitle = "মিষ্টি লোরি",
                    activityType = BabyActivityType.LULLABY,
                    character = BabyCharacter.TEDDY,
                    spokenText = "তাই তাই তাই মামার বাড়ি যাই। মামা দিল দুধ ভাত দুয়ারে বসে খাই। ঘুমিয়ে পড়ো আমার চাঁদের কণা।",
                    displayText = "তাই তাই তাই,\nমামার বাড়ি যাই।\nমামা দিল দুধ ভাত,\nদুয়ারে বসে খাই।\n\nমিষ্টি ঘুম আসুক নয়নে...",
                    language = VoiceLanguage.BENGALI,
                    soundTheme = BabySoundTheme.LULLABY_CHIMES
                )
            )
            VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> listOf(
                BabyContentItem(
                    id = "LUL_HI_01",
                    title = "चंदा मामा दूर के",
                    subtitle = "प्यारी लोरी",
                    activityType = BabyActivityType.LULLABY,
                    character = BabyCharacter.STAR,
                    spokenText = "चंदा मामा दूर के, पुए पकाएं बूर के। आप खाएं थाली में, मुन्ने को दें प्याली में। सो जाओ मेरे प्यारे बच्चे, मीठे-मीठे सपने देखो।",
                    displayText = "चंदा मामा दूर के,\nपुए पकाएं बूर के।\nआप खाएं थाली में,\nमुन्ने को दें प्याली में।\n\nसो जाओ नन्हे सितारे...",
                    language = VoiceLanguage.HINDI,
                    soundTheme = BabySoundTheme.LULLABY_CHIMES
                ),
                BabyContentItem(
                    id = "LUL_HI_02",
                    title = "लल्ला लल्ला लोरी",
                    subtitle = "मीठी लोरी",
                    activityType = BabyActivityType.LULLABY,
                    character = BabyCharacter.TEDDY,
                    spokenText = "लल्ला लल्ला लोरी, दूध की कटोरी। दूध में बताशा, मुन्ना करे तमाशा। छोटी-छोटी प्यारी आंखें बंद करो और सो जाओ।",
                    displayText = "लल्ला लल्ला लोरी,\nदूध की कटोरी।\nदूध में बताशा,\nमुन्ना करे तमाशा।\n\nप्यारी नींद आए...",
                    language = VoiceLanguage.HINDI,
                    soundTheme = BabySoundTheme.LULLABY_CHIMES
                )
            )
            else -> listOf(
                BabyContentItem(
                    id = "LUL_EN_01",
                    title = "Twinkle, Twinkle, Little Star",
                    subtitle = "Gentle Bedtime Lullaby",
                    activityType = BabyActivityType.LULLABY,
                    character = BabyCharacter.STAR,
                    spokenText = "Twinkle, twinkle, little star, how I wonder what you are. Up above the world so high, like a diamond in the sky. Close your sleepy eyes, little angel.",
                    displayText = "Twinkle, twinkle, little star,\nHow I wonder what you are.\nUp above the world so high,\nLike a diamond in the sky.\n\nSweet dreams little one...",
                    language = VoiceLanguage.ENGLISH,
                    soundTheme = BabySoundTheme.LULLABY_CHIMES
                ),
                BabyContentItem(
                    id = "LUL_EN_02",
                    title = "Hush Little Baby",
                    subtitle = "Calming Lullaby",
                    activityType = BabyActivityType.LULLABY,
                    character = BabyCharacter.TEDDY,
                    spokenText = "Hush, little baby, don't say a word, Papa's gonna buy you a mockingbird. And if that mockingbird won't sing, Papa's gonna buy you a diamond ring. Sleep gently now.",
                    displayText = "Hush, little baby, don't say a word,\nPapa's gonna buy you a mockingbird.\nAnd if that mockingbird won't sing,\nPapa's gonna buy you a diamond ring.",
                    language = VoiceLanguage.ENGLISH,
                    soundTheme = BabySoundTheme.LULLABY_CHIMES
                )
            )
        }
    }

    fun getRhymes(language: VoiceLanguage): List<BabyContentItem> {
        return when (language) {
            VoiceLanguage.BENGALI -> listOf(
                BabyContentItem(
                    id = "RHY_BN_01",
                    title = "হাট্টিমা টিম টিম",
                    subtitle = "মজার বাংলা ছড়া",
                    activityType = BabyActivityType.RHYMES,
                    character = BabyCharacter.DUCK,
                    spokenText = "হাট্টিমা টিম টিম, তারা মাঠে পাড়ে ডিম। তাদের খাড়া দুটো শিং, তারা হাট্টিমা টিম টিম! হাঁসের মতো প্যাক প্যাক করে হাসো!",
                    displayText = "হাট্টিমা টিম টিম,\nতারা মাঠে পাড়ে ডিম।\nতাদের খাড়া দুটো শিং,\nতারা হাট্টিমা টিম টিম!",
                    language = VoiceLanguage.BENGALI,
                    soundTheme = BabySoundTheme.ANIMAL_QUACK
                ),
                BabyContentItem(
                    id = "RHY_BN_02",
                    title = "খোকা গেছে মাছ ধরতে",
                    subtitle = "বাংলা ছড়া",
                    activityType = BabyActivityType.RHYMES,
                    character = BabyCharacter.KITTEN,
                    spokenText = "খোকা গেছে মাছ ধরতে ক্ষীর নদীর কূলে, ছিপ নিয়ে গেল কোলা ব্যাঙে, মাছ নিয়ে গেল চিলে!",
                    displayText = "খোকা গেছে মাছ ধরতে,\nক্ষীর নদীর কূলে।\nছিপ নিয়ে গেল কোলা ব্যাঙে,\nমাছ নিয়ে গেল চিলে!",
                    language = VoiceLanguage.BENGALI,
                    soundTheme = BabySoundTheme.GENTLE_CHIME
                )
            )
            VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> listOf(
                BabyContentItem(
                    id = "RHY_HI_01",
                    title = "मछली जल की रानी है",
                    subtitle = "मजेदार कविता",
                    activityType = BabyActivityType.RHYMES,
                    character = BabyCharacter.DUCK,
                    spokenText = "मछली जल की रानी है, जीवन उसका पानी है। हाथ लगाओ तो डर जाएगी, बाहर निकालो तो मर जाएगी! पानी में छप-छप तैराओ!",
                    displayText = "मछली जल की रानी है,\nजीवन उसका पानी है।\nहाथ लगाओ तो डर जाएगी,\nबाहर निकालो तो मर जाएगी!",
                    language = VoiceLanguage.HINDI,
                    soundTheme = BabySoundTheme.GENTLE_CHIME
                ),
                BabyContentItem(
                    id = "RHY_HI_02",
                    title = "तितली उड़ी बस पर चढ़ी",
                    subtitle = "प्यारी बाल कविता",
                    activityType = BabyActivityType.RHYMES,
                    character = BabyCharacter.BUNNY,
                    spokenText = "तितली उड़ी बस पर चढ़ी, सीट ना मिली तो रोने लगी। ड्राइवर ने बोला आजा मेरे पास, तितली बोली ना बाबा ना, मेरा घर है पास!",
                    displayText = "तितली उड़ी बस पर चढ़ी,\nसीट ना मिली तो रोने लगी।\nड्राइवर ने बोला आजा मेरे पास,\nतितली बोली ना बाबा ना!",
                    language = VoiceLanguage.HINDI,
                    soundTheme = BabySoundTheme.GENTLE_CHIME
                )
            )
            else -> listOf(
                BabyContentItem(
                    id = "RHY_EN_01",
                    title = "Old MacDonald Had A Farm",
                    subtitle = "Playful Nursery Rhyme",
                    activityType = BabyActivityType.RHYMES,
                    character = BabyCharacter.DUCK,
                    spokenText = "Old MacDonald had a farm, E-I-E-I-O! And on his farm he had a duck, E-I-E-I-O! With a quack quack here and a quack quack there, everywhere a quack quack!",
                    displayText = "Old MacDonald had a farm, E-I-E-I-O!\nAnd on that farm he had a duck, E-I-E-I-O!\nWith a quack quack here,\nAnd a quack quack there!",
                    language = VoiceLanguage.ENGLISH,
                    soundTheme = BabySoundTheme.ANIMAL_QUACK
                ),
                BabyContentItem(
                    id = "RHY_EN_02",
                    title = "Baa Baa Black Sheep",
                    subtitle = "Classic Rhyme",
                    activityType = BabyActivityType.RHYMES,
                    character = BabyCharacter.BUNNY,
                    spokenText = "Baa, baa, black sheep, have you any wool? Yes sir, yes sir, three bags full! One for the master, and one for the dame, and one for the little boy who lives down the lane!",
                    displayText = "Baa, baa, black sheep,\nHave you any wool?\nYes sir, yes sir,\nThree bags full!",
                    language = VoiceLanguage.ENGLISH,
                    soundTheme = BabySoundTheme.GENTLE_CHIME
                )
            )
        }
    }

    fun getAnimalSoundsAndGames(language: VoiceLanguage): List<BabyContentItem> {
        return listOf(
            BabyContentItem(
                id = "ANI_01",
                title = when (language) {
                    VoiceLanguage.BENGALI -> "বন্ধু ব্রুনো কুকুরছানা"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "प्यारा ब्रूनो पिल्ला"
                    else -> "Bruno The Playful Puppy"
                },
                subtitle = when (language) {
                    VoiceLanguage.BENGALI -> "পশুর ডাক ও আনন্দ"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "जानवरों की आवाज"
                    else -> "Animal Sounds"
                },
                activityType = BabyActivityType.ANIMAL_SOUNDS,
                character = BabyCharacter.BUNNY,
                spokenText = when (language) {
                    VoiceLanguage.BENGALI -> "হেই ছোট্ট সোনা! দেখো ব্রুনো কুকুরছানা লেজ নাড়ছে আর বলছে: ভুউ-ভুউ! ভুউ-ভুউ! ব্রুনো তোমার সাথে খেলতে চায়!"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "अरे देखो नन्हे दोस्त! ब्रूनो पिल्ला अपनी पूंछ हिला रहा है और बोल रहा है: वूफ-वूफ! वूफ-वूफ! वो आपके साथ खेलना चाहता है!"
                    else -> "Hello little buddy! Look at Bruno the puppy wagging his tail: Woof! Woof! Woof! He wants to be your best friend!"
                },
                displayText = "🐶 Woof! Woof!\nBruno is wagging his tail happily!",
                language = language,
                soundTheme = BabySoundTheme.ANIMAL_WOOF
            ),
            BabyContentItem(
                id = "ANI_02",
                title = when (language) {
                    VoiceLanguage.BENGALI -> "মিমি বিড়ালছানা"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "मीमी प्यारी बिल्ली"
                    else -> "Mimi The Cute Kitten"
                },
                subtitle = "Animal Sounds",
                activityType = BabyActivityType.ANIMAL_SOUNDS,
                character = BabyCharacter.KITTEN,
                spokenText = when (language) {
                    VoiceLanguage.BENGALI -> "মিঁয়াউ~ মিঁয়াউ! মিমি বিড়ালছানা নরম থাবা নাড়িয়ে আদর চাইছে: মিঁয়াউ~ মিঁয়াউ!"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "म्याऊं~ म्याऊं! मीमी बिल्ली अपने मुलायम पंजों से खेल रही है: म्याऊं~ म्याऊं!"
                    else -> "Meow~ Meow! Soft little Mimi the kitten is purring with her fluffy paws: Meow~ Meow!"
                },
                displayText = "🐱 Meow~ Meow!\nFluffy kitten is purring softly!",
                language = language,
                soundTheme = BabySoundTheme.ANIMAL_MEOW
            ),
            BabyContentItem(
                id = "ANI_03",
                title = when (language) {
                    VoiceLanguage.BENGALI -> "ডেজি মিষ্টি গাই গরু"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "डेजी प्यारी गाय"
                    else -> "Daisy The Gentle Cow"
                },
                subtitle = "Animal Sounds",
                activityType = BabyActivityType.ANIMAL_SOUNDS,
                character = BabyCharacter.TEDDY,
                spokenText = when (language) {
                    VoiceLanguage.BENGALI -> "হাম্বা~ হাম্বা! মিষ্টি গাই গরু সবুজ ঘাস খাচ্ছে আর মিষ্টি সুরে ডাকছে: হাম্বা~ হাম্বা!"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "अम्मा~ अम्मा! प्यारी गाय हरी-हरी घास खा रही है और प्यार से बोल रही है: मूं~ मूं!"
                    else -> "Moo~ Moo! Daisy the gentle cow is eating fresh green grass: Moo~ Moo!"
                },
                displayText = "🐮 Moo~ Moo!\nDaisy says hello with a warm smile!",
                language = language,
                soundTheme = BabySoundTheme.ANIMAL_MOO
            ),
            BabyContentItem(
                id = "ANI_04",
                title = when (language) {
                    VoiceLanguage.BENGALI -> "ড্যাফি নাচুনে হাঁস"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "डैफी नटखट बत्तख"
                    else -> "Daffy The Dancing Duck"
                },
                subtitle = "Animal Sounds",
                activityType = BabyActivityType.ANIMAL_SOUNDS,
                character = BabyCharacter.DUCK,
                spokenText = when (language) {
                    VoiceLanguage.BENGALI -> "প্যাক প্যাক! প্যাক প্যাক! ড্যাফি হাঁস পুকুরের জলে ডানা ঝাপটে নাচছে: প্যাক প্যাক!"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "क्वैक क्वैक! क्वैक क्वैक! डैफी बत्तख पानी में खुशी से नाच रही है: क्वैक क्वैक!"
                    else -> "Quack! Quack! Quack! Daffy the yellow duck is splashing joyfully in the pond: Quack Quack!"
                },
                displayText = "🐥 Quack! Quack!\nDaffy is splashing and dancing!",
                language = language,
                soundTheme = BabySoundTheme.ANIMAL_QUACK
            )
        )
    }

    fun getStories(language: VoiceLanguage): List<BabyContentItem> {
        return when (language) {
            VoiceLanguage.BENGALI -> listOf(
                BabyContentItem(
                    id = "STO_BN_01",
                    title = "ছোট মেঘ ও রংধনু",
                    subtitle = "ছোটদের মজার গল্প",
                    activityType = BabyActivityType.STORY,
                    character = BabyCharacter.STAR,
                    spokenText = "এক ছিল ছোট্ট তুলতুলে মেঘ। সে আকাশে ভেসে ভেসে ফুলগুলোকে জল উপহার দিল। আর সূর্যমামা খুশি হয়ে তাকে একটি সাতরঙা সুন্দর রংধনু উপহার দিল! ছোট্ট মেঘ খুশিতে ঝলমলিয়ে উঠল।",
                    displayText = "এক ছিল ছোট্ট তুলতুলে মেঘ। সে আকাশে ভেসে ভেসে ফুলগুলোকে মিষ্টি জল উপহার দিল। আর সূর্যমামা তাকে সাতরঙা রংধনু দিল!",
                    language = VoiceLanguage.BENGALI,
                    soundTheme = BabySoundTheme.GENTLE_CHIME
                )
            )
            VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> listOf(
                BabyContentItem(
                    id = "STO_HI_01",
                    title = "नन्हा बादल और सतरंगी इंद्रधनुष",
                    subtitle = "प्यारी एनिमेटेड कहानी",
                    activityType = BabyActivityType.STORY,
                    character = BabyCharacter.STAR,
                    spokenText = "एक छोटा सा रुई जैसा बादल था। उसने मुस्कुराकर सारे सूखे फूलों को ठंडा पानी पिलाया। सूरज दादा ने खुश होकर उसे प्यारा सतरंगी इंद्रधनुष दिया! नन्हा बादल खुशी से खिलखिला उठा।",
                    displayText = "एक छोटा सा रुई जैसा बादल था। उसने मुस्कुराकर फूलों को पानी दिया और सूरज दादा ने उसे एक प्यारा सतरंगी इंद्रधनुष दिया!",
                    language = VoiceLanguage.HINDI,
                    soundTheme = BabySoundTheme.GENTLE_CHIME
                )
            )
            else -> listOf(
                BabyContentItem(
                    id = "STO_EN_01",
                    title = "The Sleepy Little Cloud",
                    subtitle = "Gentle Animated Story",
                    activityType = BabyActivityType.STORY,
                    character = BabyCharacter.STAR,
                    spokenText = "Once upon a time, high in the soft blue sky, lived a fluffy little cloud named Puffy. Puffy gently sprinkled water drops on the sleepy daisies. The golden sun smiled and painted a bright, colorful rainbow across the sky. Puffy cuddled up and had the sweetest dreams.",
                    displayText = "High in the soft sky lived Puffy the little cloud.\nPuffy gave gentle water drops to daisies,\nand the warm golden sun gave Puffy a beautiful rainbow!",
                    language = VoiceLanguage.ENGLISH,
                    soundTheme = BabySoundTheme.GENTLE_CHIME
                )
            )
        }
    }

    /**
     * Safety advisory guidance for caregivers.
     */
    fun getCaregiverDistressTip(language: VoiceLanguage): String {
        return when (language) {
            VoiceLanguage.BENGALI -> "অভিভাবকদের জন্য পরামর্শ: শিশু যদি একটানা কাঁদতে থাকে, তবে অনুগ্রহ করে খিদে, ভেজা ডায়াপার, ঘরের তাপমাত্রা বা পেটের অস্বস্তি পরীক্ষা করুন। প্রয়োজনবোধে চিকিৎসকের পরামর্শ নিন।"
            VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "अभिभावक ध्यान दें: यदि बच्चा लगातार रो रहा है, तो कृपया भूख, डायपर, कमरे का तापमान, पेट दर्द या बेचैनी की जाँच करें। अधिक परेशानी में तुरंत डॉक्टर से संपर्क करें।"
            else -> "Caregiver Note: If baby continues crying, gently check hunger, a wet diaper, room temperature, colic, or teething discomfort. For persistent distress or fever, please consult your pediatrician."
        }
    }
}
