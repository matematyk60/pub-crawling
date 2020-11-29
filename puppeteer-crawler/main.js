const puppeteer = require("puppeteer");
const kafka = require("kafka-node");
const protobuf = require("protobufjs");
const redis = require("redis");

const redisHost = process.env.REDIS_HOST || "redis://127.0.0.1";
const kafkaHost = process.env.KAFKA_HOST || "kafka";
const requestTopic = process.env.REQUEST_TOPIC || "crawler-requests";
const responseTopic = process.env.RESPONSE_TOPIC || "crawler-responses";

let protoPath = "protocol.proto";

protobuf.load(protoPath, function (err, root) {
  if (err) throw err;

  let Request = root.lookupType("Request");
  let Response = root.lookupType("Response");

  const kafkaClient = new kafka.KafkaClient({ kafkaHost: kafkaHost + ":9092" });
  const redisClient = redis.createClient((host = redisHost));

  const consumer = new kafka.Consumer(
    kafkaClient,
    [
      { topic: requestTopic, partition: 0 },
      { topic: requestTopic, partition: 1 },
      { topic: requestTopic, partition: 2 },
      { topic: requestTopic, partition: 3 },
    ],
    {
      autoCommit: true,
      encoding: "buffer",
    }
  );

  const producer = new kafka.Producer(kafkaClient);

  producer.on("ready", function () {
    consumer.on("message", function (message) {
      const request = Request.decode(message.value);

      redisClient.get("crawling-config", function (err, rawConfig) {
        if (err) throw err;

        const config = JSON.parse(rawConfig);
        const url = request.crawl.url;

        (async () => {
          const browser = await puppeteer.launch({
            headless: true,
            args: [
              "--disable-dev-shm-usage",
              "--disable-web-security",
              "--no-sandbox",
            ],
          });
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

          const entityMatches = config["namedEntities"].map((entity) => {
            const regex = new RegExp(entity.regex, "g");
            const matches = [...content.matchAll(regex)];

            return matches.map((m) => ({
              entityId: entity.entityId,
              value: m[0],
              count: 1,
            }));
          });

          const matches = [].concat.apply([], entityMatches);

          let payload = {
            success: {
              requestId: request.crawl.requestId,
              urls: links,
              foundEntities: matches,
            },
          };

          let response = Response.fromObject(payload);

          console.log(response);

          let responseBytes = Response.encode(response).finish();

          producer.send(
            [
              {
                topic: responseTopic,
                messages: responseBytes,
              },
            ],
            function (err, data) {
              if (err) throw err;
              console.log("Pushed success response.");
            }
          );

          await browser.close();
        })().catch((e) => {
          console.error(e);
          console.log("I managed to recover!");

          let response = Response.create({
            failure: {
              requestId: request.crawl.requestId,
            },
          });

          let responseBytes = Response.encode(response);

          producer.send(
            [
              {
                topic: responseTopic,
                messages: responseBytes,
              },
            ],
            function (err, data) {
              if (err) throw err;
              console.log("Pushed error response.");
            }
          );
        });
      });
    });
  });
});
