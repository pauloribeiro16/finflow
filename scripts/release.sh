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
echo "▶ Passo 1/4: A executar testes unitários..."
./gradlew :app:testProdDebugUnitTest --quiet
echo "✅ Testes unitários passaram a 100%."

# Step 2: Build Signed App Bundle & APK
echo ""
echo "▶ Passo 2/4: A compilar o Android App Bundle assinado (Release)..."
./gradlew :app:bundleProdRelease --quiet
echo "✅ App Bundle compilado com sucesso."

echo ""
echo "▶ Passo 3/4: A compilar o APK assinado para distribuição direta..."
./gradlew :app:assembleProdRelease --quiet
echo "✅ APK compilado com sucesso."

# Step 3: Verify Signatures
AAB_PATH=$(find app/build/outputs/bundle -name "*prod*release*.aab" -o -name "*release*.aab" | head -n 1)
APK_PATH=$(find app/build/outputs/apk -name "*prod*release*.apk" -o -name "*release*.apk" | head -n 1)

if [ -z "$AAB_PATH" ] || [ ! -f "$AAB_PATH" ]; then
    echo "❌ Erro: Ficheiro AAB não encontrado."
    exit 1
fi

if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
    echo "❌ Erro: Ficheiro APK não encontrado."
    exit 1
fi

echo ""
echo "▶ Passo 4/4: A verificar assinaturas criptográficas..."
if jarsigner -verify "$AAB_PATH" | grep -q "jar verified."; then
    echo "✅ Assinatura AAB verificada: O Bundle está devidamente assinado."
else
    echo "❌ Aviso: O Bundle não possui uma assinatura válida."
    exit 1
fi

APKSIGNER=$(find "$ANDROID_HOME/build-tools" -name apksigner 2>/dev/null | sort -V | tail -n 1 || true)
if [ -n "$APKSIGNER" ] && [ -x "$APKSIGNER" ]; then
    if "$APKSIGNER" verify "$APK_PATH"; then
        echo "✅ Assinatura APK verificada com apksigner (v1 + v2 scheme)."
    fi
else
    echo "ℹ️ APK gerado: $APK_PATH"
fi

# Summary
AAB_SIZE=$(ls -lh "$AAB_PATH" | awk '{print $5}')
AAB_SHA256=$(shasum -a 256 "$AAB_PATH" | awk '{print $1}')

APK_SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
APK_SHA256=$(shasum -a 256 "$APK_PATH" | awk '{print $1}')

echo ""
echo "=========================================="
echo "🎉 ARTEFACTOS DE PRODUÇÃO PRONTOS"
echo "=========================================="
echo "Google Play (Bundle): $AAB_PATH"
echo "  Tamanho:  $AAB_SIZE"
echo "  SHA-256:  $AAB_SHA256"
echo ""
echo "GitHub Release (APK): $APK_PATH"
echo "  Tamanho:  $APK_SIZE"
echo "  SHA-256:  $APK_SHA256"
echo "=========================================="
