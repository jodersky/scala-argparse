# shellcheck shell=bash
# An example script to illustrate how bash completions are generated
# cmd --foo --bar --flag path non-path path
_cmd() {
    local cur
    local prev
    local pos
    pos=0

    local escaping
    escaping=0

    # first, iterate over CWORDS to determine the positional index
    local cword
    for ((cword=1;cword<COMP_CWORD;cword++)); do
        prev="${COMP_WORDS[i-1]}"
        cur="${COMP_WORDS[i]}"

        if [[ $escaping == 1 ]]; then
            ((pos++))
            continue
        fi

        if [[ $cur == -- ]]; then
            escaping=1
            continue
        fi

        # named params don't increment the position
        case "$cur" in
            -) ;; # '-' alone is often used as a value
            -*) continue ;;
        esac

        # arguments to named non-flags do not increment the position
        case "$prev" in
            --foo|--bar) continue ;;
        esac

        # TODO handle repetition
        # if [[ $pos == <pos of repeated param> ]]; then
        # fi

        ((pos++))
    done

    # then, suggest a completion for the current CWORD
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    cur="${COMP_WORDS[COMP_CWORD]}"

    # handle arguments to named params
    case "$prev" in
       --foo)
            COMPREPLY=( $(compgen -W 'foocompletion' -- "$cur") )
            return 0
            ;;
        --bar)
            COMPREPLY=( $(compgen -W 'barcompletion' -- "$cur") )
            return 0
            ;;
    esac

    # handle named param completion
    case "$cur" in
        -*)
            COMPREPLY=( $(compgen -W '--foo --bar --flag' -- "$cur") )
            return 0
            ;;
    esac

    # handle positional completion
    if [[ $pos == 0 ]]; then
        COMPREPLY=( $(compgen -W 'pos0' -- "$cur") )
        return 0
    fi
    if [[ $pos == 1 ]]; then
        COMPREPLY=( $(compgen -W 'pos1' -- "$cur") )
        return 0
    fi
    if [[ $pos == 2 ]]; then
        COMPREPLY=( $(compgen -W 'pos2' -- "$cur") )
        return 0
    fi
}

complete -F _cmd cmd
