#!/bin/bash

cd "$(dirname "$0")" || exit 1

SCRIPTS_ROOT="./scripts"

pprint_help() {
  echo -e "\nUsage: $0 <command-group> <command> [options]\n"
  echo "Available command groups and commands:"

  for group_dir in "$SCRIPTS_ROOT"/*/; do
    group_name=$(basename "$group_dir")

    commands=()
    descriptions=()
    max_len=0

    for script_path in "$group_dir"/*.sh; do
      [ -e "$script_path" ] || continue
      cmd=$(basename "$script_path" .sh)
      desc=$(grep -m1 '^# Description:' "$script_path" | sed 's/^# Description: //')
      [ -z "$desc" ] && continue
      commands+=("$cmd")
      descriptions+=("$desc")
      ((${#cmd} > max_len)) && max_len=${#cmd}
    done

    if [ ${#commands[@]} -eq 0 ]; then
      continue
    fi

    echo -e "\n$group_name:"

    for idx in "${!commands[@]}"; do
      printf "  %-*s  - %s\n" "$max_len" "${commands[$idx]}" "${descriptions[$idx]}"
    done
  done
  echo
}

if [ $# -lt 2 ]; then
  print_help
  exit 1
fi

SCRIPT_PATH="$SCRIPTS_ROOT/$1/$2.sh"

if [ ! -f "$SCRIPT_PATH" ]; then
  echo "Error: Script '$SCRIPT_PATH' not found."
  exit 1
fi

"$SCRIPT_PATH" "${@:3}"
