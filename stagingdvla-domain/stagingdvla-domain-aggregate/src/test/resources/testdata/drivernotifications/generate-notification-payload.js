#!/usr/bin/env node
'use strict';

const path = require('path');
const fs = require('fs');
const DVLA_DIR = process.env.OUTBOUND_DVLA_DIR || __dirname;
const OutboundDvlaNotification = require(path.join(DVLA_DIR, 'index'));

const inputPath = process.argv[2];
if (!inputPath) {
    console.error('Usage: node run.js <path/to/xxx-resulted-hearing.json>');
    process.exit(1);
}

const absInput = path.resolve(inputPath);
const outputPath = path.join(path.dirname(absInput), path.basename(absInput).replace('-hearing.json', '.json'));

if (!fs.existsSync(absInput)) {
    console.error(`File not found: ${absInput}`);
    process.exit(1);
}

// const hearingJson = JSON.parse(fs.readFileSync(absInput, 'utf8')).hearing;
const hearingJson = JSON.parse(fs.readFileSync(absInput, 'utf8'));

const context = {
    log: (...args) => console.log('[LOG]', ...args),
    warn: (...args) => console.warn('[WARN]', ...args),
    error: (...args) => console.error('[ERROR]', ...args),
    df: { getInput: () => ({ cjscppuid: undefined }) },
    res: { status: 0 }
};

const input = { hearingJson, isReshare: false };

OutboundDvlaNotification(input, context).then(result => {
    fs.writeFileSync(outputPath, JSON.stringify(result, null, 2));
    console.log(`Output written to: ${outputPath}`);
}).catch(err => {
    console.error('Failed:', err);
    process.exit(1);
});