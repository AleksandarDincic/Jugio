import requests
import os
import json
import time
import copy
from PIL import Image
import shutil


api_link = 'https://db.ygoprodeck.com/api/v7/cardinfo.php'

def downloadCardsJson():
    if not os.path.exists('data'):
        os.mkdir('data')
    
    if not os.path.exists('data/data.json'):
        apiCardsResponse = requests.get(api_link)
        with open("data/data.json", 'w', encoding = 'utf-8') as f:
            f.write(json.dumps(json.loads(apiCardsResponse.text), indent = 4))

def downloadCardsImages():
    with open('data/data.json', 'r', encoding = 'utf-8') as f:
        cardsJson = json.load(f)['data']
        cardImgFolderPath = os.path.join('data', 'img')
        if not os.path.exists(cardImgFolderPath):
            os.mkdir(cardImgFolderPath)
        for card in cardsJson:
            for img in card['card_images']:
                cardImgPath = os.path.join(cardImgFolderPath, str(img['id']) + '.jpg')
                if not os.path.exists(cardImgPath):
                    apiImgResponse = requests.get(img['image_url'])
                    imgContent = apiImgResponse.content
                    with open(cardImgPath, 'wb') as f:
                        f.write(imgContent)
                    print('Downloaded ', img['id'])
                    time.sleep(0.1)
                else:
                    print('Already exists', img['id'])


def downloadCardsData():
    downloadCardsJson()
    downloadCardsImages()


ART_X_START = 49 / 421
ART_X_END = 371 / 421
ART_Y_START = 111 / 614
ART_Y_END = 433 / 614

PEND_X_START = 27 / 421
PEND_Y_START = 110 / 614
PEND_X_END = 393 / 421
PEND_Y_END = 381 / 614


def cropImages():
    with open('data/data.json', 'r', encoding = 'utf-8') as f:
        cardsJson = json.load(f)['data']

        cardImgFolderPath = os.path.join('data', 'img')
        cardImgCroppedFolderPath = os.path.join('data', 'img_cropped')
        if not os.path.exists(cardImgCroppedFolderPath):
                    os.mkdir(cardImgCroppedFolderPath)

        for card in cardsJson:
            for img in card['card_images']:
                cardImgPath = os.path.join(cardImgFolderPath, str(img['id']) + '.jpg')
                cardImgCroppedPath = os.path.join(cardImgCroppedFolderPath, str(img['id']) + '.jpg')
                if not os.path.exists(cardImgCroppedPath):
                    imgObj = Image.open(cardImgPath)
                    width, height = imgObj.size
                    cropped = None
                    if 'Pendulum' in card['type']:
                        cropped = imgObj.crop((width * PEND_X_START, height * PEND_Y_START, width  * PEND_X_END, height * PEND_Y_END))
                    else:    
                        cropped = imgObj.crop((width * ART_X_START, height * ART_Y_START, width  * ART_X_END, height * ART_Y_END))
                    cropped.save(cardImgCroppedPath, 'JPEG')
                    print('Cropped ', img['id'])
                else:
                    print('Already exists', img['id'])

def cropAndRotateImages():
    with open('data/data.json', 'r', encoding = 'utf-8') as f:
        cardsJson = json.load(f)['data']

        cardImgFolderPath = os.path.join('data', 'img')
        cardImgCroppedFolderPath = os.path.join('data', 'img_cropped_rotated')
        if not os.path.exists(cardImgCroppedFolderPath):
                    os.mkdir(cardImgCroppedFolderPath)

        for card in cardsJson:
            if card['type'] == 'Token' or card['type'] == 'Skill Card':
                    continue

            for img in card['card_images']:
                cardImgPath = os.path.join(cardImgFolderPath, str(img['id']) + '.jpg')

                cardImgRotatedFolder = os.path.join(cardImgCroppedFolderPath, str(img['id']))
                if not os.path.exists(cardImgRotatedFolder):
                    os.mkdir(cardImgRotatedFolder)

                imgObj = Image.open(cardImgPath)
                for i in range(12):
                    rotatedImgObj = imgObj.rotate(i*30)
                    cardImgCroppedPath = os.path.join(cardImgRotatedFolder, str(img['id']) + '_' + str(i) + '.jpg')
                    if not os.path.exists(cardImgCroppedPath):
                        width, height = imgObj.size
                        cropped = None
                        if 'Pendulum' in card['type']:
                            cropped = rotatedImgObj.crop((width * PEND_X_START, height * PEND_Y_START, width  * PEND_X_END, height * PEND_Y_END))
                        else:    
                            cropped = rotatedImgObj.crop((width * ART_X_START, height * ART_Y_START, width  * ART_X_END, height * ART_Y_END))
                        cropped.save(cardImgCroppedPath, 'JPEG')
                        print('Cropped and rotated ', img['id'], ' ', str(i))
                    else:
                        print('Already exists', img['id'], ' ', str(i))

def removeCroppedTokensAndSkills():
    with open('data/data.json', 'r', encoding = 'utf-8') as f:
        cardsJson = json.load(f)['data']

        cardImgCroppedFolderPath = os.path.join('data', 'img_cropped')

        for card in cardsJson:
            for img in card['card_images']:
                cardImgCroppedPath = os.path.join(cardImgCroppedFolderPath, str(img['id']) + '.jpg')
                if os.path.exists(cardImgCroppedPath) and (card['type'] == 'Token' or card['type'] == 'Skill Card'):
                    os.remove(cardImgCroppedPath)

def truncateJson():
    if not os.path.exists('data/data_trunc.json'):
        with open('data/data.json', 'r', encoding = 'utf-8') as f:
            cardsJson = json.load(f)['data']
            truncCardsJson = []
            for card in cardsJson:
                if card['type'] == 'Token' or card['type'] == 'Skill Card':
                    continue

                cardCopy = copy.deepcopy(card)
                if 'archetype' in cardCopy:
                    del cardCopy['archetype']
                if 'card_images' in cardCopy:
                    del cardCopy['card_images']
                if 'card_sets' in cardCopy:
                    del cardCopy['card_sets']
                if 'card_prices' in cardCopy:
                    del cardCopy['card_prices']

                for img in card['card_images']:
                    artVariantCopy = copy.deepcopy(cardCopy)
                    artVariantCopy['id'] = img['id']
                    truncCardsJson.append(artVariantCopy)

            with open('data/data_trunc.json', 'w', encoding = 'utf-8') as fw:
                fw.write(json.dumps(truncCardsJson, indent = 4))


def findMostAlts(howMany):
    with open('data/data.json', 'r', encoding = 'utf-8') as f:
        cardsJson = json.load(f)['data']

        maxName = [None for i in range(howMany)]
        maxNum = [0 for i in range(howMany)]

        for card in cardsJson:
            if card['type'] == 'Token' or card['type'] == 'Skill Card':
                continue

            imgs = len(card['card_images'])
            for i in range(howMany):
                if imgs > maxNum[i]:
                    maxName.insert(i, card['name'])
                    maxNum.insert(i, imgs)
                    maxName = maxName[:-1]
                    maxNum = maxNum[:-1]
                    break

        print(maxName)
        print(maxNum)


def separateLabeledScrappedImages():
    scrapedImgFolder = os.path.join('data', 'img_scraped')
    scrapedLabeledImgFolder = os.path.join('data', 'img_scraped_labeled')
    if not os.path.exists(scrapedLabeledImgFolder):
        os.mkdir(scrapedLabeledImgFolder)
    files = os.listdir(scrapedImgFolder)
    for file in files:
        fileName, fileExtension = os.path.splitext(file)
        if fileExtension.lower() == '.json':
            jsonSrcPath = os.path.join(scrapedImgFolder, file)
            jsonDstPath = os.path.join(scrapedLabeledImgFolder, file)
            shutil.copy(jsonSrcPath, jsonDstPath)

            imgSrcPath = os.path.join(scrapedImgFolder, fileName + '.jpg')
            imgDstPath = os.path.join(scrapedLabeledImgFolder, fileName + '.jpg')
            shutil.copy(imgSrcPath, imgDstPath)
            print('Copied image and label for ', fileName)