import os
import subprocess

import cv2
import json 
import time
import requests 
import numpy as np
import matplotlib #import pyplot as plt

from matplotlib import colors
from matplotlib import pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
from base64 import b64encode, b64decode
from kafka import KafkaConsumer, KafkaProducer

topic_receive = "users"
topic_send = "outroTopico"
topic_log = "p3g2-log"

consumer = KafkaConsumer(topic_receive, bootstrap_servers=['192.168.160.80:39092'])
producer = KafkaProducer(bootstrap_servers='192.168.160.80:39092')
log = {}

def receiveMsg():

    for kafka_msg in consumer:
        # Read Json message received from kafka
        json_msg = json.loads(kafka_msg.value.decode())

        try:
            if json_msg['fileType'] == 'image':
                name = json_msg['fileName']

                log = {'file' : 'image'}

                # Make GET request to the API
                api_url = "http://192.168.160.73:8080/api/image/downloadPython"
                data = {'id': json_msg['id']}
                resp = requests.get(url=api_url, params=data)

                if resp.status_code != 200:
                    print(resp.text)
                    raise Exception
                
                with open(name, 'wb') as image:
                    image.write(resp.content)

                picture = cv2.imread(name, -1)

                print("Filtering image")
                clock = time.clock()
                output, log = filter(picture, log)
                process_time = time.clock() - clock

                cv2.imwrite(name,output)

                # Make POST request to the API
                api_url = "http://192.168.160.73:8080/api/image/uploadPython"
                files = {'file': (name, open(name,'rb'), 'image/jpg')}

                resp = requests.post(url=api_url, files=files, data=data)

                #os.remove(name)

                # Sending message to kafka
                msg = {
                    'fileType' : 'image',
                    'id' : json_msg['id'],
                    'processTime' : process_time
                }

                msg = json.dumps(msg)
                producer.send(topic_send, value=msg.encode())

                log = json.dumps(log)
                producer.send(topic_log, log.encode())

            elif json_msg['fileType'] == 'video':
                video_name = json_msg['fileName']   

                # Make a GET request to the API
                api_url = "http://192.168.160.73:8080/api/video/pythonDownload"
                
                data = {'id': json_msg['id']}

                resp = requests.get(url=api_url, params=data)

                if resp.status_code != 200:
                    print(resp.text)
                    raise Exception

                with open('tmp_videos/'+video_name, 'wb') as video:
                    video.write(resp.content)
                
                video_capture = cv2.VideoCapture('tmp_videos/'+video_name)
                
                fps = int(video_capture.get(cv2.CAP_PROP_FPS))
                size = (int(video_capture.get(cv2.CAP_PROP_FRAME_WIDTH)),
                        int(video_capture.get(cv2.CAP_PROP_FRAME_HEIGHT)))

                out = cv2.VideoWriter('tmp_videos/tmp.avi', cv2.VideoWriter_fourcc(*'XVID'), fps, size, 1)

                print("Filtering Video.")
                clock = time.clock()

                while video_capture.isOpened():
                    success, frame = video_capture.read()

                    if not success:
                        print("No more frames to filter.")
                        break

                    log = {'file' : 'video'}
                    print(" > New frame")

                    # Converting frame
                    new_frame, log = filter(frame, log)
                    cv2.imwrite('tmp_frame.jpg', new_frame)
                    img = cv2.imread('tmp_frame.jpg',-1)
                    out.write(img)

                    log = json.dumps(log)
                    producer.send(topic_log, log.encode())

                process_time = time.clock()-clock
                out.release()
                
                print("New video created.")
                video_path = "tmp_videos/"+video_name
                os.remove(video_path)
                subprocess.call(["ffmpeg", "-i", "tmp_videos/tmp.avi", "-ac", "2", "-b:v", "2000k", "-c:a", "aac", "-c:v", "libx264", "-b:a", "160k", "-vprofile", "high", "-bf", "0", "-strict", "experimental", "-f", "mp4", video_path])

                # Make POST request to the API
                api_url = "http://192.168.160.73:8080/api/video/pythonUpload"
                files = {'file': (video_name, open(video_path,'rb'), 'video/mp4')}

                resp = requests.post(url=api_url, files=files, data=data)

                os.remove(video_path)
                os.remove('tmp_frame.jpg')                
                #os.remove('tmp_videos/tmp.avi')

                # Sending message to kafka
                msg = {
                    'fileType' : 'video',
                    'id' : json_msg['id'],
                    'processTime' : process_time
                }

                msg = json.dumps(msg)
                producer.send(topic_send, value=msg.encode())

                log = json.dumps(log)
                producer.send(topic_log, log)

            else:
                raise Exception
        except Exception as e:
            print(e)
            print("Exception ocurred.")

def filter(frame, log):
    # Get dimensions of frame
    (h, w) = frame.shape[:2]
    hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)

    #cv2.imshow("Original",frame)
    #cv2.waitKey(0)

    # Detector configuration  
    params = cv2.SimpleBlobDetector_Params()
    params.filterByArea = True
    params.minArea = 500
    params.maxArea = 300000
    params.filterByCircularity = False
    params.filterByInertia = False
    params.filterByConvexity = False

    detector = cv2.SimpleBlobDetector_create(params)

    # RED ################################################
    # Light Red
    lower_light_red = np.array(convert([330,15,75]))
    upper_light_red = np.array(convert([350,40,100]))
    # Red
    lower_red = np.array(convert([350,40,75]))
    upper_red = np.array(convert([360,70,100]))
    # Dark Red
    lower_dark_red = np.array(convert([0,70,75]))
    upper_dark_red = np.array(convert([11,100,100]))

    # ORANGE ################################################
    # Light Orange
    lower_light_orange = np.array(convert([11,15,75]))
    upper_light_orange = np.array(convert([45,40,100]))
    # Orange
    lower_orange = np.array(convert([11,40,75]))
    upper_orange = np.array(convert([45,70,100]))
    # Dark Orange
    lower_dark_orange = np.array(convert([11,70,75]))
    upper_dark_orange = np.array(convert([45,100,100]))

    # BROWN ##########################################
    # Light Brown
    lower_light_brown = np.array(convert([11,15,10]))
    upper_light_brown = np.array(convert([45,40,75]))
    # Brown
    lower_brown = np.array(convert([11,40,10]))
    upper_brown = np.array(convert([45,70,75]))
    # Dark Brown
    lower_dark_brown = np.array(convert([11,70,10]))
    upper_dark_brown = np.array(convert([45,100,75]))

    # YELLOW #########################################
    # Light Yellow
    lower_light_yellow = np.array(convert([45,15,10]))
    upper_light_yellow = np.array(convert([64,40,100]))
    # Yellow
    lower_yellow = np.array(convert([45,40,10]))
    upper_yellow = np.array(convert([64,70,100]))
    # Dark Yellow
    lower_dark_yellow = np.array(convert([45,70,10]))
    upper_dark_yellow = np.array(convert([64,100,100]))

    # GREEN ##########################################
    # Light Green 
    lower_light_green = np.array(convert([64,15,10]))
    upper_light_green = np.array(convert([165,40,100]))
    # Green 
    lower_green = np.array(convert([64,40,10]))
    upper_green = np.array(convert([165,70,100]))
    # Dark Green
    lower_dark_green = np.array(convert([64,70,10]))
    upper_dark_green = np.array(convert([165,100,100]))

    # BLUE ##########################################
    # Light Blue 
    lower_light_blue = np.array(convert([165,15,10]))
    upper_light_blue = np.array(convert([255,40,100]))
    # Blue 
    lower_blue = np.array(convert([165,40,10]))
    upper_blue = np.array(convert([255,70,100]))
    # Dark Blue
    lower_dark_blue = np.array(convert([165,70,10]))
    upper_dark_blue = np.array(convert([255,100,100]))

    # VIOLET ########################################
    # Light Violet
    lower_light_violet = np.array(convert([255,15,10]))
    upper_light_violet = np.array(convert([330,40,100]))
    # Violet
    lower_violet = np.array(convert([255,40,10]))
    upper_violet = np.array(convert([330,70,100]))
    # Dark Violet
    lower_dark_violet = np.array(convert([255,70,10]))
    upper_dark_violet = np.array(convert([330,100,100]))

    # BLACK AND WHITE ##########################################
    # Black
    lower_black = np.array(convert([0,0,0]))
    upper_black = np.array(convert([360,100,10]))
    # White
    lower_white = np.array(convert([0,0,65]))
    upper_white = np.array(convert([360,15,100]))
    # Light Gray
    lower_light_gray = np.array(convert([0,0,10]))
    upper_light_gray = np.array(convert([360,15,40]))
    # Dark Gray
    lower_dark_gray = np.array(convert([0,0,40]))
    upper_dark_gray = np.array(convert([360,15,65]))

    # Declare output
    output = np.dstack([frame, np.ones((h, w), dtype="uint8") * 255])

    # Threshold the HSV image
    # Red
    light_red_mask = cv2.inRange(hsv, lower_light_red, upper_light_red)
    draw_symbol(light_red_mask, detector, 'vermelho.png', output, h, w)
    
    red_mask = cv2.inRange(hsv, lower_red, upper_red)
    draw_symbol(red_mask, detector, 'vermelho.png', output, h, w)
    
    dark_red_mask = cv2.inRange(hsv, lower_dark_red, upper_dark_red)
    draw_symbol(dark_red_mask, detector, 'vermelho.png', output, h, w)

    # Yellow
    light_yellow_mask = cv2.inRange(hsv, lower_light_yellow, upper_light_yellow)
    draw_symbol(light_yellow_mask, detector, 'amarelo.png', output, h, w)
    
    yellow_mask = cv2.inRange(hsv, lower_yellow, upper_yellow)
    draw_symbol(yellow_mask, detector, 'amarelo.png', output, h, w)
    
    dark_yellow_mask = cv2.inRange(hsv, lower_dark_yellow, upper_dark_yellow)
    draw_symbol(dark_yellow_mask, detector, 'amarelo.png', output, h, w)

    # Orange
    light_orange_mask = cv2.inRange(hsv, lower_light_orange, upper_light_orange)
    draw_symbol(light_orange_mask, detector, 'laranja.png', output, h, w)
    
    orange_mask = cv2.inRange(hsv, lower_orange, upper_orange)
    draw_symbol(orange_mask, detector, 'laranja.png', output, h, w)
    
    dark_orange_mask = cv2.inRange(hsv, lower_dark_orange, upper_dark_orange)
    draw_symbol(dark_orange_mask, detector, 'laranja.png', output, h, w)

    # Brown
    light_brown_mask = cv2.inRange(hsv, lower_light_brown, upper_light_brown)
    draw_symbol(light_brown_mask, detector, 'castanho.png', output, h, w)
    
    brown_mask = cv2.inRange(hsv, lower_brown, upper_brown)
    draw_symbol(brown_mask, detector, 'castanho.png', output, h, w)
    
    dark_brown_mask = cv2.inRange(hsv, lower_dark_brown, upper_dark_brown)
    draw_symbol(dark_brown_mask, detector, 'castanho.png', output, h, w)

    # Green
    light_green_mask = cv2.inRange(hsv, lower_light_green, upper_light_green)
    draw_symbol(light_green_mask, detector, 'verde.png', output, h, w)
    
    green_mask = cv2.inRange(hsv, lower_green, upper_green)
    draw_symbol(green_mask, detector, 'verde.png', output, h, w)
    
    dark_green_mask = cv2.inRange(hsv, lower_dark_green, upper_dark_green)
    draw_symbol(dark_green_mask, detector, 'verde.png', output, h, w)

    # Blue
    light_blue_mask = cv2.inRange(hsv, lower_light_blue, upper_light_blue)
    draw_symbol(light_blue_mask, detector, 'azul.png', output, h, w)

    blue_mask = cv2.inRange(hsv, lower_blue, upper_blue)
    draw_symbol(blue_mask, detector, 'azul.png', output, h, w)

    dark_blue_mask = cv2.inRange(hsv, lower_dark_blue, upper_dark_blue)
    draw_symbol(dark_blue_mask, detector, 'azul.png', output, h, w)

    # Violet
    light_violet_mask = cv2.inRange(hsv, lower_light_violet, upper_light_violet)
    draw_symbol(light_violet_mask, detector, 'violeta.png', output, h, w)
    
    violet_mask = cv2.inRange(hsv, lower_violet, upper_violet)
    draw_symbol(violet_mask, detector, 'violeta.png', output, h, w)

    dark_violet_mask = cv2.inRange(hsv, lower_dark_violet, upper_dark_violet)
    draw_symbol(dark_violet_mask, detector, 'violeta.png', output, h, w)

    # Black
    black_mask = cv2.inRange(hsv, lower_black, upper_black)
    white_mask = cv2.inRange(hsv, lower_white, upper_white)
  
    ratio_blue = getPercentage(frame, hsv, lower_dark_blue, upper_blue)
    ratio_blue = ratio_blue + getPercentage(frame, hsv, lower_light_blue, upper_light_blue)
    ratio_blue = ratio_blue + getPercentage(frame, hsv, lower_dark_blue, upper_dark_blue)
    log.update({'blue' : ratio_blue})

    ratio_green = getPercentage(frame, hsv, lower_dark_green, upper_green)
    ratio_green = ratio_green + getPercentage(frame, hsv, lower_light_green, upper_light_green)
    ratio_green = ratio_green + getPercentage(frame, hsv, lower_dark_green, upper_dark_green)
    log.update({'green' : ratio_green})

    ratio_yellow = getPercentage(frame, hsv, lower_dark_yellow, upper_yellow)
    ratio_yellow = ratio_yellow + getPercentage(frame, hsv, lower_light_yellow, upper_light_yellow)
    ratio_yellow = ratio_yellow + getPercentage(frame, hsv, lower_dark_yellow, upper_dark_yellow)
    log.update({'yellow' : ratio_yellow})

    ratio_orange = getPercentage(frame, hsv, lower_dark_orange, upper_orange)
    ratio_orange = ratio_orange + getPercentage(frame, hsv, lower_light_orange, upper_light_orange)
    ratio_orange = ratio_orange + getPercentage(frame, hsv, lower_dark_orange, upper_dark_orange)
    log.update({'orange' : ratio_orange})

    ratio_red = getPercentage(frame, hsv, lower_dark_red, upper_red)
    ratio_red = ratio_red + getPercentage(frame, hsv, lower_light_red, upper_light_red)
    ratio_red = ratio_red +  getPercentage(frame, hsv, lower_dark_red, upper_dark_red)
    log.update({'red' : ratio_red})

    ratio_violet = getPercentage(frame, hsv, lower_dark_violet, upper_violet)
    ratio_violet = ratio_violet + getPercentage(frame, hsv, lower_light_violet, upper_light_violet)
    ratio_violet = ratio_violet + getPercentage(frame, hsv, lower_dark_violet, upper_dark_violet)
    log.update({'violet' : ratio_violet})

    ratio_brown = getPercentage(frame, hsv, lower_dark_brown, upper_brown)
    ratio_brown = ratio_brown + getPercentage(frame, hsv, lower_light_brown, upper_light_brown)
    ratio_brown = ratio_brown + getPercentage(frame, hsv, lower_dark_brown, upper_dark_brown)
    log.update({'brown' : ratio_brown})

    ratio_white = getPercentage(frame, hsv, lower_white, upper_white)
    log.update({'white' : ratio_white})
    ratio_black = getPercentage(frame, hsv, lower_black, upper_black)
    log.update({'black' : ratio_black})
  
    with open("ratio.csv", "a") as write_file:
        write_file.write("\n")
        write_file.close()

    print(log)

    return output, log

def draw_symbol(color_mask, detector, file_name, output, h, w):

    #print(file_name)

    # Detect blobs.
    mask = cv2.bitwise_not(color_mask)
    keypoints = detector.detect(mask)
    
    # Load symbol
    watermark = cv2.imread("symbols/"+file_name, -1)
    watermark = cv2.resize(watermark, (20,20))
    (wH, wW) = watermark.shape[:2]

    #print(type(keypoints))
    #print(keypoints)

    for point in keypoints:
        #print(str(h)+" "+str(w))
        overlay = np.zeros((h, w, 4), dtype="uint8")
        overlay_pos = overlay[int(point.pt[1]) - int(wH/2) : int(point.pt[1]) + int(wH/2), int(point.pt[0]) - int(wW/2) : int(point.pt[0]) + int(wW/2)]

        print(overlay_pos.shape[0])
        if overlay_pos.shape[0] == 0:
            overlay_pos = overlay[0 : int(point.pt[1])*2 , overlay_pos[1]]
        if overlay_pos.shape[1] == 0:
            overlay_pos = overlay[overlay_pos[1] , 0 : int(point.pt[0])*2]

        """print(point.pt)
        print(str(h)+" "+str(w))
        print(str(wH)+" "+str(wW))
        print(overlay_pos.shape)"""

        watermark = cv2.resize(watermark, (overlay_pos.shape[:2]))

        #print(overlay[int(point.pt[1]) - int(wH/2) : int(point.pt[1]) + int(wH/2), int(point.pt[0]) - int(wW/2) : int(point.pt[0]) + int(wW/2)].shape)
        overlay[int(point.pt[1]) - int(wH/2) : int(point.pt[1]) + int(wH/2), int(point.pt[0]) - int(wW/2) : int(point.pt[0]) + int(wW/2)] = watermark
        print("After")
        """try:
        except Exception as identifier:
            print(identifier)
        """
        #print("here")
        cv2.addWeighted(overlay, 1, output, 1.0, 0, output)

    return output

def convert(a):
    a[0] = a[0]*180 / 360
    a[1] = a[1]*255 / 100
    a[2] = a[2]*255 / 100
    return a

def getPercentage(frame, hsv, lower, upper):

    boundaries = [([lower, upper])]

    for (lower, upper) in boundaries:
        lower = np.array(lower, dtype=np.uint8)
        upper = np.array(upper, dtype=np.uint8)
        mask = cv2.inRange(hsv, lower, upper)
        output = cv2.bitwise_and(frame, frame, mask=mask)

        ratio = cv2.countNonZero(mask)/(frame.size/3)

    #with open("ratio.csv", "a") as write_file:
    #    write_file.write(str(ratio) + "\t")
    #    write_file.close()

    return ratio

if __name__ == '__main__':
    print("Launching OpenCV Filter.")
    receiveMsg()