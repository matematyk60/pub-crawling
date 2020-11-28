const puppeteer = require("puppeteer");
const fs = require("fs");

const url = "https://www.duckduckgo.com/?q=tescik";

(async () => {
  const browser = await puppeteer.launch({});
  const page = await browser.newPage();
  // await page.goto(url, { waitUntil: "networkidle2" });
  await page.goto(url, { waitUntil: "load" });
  // await page.goto(url, { waitUntil: "networkidle0" });

  const [links, content] = await page.evaluate(() => {
    let links = [];
    let l = document.links;

    for (var i = 0; i < l.length; i++) {
      links.push(l[i].href);
    }

    const text = document.body.textContent;

    return [Array.from(new Set(links)), text];
  });

  console.log(links);
  console.log(links.length);
  // console.log(content.length);

  await browser.close();
})();
