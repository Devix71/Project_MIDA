package furhatos.app.objectidentifier

import furhatos.app.objectidentifer.getConnectedSocket
import furhatos.app.objectidentifier.flow.EnterEvent
import furhatos.app.objectidentifier.flow.LeaveEvent
import furhatos.app.objectidentifier.flow.Main
import furhatos.event.EventSystem
import furhatos.skills.Skill
import furhatos.flow.kotlin.*
import furhatos.util.CommonUtils
import org.zeromq.ZMQ
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import zmq.ZMQ.ZMQ_SUB
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


data class DetectedObject(
    val class_name: String,
    val classification_label: String
)

val logger = CommonUtils.getRootLogger()
// val objserv = "tcp://<Local ip of dev machine>:9999" //The TCP socket of the object server
val objserv = "tcp://127.0.0.1:9999" //The TCP socket of the object server


val subSocket: ZMQ.Socket = getConnectedSocket(ZMQ.SUB, objserv) // Socket of the object server
 // Subscribe to all messages
val enter = "enter_" //Objects that enter the view start with this string
val leave = "leave_" //Objects that leave the view start with this string

/**
 * Parses a message from the object server, turns the message into a list of objects.
 */
fun getObjects(message: String): List<String> {
    val gson = Gson()
    val listType = object : TypeToken<List<DetectedObject>>() {}.type
    val detectedObjects: List<DetectedObject> = gson.fromJson(message, listType)

    return detectedObjects.map { "${it.class_name} ${it.classification_label}" }
}


/**
 * Function that starts a thread which continuously polls the object server.
 * Based on what is in the message will either send:
 *  - EnterEvent, for objects coming into view.
 *  - LeaveEvent, for objects going out of view.
 *
 *  These events can be caught in the flow (Main), and be responded to.
 */
fun startListenThread() {
    GlobalScope.launch { // launch a new coroutine in background and continue
        logger.warn("LAUNCHING COROUTINE")
        subSocket.subscribe("")
        while (true) {
            val message = subSocket.recvStr()
            logger.warn("got: $message")

            if (message.isNullOrEmpty()) {
                logger.warn("Received empty or null message")
                continue  // Skip this iteration if message is null or empty
            }

            // Trigger an event with the list of detected objects
            EventSystem.send(EnterEvent(getObjects(message)))
            // Remove the if conditions if you are not using 'enter_' or 'leave_' prefixes
        }
    }
}



class ObjectIdentifierSkill : Skill() {
    override fun start() {
        startListenThread()
        Flow().run(Main)
    }
}

fun main(args: Array<String>) {
    Skill.main(args)
}
