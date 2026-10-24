/* Render locally authored diagrams; the only inputs are the checked Markdown inventory and local packages. */
const fs = require('fs');
const path = require('path');
const docs = path.resolve(__dirname, '..');
const asset = path.resolve(process.argv[2] || 'docs/.tools/node_modules/mermaid/dist/mermaid.min.js');
const mermaidVersion = JSON.parse(fs.readFileSync(path.resolve(path.dirname(asset), '../package.json'), 'utf8')).version;
const playwrightPath = process.argv[3];
if (!playwrightPath) throw new Error('Usage: node render_mermaid.cjs <mermaid.min.js> <playwright package>');
const {chromium} = require(path.resolve(playwrightPath));
const diagrams = JSON.parse(fs.readFileSync(path.join(docs, '.validation/diagrams.json'), 'utf8'));
const inventory = JSON.parse(fs.readFileSync(path.join(docs, 'pruefbericht.json'), 'utf8'));

(async () => {
  let browser;
  try { browser = await chromium.launch({headless: true}); }
  catch { browser = await chromium.launch({headless: true, channel: 'msedge'}); }
  const page = await browser.newPage({viewport: {width: 1440, height: 1000}});
  const results = [];
  try {
    await page.setContent('<html><head><meta charset="utf-8"></head><body></body></html>');
    await page.addScriptTag({path: asset});
    await page.evaluate(() => mermaid.initialize({startOnLoad: false, securityLevel: 'strict', theme: 'default', fontFamily: 'Arial'}));
    for (let i=0; i<diagrams.length; i++) {
      const block = diagrams[i];
      try {
        const rendered = await page.evaluate(async ({source, index}) => {
          await mermaid.parse(source);
          const {svg} = await mermaid.render('diagram' + index, source);
          document.body.innerHTML = svg;
          const node = document.querySelector('svg');
          const box = node.getBoundingClientRect();
          return {svg, width: box.width, height: box.height};
        }, {source: block.source, index: i});
        if (!(rendered.width > 0 && rendered.height > 0)) throw new Error('Empty diagram');
        const filename = 'diagram-' + String(i+1).padStart(2, '0');
        fs.writeFileSync(path.join(docs, '.validation', filename + '.svg'), rendered.svg);
        await page.locator('svg').screenshot({path: path.join(docs, '.validation', filename + '.png')});
        results.push({file: block.file, line: block.line, passed: true, width: rendered.width, height: rendered.height});
      } catch (error) { results.push({file: block.file, line: block.line, passed: false, error: String(error)}); }
    }
    const report = {checkedAtUtc: new Date().toISOString(), renderer: 'Mermaid ' + mermaidVersion + ' / local Chromium',
      browserVersion: browser.version(), diagramManifestSha256: inventory.diagramManifestSha256,
      diagrams: results.length, failures: results.filter(result => !result.passed).length, results};
    fs.writeFileSync(path.join(docs, 'mermaid-pruefbericht.json'), JSON.stringify(report, null, 2) + '\n');
    console.log(JSON.stringify({diagrams: report.diagrams, failures: report.failures, results: results.filter(result => !result.passed)}, null, 2));
    if (report.failures) process.exitCode = 1;
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
