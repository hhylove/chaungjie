#!/bin/zsh
cd "${0:A:h}"
export PATH="/opt/homebrew/bin:/opt/homebrew/opt/node@24/bin:$PATH"
open 'http://localhost:4173'
npm start
