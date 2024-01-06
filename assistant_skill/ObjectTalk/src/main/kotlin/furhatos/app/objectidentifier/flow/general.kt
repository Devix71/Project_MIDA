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
import furhatos.flow.kotlin.Furhat


// Flag variables for dialogue logic
var restaurant: String = ""
var poi: Int = 0
var temp: Int = 0
var temper: Int = 0
/**
 * Events used for communication between the thread and the flow.
 */
class EnterEvent(val objects: List<String>): Event()
class LeaveEvent(val objects: List<String>): Event()

data class Speech(
        val language: Language,
        val voice: Voice,
        //val phrases: Phrases
)

data class Persona(
    val name: String,
    val gender: Gender,
    val speech: List<Speech>, // Expecting a List<Speech>
    var active: Boolean = false
) {
    companion object {
        val list = mutableListOf(
            Persona(
                name = "Astrid",
                gender = Gender.FEMALE,
                speech = listOf(
                    Speech(
                        Language.SWEDISH, 
                        PollyVoice(name = "Astrid", language = Language.SWEDISH, gender = Gender.FEMALE)
                    )
                )
            ),
            Persona(
                name = "Tatyana",
                gender = Gender.FEMALE,
                speech = listOf(
                    Speech(
                        Language.RUSSIAN, 
                        PollyVoice(name = "Tatyana", language = Language.RUSSIAN, gender = Gender.FEMALE)
                    )
                )
            ),
            Persona(
                name = "Danielle",
                gender = Gender.FEMALE,
                speech = listOf(
                    Speech(
                        Language.ENGLISH_US, 
                        PollyVoice(name = "Danielle", language = Language.ENGLISH_US, gender = Gender.FEMALE)
                    )
                )
            )
        )
        
    val active: Persona
        get() = list.firstOrNull { it.active } ?: list.first()
    }
}

/** API Key to GPT3 language model. Get access to the API and generate your key from: https://openai.com/api/ **/
val serviceKey = ""

val service = OpenAiService("YOUR_SERVICE_KEY")

class OpenAIChatbot(val description: String, val userName: String, val agentName: String) {

    val service = OpenAiService(serviceKey)

    // Read more about these settings: https://beta.openai.com/docs/introduction
    var temperature = 0.9 // Higher values means the model will take more risks. Try 0.9 for more creative applications, and 0 (argmax sampling) for ones with a well-defined answer.
    var maxTokens = 50 // Length of output generated. 1 token is on average ~4 characters or 0.75 words for English text
    var topP = 1.0 // 1.0 is default. An alternative to sampling with temperature, called nucleus sampling, where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.
    var frequencyPenalty = 0.0 // Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.
    var presencePenalty = 0.6 // Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.

    // Method to get a response using a custom prompt
    fun getResponseWithCustomPrompt(customPrompt: String): String {
        val prompt = "$description\n\n$customPrompt\n$agentName:"
        return generateResponse(prompt)
    }

    // Modified getNextResponse method with an optional custom prompt parameter
    fun getNextResponse(customPrompt: String? = null): String {
        val historyPrompt = if (customPrompt == null) {
            val history = Furhat.dialogHistory.all.takeLast(10).mapNotNull {
                when (it) {
                    is DialogHistory.ResponseItem -> "$userName: ${it.response.text}"
                    is DialogHistory.UtteranceItem -> "$agentName: ${it.toText()}"
                    else -> null
                }
            }.joinToString(separator = "\n")
            "$description\n\n$history\n$agentName:"
        } else {
            "$description\n\n$customPrompt\n$agentName:"
        }
        return generateResponse(historyPrompt)
    }

    // Common method to generate a response
    private fun generateResponse(prompt: String): String {
        println("-----")
        println(prompt)
        println("-----")
        val completionRequest = CompletionRequest.builder()
            .temperature(temperature)
            .maxTokens(maxTokens)
            .topP(topP)
            .frequencyPenalty(frequencyPenalty)
            .presencePenalty(presencePenalty)
            .prompt(prompt)
            .stop(listOf("$userName:"))
            .echo(false)
            .model("gpt-3.5-turbo-instruct")
            .build()
        try {
            val completion = service.createCompletion(completionRequest)
            val response = completion.getChoices().first().text.trim()
            return response
        } catch (e: Exception) {
            println("Problem with connection to OpenAI: " + e.message)
        }
        return "I am not sure what to say"
    }

}

// Function to set the voice
fun Furhat.setPersona(persona: Persona, initial: Boolean = false) {
    val oldPersona = Persona.active

    if (initial || oldPersona.name != persona.name) {
        Persona.list.forEach { it.active = false }
        persona.active = true
    }

    // Access the first Speech object in the list
    val personaSpeech = persona.speech.first()
    voice = personaSpeech.voice
    setInputLanguage(personaSpeech.language)
}

var batchFileProcess: Process? = null

fun runBatchFile() {
    try {
        val batFilePath = "C:\\Users\\Devix\\OneDrive - University of Gothenburg\\FurhatProject\\MIDA\\vision_server.bat"
        val processBuilder = ProcessBuilder("cmd.exe", "/c", batFilePath)

        println("starting process")
        val process = processBuilder.start()
        batchFileProcess = process
        println("process started")

        // Optional: Wait for the process to complete
        process.waitFor()

        println("process running")

        // Optional: Print the output of the process (if any)
        val inputStream = process.inputStream
        inputStream.bufferedReader().forEachLine { println(it) }

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun stopBatchFile() {
    try {
        batchFileProcess?.let {
            println("Stopping batch file process")
            it.destroy()
            println("Batch file process stopped")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
val AskToCheckAgain = state {
    var retryCount = 0
    val maxRetries = 3  // Set a maximum number of retries

    onEntry {
        if (retryCount < maxRetries) {
            furhat.cameraFeed.enable()
            furhat.ask("I didn't quite get that. Let me take another look.")
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
                //goto(AskToCheckAgain)
            }
            val personaName = when {
                firstObject.contains("Russian", ignoreCase = true) -> "Tatyana"
                firstObject.contains("Swedish", ignoreCase = true) -> "Astrid"
                else -> null
            }
        
            personaName?.let { name ->
                val persona = Persona.list.first { it.name == name }
                furhat.setPersona(persona)
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
        runBatchFile()
    }

    onEvent<EnterEvent> { // Objects that enter the view
        val firstObject = it.objects.firstOrNull() ?: ""
        if (firstObject.contains("Unknown", ignoreCase = true)) {
            goto(AskToCheckAgain)
        }
        val personaName = when {
            firstObject.contains("Russian", ignoreCase = true) -> "Tatyana"
            firstObject.contains("Swedish", ignoreCase = true) -> "Astrid"
            else -> null
        }
    
        personaName?.let { name ->
            val persona = Persona.list.first { it.name == name }
            furhat.setPersona(persona)
        }

        if (firstObject.contains("Russian", ignoreCase = true)) {
            goto(Ru_recommend)
        } else if (firstObject.contains("Swedish", ignoreCase = true)) {
            goto(Swe_help)
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
        // Optionally go to some idle state or wait for further instructions
    }
}
/**
 * Main flow that starts the camera feed and awaits events sent from the thread
 */

val Main = state {

    onEntry {
        val persona = Persona.list.first { it.name == "Danielle" }  // Choose the persona by name
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
                goto(Eng_recommend)
            }
            it.text.contains("restaurants", ignoreCase = true) || it.text.contains("cafes", ignoreCase = true) -> {
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
        } else if (restaurant.contains("cafes", ignoreCase = true)) {
            furhat.say("For cafes nearby, I recommend Anna & Adriano and Cafe Sirius.")
        } else {
            furhat.say("As for dining, you might like Sodra Larm Bar & Bistro or Dinner 22.")
        }
        furhat.ask("Would you like to know anything else?")
    }

    onResponse {
        val responseText = it.text.trim()
        when {
            responseText.contains("yes", ignoreCase = true) -> {
                furhat.say("Very well!")
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
                furhat.say("I recommend visiting the Botanical Garden.")
                furhat.say("It takes less than 25 min by tram to get there from here.")
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
                furhat.say("You might also want to visit the Museum of Art, which is within walking distance.")
                furhat.say("The entrance costs 70 Swedish crowns.")
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
        println("value is $poi")
        if (poi == 5) {  // Use == for comparison
            furhat.say("To get to the Botanical Garden, you can take tram line 2 or 7. There are maps in English by my side.")
            goto(Idle)
        } else {
            furhat.say("To get to the Botanical Garden, you can take tram line 2 or 7. There are maps in English by my side.")
            furhat.say("For the Museum of Art, I recommend walking, which takes less than 20 min.")
            goto(Idle)
        }

    }


}

val Eng_recommend = state {
    onEntry {
        when {
            poi == 0 -> {
                poi += 1
                furhat.say("I recommend visiting the Botanical Garden.")
                furhat.say("It takes less than 25 min by tram to get there from here.")
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
                println("Received Answer: $poi")
                furhat.say("You might also want to visit the Museum of Art, which is within walking distance.")
                furhat.say("The entrance costs 70 Swedish crowns.")
                reentry()
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
            poi > 2 && it.text.contains("yes", ignoreCase = true) -> {
                goto(Eng_directions)
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
        if (poi == 5) {  // Use == for comparison
            furhat.say("To get to the Botanical Garden, you can take tram line 2 or 7. There are maps in English by my side.")
            if (restaurant.isNullOrEmpty()) {
                furhat.ask("Would you like to know about places to eat or drink?")
            } else {
                goto(Idle)  // Ensure goto is spelled correctly
            }
        } else {
            furhat.say("To get to the Botanical Garden, you can take tram line 2 or 7. There are maps in English by my side.")
            furhat.say("For the Museum of Art, I recommend walking, which takes less than 20 min.")
            goto(Idle)
        }

    }

    onResponse {
        println("Received Answer: $it.text")
        if (it.text.contains("yes", ignoreCase = true)) {
            goto(Eng_restaurant) // Transition to the next appropriate state
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
        } else {
            furhat.say("Я рекомендую Боско Кафе, У Фонтана и Бар Икры Белуга.")
        }
        furhat.ask("Хотите узнать что-нибудь еще?")
    }

    onResponse {
        val responseText = it.text.trim()
        when {

            temper != 2 && responseText.contains("рекомендуете ", ignoreCase = true) || responseText.contains("Достопримечательности", ignoreCase = true) -> {
                goto(Ru_recommend2)
            }
            else -> {
                furhat.say("Хорошо, дайте мне знать, если передумаете.")
                goto(Idle)
            }
        }
    }

    onResponse {
        furhat.say("Я этого не понял. Пожалуйста, повторите.")
        reentry()
    }
}

val Ru_recommend2 = state {
    onEntry {
        furhat.say("Вы сейчас находитесь на Красной площади. ")
        furhat.say("Я рекомендую посетить Храм Васи́лия Блаже́нного, который был построен в XVI веке.")
        furhat.ask("Хотите узнать что-нибудь еще?")


    }

    onResponse {

        if (it.text.contains("да", ignoreCase = true)) {
            goto(Ru_directions2)

        } else {
            furhat.say("Хорошо, дайте мне знать, если передумаете.")
            goto(Idle)
        }
    }

    onResponse {
        furhat.say("Я этого не понял. Пожалуйста, повторите.")
        reentry()
    }
}

val Ru_directions2 = state {
    onEntry {
        furhat.gesture(Gestures.Thoughtful(duration=2.0, strength=1.0))
        furhat.say("Вход стóит 700 рублей.")
        furhat.say("Я также рекомендую посетить Третьяковскую галерею.")
        furhat.say("Она находится в пределах трех километров отсюда.")
        goto(Idle)

    }


}

val Ru_recommend = state {
    onEntry {
        onEntry {
            furhat.say("Вы сейчас находитесь на Красной площади. ")
            furhat.say("Я рекомендую посетить Храм Васи́лия Блаже́нного, который был построен в XVI веке.")
            furhat.ask("Хотите узнать что-нибудь еще?")
        }

    }

    onResponse {

        if (it.text.contains("да", ignoreCase = true)) {
            goto(Ru_directions)

        } else {
            furhat.say("Хорошо, дайте мне знать, если передумаете.")
            goto(Idle)
        }
    }

    onResponse {
        furhat.say("Я этого не понял. Пожалуйста, повторите.")
        reentry()
    }
}


val Ru_directions = state {
    onEntry {
        furhat.gesture(Gestures.Thoughtful(duration=2.0, strength=1.0))
        furhat.say("Вход стóит 700 рублей.")
        furhat.say("Я также рекомендую посетить Третьяковскую галерею.")
        furhat.say("Она находится в пределах трех километров отсюда.")

        furhat.ask("Хотите узнать о местах, где можно поесть или выпить?")


    }

    onResponse {
        println("Received Answer: $it.text")
        if (it.text.contains("да", ignoreCase = true)) {
            temper =2
            goto(Ru_restaurant) // Transition to the next appropriate state
        }
        else{
            goto(Idle)
        }
    }

    onResponse {
        furhat.say("Я этого не понял. Пожалуйста, повторите.")
        reentry()
    }
}

val Swe_help = state {
    onEntry {
        furhat.say("Du kan ta Flybbussen till centrum.")
        furhat.say("Det kostar 119 kronor.")
    }

    onResponse {
        when {
            it.text.contains("taxi", ignoreCase = true) -> {
                // Handle the keyword
                goto(Swe_taxi)
                // Perform additional actions or go to another state
            }
            else -> {
                goto(Idle)
            }
        }
    }
}

val Swe_taxi = state {
    onEntry {
        furhat.say("Taxi kostar cirka 500 kronor.")
        furhat.say("Vänligen följ taxiskylten.")
    }

    onResponse {
        when {
            it.text.contains("tack", ignoreCase = true) -> {
                furhat.gesture(Gestures.Nod())
                // Handle the keyword
                furhat.say("Varsågod, ha en trevlig resa!")
                furhat.gesture(Gestures.BigSmile(duration=2.0, strength=2.0))
                // Perform additional actions or go to another state
            }
            else -> {
                goto(Idle)
            }
        }
    }
}

