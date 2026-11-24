#!/bin/bash
# ==============================================================================
# GitHub Upload Script for Linux-and-Android
# ==============================================================================
# This script initializes Git (if needed), prompts for or accepts a GitHub
# Personal Access Token and Repository Name, creates the repo via the GitHub API,
# and pushes the project to GitHub.
# ==============================================================================

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

echo ""
echo "============================================================"
echo "          🐧 Linux-and-Android GitHub Uploader              "
echo "============================================================"
echo ""

# 1. Get Personal Access Token
GIT_TOKEN="${GITHUB_TOKEN:-${1:-}}"
if [ -z "$GIT_TOKEN" ]; then
    echo "A GitHub Personal Access Token (PAT) with 'repo' scope is required."
    echo "You can generate one at: https://github.com/settings/tokens"
    echo ""
    read -r -s -p "Enter your GitHub Personal Access Token: " GIT_TOKEN
    echo ""
fi

if [ -z "$GIT_TOKEN" ]; then
    print_error "GitHub token cannot be empty."
    exit 1
fi

# Validate token and retrieve authenticated username
print_info "Verifying GitHub token..."
USER_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $GIT_TOKEN" \
    -H "Accept: application/vnd.github.v3+json" \
    "https://api.github.com/user")

HTTP_STATUS=$(echo "$USER_RESPONSE" | tail -n 1)
USER_JSON=$(echo "$USER_RESPONSE" | sed '$d')

if [ "$HTTP_STATUS" != "200" ]; then
    print_error "Invalid GitHub token or authentication failed (HTTP $HTTP_STATUS)."
    exit 1
fi

GITHUB_USERNAME=$(echo "$USER_JSON" | grep -o '"login": *"[^"]*"' | head -n 1 | cut -d '"' -f 4)
if [ -z "$GITHUB_USERNAME" ]; then
    print_error "Could not determine GitHub username from token."
    exit 1
fi

print_success "Authenticated as GitHub user: $GITHUB_USERNAME"

# 2. Get Repository Name
REPO_NAME="${2:-}"
if [ -z "$REPO_NAME" ]; then
    read -r -p "Enter GitHub repository name [linux-andriod]: " REPO_NAME
    REPO_NAME="${REPO_NAME:-linux-andriod}"
fi

# Sanitize repository name (spaces to hyphens, lowercase)
REPO_NAME=$(echo "$REPO_NAME" | tr '[:upper:]' '[:lower:]' | tr ' ' '-')

print_info "Target repository: $GITHUB_USERNAME/$REPO_NAME"

# 3. Create .gitignore if not present
if [ ! -f "$PROJECT_DIR/.gitignore" ]; then
    print_info "Generating .gitignore..."
    cat > "$PROJECT_DIR/.gitignore" << 'EOF'
# Android build & cache files
*.apk
*.aab
*.dex
*.class
bin/
gen/
out/
build/
.gradle/
local.properties
Captures/
.externalNativeBuild/
.cxx/

# IDE files
.idea/
.vscode/
*.iml
*.iws
*.ipr
.DS_Store

# Logs
*.log

# Proguard
proguard/
EOF
    print_success ".gitignore created"
fi

# 4. Initialize Git repository if needed
if [ ! -d "$PROJECT_DIR/.git" ]; then
    print_info "Initializing git repository..."
    git init -b main
else
    print_info "Git repository already exists."
    # Ensure branch is main
    git branch -M main 2>/dev/null || true
fi

# Configure local git user if not set
if ! git config user.name >/dev/null 2>&1; then
    git config user.name "$GITHUB_USERNAME"
fi
if ! git config user.email >/dev/null 2>&1; then
    git config user.email "$GITHUB_USERNAME@users.noreply.github.com"
fi

# 5. Stage and commit files
print_info "Staging project files..."
git add -A

if git diff --staged --quiet; then
    print_info "No changes to commit (already up to date)."
else
    print_info "Committing changes..."
    git commit -m "Initial commit: Linux on Android application

- Multi-distro Linux installer wizard (Ubuntu, Debian, Arch, Fedora, Alpine, etc.)
- Architecture selection (Native ARM64 / Emulated x86_64)
- Desktop environment selection (GNOME, KDE Plasma, XFCE, LXQt, etc.)
- Turmux and Turmux-X11 bootstrap support
- Terminal session management and document reader
- GitHub Actions CI workflow for debug APK builds"
fi

# 6. Create repository on GitHub via API if it does not exist
print_info "Checking if repository '$GITHUB_USERNAME/$REPO_NAME' exists on GitHub..."
CHECK_REPO=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $GIT_TOKEN" \
    -H "Accept: application/vnd.github.v3+json" \
    "https://api.github.com/repos/$GITHUB_USERNAME/$REPO_NAME")

if [ "$CHECK_REPO" = "404" ]; then
    print_info "Creating new repository '$REPO_NAME' on GitHub..."
    CREATE_PAYLOAD=$(cat <<EOF
{
  "name": "$REPO_NAME",
  "description": "Linux on Android - Run Linux distributions with GUI desktops on Android",
  "private": false,
  "has_issues": true,
  "has_wiki": true
}
EOF
)
    CREATE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST \
        -H "Authorization: Bearer $GIT_TOKEN" \
        -H "Accept: application/vnd.github.v3+json" \
        -d "$CREATE_PAYLOAD" \
        "https://api.github.com/user/repos")

    if [ "$CREATE_STATUS" = "201" ]; then
        print_success "Repository created successfully on GitHub!"
    else
        print_error "Failed to create repository (HTTP $CREATE_STATUS)."
        exit 1
    fi
else
    print_info "Repository already exists on GitHub."
fi

# 7. Configure remote and push
print_info "Configuring remote origin..."
REMOTE_URL="https://x-access-token:${GIT_TOKEN}@github.com/${GITHUB_USERNAME}/${REPO_NAME}.git"

if git remote | grep -q "^origin$"; then
    git remote set-url origin "$REMOTE_URL"
else
    git remote add origin "$REMOTE_URL"
fi

print_info "Pushing main branch to GitHub..."
git push -u origin main --force

print_success ""
print_success "============================================================"
print_success "  All done! Project successfully uploaded to GitHub!       "
print_success "============================================================"
echo ""
echo "Repository URL: https://github.com/$GITHUB_USERNAME/$REPO_NAME"
echo ""
