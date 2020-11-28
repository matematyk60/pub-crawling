const puppeteer = require("puppeteer");
const kafka = require("kafka-node");
const protobuf = require("protobufjs");
const redis = require("redis");

protobuf.load("../protocol/notification/protocol.proto", function (err, root) {
  if (err) throw err;

  let Request = root.lookupType("Request");
  let Response = root.lookupType("Response");

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

  const producer = new kafka.Producer(kafkaClient);

  consumer.on("message", function (message) {
    const request = Request.decode(message.value);

    redisClient.get("crawling-config", function (err, rawConfig) {
      if (err) throw err;

      const config = JSON.parse(rawConfig);
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

          const text = document.body.textContent.toString();

          return [Array.from(new Set(links)), text];
        });

        config["namedEntities"].map((entity) => {
          console.log(content);
          const regex = new RegExp(entity.regex, "g");
          const matches = [...content.matchAll(regex)];
          // .map((m) => m[0]);

          console.log(matches);

          return matches;
        });

        await browser.close();
      })();
    });
  });
});
