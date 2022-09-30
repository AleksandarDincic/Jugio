import requests
import os
from bs4 import BeautifulSoup
from PIL import Image
from io import BytesIO


url = 'https://www.alibaba.com/trade/search?fsb=y&IndexArea=product_en&CatId=&tab=all&SearchText=yugioh&page='
headers = {'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:101.0) Gecko/20100101 Firefox/101.0'}

def scrapeAlibabaImages(startPage, endPage):
    scrapedFolderPath = os.path.join('data', 'img_scraped')
    if not os.path.exists(scrapedFolderPath):
        os.mkdir(scrapedFolderPath)
    for i in range(startPage, endPage + 1):
        print('Scraping page ', i)
        req = requests.get(url + str(i), headers=headers)
        if req.status_code == 200:
            soup = BeautifulSoup(req.text, 'html.parser')
            imgs = soup.find_all('img', {'class': 'J-img-switcher-item'})
            cnt = len(imgs) * (i-1)
            for img in imgs:
                imgLink = img.get('src').replace('300x300', '600x600')
                imgName = 'alibaba_' + str(cnt) + '.jpg'
                cnt = cnt + 1

                imgReq = requests.get(imgLink)
                imgObj = Image.open(BytesIO(imgReq.content))
                
                if imgObj.mode != 'RGB':
                    imgObj = imgObj.convert('RGB')
                
                cardImgPath = os.path.join(scrapedFolderPath, imgName)
                imgObj.save(cardImgPath, format='JPEG')
                print('Downloaded ', imgLink, ' as ', imgName)
                #time.sleep(0.1)
        else:
            print('Error, status code ', req.status_code)