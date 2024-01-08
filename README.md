# Project_MIDA

Project MIDA is a project for the Artificial Intelligence Cognitive Systems master's course at the University of Gothenburg. It involves integrating the Furhat robot with GPT-4 to develop a Multilingual Information Desk Assistant (MIDA) for use in tourism.

## Running the Project

It is necessary to create a new python environment based on the `requirements` files found in the `code/` folder in order to run the code without any issues. 

The steps to create the environment are as such

`conda create --name new_env --file conda-requirements.txt`

`conda activate new_env`

`pip install -r requirements.txt`

Once this is done, all code can be ran as per the subsequent instructions.

### Flag Detection and Classification
`Models.ipynb` contains all the code needed to download the pre-trained models, fine-tune them and test them. It can be ran as is, provided that the `Images` folder is created and populated with the right images in the `data` folder.

### Furhat Skill
In order to properly use MIDA, its skill must be activated from the `code\furhat_skill\ObjectTalk\build\libs\` folder. In case the skill isn't there, it can be recreated by navigating to the  `code\furhat_skill\ObjectTalk` folder and running the command `gradlew shadowJar`, this will build the skill in the aforementioned `libs` directory.

An important mention is that depending on what operating system you are using, you should edit either `vision_server.bash` or `vision_server.bat`. Namely they must contain a path to the **Miniconda bin directory**, the path to the **furhat_skill** folder, your **environment name** for activation and the path to the **objserv.py** file, which can be found at `code\furhat_skill\object-detection-server\objserv.py`.

Using the Furhat skill requires the Furhat sdk, from which the skill ca be easilly loaded into. A valid openAI API key will also be needed to run it. The key must be used within the `code\furhat_skill\ObjectTalk\src\main\kotlin\furhatos\app\objectidentifier\flow\general.kt` file.

## Project Structure
Within the `code/` folder, the project's files can be found. Most notably they are organized as such
`Models.ipynb` is a Jupyter notebook, which shows how the flag detection and classification models have been fine-tuned.
`conda-requirements.txt` and `requirements.txt` which can be used to create a Python environment compatible with this project
`vision_server.bash` and `vision_server.bat` which are used within the Furhat skill for triggering the object detection server automatically.
4 .XML files containing the general structure and concept for the skill's dialogues.
A `furhat_skill` folder which contains the files needed to load the skill and to recreate it. Within it there is the `object-detection-server` folder, which hosts the python files required for running this service, the `launch.json` file which is the configuration file for the server and the fine-tuned models' checkpoints.
Within the `ObjectTalk` folder, the most notable files are the `gradlew` file which is used to build the skill and the `code\furhat_skill\ObjectTalk\src\main\kotlin\furhatos\app\objectidentifier\flow\general.kt` file which contains the skill's logic.


Within the `data/` folder, the dataset used in fine-tuning the models is located, the `Labelling` folder containing a `.csv` file containing the annotated data for the images, while the `Images` folder contains the corresponding images as the name would suggest. Due to Github's size constraints, the files can be downloaded only from here, where they are archived as Images.7z `https://mega.nz/folder/VLpyVIAY#6oNp6DmolOUSsscpkUJVnA` .


The `notes/` folder contains the project diary, with what each of the team members has done for this project and the initial project description and proposal.

The `paper/` folder contains the project report.

## Authorship Statement

The original code for the Furhat skill was provided by Aram Karimi to serve as a starting point for our own skill and to provide the ability to connect to the laptop's camera. It can be found at `https://github.com/AramKarimi/camerafeed-demo/tree/main` 

Code snippets from `https://docs.furhat.io/` were used in developing the Furhat skill.

Besides it, all code is original, specifically created for this project by ourselves.

## Statement of Contribution

This project has been the culmination of a joint effort between Aruna Elentari and Bogdan Laszlo. 
The exclusive responsabilities have been delegated as such: 
* Coding - Bogdan
* Testing - Bogdan
* Final project preparations for submission - Bogdan
* Devising dialogues - Aruna
* Implement Furhat immersion features (gestures, voices and textures) - Aruna
* Organised and planned the project's development - Aruna

Shared responsabilities:
* Writing the report
* Researching for the project
* Coming up with ideas
* Helping each other if need arose
