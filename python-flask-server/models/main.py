import cv2

face_cascade = cv2.CascadeClassifier('haarcascade_frontalface_default.xml')

img = cv2.imread('../images/propic.jpg')

gray = cv2.cvtColor(img,cv2.COLOR_BGR2GRAY)

faces = face_cascade.detectMultiScale(gray,1.1,4)

images = []

i=0

for (x, y, w, h) in faces:
    images.append(img[x:(x+w),y:(y+h)])
    cv2.rectangle(img, (x, y), (x+w, y+h), (255, 0, 0), 2)
    cv2.imshow('crop', images[i])
    cv2.waitKey()
    i+=1




