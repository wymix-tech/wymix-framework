#!/usr/bin/env bash
#
# 将仓库的 Maven groupId / Java 包名 / 配置前缀 / 文档品牌名全量迁移到新的组织前缀。
#
# 适用场景：
#   1. 首次在独立分支上做整体改名（groupId 变更、包名与目录重命名、文档同步）；
#   2. 上游 main 合入新提交后，在本分支上重跑，补齐新增文件中的旧标识（幂等，无残留时不会产生变更）。
#
# 用法：
#   ./scripts/migrate-groupid.sh [选项]
#
# 选项：
#   --branch   <name>   在指定分支上执行（不存在则创建，已存在则切换）；省略则在当前分支执行
#   --from     <pkg>    旧组织前缀，默认 org.wymix
#   --to       <pkg>    新组织前缀，默认 com.example
#   --brand-from <word> 旧品牌名（小写），默认 wymix
#   --brand-to   <word> 新品牌名（小写），默认 example
#   --commit            执行完成后自动提交（默认只暂存，便于人工 review）
#   --dry-run           只打印将执行的操作，不落盘
#   -h | --help         显示帮助
#
# 示例：
#   ./scripts/migrate-groupid.sh --branch chore/groupid-com.example.framework \
#        --from org.wymix --to com.example --brand-from wymix --brand-to example
#

set -euo pipefail

FROM_PKG="org.wymix"
TO_PKG="com.example"
FROM_BRAND="wymix"
TO_BRAND="example"
BRANCH=""
DO_COMMIT=0
DRY_RUN=0

# 跳过二进制文件，避免 sed 破坏内容
BINARY_EXTS="png jpg jpeg gif ico bmp jar zip gz tar pdf xlsx xls docx doc class so dll"

usage() {
    sed -n '3,30p' "$0" | sed 's/^# \{0,1\}//'
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --from)       FROM_PKG="$2";   shift 2 ;;
        --to)         TO_PKG="$2";     shift 2 ;;
        --brand-from) FROM_BRAND="$2"; shift 2 ;;
        --brand-to)   TO_BRAND="$2";   shift 2 ;;
        --branch)     BRANCH="$2";     shift 2 ;;
        --commit)     DO_COMMIT=1;     shift ;;
        --dry-run)    DRY_RUN=1;       shift ;;
        -h|--help)    usage ;;
        *) echo "未知参数: $1" >&2; usage ;;
    esac
done

run() {
    if [[ "$DRY_RUN" -eq 1 ]]; then
        printf '[dry-run] %s\n' "$*"
    else
        "$@"
    fi
}

log() { printf '\n==> %s\n' "$*"; }

git rev-parse --git-dir >/dev/null 2>&1 || { echo "错误：当前目录不是 git 仓库" >&2; exit 1; }
cd "$(git rev-parse --show-toplevel)"

if [[ -n "$(git status --porcelain)" ]]; then
    echo "错误：工作区存在未提交内容，请先提交或暂存后再执行" >&2
    exit 1
fi

FROM_PATH="${FROM_PKG//./\/}"      # org.wymix   -> org/wymix
TO_PATH="${TO_PKG//./\/}"          # com.example -> com/example
FROM_PKG_RE=$(printf '%s' "$FROM_PKG" | sed 's/[.[\*^$]/\\&/g')   # sed 正则转义

log "迁移参数：${FROM_PKG} -> ${TO_PKG} ；品牌 ${FROM_BRAND} -> ${TO_BRAND}"

# ---------------------------------------------------------------- 1. 准备分支
if [[ -n "$BRANCH" ]]; then
    if git show-ref --verify --quiet "refs/heads/${BRANCH}"; then
        log "切换到已存在分支 ${BRANCH}"
        run git checkout "$BRANCH"
    else
        log "创建分支 ${BRANCH}"
        run git checkout -b "$BRANCH"
    fi
fi

# ------------------------------------------------------- 2. 文本全量替换
log "替换受版本控制的文本文件内容"
# 跳过脚本自身，避免把脚本里的参数默认值也替换掉
SELF_REL=$(realpath --relative-to="$(pwd)" "$0" 2>/dev/null || basename "$0")
count=0
while IFS= read -r -d '' f; do
    [[ -f "$f" ]] || continue
    [[ "$f" == "$SELF_REL" ]] && continue
    ext="${f##*.}"
    for e in $BINARY_EXTS; do [[ "$ext" == "$e" ]] && continue 2; done
    count=$((count + 1))
    run sed -i \
        -e "s|${FROM_PKG_RE}|${TO_PKG}|g" \
        -e "s|${FROM_BRAND^^}|${TO_BRAND^^}|g" \
        -e "s|${FROM_BRAND^}|${TO_BRAND^}|g" \
        -e "s|${FROM_BRAND}|${TO_BRAND}|g" \
        "$f"
done < <(git ls-files -z)
log "已处理 ${count} 个文本文件"

# --------------------------------------------- 3. 包名目录重命名（保留 git 历史）
log "重命名包目录 ${FROM_PATH} -> ${TO_PATH}"
while IFS= read -r -d '' d; do
    src_root=$(dirname "$(dirname "$d")")     # .../src/main/java
    target="${src_root}/${TO_PATH}"
    if [[ -e "$target" ]]; then
        # 目标已存在（例如示例工程同前缀），逐项合并
        while IFS= read -r -d '' c; do
            run git mv "$c" "${target}/"
        done < <(find "$d" -mindepth 1 -maxdepth 1 -print0)
        [[ "$DRY_RUN" -eq 1 ]] || rmdir "$d" 2>/dev/null || true
    else
        mkdir -p "$(dirname "$target")"
        run git mv "$d" "$target"
    fi
done < <(find . -type d -path "*/${FROM_PATH}" -not -path "./.git/*" -print0 | sort -z)

# ------------------------------------------------------------- 4. 清理空目录
log "清理遗留空目录"
while IFS= read -r -d '' d; do
    [[ "$d" == "./.git"* ]] && continue
    run rmdir "$d"
done < <(find . -type d -empty -not -path "./.git/*" -print0 2>/dev/null || true)

# ------------------------------------------------------------------ 5. 校验
log "校验残留标识"
residue=$(grep -rIi "$FROM_BRAND" . --exclude-dir=.git 2>/dev/null | wc -l || true)
if [[ "$residue" -ne 0 ]]; then
    echo "警告：仍有 ${residue} 处 ${FROM_BRAND} 残留：" >&2
    grep -rIin "$FROM_BRAND" . --exclude-dir=.git 2>/dev/null | head -20 >&2 || true
else
    echo "文本残留：0"
fi

log "校验 Java package 声明与目录一致性"
bad=0
while IFS= read -r -d '' f; do
    pkg=$(grep -m1 '^package ' "$f" | sed -E 's/^package[[:space:]]+//; s/;.*$//' || true)
    [[ -z "$pkg" ]] && continue
    exp="${pkg//./\/}"
    dir=$(dirname "$f")
    [[ "$dir" == *"/${exp}" ]] || { echo "不一致：$f (package=$pkg)"; bad=1; }
done < <(git ls-files -z '*.java')
[[ "$bad" -eq 0 ]] && echo "包声明与目录：一致"

# ------------------------------------------------------------------ 6. 暂存
log "暂存变更"
run git add -A
changed=$(git diff --cached --numstat | wc -l)
echo "已暂存文件数：${changed}"

if [[ "$DO_COMMIT" -eq 1 && "$changed" -gt 0 ]]; then
    run git commit -q -m "chore: ${FROM_PKG} -> ${TO_PKG} 全量迁移（groupId/包名/目录/配置前缀/文档）"
    log "已提交：$(git rev-parse --short HEAD)"
else
    log "未自动提交，请 review 后执行：git commit -m 'chore: ${FROM_PKG} -> ${TO_PKG} 全量迁移'"
fi
