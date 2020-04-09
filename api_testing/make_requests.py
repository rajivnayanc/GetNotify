import requests
import time
import json
f = open('secrets.json')
secrets = json.load(f)
SERVER_KEY = secrets['SERVER_KEY']

DEVICE_KEY = secrets['DEVICE_KEY']

API_ENDPOINT = "https://fcm.googleapis.com/fcm/send"

message_number = 0

headers = {
        'Content-Type':'application/json',
        'Authorization':'key='+SERVER_KEY
    }
# headers = json.dumps(headers)
while(message_number<10):
    message_number+=1
    data = {
        'notification':{
            "title":"Test Message",
            "body":"Message Number {}".format(message_number)
        },
        'to':DEVICE_KEY
    }
    data = json.dumps(data)
    r = requests.post(API_ENDPOINT,headers=headers,data=data)
    print(r.text)
    time.sleep(5)
    
    