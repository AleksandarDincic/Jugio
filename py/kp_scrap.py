import requests
import os
from bs4 import BeautifulSoup
from PIL import Image
from io import BytesIO
import time

url = 'https://www.kupujemprodajem.com/igracke-i-igre/drustvene-igre/grupa/1157/724/'
urlParams = '?action=list&data[keywords]=yu+gi+oh'
bigImgUrlStart = 'https://www.kupujemprodajem.com/big-photo-' 

headers = {'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:101.0) Gecko/20100101 Firefox/101.0'}

def scrapeKPImages(startPage, endPage):
    scrapedFolderPath = os.path.join('data', 'img_scraped')
    if not os.path.exists(scrapedFolderPath):
        os.mkdir(scrapedFolderPath)
    for i in range(startPage, endPage + 1):
        print('Scraping page ', i)
        req = requests.get(url + str(i) + urlParams, headers=headers)
        if req.status_code == 200:
            soup = BeautifulSoup(req.text, 'html.parser')
            imgDivs = soup.find_all('div', {'class': 'imgAndBlurHolder'})
            for imgDiv in imgDivs:
                oglasLink = imgDiv.find('a')
                oglasId = os.path.basename(oglasLink.get('href'))
                for i in range(1, 11):
                    try:
                        bigImgName = 'kp_' + oglasId + '_' + str(i) + '.jpg'
                        bigImgUrl = bigImgUrlStart + oglasId + '-' + str(i) + '.htm'
                        bigImgReq = requests.get(bigImgUrl, headers= headers)

                        bigSoup = BeautifulSoup(bigImgReq.text, 'html.parser')
                        bigSoupImg = bigSoup.find('img', {'id' : 'photo'})

                        bigImgUrl = 'https:' + bigSoupImg.get('src')
                        bigImgReq = requests.get( bigImgUrl, headers= headers)

                        bigImgObj = Image.open(BytesIO(bigImgReq.content))

                        if bigImgObj.mode != 'RGB':
                            bigImgObj = bigImgObj.convert('RGB')

                        cardImgPath = os.path.join(scrapedFolderPath, bigImgName)
                        bigImgObj.save(cardImgPath, format='JPEG')
                        print('Downloaded ', bigImgUrl, ' as ', bigImgName)
                        time.sleep(0.1)
                    except:
                        print('kme')
        else:
            print('Error, status code ', req.status_code)