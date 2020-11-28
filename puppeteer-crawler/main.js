const puppeteer = require("puppeteer");
const kafka = require("kafka-node");
const protobuf = require("protobufjs");
const redis = require("redis");

protobuf.load("../protocol/notification/protocol.proto", function (err, root) {
  if (err) throw err;

  let Request = root.lookupType("Request");

  const kafkaClient = new kafka.KafkaClient();
  const redisClient = redis.createClient();

  const consumer = new kafka.Consumer(
    kafkaClient,
    [
      { topic: "crawler-requests", partition: 0 },
      { topic: "crawler-requests", partition: 1 },
      { topic: "crawler-requests", partition: 2 },
      { topic: "crawler-requests", partition: 3 },
    ],
    {
      autoCommit: true,
      encoding: "buffer",
    }
  );

  consumer.on("message", function (message) {
    const request = Request.decode(message.value);
    const config = JSON.parse(redisClient.get("crawling-config"));
    const url = request.crawl.url;

    (async () => {
      const browser = await puppeteer.launch({});
      const page = await browser.newPage();

      await page.goto(url, { waitUntil: "load" });

      const [links, content] = await page.evaluate(() => {
        let links = [];
        let l = document.links;

        for (var i = 0; i < l.length; i++) {
          links.push(l[i].href);
        }

        const text = document.body.textContent;

        return [Array.from(new Set(links)), text];
      });

      console.log(config);
      console.log(config["namedEntities"]);

      // config["namedEntities"].map((entity) => {
      //   const regex = new RegExp(entity.regex);
      // });

      console.log(links);

      await browser.close();
    })();
  });
});
