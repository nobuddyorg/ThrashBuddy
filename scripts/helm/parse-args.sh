IS_REMOTE=false
for arg in "$@"; do
    case "$arg" in
    -remote) IS_REMOTE=true ;;
    esac
done
