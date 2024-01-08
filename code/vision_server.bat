@echo off
SET PATH=YourPathTo\Library\bin;%PATH%
cd "ABSOLUTE/PATH/TO/furhat_skill/"
call conda activate <YOUR PYTHON ENV>
python object-detection-server\objserv.py
