#!/usr/bin/env bash
set -euo pipefail

# Finflow Senior Release Automation Script
# Usage: ./scripts/release.sh

echo "=========================================="
echo "🚀 FINFLOW — PLAY STORE RELEASE PIPELINE"
echo "=========================================="

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Step 1: Run Unit Tests
echo ""
echo "▶ Passo 1/3: A executar testes unitários..."
./gradlew :app:testDebugUnitTest --quiet
echo "✅ Testes unitários passaram a 100%."

# Step 2: Build Signed App Bundle
echo ""
echo "▶ Passo 2/3: A compilar o Android App Bundle assinado (Release)..."
./gradlew :app:bundleRelease --quiet
echo "✅ App Bundle compilado com sucesso."

# Step 3: Verify Signature
AAB_PATH="app/build/outputs/bundle/release/app-release.aab"
if [ ! -f "$AAB_PATH" ]; then
    echo "❌ Erro: Ficheiro $AAB_PATH não encontrado."
    exit 1
fi

echo ""
echo "▶ Passo 3/3: A verificar assinatura criptográfica do Bundle..."
if jarsigner -verify "$AAB_PATH" | grep -q "jar verified."; then
    echo "✅ Assinatura verificada: O Bundle está devidamente assinado."
else
    echo "❌ Aviso: O Bundle não possui uma assinatura válida."
    exit 1
fi

# Summary
SIZE=$(ls -lh "$AAB_PATH" | awk '{print $5}')
SHA256=$(shasum -a 256 "$AAB_PATH" | awk '{print $1}')

echo ""
echo "=========================================="
echo "🎉 PRONTO PARA UPLOAD NO GOOGLE PLAY CONSOLE"
echo "=========================================="
echo "Ficheiro: $AAB_PATH"
echo "Tamanho:  $SIZE"
echo "SHA-256:  $SHA256"
echo ""
echo "Próximo passo: Acede a https://play.google.com/console e carrega este ficheiro."
