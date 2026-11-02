#!/usr/bin/env bash
set -euo pipefail

# Finflow GitHub Release Automation
# Usage: ./scripts/publish-release.sh [version] [optional release notes]
# Example: ./scripts/publish-release.sh 2.2.0 "Initial public release"

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

VERSION="${1:-}"
NOTES="${2:-Official release of Finflow personal finance app for Android.}"

if [ -z "$VERSION" ]; then
    # Detect version from app/build.gradle.kts
    VERSION=$(grep "versionName =" app/build.gradle.kts | head -n 1 | sed 's/[^0-9.]*//g')
fi

TAG="v$VERSION"

echo "=========================================="
echo "🚀 FINFLOW — GITHUB RELEASE PUBLISHER"
echo "Tag: $TAG"
echo "=========================================="

# Check GitHub CLI
if ! command -v gh &> /dev/null; then
    echo "❌ Erro: GitHub CLI ('gh') não está instalado. Instala com 'brew install gh'."
    exit 1
fi

if ! gh auth status &> /dev/null; then
    echo "❌ Erro: Não estás autenticado no GitHub CLI. Executa 'gh auth login'."
    exit 1
fi

# Step 1: Run local tests & build signed artifacts
echo ""
echo "▶ Passo 1/4: A compilar e verificar artefactos de produção..."
./scripts/release.sh

APK_PATH="app/build/outputs/apk/release/app-release.apk"
AAB_PATH="app/build/outputs/bundle/release/app-release.aab"

if [ ! -f "$APK_PATH" ] || [ ! -f "$AAB_PATH" ]; then
    echo "❌ Erro: Os ficheiros compilados não foram encontrados."
    exit 1
fi

# Prepare staged release artifacts with explicit naming
DIST_DIR="dist-release"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

DIST_APK="$DIST_DIR/Finflow-$TAG.apk"
DIST_AAB="$DIST_DIR/Finflow-$TAG.aab"
cp "$APK_PATH" "$DIST_APK"
cp "$AAB_PATH" "$DIST_AAB"

cd "$DIST_DIR"
shasum -a 256 "Finflow-$TAG.apk" "Finflow-$TAG.aab" > SHA256SUMS.txt
cd "$PROJECT_ROOT"

# Step 2: Tag Git
echo ""
echo "▶ Passo 2/4: A criar tag Git $TAG..."
if git rev-parse "$TAG" >/dev/null 2>&1; then
    echo "ℹ️ A tag $TAG já existe localmente."
else
    git tag -a "$TAG" -m "Finflow release $TAG"
fi

# Step 3: Push commits and tags to remote
echo ""
echo "▶ Passo 3/4: A enviar commits e tags para o GitHub..."
git push origin HEAD --tags

# Step 4: Create GitHub Release via gh CLI
echo ""
echo "▶ Passo 4/4: A publicar GitHub Release com download direto..."
RELEASE_NOTES=$(cat <<EOF
## Finflow $TAG

### Highlights
- 🔒 **100% Offline-First:** Zero cloud tracking, no third-party servers. All data encrypted on-device with AES-256 (SQLCipher).
- 🌐 **Global Multi-Language:** Built-in instant toggle between English (default) and Portuguese in Account.
- 📊 **4-Dimensional Transactions:** Category, Entity/Context, Payment Method, and Fixed vs. Variable classification.
- ⚡ **Smart Automation:** Fast prefill engine and recurring monthly transaction generator.
- 📈 **Wealth & Portfolio Analytics:** Track emergency funds, investments, PPR tax optimization, and monthly rhythms.

### Download & Install
- **Direct APK (Sideload / Install directly on Android):** \`Finflow-$TAG.apk\`
- **Google Play Bundle:** \`Finflow-$TAG.aab\`

### SHA-256 Checksums
\`\`\`
$(cat "$DIST_DIR/SHA256SUMS.txt")
\`\`\`
EOF
)

if gh release view "$TAG" >/dev/null 2>&1; then
    echo "ℹ️ A atualizar release existente $TAG..."
    gh release upload "$TAG" "$DIST_APK" "$DIST_AAB" "$DIST_DIR/SHA256SUMS.txt" --clobber
else
    gh release create "$TAG" "$DIST_APK" "$DIST_AAB" "$DIST_DIR/SHA256SUMS.txt" \
        --title "Finflow $TAG" \
        --notes "$RELEASE_NOTES"
fi

rm -rf "$DIST_DIR"

echo ""
echo "=========================================="
echo "🎉 RELEASE PUBLICADA COM SUCESSO!"
echo "URL da Release: $(gh release view "$TAG" --json url -q .url)"
echo "=========================================="
