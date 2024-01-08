# Project MIDA: Diary

## Group work

### Group meetings

- **17.11.23, 40min:** Came up with an idea for the project.
- **24.11, 1hr:** Brainstorming, downloaded the Furhat SDK, played with Furhat.
- **27.11, 1hr:** Answered Simon's project questions, gathered resources, clarified the idea to adapt to a specific task (information desk).
- **28.11, 1.5hrs:** Reviewed and commented on the individual work, came up with the project name (MIDA), outlined the project process.
- **01/12, 1hr:** Created a Github page. Split the initial tasks: Bogdan to gather datasets (people, maps, flags), look for an object detection model, flag classification model; Aruna to acquaint herself with Furhat, start writing intro on Furhat and GPT.
- **11/12, 1hr:** Updated each other on the individual progress. Bogdan walked Aruna through his code for the models, and answered her questions on Furhat's skills, etc. Got in touch with Aram. Aruna shared the Project MIDA: paper doc skeleton with Bogdan, the team divided responsibilities for specific doc sections.
- **14.12, 1hr:** Group call with Aram: shared our project idea with Aram, narrowed down the scope (four languages: English, Spanish, Swedish, Russian; using available skills rather than create new ones in general), clarified the dialogue structure (user starts the conversation in English and then asks MIDA to switch to the language of their choice), confirmed that Furhat can direct its gaze to the auditory input, considered including a multi-agent dialogue system (Aram suggested a case of a family of three with a small child), went over the three main aspects of the project (image detection/classification, Furhat skills for dialogue and integration with GPT), confirmed that MIDA will start speaking in the language of the flag that it's been shown, considered potentially showcasing the actual robot during the final presentation, set up the next meeting on December 21st. 
- **20.12, 1.5hrs:** Bogdan updated Aruna on his work with the object detection model; we sketched out the graph for connecting the model to GPT and Furhat; prepared questions for the second call with Aram; Aruna showed the start of the Furhat dialogue; Bogdan experimented with showing GPT-4 images of places of interest (Barcelona, Gothenburg) and the maps to get specific answers from GPT-4, such as directions and cafe recommendations.
- **21.12, 45min:** Second call with Aram. Bogdan shared the results of the object detection model with Aram; briefly discussed the dialogue creation; discussed how to connect different parts of the model (object detection, Furhat, GPT), considered using Furhat remote API, and set up the third meeting.
- **03.01, 1hr:** Updated each other on the progress, sent an email to Aram.
- **07.01, 1hr:** Bogdan walked Aruna through the GPT/Furhat/object detection model integration, showed dialogues in English with MIDA, and flag detection in Swedish and Russian; the team outlined the next steps to finish the project on time.

## Individual work

## Aruna

- **27.11, 1.5hrs:** Drafted a project summary, reviewed the Furhat documentation, watched a 20-min video by Skantze, co-founder of Furhat Robotics, on Furhat/LLM integration.
- **28.11, 1hr:** Worked further on the project proposal, reviewed the resources.
- **29.11, 2.5hrs:** Improved the project proposal, read the paper by Abbo and Belpaeme (2023), started the diary.
- **30.11, 1.5hrs:** Improved the project proposal, reviewed Q&As.
- **4.12, 30 min:** Started interacting with Furhat.
- **11.12, 1.5hrs:** Further interacted with Furhat, started a draft of the project paper, and gathered more papers on the topic.
- **13.12, 1hr:** Prepared questions for the meeting with Aram on 14.12, read Simon's comments from the project discussion seminar, added more papers to read to the reference list.
- **19.12, 1hr:** Worked on introduction and on describing the user-MIDA interaction scenario in detail (Project MIDA: paper).
- **20.12, 1.5hrs:** Wrote down a more detailed dialogue interaction flow for single user and multiple users (English, other languages), posted notes on canvas.
- **21.12, 2hrs:** Started building and testing Blockly skills, chose the character and voices.
- **22.12, 2.5hrs:** Clarified the goal, wrote the first user story, created and tested the initial simple Blockly dialogue in English and Spanish.
- **26.12, 6.5hrs:** Created airport dialogues in Swedish and Russian; further improved Spanish and Swedish airport dialogues, added error correction, character and voice, attending to user, more nuanced facial expressions; watched most of the Furhat Robotics Tutorial on Blockly; created an initial prompt for GPT.
- **27.12, 5hrs:** Further improved dialogue 1 for Spanish and Swedish (when the user leaves and reenters), sent a bug report to Furhat; uploaded the finished scripted dialogue 1 (airport) in Swedish and Spanish on github; created scripted dialogue 2 in Russian (Red Square).
- **28.12, 4hrs:** 1hr to improve the scripted Swedish dialogue (Jane mask, more gestures, added greeting) and the Spanish dialogue (greeting, gestures), uploaded on github; 30 min to work on the Russian dialogue, document bugs; 1.5hrs: created a new scripted dialogue (2) at the Gothenburg Visitor Center in English; wrote down more about the concrete goal in the paper draft.
- **30.12, 1hr:** Improved Dialogue 2 (English, Gothenburg Visitor Center) by adding recommendations for cafes, directions, more gestures and waiting times.
- **02.01, 30 min:** Reviewed, tested and uploaded on github the English dialogue 2, added more to the paper draft regarding dialogues.
- **03.01, 30 min:** Improved, tested and uploaded on github the Russian dialogue.
- **07.01, 8hrs:** Wrote more on the paper abstract and intro (concrete goals), literature, etc; finished the introduction; wrote down abstract and conclusions; sketched out a high-level diagram of MIDA.

## Bogdan
Overall - `Models.ipynb` + `Furhat_skill` + Dataset
- **27.11:** Researched potential leads for submitting project proposal.
- **30.11:** Planned the dataset’s structure, started gathering and annotating suitable images for it.
- **8.12:** Initial build for flag detection and flag classification model fine-tuning.
- **16.12:** Working, fully featured build for flag detection and flag classification + dataset expansion.
- **17.12:** Finished all major changes to the model + dataset expansion.
- **4.01:** Began work on implementing flag detection/classification and dialogues within the Furhat skill.
- **6.01:** Automated flag detection/classification system + full GPT implementation.
- **7.01:** Dataset expansion and model retraining, Furhat skill bugfixes and flavor improvements (textures and gestures added for extra immersion).
- **8.01:** Preparing the project for submission and writing documentation