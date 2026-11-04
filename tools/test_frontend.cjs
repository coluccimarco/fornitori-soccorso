const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/resources/static/app.js', 'utf8');
function functionSource(name, next) {
  return source.slice(source.indexOf(`  function ${name}(`), source.indexOf(next, source.indexOf(`  function ${name}(`)));
}

function countdown(mode = 'live') {
  let calls = 0;
  let time = 100000;
  const element = { dataset: { deadline: new Date(0).toISOString() } };
  const state = { user: { ruolo: 'responsabile' }, currentView: 'missioni', deadlineRefreshAt: 0, deadlineRefreshPending: false };
  const context = vm.createContext({ state, Date: class extends Date { static now() { return time; } },
    document: { querySelectorAll: () => [element, {...element}] }, $: () => ({ value: mode }),
    loadMissions: async () => { calls++; vm.runInContext('updateCountdowns()', context); },
    loadDriverMissions: async () => { calls++; } });
  vm.runInContext(functionSource('updateCountdowns', '\n  $("#loginForm")'), context);
  return { run: () => vm.runInContext('updateCountdowns()', context), calls: () => calls,
    advance: () => { time += 30001; }, state, element };
}

test('expired cards and rerender cause one refresh, then a bounded retry', async () => {
  const harness = countdown(); harness.run();
  await new Promise(setImmediate);
  harness.run(); assert.equal(harness.calls(), 1);
  harness.advance(); harness.run(); assert.equal(harness.calls(), 2);
});
test('reference fixtures never trigger a refresh loop', () => {
  const harness = countdown('reference'); harness.run(); assert.equal(harness.calls(), 0);
});
test('logged out user never refreshes expired missions', () => {
  const harness = countdown(); harness.state.user = null; harness.run(); assert.equal(harness.calls(), 0);
});
test('rendered untrusted strings escape HTML and attribute delimiters', () => {
  const context = vm.createContext({});
  vm.runInContext(functionSource('safe', '\n  function rowFor'), context);
  assert.equal(vm.runInContext(`safe('<img src=x onerror="alert(1)">')`, context), '&lt;img src=x onerror=&quot;alert(1)&quot;&gt;');
});
