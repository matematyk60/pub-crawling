const puppeteer = require("puppeteer");
const kafka = require("kafka-node");
const protobuf = require("protobufjs");
const redis = require("redis");
const pro = require("./compiled/protocol_pb");

const redisHost = process.env.REDIS_HOST || "redis://127.0.0.1";
const kafkaHost = process.env.KAFKA_HOST || "kafka";
const requestTopic = process.env.REQUEST_TOPIC || "crawler-requests";
const responseTopic = process.env.RESPONSE_TOPIC || "crawler-responses-4";

let protoPath = "protocol.proto";

protobuf.load(protoPath, function (err, root) {
  if (err) throw err;

  let Request = root.lookupType("Request");
  let Response = root.lookupType("Response");
  let EntityMatch = root.lookupType("EntityMatch");

  const kafkaClient = new kafka.KafkaClient({ kafkaHost: kafkaHost + ":9092" });
  const redisClient = redis.createClient((host = redisHost));

  const consumer = new kafka.Consumer(
    kafkaClient,
    [
      Array.from(Array(16).keys()).map((i) => ({
        topic: requestTopic,
        partition: i,
      })),
    ],
    {
      autoCommit: true,
      groupId: "puppeteer-crwler",
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

            const matches = [["hello"]];

            return matches.map((m) => {
              const em = new pro.EntityMatch();
              em.setEntityid(entity.entityId);
              em.setValue(m[0]);
              em.setCount(1);

              return em;
            });
          });

          const matches = [].concat.apply([], entityMatches);

          const response = new pro.Response();
          const success = new pro.CrawlSuccess();

          success.setRequestid(request.crawl.requestId);
          success.setUrlsList(links);
          success.setFoundentitisList(matches);

          response.setSuccess(success);

          const responseBytes = response.serializeBinary();

          producer.send(
            [
              {
                topic: responseTopic,
                messages: [responseBytes],
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
