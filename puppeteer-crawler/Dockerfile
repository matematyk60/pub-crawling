FROM zenato/puppeteer

USER root

# RUN echo "deb http://ftp.de.debian.org/debian sid main" >> /etc/apt/sources.list
RUN apt-get update

COPY ./package.json package.json
COPY ./node_modules node_modules/
COPY ./*.js ./
COPY ./*.proto ./
COPY ./compiled ./compiled

CMD ["yarn",  "start"]
