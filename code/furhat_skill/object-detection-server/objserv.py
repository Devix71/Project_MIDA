import zmq
import numpy as np
import cv2
import time
import json
import torch.nn.functional as F_torch

import torch
import torchvision
import torchvision.transforms as transforms
from torchvision.models.detection import fasterrcnn_resnet50_fpn
from torchvision.transforms import functional as F

def preprocess_image(image):
    # Convert to PIL image and resize
    image = F.to_pil_image(image)
    image = image.convert('RGB')
    new_size = (448, 448)
    image = F.resize(image, new_size)

    # Convert to tensor
    image_tensor = F.to_tensor(image)

    return image_tensor.unsqueeze(0)  # Add batch dimension

def detect_objects(model, image):
    image_tensor = preprocess_image(image)
    with torch.no_grad():
        output = model(image_tensor)
    return output

def extract_patches(image, detections, label_of_interest):
    patches = []
    boxes = detections[0]['boxes']
    labels = detections[0]['labels']

    for idx, box in enumerate(boxes):
        if labels[idx] == label_of_interest:
            xmin, ymin, xmax, ymax = map(int, box.tolist())
            patch = image[ymin:ymax, xmin:xmax]
            patches.append(patch)

    return patches

def transform_patch(patch):
    patch = F.to_pil_image(patch)
    patch = patch.convert('RGB')
    patch = F.resize(patch, (112, 112))
    patch = F.to_tensor(patch)
    return patch.unsqueeze(0)  # Add batch dimension


def classify_patches(patches, model):
    model.eval()
    results = []
    for patch in patches:
        transformed_patch = transform_patch(patch)
        with torch.no_grad():
            output = model(transformed_patch)
        # Applying softmax to convert scores to probabilities
        probabilities = F_torch.softmax(output, dim=1)
        results.append(probabilities)
    return results

def custom_draw(image, detections, classifications, original_size, new_size=(448, 448)):
    boxes = detections[0]['boxes']
    labels = detections[0]['labels']
    scores = detections[0]['scores']

    x_scale = original_size[0] / new_size[0]
    y_scale = original_size[1] / new_size[1]

    for idx, box in enumerate(boxes):
        if scores[idx] >= detection_threshold:
            box = box.cpu().numpy()
            xmin, ymin, xmax, ymax = box
            xmin = xmin * x_scale
            xmax = xmax * x_scale
            ymin = ymin * y_scale
            ymax = ymax * y_scale

            color = (255, 0, 0)
            cv2.rectangle(image, (int(xmin), int(ymin)), (int(xmax), int(ymax)), color, 2)

            label = labels[idx].item()
            classes = ['Map','Flag','Other']
            class_name = classes[label]

            if label == 'Flag' or label == 'Map': 
                classification = classifications.pop(0)
                class_prob = torch.max(classification, 1)
                class_name += f" | Class Flag ({class_prob.item():.2f})"

            cv2.putText(image, class_name, (int(xmin), int(ymin)-10), cv2.FONT_HERSHEY_SIMPLEX, 0.9, color, 2)

    return image

def custom_draw(image, detections, classifications, original_size, new_size=(448, 448)):
    boxes = detections[0]['boxes']
    labels = detections[0]['labels']
    scores = detections[0]['scores']
    flag_label_index = 1 
    x_scale = original_size[0] / new_size[0]
    y_scale = original_size[1] / new_size[1]

    detection_info = []  # List to store information for each detection

    for idx, box in enumerate(boxes):
        if scores[idx] >= detection_threshold:
            box = box.cpu().numpy()
            xmin, ymin, xmax, ymax = box
            xmin = xmin * x_scale
            xmax = xmax * x_scale
            ymin = ymin * y_scale
            ymax = ymax * y_scale

            color = (255, 0, 0)
            cv2.rectangle(image, (int(xmin), int(ymin)), (int(xmax), int(ymax)), color, 2)

            label = labels[idx].item()
            classes = ['Map','Flag','Other']
            class_name = classes[label]
            flag_names = {0: 'Unknown', 1: 'Spanish', 2: 'Russian', 3: 'Swedish'}

            classification_label = ""

            if (label == 1 or label==0) and classifications:
                classification = classifications.pop(0)

                # Adjust the probability of the Unknown class
                classification[:, 0] /= 2.5

                class_prob, class_idx = torch.max(classification, 1)
                classification_label = flag_names[class_idx.item()]
                class_name += f" | {classification_label} ({class_prob.item():.2f})"

            cv2.putText(image, class_name, (int(xmin), int(ymin)-10), cv2.FONT_HERSHEY_SIMPLEX, 0.9, color, 2)

            detection_info.append({'class_name': "Flag", 'classification_label': classification_label})

    return image, detection_info


# Load the fine-tuned Fasterrcnn_resnet50_fpn model
model = fasterrcnn_resnet50_fpn(pretrained=False, num_classes=3)  # Adjust num_classes based on your model

# Load the checkpoint
checkpoint = torch.load('best_object_detection_model.pth', map_location=torch.device('cpu'))

# Load the weights into the model
model.load_state_dict(checkpoint)

# Set the model to evaluation mode
model.eval()


# Load the image classification model
classification_model = torchvision.models.resnet50(pretrained=False, num_classes=4)   # Replace with your actual model class
classification_checkpoint = torch.load('best_image_classification_model.pth', map_location=torch.device('cpu'))
classification_model.load_state_dict(classification_checkpoint)
classification_model.eval()


# Load configuration
with open('launch.json') as f:
  config = json.load(f)
print(config)

# Setup the sockets
context = zmq.Context()

# Open the camera (0 is usually the default camera)
cap = cv2.VideoCapture(0)

# Check if the camera is opened successfully
if not cap.isOpened():
    print("Error: Could not open camera.")
    exit()

# Output results using a PUB socket
context2 = zmq.Context()
outsocket = context2.socket(zmq.PUB)
outsocket.bind("tcp://" + config["Dev_IP"] + ":" + config["detection_exposure_port"])

print('connected, entering loop')
prevset = {}
iterations = 0
detection_period = config["detection_period"]
detection_threshold = config["detection_confidence_threshold"]


def detect(net, img, confidence_threshold):
    #detecting objects
    blob = cv2.dnn.blobFromImage(img,0.00392,(416,416),(0,0,0),True,crop=False)
        
    net.setInput(blob)
    outs = net.forward(outputlayers)

    # get confidence score of algorithm in detecting an object in blob
    class_ids=[]
    confidences=[]
    boxes=[]
    for out in outs:
        for detection in out:
            scores = detection[5:]
            class_id = np.argmax(scores)
            confidence = scores[class_id]
            if confidence > confidence_threshold:
            #onject detected
                center_x= int(detection[0]*width)
                center_y= int(detection[1]*height)
                w = int(detection[2]*width)
                h = int(detection[3]*height)        
                x=int(center_x - w/2)
                y=int(center_y - h/2)
                boxes.append([x,y,w,h]) #put all rectangle areas
                confidences.append(float(confidence)) #how confidence was that object detected and show that percentage
                class_ids.append(class_id) #name of the object tha was detected

    indexes = cv2.dnn.NMSBoxes(boxes,confidences,0.4,0.6)
    return {'indexes':indexes,'boxes':boxes, 'class_ids': class_ids}


def draw(img,res):
    boxes = res['boxes']
    indexes = res['indexes']
    class_ids = res['class_ids']
    font = cv2.FONT_HERSHEY_PLAIN
    for i in range(len(boxes)):
        if i in indexes:
            x,y,w,h = boxes[i]
            label = str(classes[class_ids[i]])
            color = colors[i]
            cv2.rectangle(img,(x,y),(x+w,y+h),color,2)
            cv2.putText(img,label,(x,y+30),font,3,(255,255,0),2)

    return img
 

def formatresult(detections, width, height):
    # Extract boxes, labels, and scores
    boxes = detections[0]['boxes']
    labels = detections[0]['labels']
    scores = detections[0]['scores']
    classes = ['Map','Flag','Other']
    output = []
    for idx in range(boxes.shape[0]):
        box = boxes[idx].cpu().numpy()
        label = labels[idx].item()

        # Normalize the box coordinates
        x, y, w, h = box
        x /= width
        w /= width
        y /= height
        h /= height

        output.append({'item': classes[label], 'bbox': [x, y, w, h]})

    return output


def objectList(detections):
    output = []
    for detection in detections:
        label = detection['item']
        output.append(label)
    return output

def getObjectSet(list):
    set = {}
    for item in list:
        set[item] = True
    return set

def compareSets(set1, set2):
    out = []
    for item in set2.keys():
        if not item in set1:
            out.append('enter_' + item)
    for item in set1.keys():
        if not item in set2:
            out.append('leave_' + item)
    return out

ret, img = cap.read()
if ret:
    original_size = img.shape[1], img.shape[0]
    if iterations % detection_period == 0:
        detections = detect_objects(model, img)
        patches = extract_patches(img, detections, label_of_interest=1)
        classifications = classify_patches(patches, classification_model)
        img2, detection_info = custom_draw(img, detections, classifications, original_size)

        # Format and publish the information
        info_message = json.dumps(detection_info)

        # Print the information for debugging
        print("Publishing:", info_message)

        outsocket.send_string(info_message)

        cv2.imshow("Detection", img2)
        cv2.waitKey(500)  # Wait for a key press to close the window

    iterations += 1

else:
    print("Error: Could not read frame.")

cap.release()
cv2.destroyAllWindows()