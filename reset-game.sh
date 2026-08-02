#!/bin/sh
# Resets game state for a clean demo: restores the committed user database
# (bazdar + demo) and clears any stay-logged-in session.
cd "$(dirname "$0")"
git checkout src/data/database/Users.json 2>/dev/null
rm -f src/data/database/session.json
echo "Game reset. Accounts: demo / Demo123!  (minigames unlocked)"
