# DualStreamPlayer Pro

Aplicativo Android nativo que reproduz até **4 streams HLS (.m3u8) simultaneamente** com 8 modos de layout dinâmicos, controle individual de volume e sistema de foco de áudio.

---

## Requisitos

| Item | Versão mínima |
|------|---------------|
| Android Studio | Hedgehog (2023.1.1) ou superior |
| JDK | 17 |
| Android SDK | API 26 (Android 8.0) |
| Gradle | 8.6 |
| AGP | 8.3.2 |
| Kotlin | 1.9.23 |

---

## Como abrir no Android Studio

1. Extraia o ZIP `DualStreamPlayerPro.zip`
2. Abra o Android Studio → **File › Open**
3. Selecione a pasta `DualStreamPlayerPro/`
4. Aguarde o sync do Gradle (baixa as dependências automaticamente)
5. Conecte um dispositivo Android ou inicie um emulador
6. Pressione **Run ▶**

---

## Estrutura do projeto

```
DualStreamPlayerPro/
├── app/
│   ├── src/main/
│   │   ├── java/com/dualstreamplayer/pro/
│   │   │   ├── MainActivity.kt         ← Activity principal + UI
│   │   │   ├── MultiPlayerController.kt← Gerencia os 4 ExoPlayers
│   │   │   ├── LayoutManager.kt        ← 8 modos de layout + animações
│   │   │   └── PlayerHolder.kt         ← Modelo de dados de cada player
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml   ← Layout principal
│   │   │   │   └── dialog_settings.xml ← Diálogo de configurações
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── drawable/               ← Ícones vetoriais
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── settings.gradle.kts
└── build.gradle.kts
```

---

## Funcionalidades

### 8 Modos de Layout
| # | Modo | Players visíveis |
|---|------|-----------------|
| 1 | Tela única | 1 |
| 2 | Divisão horizontal | 2 |
| 3 | Divisão vertical | 2 |
| 4 | Picture in Picture | 2 |
| 5 | Principal + 2 miniaturas | 3 |
| 6 | Principal + 3 miniaturas | 4 |
| 7 | Grade 2×2 | 4 |
| 8 | Layout em T | 3 |

### Sistema de Áudio
- **Múltiplos áudios:** todos os players tocam ao mesmo tempo
- **Foco PRIMARY:** stream com volume 100 %
- **Foco SECONDARY:** stream com volume 30 % (ducking)
- **Botão Trocar Áudio:** alterna o PRIMARY ciclicamente (1→2→3→4→1)

### Configurações (sem reiniciar o app)
- URLs das 4 streams (.m3u8)
- Volume individual de cada player (slider 0–100 %)
- Player principal (foco de áudio)
- Múltiplos áudios simultâneos (on/off)
- Reconexão automática em erros
- Buffer mínimo (ms)
- Timeout de rede (ms)

---

## Streams de teste incluídas
As quatro streams de demonstração carregadas por padrão são públicas e gratuitas.
Substitua-as no diálogo **Configurações** pelo botão ⚙ na barra superior.

---

## Picture-in-Picture
- Pressione o botão Home: o app entra em PiP automaticamente (Android 8+)
- A reprodução continua ininterrupta no mini-player

## Tela Cheia
- Pressione o botão ⛶ na barra superior para ocultar/mostrar as barras do sistema

---

## Dependências principais
```
androidx.media3:media3-exoplayer:1.3.1
androidx.media3:media3-exoplayer-hls:1.3.1
androidx.media3:media3-ui:1.3.1
androidx.media3:media3-common:1.3.1
com.google.android.material:material:1.12.0
androidx.constraintlayout:constraintlayout:2.1.4
```
