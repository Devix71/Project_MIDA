package furhatos.app.objectidentifier.flow

import furhatos.event.Event
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.state
import furhatos.flow.kotlin.*
import furhatos.flow.kotlin.voice.PollyVoice
import furhatos.flow.kotlin.voice.Voice
import furhatos.util.Gender
import furhatos.util.Language
import furhatos.gestures.Gestures
import java.io.File
import com.theokanning.openai.OpenAiService
import com.theokanning.openai.completion.CompletionRequest
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import furhatos.flow.kotlin.DialogHistory
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody


// Flag variables for dialogue logic
var restaurant: String = ""
var poi: Int = 0
var temp: Int = 0
var temper: Int = 0

var user_query: String = ""
var history: String = ""

var chatbot = OpenAIChatbot("You are a friendly and helpful travel assistant. Your name is MIDA. You can speak English, Swedish and Russian and can offer information on Gothenburg and Moscow.", "user", "MIDA")
/**
 * Events used for communication between the thread and the flow.
 */
class EnterEvent(val objects: List<String>): Event()
class LeaveEvent(val objects: List<String>): Event()

data class Speech(
        val language: Language,
        val voice: Voice,
)

data class Persona(
    val name: String,
    val gender: Gender,
    val speech: List<Speech>,
    val texture: String,
    var active: Boolean = false
) {
    companion object {
        val list = mutableListOf(
            Persona(
                name = "Elin",
                gender = Gender.FEMALE,
                speech = listOf(
                    Speech(
                        Language.SWEDISH, 
                        PollyVoice(name = "Elin", language = Language.SWEDISH, gender = Gender.FEMALE)
                    )
                ),
                texture = "Jane"
            ),
            Persona(
                name = "Lucia",
                gender = Gender.FEMALE,
                speech = listOf(
                    Speech(
                        Language.SPANISH_ES, 
                        PollyVoice(name = "Lucia", language = Language.SPANISH_ES, gender = Gender.FEMALE)
                    )
                ),
                texture = "ElinTexture"
            ),
            Persona(
                name = "Tatyana",
                gender = Gender.FEMALE,
                speech = listOf(
                    Speech(
                        Language.RUSSIAN, 
                        PollyVoice(name = "Tatyana", language = Language.RUSSIAN, gender = Gender.FEMALE)
                    )
                ),
                texture = "Isabel"
            ),
            Persona(
                name = "Aria",
                gender = Gender.FEMALE,
                speech = listOf(
                    Speech(
                        Language.ENGLISH_NZ, 
                        PollyVoice(name = "Aria", language = Language.ENGLISH_NZ, gender = Gender.FEMALE)
                    )
                ),
                texture = "anime"
            )
        )
        
    val active: Persona
        get() = list.firstOrNull { it.active } ?: list.first()
    }
}




class OpenAIChatbot(val description: String, val userName: String, val agentName: String) {

    val service = OpenAiService("api-key")

    // Read more about these settings: https://beta.openai.com/docs/introduction
    var temperature = 0.3 // Higher values means the model will take more risks. Try 0.9 for more creative applications, and 0 (argmax sampling) for ones with a well-defined answer.
    var maxTokens = 50 // Length of output generated. 1 token is on average ~4 characters or 0.75 words for English text
    var topP = 1.0 // 1.0 is default. An alternative to sampling with temperature, called nucleus sampling, where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.
    var frequencyPenalty = 0.0 // Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.
    var presencePenalty = 0.6 // Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.

    // Method to get a response using a custom prompt
    fun getResponseWithCustomPrompt(customPrompt: String, relevantPreviousInfo: String? = null): String {
        val prompt = StringBuilder("$description\n\n$customPrompt\n. Please don't use more than 50 tokens. USE 50 TOKENS OR LESS PLEASE.")
        if (relevantPreviousInfo != null) {
            prompt.append(". Keeping in mind the information here: $relevantPreviousInfo, ")
        }
        return generateResponse(prompt.toString())
    }

    // Modified getNextResponse method with an optional custom prompt parameter
    fun getNextResponse(userQuery: String, relevantPreviousInfo: String? = null): String {
        val context = Furhat.dialogHistory.all.takeLast(10).mapNotNull {
            when (it) {
                is DialogHistory.ResponseItem -> "$userName: ${it.response.text}"
                is DialogHistory.UtteranceItem -> "$agentName: ${it.toText()}"
                else -> null
            }
        }.joinToString(separator = "\n")
    
        val prompt = StringBuilder("$description\n\n$context\n")
        if (relevantPreviousInfo != null) {
            prompt.append("Keeping in mind the information here: $relevantPreviousInfo, ")
        }
        prompt.append("$userName: $userQuery\n$agentName:")
    
        return generateResponse(prompt.toString())
    }
    

    // Common method to generate a response
    private fun generateResponse(prompt: String): String {
        println("-----")
        println(prompt)
        println("-----")
    
        val jsonObject = JSONObject().apply {
            put("model", "gpt-4")
            put("temperature", temperature)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", description)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            put("messages", messages)
        }
    
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)
    
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer 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") // Replace with your API key
            .post(requestBody)
            .build()
    
            return try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
        
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody ?: "")
                    val choicesArray = jsonResponse.getJSONArray("choices")
                    if (choicesArray.length() > 0) {
                        val firstChoice = choicesArray.getJSONObject(0)
                        val messageObject = firstChoice.getJSONObject("message")
                        messageObject.getString("content").trim()
                    } else {
                        "No response found"
                    }
                }
            } catch (e: IOException) {
                println("Problem with connection to OpenAI: " + e.message)
                "I am not sure what to say"
            }
    }
}

// Function to set the voice
fun Furhat.setPersona(persona: Persona, initial: Boolean = false) {
    val oldPersona = Persona.active

    if (initial || oldPersona.name != persona.name) {

        setTexture(persona.texture)  // Setting the texture 

        Persona.list.forEach { it.active = false }
        persona.active = true
    }


    val personaSpeech = persona.speech.first()
    voice = personaSpeech.voice
    setInputLanguage(personaSpeech.language)
}

var process: Process? = null

fun runBatchFile() {
    val isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows")
    val scriptPath = if (isWindows) {
        "code/vision_server.bat"
    } else {
        "code/vision_server.bash"
    }

    try {
        val processBuilder = if (isWindows) {
            ProcessBuilder("cmd.exe", "/c", scriptPath)
        } else {
            ProcessBuilder("/bin/bash", scriptPath)
        }

        println("Starting process...")
        process = processBuilder.start()
        println("Process started.")

        // Wait for the process to complete
        process?.waitFor()

        println("Process running...")

        // Print the output of the process (if any)
        process?.inputStream?.bufferedReader()?.forEachLine { println(it) }

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun stopScript() {
    try {
        process?.let {
            println("Stopping script process...")
            it.destroy()
            println("Script process stopped.")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
val AskToCheckAgain = state {
    var retryCount = 0
    val maxRetries = 3  // Maximum number of retries

    onEntry {
        if (retryCount < maxRetries) {
            furhat.cameraFeed.enable()
            furhat.say("I didn't quite get that. Let me take another look.")
            furhat.say(async = true) {
                +Gestures.GazeAway
                random {
                    +"Let's see"
                    +"Let me think"
                    +"Wait a second"
                }
            }
            runBatchFile()
            retryCount++
        } else {
            furhat.say("I'm still having trouble understanding.")
            goto(Idle)
        }
    }
    
        onEvent<EnterEvent> { // Objects that enter the view
            val firstObject = it.objects.firstOrNull() ?: ""
            if (firstObject.contains("Unknown", ignoreCase = true)) {
                reentry()

            }
            val personaName = when {
                firstObject.contains("Russian", ignoreCase = true) -> "Tatyana"
                firstObject.contains("Swedish", ignoreCase = true) -> "Elin"
                firstObject.contains("Spanish", ignoreCase = true) -> "Lucia"
                else -> null
            }
        
            personaName?.let { name ->
                val persona = Persona.list.first { it.name == name }
                furhat.setPersona(persona)
            }
            if (firstObject.contains("Russian", ignoreCase = true)) {
                chatbot = OpenAIChatbot("Вы дружелюбный и отзывчивый помощник в путешествии. Ваше имя МИДА. Вы можете говорить на английском и русском языках и можете предоставить информацию о Москве и том, что вы находитесь на Красной площади.", "user", "MIDA")
                goto(Ru_recommend)
            } else if (firstObject.contains("Swedish", ignoreCase = true)) {
                chatbot = OpenAIChatbot("Du är en vänlig och hjälpsam reseassistent. Ditt namn är MIDA. Du kan engelska och svenska och kan erbjuda information om Göteborg. Du befinner dig på Landvetter flygplats.", "user", "MIDA")
                goto(Swe_help)
            }else if (firstObject.contains("Spanish", ignoreCase = true)) {
                chatbot = OpenAIChatbot("Eres un asistente de viaje amigable y servicial. Tu nombre es MIDA. Sabes inglés y español y puedes ofrecer información sobre Barcelona. Estás en el Aeropuerto de El Prat de Llobregat.", "user", "MIDA")
                goto(Es_help)
            }
        
            if (it.objects.size > 1) {
                furhat.say("You showed me multiple objects: ${it.objects.joinToString(" and ")}")
            } else {
                println("Received EnterEvent with object: $firstObject")
                furhat.say("Oh cool, that is a $firstObject")
            }
        }
    
        onExit {
            furhat.cameraFeed.disable()
        }
    }


val FlagDetection = state {
    onEntry {
        furhat.cameraFeed.enable()
        furhat.say("Let me take a look at it")
        furhat.say(async = true) {
            +Gestures.GazeAway
            random {
                +"Let's see"
                +"Let me think"
                +"Wait a second"
            }
        }
        runBatchFile()
    }

    onEvent<EnterEvent> { // Objects that enter the view
        val firstObject = it.objects.firstOrNull() ?: ""
        if (firstObject.contains("Unknown", ignoreCase = true)) {
            goto(AskToCheckAgain)
        }
        val personaName = when {
            firstObject.contains("Russian", ignoreCase = true) -> "Tatyana"
            firstObject.contains("Swedish", ignoreCase = true) -> "Elin"
            firstObject.contains("Spanish", ignoreCase = true) -> "Lucia"
            else -> null
        }
    
        personaName?.let { name ->
            val persona = Persona.list.first { it.name == name }
            furhat.setPersona(persona)
        }

        if (firstObject.contains("Russian", ignoreCase = true)) {
            chatbot = OpenAIChatbot("Вы дружелюбный и отзывчивый помощник в путешествии. Ваше имя МИДА. Вы можете говорить на английском, шведском и русском языках и можете предоставить информацию о Москве и том, что вы находитесь на Красной площади.", "user", "MIDA")
            goto(Ru_recommend)
        } else if (firstObject.contains("Swedish", ignoreCase = true)) {
            chatbot = OpenAIChatbot("Du är en vänlig och hjälpsam reseassistent. Ditt namn är MIDA. Du kan engelska, svenska och ryska och kan erbjuda information om Göteborg. Du befinner dig på Landvetter flygplats.", "user", "MIDA")
            goto(Swe_help)
        }else if (firstObject.contains("Spanish", ignoreCase = true)) {
            chatbot = OpenAIChatbot("Eres un asistente de viaje amigable y servicial. Tu nombre es MIDA. Sabes inglés y español y puedes ofrecer información sobre Barcelona. Estás en Barcelona.", "user", "MIDA")
            goto(Es_help)
        }
    
        if (it.objects.size > 1) {
            furhat.say("You showed me multiple objects: ${it.objects.joinToString(" and ")}")
        } else {
            println("Received EnterEvent with object: $firstObject")
            furhat.say("Oh cool, that is a $firstObject")
        }
    }

    onExit {
        furhat.cameraFeed.disable()
    }
}

val Idle = state {
    onEntry {
        furhat.gesture(Gestures.BigSmile(duration=2.0, strength=2.0))
        furhat.say("If you need anything else, just ask.")
    }
}

val Swe_Idle = state {
    onEntry {
        furhat.gesture(Gestures.BigSmile(duration=2.0, strength=2.0))
        furhat.say("Om du behöver något annat är det bara att fråga.")
        
    }
}

val Ru_Idle = state {
    onEntry {
        furhat.gesture(Gestures.BigSmile(duration=2.0, strength=2.0))
        furhat.say("Если вам нужно что-то еще, просто спросите.")
        
    }
}
val Es_Idle = state {
    onEntry {
        furhat.gesture(Gestures.BigSmile(duration=2.0, strength=2.0))
        furhat.say("Si necesitas algo más, solo pregunta")
        
    }
}
/**
 * Main flow that starts the camera feed and awaits events sent from the thread
 */

val Main = state {

    onEntry {

        val persona = Persona.list.first { it.name == "Aria" }  // Choose the persona by name
        furhat.setPersona(persona)

        furhat.say("Hello, I am MIDA. Welcome to the visitor center!")  // Setting Furhat's voice to English voice
        furhat.ask("How can I help you?")

    }

    onResponse {
        when {
            it.text.contains("language", ignoreCase = true) -> {
                furhat.say("Great, please place the flag in front of me.")
                goto(FlagDetection)
            }
            it.text.contains("recommend", ignoreCase = true) || it.text.contains("sightseeing", ignoreCase = true) -> {
                chatbot = OpenAIChatbot("You are a friendly and helpful travel assistant. Your name is MIDA. You can speak English and Swedish and can offer information on Gothenburg and you are located at Götaplatsen.", "user", "MIDA")
                user_query = it.text
                goto(Eng_recommend)
            }
            it.text.contains("restaurant", ignoreCase = true) || it.text.contains("cafe", ignoreCase = true) -> {
                chatbot = OpenAIChatbot("You are a friendly and helpful travel assistant. Your name is MIDA. You can speak English and Swedish and can offer information on Gothenburg and you are located at Götaplatsen.", "user", "MIDA")
                restaurant = it.text
                goto(Eng_restaurant)
            }
            else -> {
                furhat.say("Okay, let me know if you change your mind.")
                goto(Idle)
            }
        }
    }

    onResponse {
        furhat.say("I didn't understand that. Please repeat.")
        reentry()
    }
}


val Eng_restaurant = state {
    onEntry {
        if (temper == 2) {
            furhat.ask("How else can I help you?")
        } else {

            val response = chatbot.getResponseWithCustomPrompt(restaurant)
            furhat.say(response)
        }
        furhat.ask("Would you like to know anything else?")
    }

    onResponse {
        val responseText = it.text.trim()
        user_query = responseText
        when {
            responseText.contains("yes", ignoreCase = true) -> {
                val response = chatbot.getResponseWithCustomPrompt(responseText)
                furhat.say(response)
                temper = 2
                reentry()
            }
            responseText.contains("recommend", ignoreCase = true) || responseText.contains("sightseeing", ignoreCase = true) -> {
                goto(Eng_recommend2)
            }
            else -> {
                furhat.say("Okay, let me know if you change your mind.")
                goto(Idle)
            }
        }
    }

    onResponse {
        furhat.say("I didn't understand that. Please repeat.")
        reentry()
    }
}

val Eng_recommend2 = state {
    onEntry {
        when {
            poi == 0 -> {
                poi += 1
                val response = chatbot.getResponseWithCustomPrompt(user_query)
                history += response
                furhat.say(response)
                furhat.ask("Would you like to know anything more?")
            }
            poi < 2 -> {
                poi += 1
                furhat.ask("Would you like to know anything more?")
            }
            else -> {
                furhat.ask("Would you like assistance with getting there?")
            }
        }

    }

    onResponse {
        when {
            poi < 2 && it.text.contains("yes", ignoreCase = true) -> {
                poi += 1
                println("Received Answer: $poi")
                user_query = it.text.trim()
                val response = chatbot.getResponseWithCustomPrompt(user_query, history)
                history += response
                furhat.say(response)
                reentry()
            }
            poi == 6 -> {
                if (it.text.contains("no", ignoreCase = true)) {
                    furhat.say("Okay, let me know if you change your mind.")
                    goto(Idle)
                } else {
                    goto(Eng_directions2)
                }
            }
            poi >= 2 && it.text.contains("yes", ignoreCase = true) -> {
                goto(Eng_directions2)
            }
            else -> {
                furhat.say("Okay, let me know if you change your mind.")
                goto(Idle)
            }
        }
    }

    onResponse {
        furhat.say("I didn't understand that. Please repeat.")
        reentry()
    }
}

val Eng_directions2 = state {
    onEntry {
        
        furhat.gesture(Gestures.Thoughtful(duration=3.0, strength=3.0))
        furhat.say(async = true) {
            +Gestures.GazeAway
            random {
                +"Let's see"
                +"Let me think"
                +"Wait a second"
            }
        }
        println("value is $poi")

            val response = chatbot.getResponseWithCustomPrompt("Can you give me directions on how to get to the following places from Götaplatsen?", history)

            furhat.say(response)

            goto(Idle)


    }


}

val Eng_recommend = state {
    onEntry {
        when {
            poi == 0 -> {
                poi += 1
                val response = chatbot.getResponseWithCustomPrompt(user_query)
                history += response
                furhat.say(response)
                furhat.ask("Would you like to know anything more?")
            }

            else -> {
                poi += 1
                furhat.ask("Would you like assistance with getting there?")
            }
        }

    }

    onResponse {
        when {
            poi == 1 && it.text.contains("yes", ignoreCase = true) -> {
                println("Received Answer: $poi")
                val response = chatbot.getResponseWithCustomPrompt(it.text.trim())
                history += response
                furhat.say(response)
                reentry()
            }
            poi >= 1 && it.text.contains("yes", ignoreCase = true) -> {
                goto(Eng_directions)
            }
            poi == 1 && it.text.contains("no", ignoreCase = true) && restaurant.isNullOrEmpty() -> {
                poi = 5
                reentry()
            }

            poi == 6 -> {
                if (it.text.contains("no", ignoreCase = true)) {
                    furhat.say("Okay, let me know if you change your mind.")
                    goto(Idle)
                } else {
                    goto(Eng_directions)
                }
            }
            poi > 1 && it.text.contains("no", ignoreCase = true) && restaurant.isNullOrEmpty() -> {
                furhat.say("I'm glad to have been of service")
                goto(Idle)
            }

            else -> {
                furhat.say("Okay, let me know if you change your mind.")
                goto(Idle)
            }
        }
    }

    onResponse {
        furhat.say("I didn't understand that. Please repeat.")
        reentry()
    }
}


val Eng_directions = state {
    onEntry {
        furhat.gesture(Gestures.Thoughtful(duration=3.0, strength=3.0))
        println("value is $poi")
        furhat.say(async = true) {
            +Gestures.GazeAway
            random {
                +"Let's see"
                +"Let me think"
                +"Wait a second"
            }
        }
        val response = chatbot.getResponseWithCustomPrompt("Can you please give me directions on how to get to the following places from Götaplatsen?", history)
        furhat.say(response)
            if (restaurant.isNullOrEmpty()) {
                furhat.ask("Would you like to know about places to eat or drink?")
            } else {
                goto(Idle)
            }
        }

    

    onResponse {
        println("Received Answer: $it.text")
        if (it.text.contains("yes", ignoreCase = true)) {
            restaurant = "reccomend place where I can eat or drink based on:"+it.text.trim()
            goto(Eng_restaurant) 
        }
        else{
            goto(Idle)
        }
    }

    onResponse {
        furhat.say("I didn't understand that. Please repeat.")
        reentry()
    }
}

val Ru_restaurant = state {
    onEntry {
        if (temper == 2) {
            
            furhat.ask("Как еще я могу вам помочь?")
        } 
        furhat.ask("Что бы вы хотели узнать?")
    }

    onResponse {
        val responseText = it.text.trim()
        when {

            temper != 2 && responseText.contains("рекомендуете ", ignoreCase = true) || responseText.contains("Достопримечательности", ignoreCase = true) -> {
                user_query = responseText
                goto(Ru_recommend2)
            }
            temper != 2 &&  !responseText.contains("нет ", ignoreCase = true)-> {
                furhat.say(async = true) {
                    +Gestures.GazeAway
                    random {
                        +"Давайте посмотрим"
                        +"Дай мне подумать"
                        +"Подожди секунд"
                    }
                }
                val response = chatbot.getResponseWithCustomPrompt(responseText)
                furhat.say(response)
                temper = 2
                reentry()
            }
            else -> {
                furhat.say(async = true) {
                    +Gestures.GazeAway
                    random {
                        +"Давайте посмотрим"
                        +"Дай мне подумать"
                        +"Подожди секунд"
                    }
                }
                
                furhat.say("Хорошо, дайте мне знать, если передумаете.")
                goto(Ru_Idle)
            }
        }
    }

    onResponse {
        furhat.say("Я этого не понялa. Пожалуйста, повторите.")
        reentry()
    }
}

val Ru_recommend2 = state {
    onEntry {
        history = ""
        furhat.say(async = true) {
            +Gestures.GazeAway
            random {
                +"Давайте посмотрим"
                +"Дай мне подумать"
                +"Подожди секунд"
            }
        }
        val response = chatbot.getResponseWithCustomPrompt(user_query)
        furhat.say(response)
        history+=response

        furhat.ask("Хотите узнать что-нибудь еще?")

    }

    onResponse {

        if (it.text.contains("да", ignoreCase = true)) {
            goto(Ru_directions2)

        } else {
            furhat.say("Хорошо, дайте мне знать, если передумаете.")
            goto(Ru_Idle)
        }
    }

    onResponse {
        furhat.say("Я этого не понялa. Пожалуйста, повторите.")
        reentry()
    }
}

val Ru_directions2 = state {
    onEntry {
        furhat.gesture(Gestures.Thoughtful(duration=2.0, strength=1.0))
        furhat.say(async = true) {
            +Gestures.GazeAway
            random {
                +"Давайте посмотрим"
                +"Дай мне подумать"
                +"Подожди секунд"
            }
        }
        val response = chatbot.getResponseWithCustomPrompt("Предложите более подробную информацию о следующем", history)
        furhat.say(response)
        goto(Ru_Idle)

    }


}

val Ru_recommend = state {
    onEntry {

            furhat.say("Хотите узнать что-нибудь еще?")
            furhat.say(async = true) {
                +Gestures.GazeAway
                random {
                    +"Давайте посмотрим"
                    +"Дай мне подумать"
                    +"Подожди секунд"
                }
            }
            val response = chatbot.getResponseWithCustomPrompt("Дать туристический обзор Красной площади и окрестностей.")
            furhat.say(response)
            furhat.ask("Хотите узнать что-нибудь еще?")


    }

    onResponse {

        if (it.text.contains("да", ignoreCase = true)) {
            user_query = it.text.trim()
            goto(Ru_directions)

        } else {
            furhat.say("Хорошо, дайте мне знать, если передумаете.")
            goto(Ru_Idle)
        }
    }

    onResponse {
        furhat.say("Я этого не понялa. Пожалуйста, повторите.")
        reentry()
    }
}


val Ru_directions = state {
    onEntry {
        furhat.gesture(Gestures.Thoughtful(duration=2.0, strength=1.0))
        furhat.say(async = true) {
            +Gestures.GazeAway
            random {
                +"Давайте посмотрим"
                +"Дай мне подумать"
                +"Подожди секунд"
            }
        }
        val response = chatbot.getResponseWithCustomPrompt(user_query)
        furhat.say(response)

        furhat.ask("Хотите узнать о местах, где можно поесть или выпить?")


    }

    onResponse {
        println("Received Answer: $it.text")
        if (it.text.contains("да", ignoreCase = true)) {
            temper =2
            goto(Ru_restaurant)
        }
        else{
            goto(Ru_Idle)
        }
    }

    onResponse {
        furhat.say("Я этого не понялa. Пожалуйста, повторите.")
        reentry()
    }
}

val Swe_help = state {
    onEntry {
        val response = chatbot.getResponseWithCustomPrompt("Hur tar jag mig till Göteborg centrum?")
        furhat.say(response)
        furhat.ask("add how to ask about taxi")
    }

    onResponse {
        when {
            it.text.contains("taxi", ignoreCase = true) -> {
                
                goto(Swe_taxi)
               
            }
            else -> {
                goto(Swe_Idle)
            }
        }
    }
}

val Swe_taxi = state {
    onEntry {
        val response = chatbot.getResponseWithCustomPrompt("Hur mycket kostar en taxi härifrån till centrum?")
        furhat.say(response)
    }

    onResponse {
        when {
            it.text.contains("tack", ignoreCase = true) -> {
                furhat.gesture(Gestures.Nod())
                
                furhat.say("Varsågod, ha en trevlig resa!")
                furhat.gesture(Gestures.BigSmile(duration=2.0, strength=2.0))
               
            }
            else -> {
                goto(Swe_Idle)
            }
        }
    }
}
val Es_help = state {
    onEntry {
        furhat.ask("¡Bienvenido a Barcelona! ¿Le puedo ayudar en algo?")

    }

    onResponse {
        when {
            it.text.contains("plaza", ignoreCase = true) -> {
                furhat.say(async = true) {
                    +Gestures.GazeAway
                    random {
                        +"Vamos a ver"
                        +"Déjame pensar"
                        +"Espera un segundo"
                    }
                }
                val response = chatbot.getResponseWithCustomPrompt(it.text.trim())
                furhat.say(response)
                history+=response
                
                goto(Es_directions)
                
            }
            else -> {
                goto(Es_Idle)
            }
        }
    }
}

val Es_directions = state {
    onEntry {
        furhat.ask("¿Quieres sugerencias sobre cómo llegar?")

    }

    onResponse {
        when {
            it.text.contains("Sí", ignoreCase = true) -> {
                furhat.gesture(Gestures.Nod())
                val response = chatbot.getResponseWithCustomPrompt("Como llegar a este lugar", history)
                furhat.say(response)
                history+=response

                furhat.gesture(Gestures.BigSmile(duration=2.0, strength=2.0))

                goto(Es_info)
                
            }
            else -> {
                goto(Es_Idle)
            }
        }
    }
}
val Es_info = state {
    onEntry {
        furhat.ask("¿Quieres saber sobre las tarifas para llegar?")

    }

    onResponse {
        when {
            it.text.contains("Sí", ignoreCase = true) -> {
                val response = chatbot.getResponseWithCustomPrompt("¿Cuánto costaría esto?", history)
                furhat.say(response)

                
                goto(Es_Idle)
                
            }
            else -> {
                goto(Es_Idle)
            }
        }
    }
}


