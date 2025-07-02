#!/bin/bash

pushd "$(dirname "$0")" >/dev/null

ENV_FILE="../../configs/.env"

if [ ! -f $ENV_FILE ]; then
    echo -e "<root>/configs/.env file not found. Creating one now.\nYou can decide all values yourself.\nThey will be used automatically for all tools."

    read -p "Enter USERNAME_TOOLS: " USERNAME_TOOLS
    read -p "Enter PASSWORD_TOOLS: " PASSWORD_TOOLS

    cat <<EOF >$ENV_FILE
USERNAME_TOOLS=$USERNAME_TOOLS
PASSWORD_TOOLS=$PASSWORD_TOOLS
EOF

    echo "$ENV_FILE file created successfully."
fi

popd >/dev/null
